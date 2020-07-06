package app.ServerTasks;

import app.Peer;
import app.Server;
import app.Utils.Utils;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class ConnectTask implements Runnable {
    private Server server;
    private SSLSocket client;
    private Peer peer;

    public ConnectTask(Server server, SSLSocket client, Peer peer) {
        this.server= server;
        this.client = client;
        this.peer = peer;
    }

    public void run() {
        if(!this.server.getPeers().containsKey(this.peer.getPeerId())){
            this.server.addPeer(this.peer);
            System.out.println("CONNECTED " + this.peer.getPeerId());
            Utils.sendMessage("Success", this.client);
        }
        else {
            for(String key: this.server.getPeers().keySet()){
                if(this.server.getPeers().get(key).equals(this.peer)){ //if it has the same address or port don't accept it
                    Utils.sendMessage("Failed", this.client);
                }
            }
            Utils.sendMessage("Failed", this.client);
        }

        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
