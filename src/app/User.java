package app;

import java.util.Objects;

public class User {
    private String username;
    private String hashPass;
    private boolean loggedIn;

    public User(String username, String hashPass) {
        this.username = username;
        this.hashPass = hashPass;
        this.loggedIn = false;
    }

    public User(String username){
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashPass() {
        return hashPass;
    }

    public void setHashPass(String hashPass) {
        this.hashPass = hashPass;
    }

    public void login(){
        this.loggedIn = true;
    }

    public void logout(){
        this.loggedIn = false;
    }

    public boolean isLoggedIn(){
        return this.loggedIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
