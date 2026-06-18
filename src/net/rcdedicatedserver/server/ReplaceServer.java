package net.rcdedicatedserver.server;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReplaceServer {
    private ServerSocket serverSocket;
    private volatile boolean running;
    private World world;
    private WorldSaveManager saveManager;
    private byte nextId = 0;
    private ServerConfig config;
    private List<PlayerConnection> players = new CopyOnWriteArrayList<>();

    public void start() {
        Logger.init("server.log");
        config = new ServerConfig("server.properties");
        world = new World(config.worldWidth, config.worldHeight, config.worldDepth);
        saveManager = new WorldSaveManager(world, "server_world");

        if (!saveManager.loadWorld()) {
            world.generate();
            saveManager.saveWorld();
        }

        running = true;
        new Thread(() -> {
            while (running) {
                try { Thread.sleep(30000); saveManager.saveWorld(); }
                catch (InterruptedException e) { break; }
            }
        }, "AutoSave").start();

        try {
            serverSocket = new ServerSocket(config.serverPort);
            Logger.log("Server started on port " + config.serverPort);

            while (running) {
                Socket sock = serverSocket.accept();
                Logger.log("Connection from " + sock.getInetAddress());
                PlayerConnection pc = new PlayerConnection(this, sock, nextId++);
                players.add(pc);
                new Thread(pc).start();
            }
        } catch (IOException e) {
            Logger.log("Error: " + e.getMessage());
        } finally {
            running = false;
            saveManager.saveWorld();
            Logger.close();
        }
    }

    public World getWorld() { return world; }
    public ServerConfig getConfig() { return config; }
    public List<PlayerConnection> getPlayers() { return players; }

    public void removePlayer(PlayerConnection pc) {
        players.remove(pc);
        Logger.log("Player removed. Online: " + players.size());
    }

    public void broadcastMessage(String msg) {
        for (PlayerConnection pc : players) {
            pc.sendMessage(msg);
        }
    }
    
    public void broadcastToAllExcept(PlayerConnection sender, byte[] packet) {
        for (PlayerConnection pc : players) {
            if (pc != sender) {
                try { pc.sendRawPacket(packet); } catch (IOException e) {}
            }
        }
    }

    public static void main(String[] args) {
        new ReplaceServer().start();
    }
}