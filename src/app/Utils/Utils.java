package app.Utils;

import app.User;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {

    private static final int BUFFER_SIZE = 2000;

    public static String readMessage(SSLSocket client) {
        
        try {
            InputStream input = client.getInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            input.read(buffer);
            String message = new String(buffer);
            return message.trim();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void sendMessage(String message, SSLSocket client) {
        try {
            OutputStream output = client.getOutputStream();
            output.write(message.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public static ServerSocketFactory getServerSocketFactory(String type, String keyPath, String password) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try {
                // set up key manager to do server authentication
                SSLContext ctx;
                KeyManagerFactory kmf;
                KeyStore ks;
                char[] passphrase = password.toCharArray();

                ctx = SSLContext.getInstance("TLS");
                kmf = KeyManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS");

                ks.load(new FileInputStream(keyPath), passphrase);
                kmf.init(ks, passphrase);
                ctx.init(kmf.getKeyManagers(), null, null);

                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    }

    public static void loadAndWriteFileContent(File file, SSLSocket socket){
        int bytesRead = 0;
        byte[] fileBuffer = new byte[ (int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {

            while( fis.available() > 0 ) {

                byte fileByte = (byte) fis.read();

                fileBuffer[bytesRead] = fileByte;

                bytesRead++;
            }

        } catch(Exception e) {
            System.err.println("Failed to read File: " + e.toString());
            e.printStackTrace();
        }

        int i;
        for(i=0; i<fileBuffer.length; i++) {
            OutputStream output = null;
            try {
                output = socket.getOutputStream();
                output.write(fileBuffer[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void sendFileContent(byte[] fileBuffer, SSLSocket socket){
        int i;
        for(i=0; i<fileBuffer.length; i++) {
            OutputStream output = null;
            try {
                output = socket.getOutputStream();
                output.write(fileBuffer[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] readFileContent(SSLSocket socket, int fileSize){
        byte[] fileContent = new byte[fileSize];
        int bytesReceived = 0;

        try {
            InputStream input = socket.getInputStream();

            while( bytesReceived < fileSize) {

                byte fileContentByte = (byte) input.read();

                fileContent[bytesReceived] = fileContentByte;

                bytesReceived++;
            }

        } catch(Exception e) {
            System.err.println("error receiving file contents: " + e.toString());
            e.printStackTrace();
        }
        return fileContent;
    }

    public static boolean checkUsername(ConcurrentHashMap<User, HashMap<String, FileInfo>> userInfo, String requestedUserName) {
        User userToCheck = new User(requestedUserName);
        if(userInfo.containsKey(userToCheck))
            return false;


        return true;
    }
}
