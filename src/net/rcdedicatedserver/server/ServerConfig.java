package net.rcdedicatedserver.server;

import java.io.*;
import java.util.Properties;

public class ServerConfig {
    private Properties props;
    private File configFile;

    public int serverPort = 25565;
    public String serverName = "ReplaceCraft Server";
    public String serverMotd = "Welcome to Classic!";
    public int worldWidth = 256;
    public int worldHeight = 64;
    public int worldDepth = 256;
    public int maxPlayers = 16;
    public boolean onlineMode = false;

    public ServerConfig(String path) {
        configFile = new File(path);
        props = new Properties();
        load();
    }

    public void load() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                serverPort = getInt("server-port", 25565);
                serverName = getString("server-name", "ReplaceCraft Server");
                serverMotd = getString("server-motd", "Welcome to Classic!");
                worldWidth = getInt("world-width", 256);
                worldHeight = getInt("world-height", 64);
                worldDepth = getInt("world-depth", 256);
                maxPlayers = getInt("max-players", 16);
                onlineMode = getBool("online-mode", false);
                System.out.println("Config loaded.");
            } catch (IOException e) {
                System.out.println("Failed to load config, using defaults");
            }
        } else {
            save();
            System.out.println("Default config created.");
        }
    }

    public void save() {
        props.setProperty("server-port", String.valueOf(serverPort));
        props.setProperty("server-name", serverName);
        props.setProperty("server-motd", serverMotd);
        props.setProperty("world-width", String.valueOf(worldWidth));
        props.setProperty("world-height", String.valueOf(worldHeight));
        props.setProperty("world-depth", String.valueOf(worldDepth));
        props.setProperty("max-players", String.valueOf(maxPlayers));
        props.setProperty("online-mode", String.valueOf(onlineMode));
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "ReplaceCraft Server Config");
        } catch (IOException e) {
            System.err.println("Failed to save config");
        }
    }

    private int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private String getString(String key, String def) {
        return props.getProperty(key, def);
    }

    private boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
    }
}