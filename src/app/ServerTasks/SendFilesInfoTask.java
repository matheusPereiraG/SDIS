package app.ServerTasks;

import app.Server;
import app.User;
import app.Utils.FileInfo;
import app.Utils.Utils;

import javax.net.ssl.SSLSocket;

public class SendFilesInfoTask {
    private Server server;
    private SSLSocket client;
    private String username;

    public SendFilesInfoTask(Server server, SSLSocket client, String username) {
        this.server = server;
        this.client = client;
        this.username = username;
        sendInfo();
    }

    private void sendInfo() {
        User thisUser = new User(username);

        if(this.server.getUserInfo().get(thisUser).size() == 0){
            Utils.sendMessage("NOFILES",this.client);
        }
        else {
            String message = "";
            for(String fileHash : this.server.getUserInfo().get(thisUser).keySet()){
                FileInfo file = this.server.getUserInfo().get(thisUser).get(fileHash);
                message += file.getFileName() + " " +file.getlastModified() + " " + file.getCreationTime() + " " +file.getFileSize() + "\n";
            }

            Utils.sendMessage(message, this.client);

        }



    }
}
