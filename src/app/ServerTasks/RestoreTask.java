package app.ServerTasks;

import app.Peer;
import app.Server;

import app.User;
import app.Utils.FileInfo;
import app.Utils.Utils;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;


public class RestoreTask {

    private Server server;
    private SSLSocket client;
    private String args[];

    public RestoreTask(Server server, SSLSocket client, String[] args) {
        this.server = server;
        this.client = client;
        this.args = args;
        initRestore();
    }

    private void initRestore() {

        String username = args[1];
        String fileName = args[2];
        Boolean restoreDone = false;

        ArrayList<FileInfo> filesFoundUser = new ArrayList<>();

        //get file(s) in user database
        for (User user : this.server.getUserInfo().keySet()) {
            if (user.getUsername().equals(username)) {
                for (String hashFile : this.server.getUserInfo().get(user).keySet()) {
                    if (this.server.getUserInfo().get(user).get(hashFile).getFileName().equals(fileName))
                        filesFoundUser.add(this.server.getUserInfo().get(user).get(hashFile));
                }
            }
        }

        FileInfo toRestore = new FileInfo();
        if (filesFoundUser.isEmpty()) { //no files found with that name in our database
            Utils.sendMessage("ERROR ", this.client);
        } else if (filesFoundUser.size() > 1) { //if more than one, ask client which file to restore
            String message = "MOREFILES ";
            for (int i = 0; i < filesFoundUser.size(); i++) {
                message += "\n" + filesFoundUser.get(i).getFileName() + " " + filesFoundUser.get(i).getlastModified() + " " + filesFoundUser.get(i).getCreationTime() + " " + filesFoundUser.get(i).getFileSize();
            }
            Utils.sendMessage(message, this.client);
            String[] response = Utils.readMessage(this.client).split(" "); //FILEINDEX fileIndex
            int fileIndex = Integer.parseInt(response[1]);
            toRestore = filesFoundUser.get(fileIndex - 1);
        } else {
            Utils.sendMessage("ALLGOOD", this.client);
            toRestore = filesFoundUser.get(0);
        }

        //now loop peers and send them a restore request

        for (Peer peer : this.server.getPeers().values()) {
            if (peer.getFileDatabase().containsKey(toRestore.getFileID())) {
                try {
                    SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(peer.getPeerAddress(), peer.getPeerPort());
                    Utils.sendMessage("RESTORE " + toRestore.getFileID(), socket);

                    String fileCheck = Utils.readMessage(socket);

                    if (!fileCheck.equals("NOFILE")) {

                        byte[] fileContent = Utils.readFileContent(socket, toRestore.getFileSize());

                        Utils.sendMessage("READY " + toRestore.getFileSize(), this.client);

                        Utils.sendFileContent(fileContent, this.client);

                        System.out.println("RESTORE " + peer.getPeerId() + " " + toRestore.getFileID());
                        
                        restoreDone = true;

                        break;
                    }
                    else {
                        peer.getFileDatabase().remove(toRestore.getFileID()); //update peer database
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if(restoreDone)
                Utils.sendMessage("SUCESS", this.client);
            else {
                Utils.sendMessage("FAILED", this.client);
            }
                
        }

    }
}
