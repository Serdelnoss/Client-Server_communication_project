package utilz;

import exceptions.NotFoundInConfigurationException;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Utilz {
    public static List<String[]> readConfig() {
        File file = new File("src/config.txt");
        try (Scanner scanner = new Scanner(file)) {
            List<String> config = new ArrayList<>();
            while (scanner.hasNext()) {
                config.add(scanner.nextLine());
            }
            return splitList(config);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String[]> splitList(List<String> toSplit) {
        List<String[]> returnedString = new ArrayList<>();
        for (String s : toSplit) {
            returnedString.add(s.split(": "));
        }
        return returnedString;
    }

    public static int getPort(List<String[]> list, String portType) {
        boolean found = false;
        for (String [] s : list) {
            if (s[0].equals(portType + "-port")) return Integer.parseInt(s[1]);
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Port not found in configuration file.\n" +
                    "Put 'server-port: ' in configuration file.");
        }
        return 0;
    }

    public static void log(String logs) {
        File file = new File("src/logs/logs.txt");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    System.err.println("Error in creating logs file!");
                } else {
                    write(file, logs);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            write(file, logs);
        }
    }

    private static void write(File file, String logs) {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(LocalDateTime.now() + ": " + logs + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDbUser(List<String[]> config) {
        for (String [] s : config) {
            if (s[0].equals("database-user")) return s[1];
        }
        return null;
    }

    public static String getDbPassword(List<String[]> config) {
        for (String [] s : config) {
            if (s[0].equals("database-password")) return s[1];
        }
        return null;
    }

    public static String getHost(List<String[]> config) {
        for (String [] s : config) {
            if (s[0].equals("host")) return s[1];
        }
        return null;
    }
}
