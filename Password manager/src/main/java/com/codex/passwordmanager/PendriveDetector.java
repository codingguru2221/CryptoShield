package com.codex.passwordmanager;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

public class PendriveDetector {
    // Name of the hidden authorization token file that must exist on the legit USB
    public static final String AUTH_TOKEN_FILENAME = ".cryptoshield.key";
    // Alternate non-hidden filename accepted as well (helps when Windows adds extensions)
    public static final String AUTH_TOKEN_ALT_FILENAME = "cryptoshield.key";

    /**
     * Uses Windows WMIC to detect USB pendrives (drivetype=2).
     * Returns a list of drive letters for detected pendrives.
     */
    public static List<String> getUsbPendriveLetters() {
        List<String> pendrives = new ArrayList<>();
        try {
            System.out.println("Executing WMIC command to detect USB drives...");
            Process process = Runtime.getRuntime().exec(
                new String[] {"cmd.exe", "/c", "wmic logicaldisk where drivetype=2 get deviceid"}
            );
            
            // Wait for process to complete
            int exitCode = process.waitFor();
            System.out.println("WMIC command exit code: " + exitCode);
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            java.io.BufferedReader errorReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream())
            );
            
            String line;
            System.out.println("WMIC output:");
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
                line = line.trim();
                if (line.matches("[A-Z]:")) {
                    pendrives.add(line + "\\");
                    System.out.println("Found pendrive: " + line);
                }
            }
            
            // Check for errors
            System.out.println("WMIC errors:");
            while ((line = errorReader.readLine()) != null) {
                System.out.println("  ERROR: " + line);
            }
            
            reader.close();
            errorReader.close();
        } catch (Exception e) {
            System.err.println("Exception in getUsbPendriveLetters: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Total pendrives found: " + pendrives.size());
        return pendrives;
    }
    /**
     * Returns a list of removable drives (pendrives) currently connected to the system.
     */
    public static List<File> getRemovableDrives() {
        List<File> removableDrives = new ArrayList<>();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File[] roots = File.listRoots();
        
        System.out.println("Checking all root drives for removable devices...");
        System.out.println("Total root drives found: " + roots.length);
        
        for (File root : roots) {
            String type = fsv.getSystemTypeDescription(root);
            System.out.println("Drive: " + root.getAbsolutePath() + 
                             ", Type: " + type + 
                             ", Exists: " + root.exists() + 
                             ", CanRead: " + root.canRead());
            
            if (type != null && type.toLowerCase().contains("removable") && root.exists()) {
                removableDrives.add(root);
                System.out.println("Found removable drive: " + root.getAbsolutePath());
            }
        }
        
        System.out.println("Total removable drives found: " + removableDrives.size());
        return removableDrives;
    }
    
    /**
     * Enhanced pendrive detection using multiple methods
     */
    public static List<String> getAllPendrivePaths() {
        List<String> allPendrives = new ArrayList<>();
        
        System.out.println("=== Starting comprehensive pendrive detection ===");
        
        // Method 1: WMIC detection
        System.out.println("\n--- Method 1: WMIC Detection ---");
        List<String> wmicPendrives = getUsbPendriveLetters();
        allPendrives.addAll(wmicPendrives);
        
        // Method 2: FileSystemView detection
        System.out.println("\n--- Method 2: FileSystemView Detection ---");
        List<File> removableDrives = getRemovableDrives();
        for (File drive : removableDrives) {
            String path = drive.getAbsolutePath();
            if (!allPendrives.contains(path)) {
                allPendrives.add(path);
            }
        }
        
        // Method 3: Direct drive enumeration
        System.out.println("\n--- Method 3: Direct Drive Enumeration ---");
        try {
            Process process = Runtime.getRuntime().exec(
                new String[] {"cmd.exe", "/c", "wmic logicaldisk get deviceid,drivetype,volumename"}
            );
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            System.out.println("All drives information:");
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
                if (line.contains("2") && line.matches(".*[A-Z]:.*")) {
                    String[] parts = line.trim().split("\\s+");
                    for (String part : parts) {
                        if (part.matches("[A-Z]:")) {
                            String drivePath = part + "\\";
                            if (!allPendrives.contains(drivePath)) {
                                allPendrives.add(drivePath);
                                System.out.println("Found additional pendrive: " + drivePath);
                            }
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error in direct drive enumeration: " + e.getMessage());
        }
        
        System.out.println("\n=== Final Results ===");
        System.out.println("Total unique pendrives found: " + allPendrives.size());
        for (String pendrive : allPendrives) {
            System.out.println("  Pendrive: " + pendrive);
        }
        
        return allPendrives;
    }

    /**
     * Checks whether the given drive root contains the CryptoShield authorization token file.
     * If the hidden file exists at the root, we consider the drive authorized.
     */
    public static boolean isAuthorizedDrive(File driveRoot) {
        if (driveRoot == null) return false;
        try {
            // Accept either hidden or non-hidden token name
            File tokenHidden = new File(driveRoot, AUTH_TOKEN_FILENAME);
            if (tokenHidden.exists() && tokenHidden.isFile()) return true;
            File tokenAlt = new File(driveRoot, AUTH_TOKEN_ALT_FILENAME);
            if (tokenAlt.exists() && tokenAlt.isFile()) return true;
            // Also accept if a common .txt suffix was accidentally added by editors
            File tokenHiddenTxt = new File(driveRoot, AUTH_TOKEN_FILENAME + ".txt");
            if (tokenHiddenTxt.exists() && tokenHiddenTxt.isFile()) return true;
            File tokenAltTxt = new File(driveRoot, AUTH_TOKEN_ALT_FILENAME + ".txt");
            if (tokenAltTxt.exists() && tokenAltTxt.isFile()) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the paths of pendrives that are authorized (contain the token file).
     */
    public static List<String> getAuthorizedPendrivePaths() {
        List<String> authorized = new ArrayList<>();
        List<String> candidates = getAllPendrivePaths();
        for (String path : candidates) {
            try {
                File root = new File(path);
                if (isAuthorizedDrive(root)) {
                    authorized.add(path);
                }
            } catch (Exception ignored) {}
        }
        return authorized;
    }

    /**
     * Creates the authorization token on the given drive. Tries hidden ".cryptoshield.key" first,
     * then falls back to "cryptoshield.key" if needed. Attempts to set hidden attribute on Windows.
     */
    public static boolean createAuthorizationToken(File driveRoot) {
        if (driveRoot == null || !driveRoot.exists()) return false;
        try {
            // If already authorized, nothing to do
            if (isAuthorizedDrive(driveRoot)) return true;

            // Prefer hidden name
            File tokenHidden = new File(driveRoot, AUTH_TOKEN_FILENAME);
            File tokenAlt = new File(driveRoot, AUTH_TOKEN_ALT_FILENAME);

            File chosen = tokenHidden;
            try {
                if (!tokenHidden.exists()) {
                    try (FileWriter fw = new FileWriter(tokenHidden)) {
                        fw.write("authorized\n");
                    }
                }
            } catch (Exception e) {
                // fallback to non-hidden name
                chosen = tokenAlt;
                if (!tokenAlt.exists()) {
                    try (FileWriter fw = new FileWriter(tokenAlt)) {
                        fw.write("authorized\n");
                    }
                }
            }

            // Try to mark as hidden on Windows for cleanliness
            try {
                Path p = chosen.toPath();
                DosFileAttributeView view = Files.getFileAttributeView(p, DosFileAttributeView.class);
                if (view != null) {
                    view.setHidden(true);
                }
            } catch (Exception ignore) {}

            return chosen.exists();
        } catch (Exception e) {
            return false;
        }
    }
}

