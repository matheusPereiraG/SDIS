package app.PeerTasks;

import java.io.File;

import javax.net.ssl.SSLSocket;

import app.Peer;
import app.Utils.FileInfo;
import app.Utils.Utils;

public class RestoreTask {
    private Peer peer;
    private SSLSocket client;
    private String fileHash;

    public RestoreTask(Peer peer, SSLSocket client, String fileHash) {
        this.peer = peer;
        this.client = client;
        this.fileHash = fileHash;
        restore();
    }

    private void restore() {

        //first verify if file is already present in the database
        if (this.peer.getFileDatabase().containsKey(this.fileHash)) {
            File file = new File("Storage" + File.separator + this.peer.getPeerId() + File.separator + this.fileHash);
            if(!file.exists()){
                Utils.sendMessage("NOFILE", this.client);
                this.peer.getFileDatabase().remove(this.fileHash); //if file is not in the filesystem dont store it in peer db
            }
            else {
                Utils.sendMessage("TRANSFERRING", this.client);
                System.out.println("RESTORE " + this.fileHash);
                Utils.loadAndWriteFileContent(file, this.client);
            }

        } else {
            Utils.sendMessage("ERROR " + fileHash, this.client);
        }

    }

    public Peer getPeer() {
        return peer;
    }

    public SSLSocket getClient() {
        return client;
    }

    public String getFileHash() {
        return fileHash;
    }
}