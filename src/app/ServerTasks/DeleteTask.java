package app.ServerTasks;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import app.Peer;
import app.Server;
import app.User;
import app.Utils.FileInfo;
import app.Utils.Utils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class DeleteTask {
    private Server server;
    private SSLSocket client;
    private String fileName;
    private String username;


    public DeleteTask(Server server, SSLSocket client, String username, String fileName) {
        this.server = server;
        this.client = client;
        this.username = username;
        this.fileName = fileName;
        delete();
    }

    private void delete() { //deletes all files with that file name
        User thisUser = new User(username);
        ArrayList<FileInfo> toDeleteFilesHash = new ArrayList<>();
        for (String fileHash : this.server.getUserInfo().get(thisUser).keySet()) {
            FileInfo info = this.server.getUserInfo().get(thisUser).get(fileHash);
            if (info.getFileName().equals(this.fileName)) {
                toDeleteFilesHash.add(info);
            }
        }

        FileInfo toDelete = new FileInfo();

        if (toDeleteFilesHash.size() == 0) { //File not found
            Utils.sendMessage("NOFILE", this.client);
            return;
        } else if (toDeleteFilesHash.size() > 1) //found more than one file, ask user which to delete first
        {
            String message = "MOREFILES ";
            for (int i = 0; i < toDeleteFilesHash.size(); i++) {
                FileInfo s = toDeleteFilesHash.get(i);
                message += "\n" + s.getFileName() + " " + s.getlastModified() + " " + s.getCreationTime() + " " + s.getFileSize();
            }

            Utils.sendMessage(message, this.client);

            String response = Utils.readMessage(this.client);
            int fileIndex = Integer.parseInt(response.split(" ")[1]) - 1;
            toDelete = toDeleteFilesHash.get(fileIndex);

        } else {
            Utils.sendMessage("ALLGOOD", this.client);
            toDelete = toDeleteFilesHash.get(0);
        }


        //verify which peer(s) have the file stored
        ArrayList<Peer> toSend = new ArrayList<>();
        for (Peer p : this.server.getPeers().values()) {
            if (p.getFileDatabase().containsKey(toDelete.getFileID()))
                toSend.add(p);
        }

        ArrayList<String> peerSuccessResponses = new ArrayList<>();
        for (Peer s : toSend) {
            SSLSocket socket = null;
            try {
                socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(s.getPeerAddress(), s.getPeerPort());
                Utils.sendMessage("DELETE " + toDelete.getFileID(), socket);

                String peerResponse = Utils.readMessage(socket);
                String[] peerResponseParsed = peerResponse.split(" ");

                if (peerResponseParsed[0].equals("DELETED"))
                    peerSuccessResponses.add(peerResponse);
                else {
                    this.server.getPeers().get(s).getFileDatabase().remove(toDelete.getFileID());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        for (int j = 0; j < peerSuccessResponses.size(); j++) {
            System.out.println(peerSuccessResponses.get(j));
        }

        //delete from server db
        this.server.removeFromUserInfo(thisUser, this.fileName);

        if (peerSuccessResponses.size() == 0) {
            Utils.sendMessage("FAILED", this.client);
        } else
            Utils.sendMessage("SUCCESS " + toSend.size() + " " + peerSuccessResponses.size(), this.client);
        
    }
}
