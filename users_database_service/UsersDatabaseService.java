package users_database_service;

import exceptions.NotFoundInConfigurationException;
import interfaces.DatabaseService;
import utilz.Utilz;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class UsersDatabaseService implements DatabaseService {
    private String dbUrl;
    private int port;
    private int localhostPort;

    public static void main(String [] args) {
        List<String []> configList = Utilz.readConfig();

        new UsersDatabaseService(configList);
    }

    public UsersDatabaseService(List<String []> configList) {
        String dbName = getDbName(configList);
        getProperties(configList);
        if (!dbName.isEmpty()) {
            this.dbUrl = getUrl(configList, localhostPort, dbName);
            boolean isDbUrlOK = checkDbUrlServer();
            prepareToStart(isDbUrlOK);
        }
    }

    private void prepareToStart(boolean isReady) {
        if (isReady) {
            Utilz.log(LocalDateTime.now() + ": User database started.");
            startServer();
        } else {
            Utilz.log(LocalDateTime.now() + ": User database was unable to start.");
        }
    }

    private boolean checkDbUrlServer() {
        if (dbUrl.isEmpty()) {
            throw new RuntimeException("Database url not found. Please check the documentation.");
        } else {
            return true;
        }
    }

    @Override
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Utilz.log(LocalDateTime.now() + ": users database is running and listening on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utilz.log(LocalDateTime.now() + ": Users-database-request-handler has connected.");
                new UsersDatabaseRequestHandler(clientSocket, dbUrl).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getProperties(List<String[]> configList) {
        port = Utilz.getPort(configList, "users-database");
        localhostPort = Utilz.getPort(configList, "localhost");
    }

    @Override
    public String getUrl(List<String []> list, int port, String dbName) {
        boolean found = false;
        for (String [] s : list) {
            if (s[0].equals("database-url")) return s[1] + port + "/" + dbName;
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Database url not found in configuration file.\n" +
                    "Put 'database-url: ' in configuration file.");
        }
        return "";
    }

    @Override
    public String getDbName(List<String []> list) {
        boolean found = false;
        for (String [] s : list) {
            if (s[0].equals("users-database-name")) return s[1];
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Database name not found in configuration file.\n" +
                    "Put 'database-name: ' in configuration file.");
        }
        return "";
    }
}