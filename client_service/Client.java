package client_service;

import exceptions.NotFoundInConfigurationException;
import models.FileFullName;
import utilz.Utilz;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private boolean isAuthenticated = false;
    private boolean isRunning = true;
    private int userId;
    private String userName;
    private DataOutputStream dos;
    private DataInputStream dis;


    public static void main(String[] args) {
        List<String[]> config = Utilz.readConfig();
        new Client(config);
    }

    public Client(List<String[]> config) {
        String host = getHost(config);
        int port = Utilz.getPort(config, "server");
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            clientLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void clientLoop() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (isRunning) {
                displayCommands(isAuthenticated);
                String choice = scanner.nextLine();
                switch (choice) {
                    case "1":
                        if (isAuthenticated) {
                            logout();
                        } else {
                            loginRequest(scanner);
                        }
                        break;
                    case "2":
                        if (isAuthenticated) {
                            sendPostRequest(scanner);
                        } else {
                            registrationRequest(scanner);
                        }
                        break;
                    case "3":
                        if (!isAuthenticated) {
                            isRunning = false;
                        } else {
                            displayLast10PostsRequest();
                        }
                        break;
                    case "4":
                        if (isAuthenticated) {
                            sendFile(readFilePath(scanner));
                        }
                        break;
                    case "5":
                        if (isAuthenticated) {
                            retrieveFile(scanner);
                        }
                        break;
                    default:
                        return;
                }
            }
        } finally {
            closeResources();
        }
    }

    private void retrieveFile(Scanner scanner) {
        String[] filesNames = getFilesNames();
        System.out.println("Available files:");
        if (filesNames[0].equals("Empty files list")) {
            System.out.println("No files found.");
            return;
        }
        for (String s : filesNames) {
            if (!s.equals("SEND_FILES_NAMES_TO_USER")) System.out.println(s);
        }
        FileFullName ffn = readFileName(scanner);
        receiveFile(ffn);
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (dos != null) dos.close();
            System.out.println("Client connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getHost(List<String[]> list) {
        boolean found = false;
        for (String [] s : list) {
            if (s[0].equals("host")) return s[1];
        }
        if (!found) {
            throw new NotFoundInConfigurationException("Host not found in configuration file.\n" +
                    "Put 'host: ' in configuration file.");
        }
        return "";
    }

    private String [] getFilesNames() {
        out.println("SELECT_FILES_NAMES;");
        out.flush();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            String line = in.readLine();
            Thread.sleep(300);
            if (line.split(";")[0].equals("SEND_FILES_NAMES_TO_USER")) {
                return line.split(";");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new String[]{"Empty files list"};
    }

    private FileFullName readFileName(Scanner scanner) {
        System.out.println("Enter filename:");
        String filename = scanner.nextLine();
        System.out.println("Enter destination directory:");
        String destination = scanner.nextLine();
        return new FileFullName(filename, destination);
    }

    private String readFilePath(Scanner scanner) {
        System.out.println("Enter filepath:");
        return scanner.nextLine();
    }

    private void sendFile(String filepath) {
        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found or isn't a file.");
            return;
        }
        try {
            sendFileProcess(file);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendFileProcess(File file) throws InterruptedException, IOException {
        out.println("SEND_FILE;");
        out.flush();
        Thread.sleep(300);
        byte [] fileNameBytes = file.getName().getBytes(StandardCharsets.UTF_8);
        dos.writeInt(fileNameBytes.length);
        dos.write(fileNameBytes);
        dos.writeInt((int) file.length());
        dos.flush();
        Thread.sleep(300);
        writeFile(file);
        handleResponse();
    }

    private void handleResponse() {
        try {
            String [] response = in.readLine().split(";");
            if (response[0].equals("status: OK")) {
                System.out.println(response[1]);
            } else {
                System.err.println(response[1]);
            }
        } catch (IOException e) {
            Utilz.log("Error in handling response: " + e);
            throw new RuntimeException(e);
        }
    }

    private void writeFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte [] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveFile(FileFullName ffn) {
        try {
            receiveFileProcess(ffn);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void receiveFileProcess(FileFullName ffn) throws InterruptedException, IOException {
        out.println("RECEIVE_FILE;" + ffn.getName() + ";" + ffn.getPath());
        out.flush();
        Thread.sleep(300);
        handleResponse();
        dis.readInt();
        File file = new File(ffn.getPath() + "/" + ffn.getName());
        writeFosToFile(file);
    }

    private void writeFosToFile(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte [] buffer = new byte[4096];
            int bytesRead;
            bytesRead = dis.read(buffer);
            fos.write(buffer, 0, bytesRead);
            fos.flush();
        } catch (IOException e) {
            Utilz.log("Error in writing file to destination directory.");
            e.printStackTrace();
        }
    }

    private void sendPostRequest(Scanner scanner) {
        System.out.println("Write post content (don't use ';' characters).");
        String content = scanner.nextLine();
        if (content.contains(";")) {
            System.out.println("Don't use ';' characters.");
        } else {
            out.println("SEND_POST;" + userId + ";" + content);
        }
        handleResponse();
    }

    private void logout() {
        userId = 0;
        userName = null;
        isAuthenticated = false;
    }

    private void displayLast10PostsRequest() {
        out.println("FETCH_LATEST_POSTS");
        out.flush();
        try {
            handleRequest(in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registrationRequest(Scanner scanner) {
        System.out.print("Enter name: ");
        String name = scanner.nextLine();
        System.out.print("Enter login: ");
        String login = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        if (name.contains(";") || login.contains(";") || password.contains(";")) {
            throw new IllegalArgumentException("Name, login and password can't contains ':'!");
        }
        out.println("REGISTRATION;" + name + ";" + login + ";" + password);
        handleRegistrationResponse();
    }

    private void handleRegistrationResponse() {
        try {
            String responseFromServer = in.readLine();
            String [] dividedResponse = responseFromServer.split(";");
            if (dividedResponse[0].equals("status: OK")) {
                System.out.println(dividedResponse[1]);
            } else {
                System.err.println(dividedResponse[1]);
            }
        } catch (IOException e) {
            Utilz.log("Error in handling response from server in registration process: " + e);
            throw new RuntimeException(e);
        }
    }

    private void loginRequest(Scanner scanner) {
        System.out.print("Enter login: ");
        String login = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        if (login.contains(";") || password.contains(";")) {
            throw new IllegalArgumentException("Login and password can't contains ':'!");
        }
        out.println("AUTHORIZATION;" + login + ";" + password);
        prepareToHandleRequest();
    }

    private void prepareToHandleRequest() {
        try {
            String [] divided = in.readLine().split(";");
            String [] dividedPlusLogin = new String[divided.length + 1];
            dividedPlusLogin[0] = divided[0];
            dividedPlusLogin[1] = "LOGIN";
            for (int i = 2; i < dividedPlusLogin.length; i++) {
                dividedPlusLogin[i] = divided[i - 1];
            }
            handleRequest(dividedPlusLogin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRequest(String [] response) {
        if (response != null && response[0].equals("status: OK") && response[1].equals("LOGIN")) {
            userId = Integer.parseInt(response[2]);
            userName = response[3];
            isAuthenticated = true;
            System.out.println("Welcome " + userName + "!");
        }
    }

    private void handleRequest(String response) {
        if (response != null && response.startsWith("status: OK")) {
            String [] splitResponse = response.split(";");
            String [] clearedResponse = new String[splitResponse.length - 2];
            for (int i = 2; i < splitResponse.length - 1; i++) {
                clearedResponse[i - 2] = splitResponse[i];
            }
            printPosts(clearedResponse);
        }
    }

    private void printPosts(String [] clearedResponse) {
        for (int i = 0; i < clearedResponse.length; i++) {
            if (i % 6 == 0) {
                System.out.println("Post ID: " + clearedResponse[i + 1]);
                System.out.println("Post content: " + clearedResponse[i + 2]);
                System.out.println("Post author: " + clearedResponse[i + 3]);
                System.out.println("Posted at: " + clearedResponse[i + 4]);
                System.out.println();
            }
        }
    }

    private void displayCommands(boolean isAuthenticated) {
        String [] strings;
        if (isAuthenticated) {
            strings = buildStringIfLoggedIn();
        } else {
            strings = buildString();
        }
        for (String s : strings) {
            System.out.println(s);
        }
    }

    private String [] buildString() {
        return new String[]{
                "---------------------------------------------",
                "|            1 - log - in                   |",
                "|            2 - registration               |",
                "|            3 - exit                       |",
                "---------------------------------------------"
        };
    }

    private String [] buildStringIfLoggedIn() {
        return new String[]{
                "---------------------------------------------",
                "|            1 - logout                     |",
                "|            2 - send post                  |",
                "|            3 - last 10 posts              |",
                "|            4 - send file                  |",
                "|            5 - receive file               |",
                "|            6 - exit                       |",
                "---------------------------------------------"
        };
    }
}