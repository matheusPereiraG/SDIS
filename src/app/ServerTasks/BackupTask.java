package app.ServerTasks;

import app.Server;

import app.User;
import app.Utils.FileInfo;
import app.Utils.Utils;


import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;


public class BackupTask {

    private Server server;
    private SSLSocket client;
    private String args[];

    public BackupTask(Server server, SSLSocket client, String[] args) {
        this.server = server;
        this.client = client;
        this.args = args;
        initBackup();
    }

    private void initBackup() {

        String username = args[1];
        int desiredReplication = Integer.parseInt(args[2]);
        String fileName = args[3];
        String creationTime = args[4];
        String lastModified = args[5];
        boolean isRegular = Boolean.parseBoolean(args[6]);
        int size = Integer.parseInt(args[7]);

        FileInfo newFile = new FileInfo(fileName, creationTime, lastModified, size, isRegular);

        Utils.sendMessage("READY " + newFile.getFileID(), this.client);

        byte[] fileContent = Utils.readFileContent(this.client, newFile.getFileSize());

        ArrayList<String> peersIds = new ArrayList<>();
        ArrayList<String> peersToSend = new ArrayList<>();

        for (String key : this.server.getPeers().keySet()) {
            peersIds.add(key);
        }
        
        int replication = desiredReplication;
        
        if (peersIds.size() < desiredReplication) {
            replication = peersIds.size();
        }

        //pick at random peers with possible rep degree
        for(int i = 0; i < replication; i++){
            int randomIndex = ThreadLocalRandom.current().nextInt(peersIds.size());
            peersToSend.add(peersIds.get(randomIndex));
            peersIds.remove(randomIndex);
        }

        //send backup message to peers
        for (String key : this.server.getPeers().keySet()) {
            for (int i = 0; i < peersToSend.size(); i++) {
                if (key.equals(peersToSend.get(i))) {
                    String message = "BACKUP " + newFile.getFileID() + " " + newFile.getFileSize();
                    try {
                        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.server.getPeers().get(key).getPeerAddress(), this.server.getPeers().get(key).getPeerPort());
                        Utils.sendMessage(message, socket);
                        
                        String firstResponseToParse = Utils.readMessage(socket);
                        String[] firstResponse = firstResponseToParse.split(" ");

                        if (firstResponse[0].equals("READY")) { //if peer ready send contents
                            OutputStream socketOutputStream = socket.getOutputStream();
                            socketOutputStream.write(fileContent);

                            String peerResponse = Utils.readMessage(socket);
                            String[] arguments = peerResponse.split(" ");

                            if (arguments[0].equals("STORED")) {
                                System.out.println(peerResponse);
                                this.server.getPeers().get(key).addToDatabase(newFile);
                            }
                        } else if(firstResponse[0].equals("STORED")) { //peer already has a backup of the file in the fileSystem
                            System.out.println(firstResponse[0] + " "+ key + firstResponse[1]);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //finally add file to user database
        User thisUser = new User(username);
        this.server.addToUserInfo(thisUser, newFile.getFileID(), newFile);
        
        //send message to client
        Utils.sendMessage("REPLICATION " + replication,this.client);

    }
}
