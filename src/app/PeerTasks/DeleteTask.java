package app.PeerTasks;

import javax.net.ssl.SSLSocket;

import app.Peer;
import app.Utils.Utils;

import java.io.File;

public class DeleteTask {
	private Peer peer;
    private SSLSocket client;
    private String fileHash;
    
    public DeleteTask(Peer peer, SSLSocket client, String fileHash) {
        this.peer = peer;
        this.client = client;
        this.fileHash = fileHash;
        delete();
    }

    private void delete() {

        if(this.peer.getFileDatabase().containsKey(this.fileHash)){
            if(removeFromFileSystem()){
                Utils.sendMessage("DELETED " + this.peer.getPeerId() + " " + this.fileHash, this.client);
            }
            else {
                Utils.sendMessage("NOFILE " + this.peer.getPeerId() + " " + this.fileHash, this.client);
                this.peer.getFileDatabase().remove(this.fileHash);
            }
            this.peer.getFileDatabase().remove(this.fileHash);
        }
        else Utils.sendMessage("NOFILE", this.client);
    }

    private boolean removeFromFileSystem() {
        String path = "Storage" + File.separator + this.peer.getPeerId() + File.separator + this.fileHash;
        String peerPath = "Storage" + File.separator + this.peer.getPeerId();
        String storagePath = "Storage";
        
        File peerDir = new File(peerPath);
        File storageDir = new File(storagePath);

        File toDelete = new File(path);
        
        if(toDelete.exists()){
            if(toDelete.delete())
            {
                System.out.println("DELETED " + this.fileHash);
            }
        }
        else return false;
        
        if(peerDir.listFiles().length == 0)
            peerDir.delete();
        
        if(storageDir.listFiles().length == 0)
            storageDir.delete();
        
        return true;
    }


}
