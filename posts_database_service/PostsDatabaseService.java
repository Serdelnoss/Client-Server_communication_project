package posts_database_service;

import exceptions.NotFoundInConfigurationException;
import interfaces.DatabaseService;
import users_database_service.UsersDatabaseRequestHandler;
import utilz.Utilz;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class PostsDatabaseService implements DatabaseService {
    private String postsDbUrl;
    private String usersDbUrl;
    private int port;
    private int localhostPort;

    public static void main(String[] args) {
        List<String[]> config = Utilz.readConfig();

        new PostsDatabaseService(config);
    }

    public PostsDatabaseService(List<String[]> config) {
        String dbName = getDbName(config);
        String usersDbName = getUsersDbName(config);
        getProperties(config);
        if (!dbName.isEmpty()) {
            this.postsDbUrl = getUrl(config, localhostPort, dbName);
            this.usersDbUrl = getUrl(config, localhostPort, usersDbName);
            boolean isDbUrlOK = checkDbUrlServer();
            prepareToStart(isDbUrlOK);
        }
    }

    private void prepareToStart(boolean isReady) {
        if (isReady) {
            Utilz.log("Post database started.");
            startServer();
        } else {
            Utilz.log("Post database was unable to start.");
        }
    }

    @Override
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Utilz.log("Posts database is running and listening on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utilz.log("Users-database-request-handler has connected.");
                new PostsDatabaseRequestHandler(clientSocket, postsDbUrl, usersDbUrl).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkDbUrlServer() {
        if (postsDbUrl.isEmpty()) {
            throw new RuntimeException("Database url not found. Please check the documentation.");
        } else {
            return true;
        }
    }

    private void getProperties(List<String []> config) {
        port = Utilz.getPort(config, "posts-database");
        localhostPort = Utilz.getPort(config, "localhost");
    }

    @Override
    public String getUrl(List<String[]> list, int port, String dbName) {
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
    public String getDbName(List<String[]> list) {
        boolean found = false;
        for (String [] s : list) {
            if (s[0].equals("posts-database-name")) return s[1];
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Database name not found in configuration file.\n" +
                    "Put 'posts-database-name: ' in configuration file.");
        }
        return "";
    }

    public String getUsersDbName(List<String[]> config) {
        boolean found = false;
        for (String [] s : config) {
            if (s[0].equals("users-database-name")) return s[1];
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Database name not found in configuration file.\n" +
                    "Put 'users-database-name: ' in configuration file.");
        }
        return "";
    }
}
