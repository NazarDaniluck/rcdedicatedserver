package net.rcdedicatedserver.server;

import java.io.*;
import java.net.Socket;

public class PlayerConnection implements Runnable {
    private ReplaceServer server;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private byte playerId;
    private String playerName;
    private boolean connected;
    private float x, y, z;
    private byte yaw, pitch;

    public PlayerConnection(ReplaceServer server, Socket socket, byte playerId) {
        this.server = server;
        this.socket = socket;
        this.playerId = playerId;
        this.connected = true;
        
        // Ищем поверхность для спавна
        World world = server.getWorld();
        int spawnX = world.getSpawnX();
        int spawnZ = world.getSpawnZ();
        int surfaceY = world.height / 2;
        for (int y = world.height - 1; y > 0; y--) {
            if (world.getBlock(spawnX, y, spawnZ) != 0) {
                surfaceY = y + 2;
                break;
            }
        }
        this.x = spawnX + 0.5f;
        this.y = surfaceY;
        this.z = spawnZ + 0.5f;
    }

    public byte getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            int packetId = input.readUnsignedByte();
            if (packetId == 0x00) {
                int protocolVersion = input.readUnsignedByte();
                playerName = readString64();
                readString64();
                input.readUnsignedByte();

                Logger.log("Player: " + playerName);

                sendServerIdentification();
                sendLevel();
                
                // Спавним самого игрока
                sendSpawnPlayer((byte)0xFF, playerName, x, y, z, yaw, pitch);
                
                // Отправляем новому игроку всех существующих
                for (PlayerConnection other : server.getPlayers()) {
                    if (other != this) {
                        sendSpawnPlayer(other.playerId, other.playerName, other.x, other.y, other.z, other.yaw, other.pitch);
                    }
                }
                
                // Оповещаем всех о новом игроке
                byte[] spawnPacket = buildSpawnPacket(playerId, playerName, x, y, z, yaw, pitch);
                server.broadcastToAllExcept(this, spawnPacket);
                
                server.broadcastMessage("&e" + playerName + " joined");
            }

            while (connected) {
                packetId = input.readUnsignedByte();
                switch (packetId) {
                    case 0x05: handleSetBlock(); break;
                    case 0x08: handlePlayerPosition(); break;
                    case 0x0D: handleChatMessage(); break;
                }
            }
        } catch (IOException e) {
            Logger.log(playerName + " disconnected");
        } finally {
            connected = false;
            try { socket.close(); } catch (IOException e) {}
            server.removePlayer(this);
        }
    }

    private void handleSetBlock() throws IOException {
        int bx = input.readShort();
        int by = input.readShort();
        int bz = input.readShort();
        byte mode = input.readByte();
        byte blockType = input.readByte();
        
        if (mode == 0x00) server.getWorld().setBlock(bx, by, bz, blockType);
        else server.getWorld().setBlock(bx, by, bz, 0);
        
        // Рассылаем всем остальным
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            d.writeByte(0x06);
            d.writeShort(bx); d.writeShort(by); d.writeShort(bz);
            d.writeByte(mode);
            d.writeByte(blockType);
            server.broadcastToAllExcept(this, b.toByteArray());
        } catch (IOException e) {}
    }

    private void handlePlayerPosition() throws IOException {
        input.readByte(); // playerId
        x = readFloat16(); y = readFloat16(); z = readFloat16();
        yaw = input.readByte(); pitch = input.readByte();
        
        // Рассылаем позицию другим
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            d.writeByte(0x08); d.writeByte(playerId);
            writeFloat16(d, x); writeFloat16(d, y); writeFloat16(d, z);
            d.writeByte(yaw); d.writeByte(pitch);
            server.broadcastToAllExcept(this, b.toByteArray());
        } catch (IOException e) {}
    }

    private void handleChatMessage() throws IOException {
        input.readByte();
        String msg = readString64();
        Logger.log("[Chat] " + playerName + ": " + msg);
        server.broadcastMessage(playerName + ": " + msg);
    }

    private void sendServerIdentification() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(b);
        d.writeByte(0x00); d.writeByte(0x07);
        writeString64(d, server.getConfig().serverName);
        writeString64(d, server.getConfig().serverMotd);
        d.writeByte(0x00);
        output.write(b.toByteArray()); output.flush();
    }

    private void sendLevel() throws IOException {
        byte[] blocks = server.getWorld().getBlocks();
        int size = blocks.length;
        int max = 32767;
        int off = 0;
        while (off < size) {
            int chunk = Math.min(max, size - off);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            d.writeByte(0x03); d.writeShort((short)chunk);
            d.write(blocks, off, chunk);
            d.writeByte((byte)((off+chunk)*100/size));
            output.write(b.toByteArray()); output.flush();
            off += chunk;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(b);
        d.writeByte(0x04);
        d.writeShort(server.getWorld().width);
        d.writeShort(server.getWorld().height);
        d.writeShort(server.getWorld().depth);
        output.write(b.toByteArray()); output.flush();
        Logger.log("Level sent to " + playerName);
    }

    private byte[] buildSpawnPacket(byte pid, String name, float x, float y, float z, byte yaw, byte pitch) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(b);
        d.writeByte(0x07); d.writeByte(pid);
        writeString64(d, name);
        writeFloat16(d, x); writeFloat16(d, y); writeFloat16(d, z);
        d.writeByte(yaw); d.writeByte(pitch);
        return b.toByteArray();
    }

    private void sendSpawnPlayer(byte pid, String name, float x, float y, float z, byte yaw, byte pitch) {
        try {
            sendRawPacket(buildSpawnPacket(pid, name, x, y, z, yaw, pitch));
        } catch (IOException e) {}
    }

    public void sendMessage(String msg) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            d.writeByte(0x0C); d.writeByte((byte)0xFF);
            writeString64(d, msg);
            sendRawPacket(b.toByteArray());
        } catch (IOException e) {}
    }

    public void sendRawPacket(byte[] data) throws IOException {
        if (!connected || socket.isClosed()) return;
        synchronized (output) {
            output.write(data); output.flush();
        }
    }

    private void writeString64(DataOutputStream out, String s) throws IOException {
        byte[] data = new byte[64];
        byte[] bytes = s.getBytes("UTF-8");
        int len = Math.min(bytes.length, 64);
        System.arraycopy(bytes, 0, data, 0, len);
        for (int i = len; i < 64; i++) data[i] = 0x20;
        out.write(data);
    }

    private String readString64() throws IOException {
        byte[] data = new byte[64];
        input.readFully(data);
        int end = 63;
        while (end >= 0 && data[end] == 0x20) end--;
        return new String(data, 0, end + 1, "UTF-8").trim();
    }

    private void writeFloat16(DataOutputStream out, float v) throws IOException {
        out.writeInt((int)(v * 32.0f));
    }

    private float readFloat16() throws IOException {
        return input.readInt() / 32.0f;
    }
}