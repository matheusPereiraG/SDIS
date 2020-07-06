package app;

import app.PeerTasks.BackupTask;
import app.PeerTasks.DeleteTask;
import app.PeerTasks.LoadDatabase;
import app.PeerTasks.RestoreTask;
import app.Utils.FileInfo;
import app.Utils.Utils;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {

    private String peerId;
    private ServerSocketFactory ssf;
    private InetAddress peerAddress;
    private int peerPort;
    private InetAddress serverAddress;
    private int serverPort;

    private ConcurrentHashMap<String, FileInfo> fileDatabase;

    public static void main(String[] args) throws IOException {

        if (args.length < 5) {
            System.out.println("Usage: Peer <peer_address> <peer_port> <server_address> <server_port>");
            System.exit(1);
        }

        String peerId = args[0];
        InetAddress peerAddress = InetAddress.getByName(args[1]);
        int peerPort = Integer.parseInt(args[2]);
        InetAddress serverAddress = InetAddress.getByName(args[3]);
        int serverPort = Integer.parseInt(args[4]);

        new Peer(peerId, peerAddress, peerPort, serverAddress, serverPort);
    }

    public Peer(String peerId, InetAddress peerAddress, int peerPort, InetAddress serverAddress, int serverPort) throws IOException {
        this.peerId = peerId;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.ssf = Utils.getServerSocketFactory("TLS", "keys/peer.key", "sdis2020");

        this.fileDatabase = new ConcurrentHashMap<>();

        new LoadDatabase(this);

        start();
    }

    public Peer(String peerId, InetAddress peerAddress, int peerPort) { //constructor for server side
        this.peerId = peerId;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.fileDatabase = new ConcurrentHashMap<>();
    }

    private void start() throws IOException {
        System.out.println("Starting Peer: " + this.peerId + " " + this.peerAddress.getHostAddress() + ":" + this.peerPort);
        SSLServerSocket peerSocket = (SSLServerSocket) ssf.createServerSocket(this.peerPort, 0, this.peerAddress);

        peerSocket.setNeedClientAuth(true);

        if (!joinServer()) {
            System.out.println("Could not join server");
            System.exit(1);
        }

        System.out.println("Awaiting requests...");

        while (true) {
            SSLSocket clientSocket = (SSLSocket) peerSocket.accept();
            new Thread(new HandleConnection(this, clientSocket)).start();
        }
    }

    private boolean joinServer() throws IOException {
        //prepare socket to send
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(this.serverAddress, this.serverPort);

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        byte[] buffer = new byte[1024];
        String message = "CONNECT " + this.peerId + " " + this.peerAddress.getHostAddress() + " " + this.peerPort;

        out.write(message.getBytes());

        in.read(buffer);
        String answer = new String(buffer);

        socket.close();

        //FIXME: This statement isn't working properly
        if (answer.trim().equals("Failed")) {
            return false;
        }


        return true;
    }

    public void addToDatabase(String fileHash, FileInfo newFile) {
        this.fileDatabase.put(fileHash, newFile);
    }

    public void addToDatabase(FileInfo newFile) {
        this.fileDatabase.put(newFile.getFileID(), newFile);
    }

    private class HandleConnection implements Runnable {

        SSLSocket client;
        Peer peer;

        public HandleConnection(Peer peer, SSLSocket client) {
            this.client = client;
            this.peer = peer;
        }

        public void run() {

            String message = Utils.readMessage(this.client);
            String args[] = message.split(" ");
            String command = args[0];

            switch (command) {
                case "BACKUP":
                    String fileIdBackup = args[1];
                    int size = Integer.parseInt(args[2]);
                    new BackupTask(this.peer, this.client, fileIdBackup, size);
                    break;
                case "RESTORE":
                    String fileIdRestore = args[1];
                    new RestoreTask(this.peer, this.client, fileIdRestore);
                    break;
                case "DELETE":
                    String fileIdDelete = args[1];
                    new DeleteTask(this.peer, this.client, fileIdDelete);
            }
        }
    }


    public String getPeerId() {
        return peerId;
    }

    public InetAddress getPeerAddress() {
        return peerAddress;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public ConcurrentHashMap<String, FileInfo> getFileDatabase() {
        return this.fileDatabase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return peerPort == peer.peerPort &&
                Objects.equals(peerAddress, peer.peerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerAddress, peerPort);
    }
}