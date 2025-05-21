package workSpace;

/**
 * ProjectLock provides a mechanism to ensure that a project is not simultaneously accessed by multiple instances
 * of the eHOST application. It uses a lock file with heartbeat updates to track active usage and detect stale locks.
 * 
 * The lock file stores:
 * 1. Username of the user who has the project open
 * 2. Machine name where the project is open
 * 3. Timestamp of the last heartbeat update
 * 
 * Locks are considered stale after a configurable period (default 2 minutes) without heartbeat updates,
 * which can happen if an application crashes or is terminated unexpectedly.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ProjectLock {

    private File lockFile;
    private Timer heartbeatTimer;
    private long heartbeatInterval = 120000; // 2 minutes (default)
    private long staleThreshold = 300000; // 5 minutes (default)
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String userName = "NA";
    private String machineName = "NA";
    private LocalDateTime lastHeartbeat;
    
    // Original constructors
    public ProjectLock(String directoryPath) {
        this.init(new File(directoryPath));
    }

    public ProjectLock(File directoryPath) {
        this.init(directoryPath);
    }
    
    // New constructors with timing parameters
    public ProjectLock(String directoryPath, long heartbeatInterval, long staleThreshold) {
        this.heartbeatInterval = heartbeatInterval;
        this.staleThreshold = staleThreshold;
        this.init(new File(directoryPath));
    }
    
    public ProjectLock(File directoryPath, long heartbeatInterval, long staleThreshold) {
        this.heartbeatInterval = heartbeatInterval;
        this.staleThreshold = staleThreshold;
        this.init(directoryPath);
    }
    
    // The rest of your existing code...
    
    
    private void init(File directoryPath) {
        this.lockFile = new File(directoryPath, ".lock");
        this.lastHeartbeat = LocalDateTime.now();
        readLockContent();
    }
    
    private void readLockContent() {
        if (lockFile.exists()) {
            // Lock file exists, read the lock file to see who is using the project
            try {
                String lockContent = new String(Files.readAllBytes(lockFile.toPath()));
                // Check for new format with timestamp
                if (lockContent.contains(" at ")) {
                    // Format: "username on machinename at timestamp"
                    String[] mainParts = lockContent.split(" at ");
                    if (mainParts.length > 1) {
                        // Parse the timestamp
                        try {
                            lastHeartbeat = LocalDateTime.parse(mainParts[1].trim(), DATETIME_FORMAT);
                        } catch (Exception e) {
                            lastHeartbeat = LocalDateTime.now().minus(staleThreshold * 2, ChronoUnit.MILLIS); // Set as stale
                        }
                        
                        // Parse username and machine name
                        String[] userMachineParts = mainParts[0].split(" on ");
                        setUserName(userMachineParts[0]);
                        if (userMachineParts.length > 1) {
                            setMachineName(userMachineParts[1]);
                        }
                    }
                } else {
                    // Old format: "username on machinename"
                    String[] args = lockContent.split(" on ");
                    setUserName(args[0]);
                    if (args.length > 1) {
                        setMachineName(args[1]);
                    }
                    // Set a default timestamp for backward compatibility
                    lastHeartbeat = LocalDateTime.now().minus(staleThreshold / 2, ChronoUnit.MILLIS);
                }
            } catch (IOException e) {
                showMessage("An error occurred: " + e.getMessage());
            }
        }
    }
    
    public boolean acquireLock() {
        if (lockFile.exists()) {
            // Lock file exists, read the lock file to see who is using the project
            readLockContent();
            
            // Check if the lock is stale
            if (isLockStale()) {
                // Lock is stale, we can override it
                if (lockFile.delete()) {
                    return createNewLock();
                }
            }
            
            showMessage("The project is already locked by: " + this.getUserName() + " from machine: " + this.getMachineName() + " \n" +
                    "Last active: " + this.lastHeartbeat.format(DATETIME_FORMAT) + "\n" +
                    "Please ensure that all other eHOST instances that are opening this project are closed before proceeding.\n" +
                    "Failure to do so may result in the loss of annotations from all eHOST instances currently working on this project.\n" +
                    "If this message continues to appear, it may be due to a previous eHOST instance being closed unexpectedly, e.g., due to a system shutdown.\n" +
                    "In such cases, you can manually remove the .lock file in this project's directory to forcibly open the project.");
            return false;
        } else {
            return createNewLock();
        }
    }
    
    private boolean createNewLock() {
        // Create lock file to mark the project as in use
        try (FileWriter writer = new FileWriter(lockFile)) {
            String userName = System.getProperty("user.name");
            String machineName = java.net.InetAddress.getLocalHost().getHostName();
            setMachineName(machineName);
            setUserName(userName);
            lastHeartbeat = LocalDateTime.now();
            String formattedTime = lastHeartbeat.format(DATETIME_FORMAT);
            writer.write(userName + " on " + machineName + " at " + formattedTime);
            
            // Start the heartbeat timer
            startHeartbeat();
            return true;
        } catch (IOException e) {
            showMessage("An error occurred: " + e.getMessage());
            return false;
        }
    }
    
    // Update the isLockStale method to use the instance variable
    public boolean isLockStale() {
        if (!lockFile.exists()) {
            return false; // No lock to be stale
        }
        
        LocalDateTime now = LocalDateTime.now();
        long millisSinceLastHeartbeat = ChronoUnit.MILLIS.between(lastHeartbeat, now);
        return millisSinceLastHeartbeat > this.staleThreshold;
    }
      /**
     * Releases the lock on the project.
     * Stops the heartbeat timer and removes the lock file if it exists and is owned by the current user.
     */
    public synchronized void releaseLock() {
        // Stop the heartbeat timer
        stopHeartbeat();
        
        if (lockFile.exists()) {
            if (!lockedByMe()) {
                showMessage("<html>It seems another user forced to open this project while you are working on it. Close the project will not release the lock.<br/>" +
                        "If you believe no one else is working on the project, you can manually remove the lock file:<br/>"+this.lockFile.getAbsolutePath()+"</html>");
            } else {
                if (!lockFile.delete()) {
                    showMessage("Failed to release the project lock.");
                }
            }
        }
    }    /**
     * Checks if the current user and machine are the owners of the lock
     * @return true if the lock is owned by the current user and machine, false otherwise
     */
    public boolean lockedByMe(){
        String userName = System.getProperty("user.name");
        String machineName = "";
        try {
            machineName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println("Could not determine machine name: " + e.getMessage());
            // Default to the stored machine name to avoid accidental lock breaking
            return false;
        }
        return (userName.equals(this.getUserName()) && machineName.equals(this.getMachineName()));
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, message);
            }
        });

    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    // Update the startHeartbeat method to use the instance variable
    private void startHeartbeat() {
        // Cancel existing timer if it exists
        stopHeartbeat();
        
        // Create new timer and schedule heartbeat task
        heartbeatTimer = new Timer("LockHeartbeat", true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateHeartbeat();
            }
        }, heartbeatInterval, heartbeatInterval);
    }
    
    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }
      private void updateHeartbeat() {
        if (lockFile.exists() && lockedByMe()) {
            try (FileWriter writer = new FileWriter(lockFile)) {
                // Use synchronized to prevent race conditions when updating heartbeat time
                synchronized (this) {
                    lastHeartbeat = LocalDateTime.now();
                    String formattedTime = lastHeartbeat.format(DATETIME_FORMAT);
                    writer.write(userName + " on " + machineName + " at " + formattedTime);
                }
            } catch (IOException e) {
                // Silent fail on background thread
                System.err.println("Failed to update heartbeat: " + e.getMessage());
            }
        } else {
            // Stop heartbeat if lock no longer exists or not owned by us
            stopHeartbeat();
        }
    }
      /**
     * Checks if the lock is stale and alerts the user if needed
     * @return true if the lock is stale, false otherwise
     */
    public boolean checkHeartbeat() {
        if (lastHeartbeat != null && isLockStale()) {
            showMessage("Warning: The project lock is stale. It was last updated at " + DATETIME_FORMAT.format(lastHeartbeat) +
                    " (" + getMillisSinceLastHeartbeat()/1000 + " seconds ago).\n" +
                    "Please ensure that the project is still in use by checking with the current user: " + getUserName() +
                    " on machine: " + getMachineName() + ".\n" +
                    "If you believe no one is using this project, you can manually delete the lock file at:\n" +
                    lockFile.getAbsolutePath());
            return true;
        }
        return false;
    }/**
     * Sets the interval for heartbeat updates
     * @param interval the time in milliseconds between each update
     */
    public void setHeartbeatInterval(long interval) {
        if (interval > 0) {
            this.heartbeatInterval = interval;
            if (heartbeatTimer != null) {
                stopHeartbeat();
                startHeartbeat();
            }
        }
    }

    /**
     * Sets the threshold for considering a lock stale
     * @param threshold the time in milliseconds after which a lock is considered stale
     */
    public void setStaleThreshold(long threshold) {
        if (threshold > 0) {
            this.staleThreshold = threshold;
        }
    }

    /**
     * Returns the last time the heartbeat was updated
     * @return LocalDateTime representing the last heartbeat time
     */
    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeat;
    }
    
    /**
     * Returns the milliseconds since the last heartbeat
     * @return milliseconds since the last heartbeat or -1 if no heartbeat
     */
    public long getMillisSinceLastHeartbeat() {
        if (lastHeartbeat == null) {
            return -1;
        }
        return ChronoUnit.MILLIS.between(lastHeartbeat, LocalDateTime.now());
    }    /**
     * Manually trigger an update to the heartbeat timestamp
     * Useful when significant application activity happens that should reset the timeout
     * @return true if the update was successful, false otherwise
     */
    public synchronized boolean updateHeartbeatNow() {
        if (lockFile.exists() && lockedByMe()) {
            try (FileWriter writer = new FileWriter(lockFile)) {
                lastHeartbeat = LocalDateTime.now();
                String formattedTime = lastHeartbeat.format(DATETIME_FORMAT);
                writer.write(userName + " on " + machineName + " at " + formattedTime);
                return true;
            } catch (IOException e) {
                System.err.println("Failed to update heartbeat: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
}