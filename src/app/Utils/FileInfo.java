package app.Utils;

import java.security.*;
import java.math.*;
import java.util.Objects;

public class FileInfo {

    private String fileName;
    private String fileID;                  // encrypted file id
    private String creationTime;
    private String lastModified;
    private int fileSize;
    private boolean isRegular;
    private byte[] fileContent;

    //server file info
    public FileInfo(String fileName, String creationTime, String lastModified, int fileSize, boolean isRegular) {

        this.fileName = fileName;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
        this.fileSize = fileSize;
        this.isRegular = isRegular;

        this.fileID = setFileID();
    }

    //peer file info
    public FileInfo(String fileID, byte[] content){
        this.fileID = fileID;
        this.fileContent = content;
    }
    
    public FileInfo(){}

    private String setFileID() {

        MessageDigest md;
        String encryptedFileID = "";

        try {
            md = MessageDigest.getInstance("SHA-256");
            String toEncrypt = this.fileName + this.creationTime + this.lastModified + this.fileSize + this.isRegular;
            byte[] encryptedFileIDBytes = md.digest(toEncrypt.getBytes());

            encryptedFileID = String.format("%064x", new BigInteger(1, encryptedFileIDBytes));

        } catch (Exception e) {
            System.err.println("Encryption exception: " + e.toString());
            e.printStackTrace();
        }

        return encryptedFileID;
    }

    public String getFileID() {

        return this.fileID;
    }

    public String getFileName() {

        return this.fileName;
    }

    public String getCreationTime() {

        return this.creationTime;
    }

    public String getlastModified() {

        return this.lastModified;
    }

    public int getFileSize() {

        return this.fileSize;
    }
    
    public byte[] getFileContent() {
        return fileContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return fileSize == fileInfo.fileSize &&
                isRegular == fileInfo.isRegular &&
                Objects.equals(fileName, fileInfo.fileName) &&
                Objects.equals(creationTime, fileInfo.creationTime) &&
                Objects.equals(lastModified, fileInfo.lastModified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, creationTime, lastModified, fileSize, isRegular);
    }

}