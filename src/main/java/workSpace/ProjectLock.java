package workSpace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import javax.swing.*;

public class ProjectLock {

    private File lockFile;


    private String userName = "NA";
    private String machineName = "NA";

    public ProjectLock(String directoryPath) {
        this.init(new File(directoryPath));
    }

    public ProjectLock(File directoryPath) {
        this.init(directoryPath);
    }

    private void init(File directoryPath) {
        this.lockFile = new File(directoryPath, ".lock");
        readLockContent();
    }

    private void readLockContent() {
        if (lockFile.exists()) {
            // Lock file exists, read the lock file to see who is using the project
            try {
                String lockContent = new String(Files.readAllBytes(lockFile.toPath()));
                String[] args = lockContent.split(" on ");
                setUserName(args[0]);
                if (args.length > 1) {
                    setMachineName(args[1]);
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
            showMessage("The project is already locked by: " + this.getUserName() + " from machine: " + this.getMachineName() + " \n" +
                    "Please ensure that all other eHOST instances that are opening this project are closed before proceeding.\n" +
                    "Failure to do so may result in the loss of annotations from all eHOST instances currently working on this project.\n" +
                    "If this message continues to appear, it may be due to a previous eHOST instance being closed unexpectedly, e.g., due to a system shutdown.\n" +
                    "In such cases, you can manually remove the .lock file in this project's directory to forcibly open the project.");
            return false;
        } else {
            // Create lock file to mark the project as in use
            try (FileWriter writer = new FileWriter(lockFile)) {
                String userName = System.getProperty("user.name");
                String machineName = java.net.InetAddress.getLocalHost().getHostName();
                setMachineName(machineName);
                setUserName(userName);
                writer.write(userName + " on " + machineName);
                return true;
            } catch (IOException e) {
                showMessage("An error occurred: " + e.getMessage());
                return false;
            }
        }
    }

    public void releaseLock() {
        if (lockFile.exists()) {
            if (!lockedByMe()){
                showMessage("<html>It seems another user forced to open this project while you are working on it. Close the project will not release the lock.<br/>" +
                        "If you believe no one else is working on the project, you can manually remove the lock file:<br/>"+this.lockFile.getAbsolutePath()+"</html>");
            }else {
                if (!lockFile.delete()) {
                    showMessage("Failed to release the project lock.");
                }
            }
        }
    }

    public boolean lockedByMe(){
        String userName = System.getProperty("user.name");
        String machineName = "";
        try {
            machineName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return  (userName.equals(this.getUserName()) && machineName.equals(this.getMachineName()));
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
}
