package app.ServerTasks;

import app.Server;
import app.User;

import javax.net.ssl.SSLSocket;

public class LogoutTask {
    private Server server;
    private SSLSocket client;
    private String username;

    public LogoutTask(Server server, SSLSocket client, String username) {
        this.server = server;
        this.client = client;
        this.username = username;
        logout();
    }

    private void logout() {
        for(User key : this.server.getUserInfo().keySet()){
            if(key.getUsername().equals(this.username))
                key.logout();
            System.out.println("LOGOUT " + this.username);
        }
    }
}
