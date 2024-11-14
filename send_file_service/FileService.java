package send_file_service;

import server_service.Server;
import utilz.Utilz;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileService {
    private final int port;
    private final Map<String, File> fileStorage = new HashMap<>();

    public static void main(String[] args) {
        List<String[]> config = Utilz.readConfig();
        new FileService(config);
    }

    public FileService(List<String[]> config) {
        port = Utilz.getPort(config, "files");
        prepareServer();
    }

    private void prepareServer() {
        boolean isPortOK = checkFileServerPort();
        if (isPortOK) {
            runServer();
        } else {
            Utilz.log("Error in preparing file server.");
        }
    }

    private boolean checkFileServerPort() {
        if (port < 1) {
            throw new IllegalArgumentException("Something went wrong in configuration file." +
                    "Please check the documentation.");
        } else {
            return true;
        }
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Utilz.log("FileService is running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new FileHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, File> getFileStorage() {
        return fileStorage;
    }
}