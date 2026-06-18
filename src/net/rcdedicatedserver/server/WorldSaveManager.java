package net.rcdedicatedserver.server;

import java.io.*;

public class WorldSaveManager {
    private File worldFile;
    private World world;
    private long lastSaveTime;
    private long autoSaveInterval = 30000;

    public WorldSaveManager(World world, String worldName) {
        this.world = world;
        this.worldFile = new File(worldName + ".rc");
        this.lastSaveTime = System.currentTimeMillis();
    }

    public boolean loadWorld() {
        if (!worldFile.exists()) {
        	Logger.log("World file not found. Creating new world...");
            return false;
        }
        try (FileInputStream fis = new FileInputStream(worldFile)) {
            byte[] blocks = world.getBlocks();
            int bytesRead = fis.read(blocks);
            Logger.log("World loaded: " + bytesRead + " bytes");
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load world: " + e.getMessage());
            return false;
        }
    }

    public void saveWorld() {
        try {
            if (worldFile.exists()) {
                File backup = new File(worldFile.getName() + ".backup");
                worldFile.renameTo(backup);
            }
            try (FileOutputStream fos = new FileOutputStream(worldFile)) {
                fos.write(world.getBlocks());
            }
            File backup = new File(worldFile.getName() + ".backup");
            if (backup.exists()) backup.delete();
            lastSaveTime = System.currentTimeMillis();
            Logger.log("World saved: " + worldFile.getName());
        } catch (IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
            File backup = new File(worldFile.getName() + ".backup");
            if (backup.exists() && !worldFile.exists()) backup.renameTo(worldFile);
        }
    }

    public void tick() {
        if (System.currentTimeMillis() - lastSaveTime >= autoSaveInterval) {
            saveWorld();
        }
    }

    public void saveOnShutdown() {
    	Logger.log("Saving world before shutdown...");
        saveWorld();
    }
}