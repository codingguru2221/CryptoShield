import java.io.*;
import java.util.*;

public class test_pendrive_detection {
    public static void main(String[] args) {
        System.out.println("=== Pendrive Detection Test ===");
        
        // Test 1: Check if WMIC is available
        System.out.println("\n1. Testing WMIC availability...");
        try {
            Process process = Runtime.getRuntime().exec("wmic --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("WMIC version: " + line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("WMIC not available: " + e.getMessage());
        }
        
        // Test 2: List all drives
        System.out.println("\n2. Listing all drives...");
        File[] roots = File.listRoots();
        for (File root : roots) {
            System.out.println("Drive: " + root.getAbsolutePath() + 
                             ", Exists: " + root.exists() + 
                             ", CanRead: " + root.canRead() +
                             ", CanWrite: " + root.canWrite());
        }
        
        // Test 3: WMIC command for all drives
        System.out.println("\n3. WMIC command for all drives...");
        try {
            Process process = Runtime.getRuntime().exec(
                new String[] {"cmd.exe", "/c", "wmic logicaldisk get deviceid,drivetype,volumename,size"}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String line;
            System.out.println("WMIC output:");
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }
            
            System.out.println("WMIC errors:");
            while ((line = errorReader.readLine()) != null) {
                System.out.println("  ERROR: " + line);
            }
            
            reader.close();
            errorReader.close();
        } catch (Exception e) {
            System.err.println("Error running WMIC: " + e.getMessage());
        }
        
        // Test 4: PowerShell alternative
        System.out.println("\n4. PowerShell alternative...");
        try {
            Process process = Runtime.getRuntime().exec(
                new String[] {"powershell.exe", "-Command", "Get-WmiObject -Class Win32_LogicalDisk | Select-Object DeviceID, DriveType, VolumeName, Size"}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("PowerShell: " + line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("PowerShell not available: " + e.getMessage());
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}
