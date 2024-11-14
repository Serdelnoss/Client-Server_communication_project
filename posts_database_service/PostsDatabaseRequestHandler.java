package posts_database_service;

import utilz.Utilz;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class PostsDatabaseRequestHandler extends Thread {
    private final Socket socket;
    private final String postsUrl;
    private final String usersUrl;

    public PostsDatabaseRequestHandler(Socket socket, String postsUrl, String usersUrl) {
        this.socket = socket;
        this.postsUrl = postsUrl;
        this.usersUrl = usersUrl;
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
                    Utilz.log("Received empty or null request in" +
                            "Posts Database Request Handler. Connection was closed.");
                    break;
                }
                String [] divided = request.split(";");
                handleRequests(divided, out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleRequests(String [] request, PrintWriter out) {
        switch (request[0]) {
            case "FETCH_LATEST_POSTS":
                RetrieveLast10PostsProcess fetchLatestPosts = new RetrieveLast10PostsProcess(postsUrl, usersUrl, out);
                fetchLatestPosts.fetchPosts();
                break;
            case "SEND_POST":
                SendPostProcess sendPostProcess = new SendPostProcess(postsUrl, out);
                sendPostProcess.sendPost(Integer.parseInt(request[1]), request[2]);
                break;
        }
    }
}
