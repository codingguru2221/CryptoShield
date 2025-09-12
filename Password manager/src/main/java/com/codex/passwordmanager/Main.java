package com.codex.passwordmanager;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static Process pythonServerProcess;
    public static void main(String[] args) {
        System.out.println("=== Password Manager Starting ===");
        System.out.println("Checking for pendrive detection...");
        
        // Use enhanced detection method
        var allPendrives = PendriveDetector.getAllPendrivePaths();
        boolean detected = !allPendrives.isEmpty();
        
        if (!detected) {
            System.out.println("No pendrive detected. Showing error dialog...");
            javax.swing.JOptionPane.showMessageDialog(null, 
                "No pendrive detected!\n\n" +
                "Please ensure:\n" +
                "1. A USB pendrive is connected to your computer\n" +
                "2. The pendrive is properly recognized by Windows\n" +
                "3. The pendrive has a drive letter assigned\n\n" +
                "Check the console output for detailed detection information.", 
                "Pendrive Not Detected", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else {
            System.out.println("Pendrive(s) detected successfully! Launching UI...");
        }
        
        // Start Python server in background
        startPythonServer(allPendrives.get(0));
        // Start heartbeat to notify local server that Java app is running
        startHeartbeat();
        
        // Launch the Swing UI for password manager
        SwingUtilities.invokeLater(() -> {
            new PasswordManagerUI().setVisible(true);
        });
    }
    
    private static void startPythonServer(String pendrivePath) {
        try {
            System.out.println("Starting Python server for browser extension...");
            
            // Get the current directory where the Python script is located
            String currentDir = System.getProperty("user.dir");
            String pythonScript = currentDir + "\\password_server.py";
            
            // Start Python server in background
            ProcessBuilder pb = new ProcessBuilder(
                "python", pythonScript, pendrivePath, "5000"
            );
            pb.directory(new java.io.File(currentDir));
            pythonServerProcess = pb.start();
            
            // Ensure Python server stops when Java app exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (pythonServerProcess != null && pythonServerProcess.isAlive()) {
                        System.out.println("Stopping Python server...");
                        pythonServerProcess.destroy();
                        pythonServerProcess.waitFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (pythonServerProcess.isAlive()) {
                            pythonServerProcess.destroyForcibly();
                        }
                    }
                } catch (InterruptedException ignored) { }
            }));
            
            System.out.println("Python server started on http://localhost:5000");
            System.out.println("Browser extension can now connect to the server");
            
        } catch (IOException e) {
            System.err.println("Failed to start Python server: " + e.getMessage());
            System.err.println("Please ensure Python is installed and Flask is available");
        }
    }

    private static ScheduledExecutorService heartbeatExecutor;

    private static void startHeartbeat() {
        // Send POST /api/heartbeat every 2 seconds to indicate Java is running
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-thread");
            t.setDaemon(true);
            return t;
        });

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                URL url = new URL("http://127.0.0.1:5000/api/heartbeat");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(800);
                conn.setReadTimeout(800);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {
                // Server may not be up yet; ignore
            }
        }, 0, 2, TimeUnit.SECONDS);

        // Ensure executor stops on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdownNow();
            }
        }));
    }
}