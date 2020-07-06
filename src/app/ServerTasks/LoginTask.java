package app.ServerTasks;

import app.Server;
import app.User;
import app.Utils.Utils;

import javax.net.ssl.SSLSocket;

public class LoginTask {

    private Server server;
    private SSLSocket client;
    private String username;

    public LoginTask(Server server, SSLSocket client, String username) {
        this.server = server;
        this.client = client;
        this.username = username;
        login();
    }

    private void login() {
        String message = null;
        for (User key : this.server.getUserInfo().keySet()) {
            if (key.getUsername().equals(this.username)) {
                //first verify password
                Utils.sendMessage(key.getHashPass(), this.client);
                String response = Utils.readMessage(this.client);

                if(response.equals("Success")){
                    if(key.isLoggedIn()){
                        Utils.sendMessage("User already logged in", this.client);
                    }
                    else {
                        key.login();
                        Utils.sendMessage("Success", this.client);
                        System.out.println("LOGIN " + key.getUsername());
                    }

                }
                return;
            }
        }

        message = "User not found";
        Utils.sendMessage(message, this.client);
    }
}
