package net.rcdedicatedserver.server;

import java.util.Arrays;

public class World {
    public final int width;
    public final int height;
    public final int depth;
    private byte[] blocks;

    public World(int w, int h, int d) {
        this.width = w;
        this.height = h;
        this.depth = d;
        this.blocks = new byte[w * h * d];
    }

    public void generate() {
        // Используем ТВОЙ проверенный генератор из клиента
    	net.rcdedicatedserver.server.WorldGenerator gen = new net.rcdedicatedserver.server.WorldGenerator(width, height, depth);
        this.blocks = gen.generateMap();
        System.out.println("World generated: " + width + "x" + height + "x" + depth);
    }
    public int getSurfaceY(int x, int z) {
        for (int y = height - 1; y > 0; y--) {
            if (getBlock(x, y, z) != 0) {
                return y + 1; // На один блок выше поверхности
            }
        }
        return height / 2 + 2; // Запасной вариант
    }
   
    public int getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return 0;
        return blocks[(y * depth + z) * width + x] & 0xFF;
    }

    public void setBlock(int x, int y, int z, int type) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return;
        blocks[(y * depth + z) * width + x] = (byte) type;
    }

    public byte[] getBlocks() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    public int getSpawnX() { return width / 2; }
    public int getSpawnY() { return height / 2 + 2; }
    public int getSpawnZ() { return depth / 2; }
}