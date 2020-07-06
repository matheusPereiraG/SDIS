package app.PeerTasks;

import app.Peer;
import app.Utils.FileInfo;
import app.Utils.Utils;

import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BackupTask {
    private Peer peer;
    private SSLSocket client;
    private String fileHash;
    private byte[] fileContent;
    private int fileSize;

    public BackupTask(Peer peer, SSLSocket client, String fileHash, int fileSize) {
        this.peer = peer;
        this.client = client;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        backup();
    }

    private void backup() {

        //first verify if file is already present in the database
        if (this.peer.getFileDatabase().containsKey(fileHash)) {
            Utils.sendMessage("STORED " + fileHash , this.client);
            System.out.println("STORED " + fileHash);
        }
        else {
            //receive file content
            Utils.sendMessage("READY " + this.peer.getPeerId() + " " + fileHash, this.client);

            this.fileContent = Utils.readFileContent(this.client, this.fileSize);

            FileInfo newFile = new FileInfo(this.fileHash, this.fileContent);

            this.peer.addToDatabase(this.fileHash, newFile);

            saveToFileSystem(this.fileHash, this.fileContent);

            System.out.println("STORED " + this.fileHash);
            Utils.sendMessage("STORED " + this.peer.getPeerId()+ " " + this.fileHash, this.client);
        }
    }

    private void saveToFileSystem(String fileHash, byte[] fileContent) {
        File file = new File("Storage" + File.separator + this.peer.getPeerId() + File.separator + fileHash);

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(fileContent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }




    }
}