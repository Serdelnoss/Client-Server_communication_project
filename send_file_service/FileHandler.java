package send_file_service;

import utilz.Utilz;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class FileHandler implements Runnable {
    private final Socket socket;
    private final FileService fileService;

    public FileHandler(Socket socket, FileService fileService) {
        this.fileService = fileService;
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dis = new DataInputStream(socket.getInputStream());
            String request;
            while ((request = in.readLine()) != null) {
                Thread.sleep(300);
                String [] dividedRequest = request.split(";");
                switch (dividedRequest[0]) {
                    case "SEND_FILE_TO_FILE_SERVICE":
                        FileSender fileSender = new FileSender(out);
                        File file = fileSender.sendFile(dis, fileService);
                        fileService.getFileStorage().put(file.getName(), file);
                        Utilz.log("Sent a file with name: " + file.getName());
                        out.println("status: OK;Success.");
                        out.flush();
                        break;
                    case "FILE_RECEIVER":
                        FileReceiver fileReceiver = new FileReceiver(
                                dos, out, fileService, dividedRequest[1], dividedRequest[2]
                        );
                        fileReceiver.receiveFile();
                        break;
                    case "FILES_NAMES_RECEIVER":
                        StringBuilder builder = getFilesNames();
                        Thread.sleep(300);
                        out.println(builder);
                        out.flush();
                        Thread.sleep(300);
                        break;
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            Utilz.log(LocalDateTime.now() + ": File service closed.");
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (dis != null) dis.close();
                if (dos != null) dos.close();
            } catch (IOException e) {
                Utilz.log(LocalDateTime.now() + ": Unexpected error until closing resources in " +
                        "file service.");
                throw new RuntimeException(e);
            }
        }
    }

    private StringBuilder getFilesNames() {
        StringBuilder strBldr = new StringBuilder("SEND_FILES_NAMES_TO_USER;");
        for (File f : fileService.getFileStorage().values()) {
            strBldr.append(f.getName()).append(";");
        }
        return strBldr;
    }
}
