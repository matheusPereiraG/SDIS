package app.ServerTasks;

import app.Server;
import app.User;

import javax.net.ssl.SSLSocket;

public class RegisterTask {

    private Server server;
    private SSLSocket client;
    private String username;
    private String hashedPass;

    public RegisterTask(Server server, SSLSocket client, String username, String hashedPass) {
        this.server = server;
        this.client = client;
        this.username = username;
        this.hashedPass = hashedPass;
        registerUser();
    }

    private void registerUser() {
        User newUser = new User(this.username, this.hashedPass);
        this.server.addUser(newUser);
        System.out.println("REGISTERED " + this.username);
    }
}
