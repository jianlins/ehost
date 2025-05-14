package workSpace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Test case for the ProjectLock class. 
 * Tests the functionality of the heartbeat mechanism and lock management.
 */
public class ProjectLockTest {

    private File tempDir;
    private ProjectLock projectLock;
    private static final DateTimeFormatter TEST_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Setup method that runs before each test.
     * Creates a temporary directory for testing.
     */
    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("projectLockTest").toFile();
        // Ensure the directory exists
        tempDir.mkdirs();
        // Create a new ProjectLock instance
        projectLock = new ProjectLock(tempDir);
    }
    
    /**
     * Tear down method that runs after each test.
     * Deletes the temporary directory.
     */
    @AfterEach
    public void tearDown() {
        // Release lock if it exists
        projectLock.releaseLock();
        
        // Delete the lock file if it still exists
        File lockFile = new File(tempDir, ".lock");
        if (lockFile.exists()) {
            lockFile.delete();
        }
        
        // Delete the temporary directory
        tempDir.delete();
    }
    
    /**
     * Test that a lock can be acquired when none exists.
     */
    @Test
    public void testAcquireLock() {
        // Acquire the lock
        boolean result = projectLock.acquireLock();
        
        // Verify the lock was acquired
        assertTrue(result, "Should be able to acquire lock when none exists");
        
        // Verify the lock file exists
        File lockFile = new File(tempDir, ".lock");
        assertTrue(lockFile.exists(), "Lock file should exist after acquiring lock");
    }
    
    /**
     * Test that a lock cannot be acquired when one already exists.
     */
    @Test
    public void testAcquireLockWhenAlreadyLocked() {
        // First acquisition should succeed
        boolean firstResult = projectLock.acquireLock();
        assertTrue(firstResult, "First lock acquisition should succeed");
        
        // Create a new ProjectLock instance to simulate another process
        ProjectLock secondLock = new ProjectLock(tempDir);
        
        // Second acquisition should fail
        boolean secondResult = secondLock.acquireLock();
        assertFalse(secondResult, "Second lock acquisition should fail");
    }
    
    /**
     * Test that a lock is properly released.
     */
    @Test
    public void testReleaseLock() {
        // Acquire the lock
        projectLock.acquireLock();
        
        // Verify the lock file exists
        File lockFile = new File(tempDir, ".lock");
        assertTrue(lockFile.exists(), "Lock file should exist after acquiring lock");
        
        // Release the lock
        projectLock.releaseLock();
        
        // Verify the lock file no longer exists
        assertFalse(lockFile.exists(), "Lock file should not exist after releasing lock");
    }
    
    /**
     * Test that a stale lock can be detected.
     */
    @Test
    public void testStaleDetection() throws IOException, InterruptedException {
        // Create a lock file with an old timestamp
        File lockFile = new File(tempDir, ".lock");
        String userName = System.getProperty("user.name");
        String machineName = java.net.InetAddress.getLocalHost().getHostName();
          // Create a timestamp from 6 minutes ago (beyond the 5-minute stale threshold)
        LocalDateTime oldTime = LocalDateTime.now().minus(6, ChronoUnit.MINUTES);
        // Use DateTimeFormatter directly since DATETIME_FORMAT is private in ProjectLock
        String formattedTime = oldTime.format(TEST_DATETIME_FORMAT);
        // Write the old timestamp to the lock file
        Files.write(
            lockFile.toPath(), 
            (userName + " on " + machineName + " at " + formattedTime).getBytes()
        );
        
        // Create a new ProjectLock instance
        ProjectLock newLock = new ProjectLock(tempDir);
          // Check if the lock is detected as stale (timestamp is older than 5-minute threshold)
        assertTrue(newLock.isLockStale(), "Lock should be detected as stale");
        
        // Attempt to acquire the lock (should succeed due to stale detection)
        boolean result = newLock.acquireLock();
        assertTrue(result, "Should be able to acquire a stale lock");
    }
    
    /**
     * Test that the heartbeat mechanism properly updates the timestamp.
     */    @Test
    @Timeout(value = 150, unit = TimeUnit.SECONDS)
    public void testHeartbeatUpdatesTimestamp() throws IOException, InterruptedException {
        // Acquire the lock
        projectLock.acquireLock();
        
        // Get the initial heartbeat time
        LocalDateTime initialHeartbeat = projectLock.getLastHeartbeatTime();
        
        // Wait for the heartbeat to update (longer than the heartbeat interval)
        Thread.sleep(130000); // 130 seconds (slightly longer than the 2-minute heartbeat interval)
        
        // Create a new ProjectLock instance to read the updated file
        ProjectLock newLock = new ProjectLock(tempDir);
        
        // Get the updated heartbeat time
        LocalDateTime updatedHeartbeat = newLock.getLastHeartbeatTime();
        
        // Verify the heartbeat was updated
        assertTrue(updatedHeartbeat.isAfter(initialHeartbeat),
            "Heartbeat timestamp should be updated");
    }
    
    /**
     * Test that manually updating the heartbeat works.
     */
    @Test
    public void testManualHeartbeatUpdate() throws InterruptedException {
        // Acquire the lock
        projectLock.acquireLock();
        
        // Get the initial heartbeat time
        LocalDateTime initialHeartbeat = projectLock.getLastHeartbeatTime();
        
        // Wait a bit
        Thread.sleep(1000);
        
        // Manually update the heartbeat
        boolean result = projectLock.updateHeartbeatNow();
        
        // Verify the update succeeded
        assertTrue(result, "Manual heartbeat update should succeed");
        
        // Get the updated heartbeat time
        LocalDateTime updatedHeartbeat = projectLock.getLastHeartbeatTime();
        
        // Verify the heartbeat was updated
        assertTrue(updatedHeartbeat.isAfter(initialHeartbeat),
            "Heartbeat timestamp should be updated");
    }
}
