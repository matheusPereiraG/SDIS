package app.ServerTasks;

import app.Server;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;

public class FaultToleranceTask implements Runnable {

    private Server server;

    public FaultToleranceTask(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        if(this.server.getPeers().size() != 0){
            for(String key : this.server.getPeers().keySet()){
                //connect to peer
                InetAddress peerAddress = this.server.getPeers().get(key).getPeerAddress();
                int peerPort = this.server.getPeers().get(key).getPeerPort();
                try {
                    SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(peerAddress, peerPort);
                    socket.startHandshake();
                    socket.close();
                } catch (IOException e) {
                    System.out.println("CONNECTION LOST " + key);
                    this.server.getPeers().remove(key);
                }
            }
        }
    }
}
