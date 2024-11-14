package server_service;

import utilz.Utilz;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class Server {
    private static Set<ClientHandler> clientHandlerSet = new HashSet<>();
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        int port = getPort();
        Utilz.log("Server started on port " + port + " and waiting for connections...");
        try {
            serverSocket = new ServerSocket(port);
            Utilz.log("Server started on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utilz.log("Accepted connection from client.");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlerSet.add(clientHandler);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            Utilz.log(LocalDateTime.now() + ": Server on port " + port + " was unable to start.");
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
                Utilz.log(LocalDateTime.now() + ": Server was closed.");
            } catch (IOException e) {
                Utilz.log(LocalDateTime.now() + ": Error in closing server.");
                throw new RuntimeException(e);
            }
        }
    }

    private static int getPort() {
        List<String[]> config = Utilz.readConfig();
        return Utilz.getPort(config, "server");
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private Socket filesSocket;
        private Socket usersDbSocket;
        private Socket postsDbSocket;
        private PrintWriter fout;
        private PrintWriter uout;
        private PrintWriter pout;
        private PrintWriter cout;
        private BufferedReader fin;
        private BufferedReader uin;
        private BufferedReader pin;
        private BufferedReader cin;
        private DataInputStream cdis;
        private DataOutputStream cdos;
        private DataOutputStream fdos;
        private DataInputStream fdis;
        private String host;
        private int filesPort;
        private int usersDbPort;
        private int postsDbPort;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            System.out.println("Initializing clientHandler...");
            readProperties();
            initSockets();
            initReaders();
            initWriters();
            initDataOutputInputStreams();
        }

        private void initDataOutputInputStreams() {
            try {
                cdis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                cdos = new DataOutputStream(clientSocket.getOutputStream());
                fdos = new DataOutputStream(filesSocket.getOutputStream());
                fdis = new DataInputStream(new BufferedInputStream(filesSocket.getInputStream()));
                Utilz.log("Successfully initialized input/output streams.");
            } catch (IOException e) {
                Utilz.log("Exception in opening output/input streams: " + e);
                throw new RuntimeException(e);
            }
        }

        private void initWriters() {
            try {
                cout = new PrintWriter(clientSocket.getOutputStream(), true);
                fout = new PrintWriter(filesSocket.getOutputStream(), true);
                uout = new PrintWriter(usersDbSocket.getOutputStream(), true);
                pout = new PrintWriter(postsDbSocket.getOutputStream(), true);
                Utilz.log("Successfully initialized PrintWriters.");
            } catch (IOException e) {
                Utilz.log("Exception in opening PrintWriters: " + e);
                e.printStackTrace();
            }
        }

        private void initReaders() {
            try {
                cin = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                fin = new BufferedReader(new InputStreamReader(filesSocket.getInputStream()));
                uin = new BufferedReader(new InputStreamReader(usersDbSocket.getInputStream()));
                pin = new BufferedReader(new InputStreamReader(postsDbSocket.getInputStream()));
                Utilz.log("Successfully initialized BufferedReaders.");
            } catch (IOException e) {
                Utilz.log("Exception in opening BufferedReaders: " + e);
                throw new RuntimeException(e);
            }
        }

        private void initSockets() {
            try {
                filesSocket = new Socket(host, filesPort);
                usersDbSocket = new Socket(host, usersDbPort);
                postsDbSocket = new Socket(host, postsDbPort);
                Utilz.log("Successfully initialized sockets.");
            } catch (IOException e) {
                Utilz.log("Exception in opening Sockets: " + e);
                throw new RuntimeException(e);
            }

        }

        private void readProperties() {
            List<String[]> config = Utilz.readConfig();
            this.host = Utilz.getHost(config);
            this.filesPort = Utilz.getPort(config, "files");
            this.usersDbPort = Utilz.getPort(config, "users-database");
            this.postsDbPort = Utilz.getPort(config, "posts-database");
        }

        @Override
        public void run() {
            try {
                String clientRequest;
                Utilz.log("ClientHandler is running.");
                while ((clientRequest = cin.readLine()) != null) {
                    String [] splitRequest = clientRequest.split(";");
                    handleRequests(splitRequest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    closeResources();
                    clientSocket.close();
                    clientHandlerSet.remove(this);
                    Utilz.log("Closed resources.");
                } catch (IOException e) {
                    Utilz.log("Something went wrong in closing resources: " + e);
                    throw new RuntimeException(e);
                }
            }
        }

        private void closeResources() throws IOException {
            closeSockets();
            closePrintWriters();
            closeBufferedReaders();
            closeInputOutputStreams();
        }

        private void closeInputOutputStreams() throws IOException {
            cdis.close();
            cdos.close();
            fdos.close();
            fdis.close();
            Utilz.log("Closed input/output streams.");
        }

        private void closeBufferedReaders() throws IOException {
            fin.close();
            uin.close();
            pin.close();
            cin.close();
            Utilz.log("Closed BufferedReaders.");
        }

        private void closePrintWriters() {
            fout.close();
            uout.close();
            pout.close();
            cout.close();
            Utilz.log("Closed PrintWriters.");
        }

        private void closeSockets() throws IOException {
            filesSocket.close();
            usersDbSocket.close();
            postsDbSocket.close();
            Utilz.log("Closed Sockets.");
        }

        private void handleRequests(String [] request) throws IOException {
            switch (request[0]) {
                case "AUTHORIZATION":
                    String authResponse = sendAuthorizationRequestToDatabaseService(request[1],
                            request[2]);
                    cout.println(authResponse);
                    break;
                case "REGISTRATION":
                    String registrationResponse = sendRegistrationRequestToDatabaseService(
                            request[1],
                            request[2],
                            request[3]
                    );
                    cout.println(registrationResponse);
                    break;
                case "FETCH_LATEST_POSTS":
                    String last10Response = sendGetLastTenPostsRequestToDatabaseService();
                    cout.println(last10Response);
                    break;
                case "SEND_POST":
                    String sendPostResponse = sendPostToDatabaseService(
                            Integer.parseInt(request[1]), request[2]
                    );
                    cout.println(sendPostResponse);
                    break;
                case "SEND_FILE":
                    try {
                        File file = readFileFromUser();
                        if (file != null) sendFileToFileService(file);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "RECEIVE_FILE":
                    receiveFileRequest(request[1], request[2]);
                    break;
                case "SELECT_FILES_NAMES":
                    sendRequestToFilesReceiver();
                    break;
                case "FILES_NAMES":
                    String response = cin.readLine();
                    cout.println(response);
                    cout.flush();
                    break;
                case "SEND_FILES_NAMES_TO_USER":
                    sendFilesNamesToUser();
                    break;
            }
        }

        private void sendFilesNamesToUser() {
            String responseFromFileHandler;
            try {
                responseFromFileHandler = cin.readLine();
                Thread.sleep(300);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            cout.println(responseFromFileHandler);
            cout.flush();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendRequestToFilesReceiver() {
            try {
                fout.println("FILES_NAMES_RECEIVER");
                fout.flush();
                Thread.sleep(300);
                String response = fin.readLine();
                String [] filesNames = response.split(";");
                sendRequestToFilesReceiverProcess(filesNames, response);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendRequestToFilesReceiverProcess(String [] filesNames, String response) throws InterruptedException {
            if (filesNames[0].equals("SEND_FILES_NAMES_TO_USER")) {
                cout.println(response);
                cout.flush();
                Thread.sleep(300);
            }
        }

        private File readFileFromUser() throws IOException, InterruptedException {
            int fileNameLength = cdis.readInt();
            if (fileNameLength > 0) {
                return readFileFromUserProcess(fileNameLength);
            }
            return null;
        }

        private File readFileFromUserProcess(int fileNameLength) throws InterruptedException, IOException {
            Thread.sleep(300);
            byte [] fileNameBytes = new byte[fileNameLength];
            cdis.readFully(fileNameBytes, 0, fileNameBytes.length);
            String filename = new String(fileNameBytes, StandardCharsets.UTF_8);
            int fileContentLength = cdis.readInt();
            if (fileContentLength > 0) {
                return sendFile(fileContentLength, filename);
            }
            return null;
        }

        private File sendFile(int fileContentLength, String filename) throws IOException {
            byte [] fileContentBytes = new byte[fileContentLength];
            cdis.readFully(fileContentBytes, 0, fileContentLength);
            File file = new File(filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileContentBytes);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return file;
        }

        private void receiveFileRequest(String name, String destination) {
            try {
                fout.println("FILE_RECEIVER;" + name + ";" + destination);
                fout.flush();
                Thread.sleep(300);
                String response = fin.readLine();
                Thread.sleep(300);
                receiveFileProcess(response);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFileProcess(String response) throws InterruptedException, IOException {
            if (response.split(";")[0].equals("status: OK")) {
                cout.println(response);
                cout.flush();
                Thread.sleep(300);
                int fileSize = fdis.readInt();
                cdos.writeInt(fileSize);
                cdos.flush();
                Thread.sleep(300);
                byte [] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fdis.read(buffer)) != -1) {
                    cdos.write(buffer, 0, bytesRead);
                    cdos.flush();
                }
                Thread.sleep(300);
            } else {
                cout.println("status: ERROR;Error in receiving file.");
            }
        }

        private String sendPostToDatabaseService(int userId, String content) {
            try {
                pout.println("SEND_POST;" + userId + ";" + content);
                return pin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return errorMessage();
            }
        }

        private void sendFileToFileService(File file) {
            try {
                fout.println("SEND_FILE_TO_FILE_SERVICE;" + file.getAbsolutePath());
                fout.flush();
                Thread.sleep(300);
                byte [] fileNameBytes = file.getName().getBytes(StandardCharsets.UTF_8);
                fdos.writeInt(fileNameBytes.length);
                fdos.write(fileNameBytes);
                fdos.writeInt((int) file.length());
                fdos.flush();
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte [] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fdos.write(buffer, 0, bytesRead);
                    }
                    fdos.flush();
                    cout.println(fin.readLine());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private String sendGetLastTenPostsRequestToDatabaseService() {
            try {
                pout.println("FETCH_LATEST_POSTS");
                return pin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return errorMessage();
            }
        }

        private String sendRegistrationRequestToDatabaseService(String name, String login, String password) {
            try {
                uout.println("REGISTRATION;" + name + ";" + login + ";" + password);
                return uin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return errorMessage();
            }
        }

        private String sendAuthorizationRequestToDatabaseService(String login, String password) {
            try {
                Utilz.log("Authorization process.");
                uout.println("AUTHORIZATION;" + login + ";" + password);
                return uin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return errorMessage();
            }
        }

        private String errorMessage() {
            return "ERROR: unable to connect to database service.";
        }
    }
}
