package send_file_service;

import server_service.Server;
import utilz.Utilz;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.logging.FileHandler;

public class FileSender {
    private final PrintWriter out;

    public FileSender(PrintWriter out) {
        this.out = out;
    }

    public File sendFile(DataInputStream dis, FileService fileService) throws IOException {
        int fileNameLength = dis.readInt();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (fileNameLength > 0) {
            byte [] fileNameBytes = new byte[fileNameLength];
            dis.readFully(fileNameBytes, 0, fileNameBytes.length);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String filename = new String(fileNameBytes, StandardCharsets.UTF_8);
            int fileContentLength = dis.readInt();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (fileContentLength > 0) {
                return sendFileProcess(fileContentLength, dis, fileService, filename);
            }
        }
        return null;
    }

    private File sendFileProcess(int fileContentLength, DataInputStream dis, FileService fileService, String filename) throws IOException {
        byte [] fileContentBytes = new byte[fileContentLength];
        dis.readFully(fileContentBytes, 0, fileContentLength);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        File file = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(fileContentBytes);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileService.getFileStorage().put(filename, file);
        out.flush();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
