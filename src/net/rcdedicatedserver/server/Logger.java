package net.rcdedicatedserver.server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static PrintWriter logWriter;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void init(String filename) {
        try {
            logWriter = new PrintWriter(new FileWriter(filename, true), true);
            log("=== Server Log Started ===");
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
    }

    public static void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String line = "[" + timestamp + "] " + message;
        System.out.println(message);
        if (logWriter != null) {
            logWriter.println(line);
            logWriter.flush();
        }
    }

    public static void close() {
        log("=== Server Log Closed ===");
        if (logWriter != null) {
            logWriter.close();
        }
    }
}