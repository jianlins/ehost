package main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {
    private static final Properties VERSION_INFO = new Properties();

    static {
        try (InputStream is = VersionInfo.class.getResourceAsStream("/version.properties")) {
            VERSION_INFO.load(is);
        } catch (IOException e) {
            // Handle exception appropriately
        }
    }

    public static String getVersion() {
        return VERSION_INFO.getProperty("version", "unknown");
    }

    public static String getBuildTime() {
        return VERSION_INFO.getProperty("buildTimestamp", "unknown");
    }

    private static String createBox(String... lines) {
        // Find the longest line to determine box width
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }

        // Add extra padding on sides (4 spaces on each side)
        int sidePadding = 4;
        int totalWidth = maxLength + (sidePadding * 2);

        // Create box with padding
        StringBuilder box = new StringBuilder();
        String horizontalLine = new String(new char[totalWidth + 4]).replace('\0', '*') + "\n";

        box.append(horizontalLine);
        for (String line : lines) {
            box.append("* ")
                    // Left padding
                    .append(new String(new char[sidePadding]).replace('\0', ' '))
                    .append(line)
                    // Right padding (including space for any shorter lines)
                    .append(new String(new char[totalWidth - line.length() - sidePadding]).replace('\0', ' '))
                    .append(" *\n");
        }
        box.append(horizontalLine);

        return box.toString();
    }


    public static void printVersionInfo() {


            String version = getVersion();
            String buildTime = getBuildTime();

            String[] lines = {
                    "Welcome to eHOST",
                    "Version: " + version,
                    "Built on: " + buildTime
            };

            System.out.println(createBox(lines));


    }

}

