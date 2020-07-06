package app;

import app.Utils.BCrypt;
import app.Utils.FilePrinter;
import app.Utils.Utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.Scanner;

public class Client {

    private static String host;
    private static int port;
    private static SSLSocketFactory csf;
    private static String user;

    private static final int BUFFER_SIZE = 2000;


    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.printf("Usage:Client <server_host> <server_port>");
            System.exit(1);
        }

        host = args[0];
        port = Integer.parseInt(args[1]);

        csf = getSocketFactory();
        
        auth();
    }

    private static void auth() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1 - Login");
        System.out.println("2 - Register");
        System.out.println("0 - Exit");

        int operation = scanner.nextInt();

        switch (operation) {
            case 1:
                loginClient();
                break;
            case 2:
                registerClient();
                break;
            case 0:
                System.exit(0);
                break;
        }
    }

    private static void registerClient() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Insert a new username");
        String requestedUsername = scanner.nextLine();

        requestedUsername = requestedUsername.trim();

        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        String message = "CHECKUSERNAME " + requestedUsername;
        Utils.sendMessage(message, socket);
        String response = Utils.readMessage(socket);

        socket.close();

        if (!response.equals("AVAILABLE")) {
            System.err.println("Username already taken, please try again");
            registerClient();
        }
        SSLSocket socket2 = (SSLSocket) csf.createSocket(host, port);
        Console console = System.console();
        String enteredPassword =
                new String(console.readPassword("Please enter your new password: "));

        //TODO: check password security

        String hashedPass = BCrypt.hashpw(enteredPassword, BCrypt.gensalt());

        String registerMessage = "REGISTER " + requestedUsername + " " + hashedPass;

        Utils.sendMessage(registerMessage, socket2);

        user = requestedUsername;

        System.out.println();
        selectOperation();

    }

    private static void loginClient() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Insert Username:");
        String username = scanner.nextLine();
        Console console = System.console();
        String enteredPassword =
                new String(console.readPassword("Please enter your password: "));

        //String hashedPass = BCrypt.hashpw(enteredPassword, BCrypt.gensalt());
        String message = "LOGIN " + username.trim();

        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        Utils.sendMessage(message, socket);

        String response = Utils.readMessage(socket);

        if (!response.equals("User not found")) { //password check
            if (!BCrypt.checkpw(enteredPassword, response)) {
                Utils.sendMessage("Password not match", socket);
                System.out.println("Incorrect password");
                auth();
            }
            Utils.sendMessage("Success", socket);

            String response2 = Utils.readMessage(socket);

            if (!response2.equals("Success")) {
                System.out.println();
                System.out.println(response2);
                auth();
            }
            System.out.println("Login Successfully as " + username);
            user = username;
            selectOperation();
        }
        
        System.out.println();
        auth();
    }

    private static void logoutClient() throws IOException {
        String logoutMessage = "LOGOUT " + user;
        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        Utils.sendMessage(logoutMessage, socket);

        System.out.println("Logout successfully");
    }

    private static void selectOperation() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println();
        System.out.println("Welcome " + user + ", please select an operation:");
        System.out.println("1 - Backup");
        System.out.println("2 - Restore");
        System.out.println("3 - Delete");
        System.out.println("4 - Show backed up files");
        System.out.println("0 - LogOut");

        int operation = scanner.nextInt();

        switch (operation) {
            case 1:
                backupOperation();
                break;
            case 2:
                restoreOperation();
                break;
            case 3:
                deleteOperation();
                break;
            case 4:
                listFiles();
                break;
            case 0:
                logoutClient();
                System.exit(0);
                break;
        }

    }

    private static void listFiles() throws IOException {
        String message = "GETFILES " + user;
        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        Utils.sendMessage(message, socket);

        String response = Utils.readMessage(socket);
        String[] responseParsed = response.split(" ");

        if (responseParsed[0].equals("NOFILES")) {
            System.out.println();
            System.out.println("No files backed up yet");
        } else {
            String[] filesInfo = response.split("\n");
            for (int i = 0; i < filesInfo.length; i++) {
                System.out.println();
                System.out.println(FilePrinter.getFileContentsParsed(filesInfo[i]));
            }

        }


        selectOperation();

    }

    private static void backupOperation() throws IOException {

        System.out.println(" ");
        System.out.println("Please input the file path");
        String path = null;

        Scanner scanner = new Scanner(System.in);

        path = scanner.nextLine();
        path = path.trim();

        File toBackup = new File(path);
        if (!toBackup.exists()) {
            System.out.println();
            System.err.println("The specified file does not exist in your system");
            System.out.println();
            selectOperation();
        }

        System.out.println();
        System.out.println("Please input the wanted replication degree");
        int replicationDegree = scanner.nextInt();

        BasicFileAttributes attr = Files.readAttributes(Paths.get(toBackup.getPath()), BasicFileAttributes.class);
        String fileInfo = toBackup.getName() + " " + attr.creationTime() + " " + attr.lastModifiedTime() + " " + attr.isRegularFile() + " " + attr.size();
        //first send backup info
        String headerString = "BACKUP " + user + " " + replicationDegree + " " + fileInfo;

        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        Utils.sendMessage(headerString, socket);

        //check if server is ready to receive
        String response2 = Utils.readMessage(socket);
        String[] response2Ready = response2.split(" ");

        if (response2Ready[0].equals("READY"))
            Utils.loadAndWriteFileContent(toBackup, socket);

        //check if replication was possible
        String response3 = Utils.readMessage(socket);
        String[] response3Repli = response3.split(" ");

        System.out.println();
        if (Integer.parseInt(response3Repli[1]) < replicationDegree)
            System.out.println("Replicated the file in " + response3Repli[1] + " online peers");
        else System.out.println("File Backed up with desired replication");


        selectOperation();

    }

    private static void deleteOperation() throws IOException {
        System.out.println();
        System.out.println("Please input the file name");
        String fileName = null;
        Scanner scanner = new Scanner(System.in);
        fileName = scanner.nextLine();
        fileName = fileName.trim();
        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        String message = "DELETE " + user + " " + fileName;
        Utils.sendMessage(message, socket);
        String response = Utils.readMessage(socket);
        String[] parsedResponse = response.split(" ");
        
        if(parsedResponse[0].equals("NOFILE")){
            System.out.println();
            System.out.println("No files found with that file name");
            selectOperation();
        }
        else if (parsedResponse[0].equals("MOREFILES")){
            System.out.println();
            System.out.println("Found more than one file with that file name, please select one to delete");
            System.out.println();
            
            String[] fileInfos = response.split("\n");
            for(int i = 1; i < fileInfos.length; i++){
                System.out.println( i + ": " + FilePrinter.getFileContentsParsed(fileInfos[i]));
            }
            
            int fileIndex = scanner.nextInt();
            
            if(fileIndex > fileInfos.length || fileIndex <= 0){
                System.out.println();
                System.out.println("Invalid input");
                selectOperation();
            }
            
            Utils.sendMessage("FILEINDEX " + fileIndex, socket);
        }
        else {
            System.out.println();
            System.out.println("Found one file with that filename in our database");
        }
        
        String finalResponse = Utils.readMessage(socket);
        String[] finalM = finalResponse.split(" ");
        
        if(finalM[0].equals("FAILED")){
            System.out.println();
            System.out.println("Found your file at least in one peer, but somehow the peers don't have it stored in their filesystem");
            socket.close();
            selectOperation();
        }
        else{
            System.out.println();
            System.out.println("Success deleting from " + finalM[2] + " peers");
            socket.close();
            selectOperation();
        }
        selectOperation();
    }

    private static void restoreOperation() throws IOException {

        System.out.println(" ");
        System.out.println("Please input file name");
        String name = null;

        Scanner scanner = new Scanner(System.in);

        name = scanner.nextLine();
        name = name.trim();
        String fileName = name;

        String headerString = "RESTORE " + user + " " + name;

        SSLSocket socket = (SSLSocket) csf.createSocket(host, port);
        Utils.sendMessage(headerString, socket);

        String responseToParse = Utils.readMessage(socket);
        String[] response1 = responseToParse.split(" ");
        if (response1[0].equals("ERROR")) {
            System.out.println();
            System.out.println("Sorry, no files backed up with that file name");
            selectOperation();
        } else if (response1[0].equals("MOREFILES")) { //more files backed up with that name
            System.out.println();
            System.out.println("Found more than one file backed up with that file name, please select one:");
            String[] fileInfo = responseToParse.split("\n");

            for (int i = 1; i < fileInfo.length; i++)
                System.out.println(i + ": " + FilePrinter.getFileContentsParsed(fileInfo[i]));

            int fileIndex = scanner.nextInt();
            if (fileIndex > fileInfo.length - 1) {
                System.out.println();
                System.out.println("Invalid input");
                selectOperation();
            } else {
                Utils.sendMessage("FILEINDEX " + fileIndex, socket);
            }
        } else if (response1[0].equals("ALLGOOD")) {
            System.out.println();
            System.out.println("Found one file with that name in user database");
        }


        //prepare to receive 
        String response123 = Utils.readMessage(socket);

        if (!response123.equals("FAILED")) {
            int fileSize = Integer.parseInt(response123.split(" ")[1]);

            // //load file and send it to server
            byte[] fileContent = Utils.readFileContent(socket, fileSize);

            String path = "Restore" + File.separator;


            File restoreFile = new File(path + fileName);

            String[] fileExtension = fileName.split("\\.");

            for (int i = 1; restoreFile.exists(); i++) {
                if (fileExtension[1].isEmpty())
                    restoreFile = new File(path + String.format(fileExtension[0] + "(%d)", i));
                else
                    restoreFile = new File(path + String.format(fileExtension[0] + "(%d)." + fileExtension[1], i));
            }

            if (!restoreFile.exists()) {
                try {
                    restoreFile.getParentFile().mkdirs();
                    restoreFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            try (FileOutputStream output = new FileOutputStream(restoreFile)) {
                output.write(fileContent);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println();
            System.out.println("Restore done with success");
        }
        else{
            System.out.println();
            System.out.println("Failed restore due to peers deletion of the files in their filesystem\n or no peers online with your backups");
        }

        selectOperation();

    }

    private static SSLSocketFactory getSocketFactory() throws Exception {
        SSLContext ctx;
        KeyManagerFactory kmf;
        KeyStore ks;
        char[] passphrase = "sdis2020".toCharArray();

        ctx = SSLContext.getInstance("TLS");
        kmf = KeyManagerFactory.getInstance("SunX509");
        ks = KeyStore.getInstance("JKS");

        ks.load(new FileInputStream("keys/client.key"), passphrase);

        kmf.init(ks, passphrase);
        ctx.init(kmf.getKeyManagers(), null, null);

        return ctx.getSocketFactory();
    }

}