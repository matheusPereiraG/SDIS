package app;

// import app.ServerTasks.BackupTask;

import app.ServerTasks.*;

import app.Utils.FileInfo;
import app.Utils.Utils;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;
import java.util.*;

public class Server {

    private ConcurrentHashMap<String, Peer> peers;
    private InetAddress address;
    private int port;
    private ServerSocketFactory ssf;
    private ScheduledExecutorService scheduler;

    //TODO: i changed this
    private ConcurrentHashMap<User, HashMap<String, FileInfo>> userInfo;


    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.printf("Usage: Server <address> <port>");
            System.exit(1);
        }

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        new Server(address, port);
    }

    public Server(InetAddress address, int port) throws IOException {
        this.address = address;
        this.port = port;
        this.ssf = Utils.getServerSocketFactory("TLS", "keys/server.key", "sdis2020");
        this.peers = new ConcurrentHashMap<>();
        this.userInfo = new ConcurrentHashMap<>();

        start();
    }

    private void start() throws IOException {
        System.out.println("Started Server: " + this.address.getHostAddress() + ":" + this.port);
        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(this.port, 0, this.address);
        serverSocket.setNeedClientAuth(true);

        //fault tolerance
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(new FaultToleranceTask(this), 0, 4, TimeUnit.SECONDS);


        while (true) {
            SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
            new Thread(new HandleConnection(this, clientSocket)).start();
        }
    }

    private class HandleConnection implements Runnable {

        SSLSocket client;
        Server server;

        public HandleConnection(Server server, SSLSocket client) {
            this.client = client;
            this.server = server;
        }

        public void run() {
            String clientRequest = Utils.readMessage(this.client);
            String args[] = clientRequest.split(" ");
            String command = args[0];

            switch (command) {

                case "CONNECT":
                    InetAddress peerHost = null;

                    try {
                        String peerId = args[1];
                        peerHost = InetAddress.getByName(args[2]);
                        int peerPort = Integer.parseInt(args[3]);
                        Peer receivingPeer = new Peer(peerId, peerHost, peerPort);

                        new Thread(new ConnectTask(this.server, this.client, receivingPeer)).start();

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    break;

                case "BACKUP":
                    new BackupTask(this.server, this.client, args);
                    break;

                case "RESTORE":
                    new RestoreTask(this.server, this.client, args);
                    break;

                case "CHECKUSERNAME":
                    String requestedUsername = args[1];
                    if (Utils.checkUsername(this.server.userInfo, requestedUsername))
                        Utils.sendMessage("AVAILABLE", this.client);
                    else Utils.sendMessage("TAKEN", this.client);
                    break;

                case "REGISTER":
                    String username = args[1];
                    String hashedPass = args[2];
                    new RegisterTask(this.server, this.client, username, hashedPass);
                    break;
                case "LOGIN":
                    String loginUsername = args[1];
                    new LoginTask(this.server, this.client, loginUsername);
                    break;
                case "LOGOUT":
                    String logoutUsername = args[1];
                    new LogoutTask(this.server, this.client, logoutUsername);
                    break;
                case "GETFILES":
                    String getFilesUsername = args[1];
                    new SendFilesInfoTask(this.server, this.client, getFilesUsername);
                    break;

                case "DELETE":
                    String deleteUsername = args[1];
                    String fileName = args[2];
                    new DeleteTask(this.server, this.client, deleteUsername, fileName);

            }
        }
    }

    public ConcurrentHashMap<String, Peer> getPeers() {
        return peers;
    }

    public void addPeer(Peer peer) {
        this.peers.put(peer.getPeerId(), peer);
    }

    public ServerSocketFactory getSocketFactory() {
        return this.ssf;
    }

    public ConcurrentHashMap<User, HashMap<String, FileInfo>> getUserInfo() {
        return this.userInfo;
    }

    public void addToUserInfo(User user, String fileHash, FileInfo info){
        this.userInfo.get(user).put(fileHash, info);
    }

    public void removeFromUserInfo(User user, String filename){

        ArrayList<String> toRemove = new ArrayList<>();
        for(String key : this.userInfo.get(user).keySet()){
            FileInfo info = this.userInfo.get(user).get(key);

            if(info.getFileName().equals(filename)){
                toRemove.add(key);
            }
        }

        for(int i = 0; i < toRemove.size(); i++){
            this.userInfo.get(user).remove(toRemove.get(i));
        }

    }

    public void addUser(User toAdd) {
        this.userInfo.put(toAdd, new HashMap<>());
    }
}
