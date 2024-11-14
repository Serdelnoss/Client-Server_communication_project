package users_database_service;

import posts_database_service.RetrieveLast10PostsProcess;
import posts_database_service.SendPostProcess;
import utilz.Utilz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

public class UsersDatabaseRequestHandler extends Thread {
    private final Socket socket;
    private final String url;

    public UsersDatabaseRequestHandler(Socket socket, String url) {
        this.socket = socket;
        this.url = url;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while ((request = in.readLine()) != null) {
                if (request.isEmpty()) {
                    Utilz.log(LocalDateTime.now() + ": Received empty or null request in " +
                            "Users Database Requests Handler. Connection was closed.");
                    return;
                }
                String [] divided = request.split(";");
                handleRequests(divided, out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private void handleRequests(String [] request, PrintWriter out) {
        switch (request[0]) {
            case "REGISTRATION":
                RegistrationProcess registrationProcess = new RegistrationProcess(url);
                registrationProcess.registerUser(request[1], request[2], request[3], out);
                break;
            case "AUTHORIZATION":
                AuthProcess authProcess = new AuthProcess(url);
                authProcess.validateUser(request[1], request[2], out);
                break;
        }
    }
}
