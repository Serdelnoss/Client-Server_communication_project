package send_file_service;

import utilz.Utilz;

import java.io.*;
import java.net.Socket;

public class FileReceiver {
    private final String filepath;
    private final String filename;
    private final FileService fileService;
    private final PrintWriter out;
    private final DataOutputStream dos;

    public FileReceiver(DataOutputStream dos, PrintWriter out, FileService fileService, String filename,
                        String filepath) {
        this.dos = dos;
        this.out = out;
        this.fileService = fileService;
        this.filepath = filepath;
        this.filename = filename;
    }

    public void receiveFile() {
            File file = fileService.getFileStorage().get(filename);
            if (file != null && file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    receiveFileProcess(file, fis);
                } catch (IOException | InterruptedException e) {
                    Utilz.log("Error in receiving file: " + e);
                    e.printStackTrace();
                }
            } else {
                out.println("status: ERROR;File doesn't exist.");
                out.flush();
                Utilz.log("Error in receiving file.");
            }
    }

    private void receiveFileProcess(File file, FileInputStream fis) throws InterruptedException, IOException {
        byte [] buffer = new byte[4096];
        int bytesRead;
        out.println("status: OK;Success.");
        out.flush();
        Thread.sleep(300);
        while ((bytesRead = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
            dos.flush();
        }
        Thread.sleep(300);
        dos.writeInt((int) file.length());
        dos.flush();
    }
}