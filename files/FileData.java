package files;

import java.io.Serializable;

public class FileData implements Serializable {
    private String filename;
    private long filesize;
    private String filepath;
    private String hashedFileId;
    private Object body;
    private int actualChunk;
    private double totalNumChunks;
    private String username;

    public FileData(String filename, long filesize, String filepath, String hashedFileId, String username, Object body, int actualChunk, double totalNumChunks) {
        this.filename = filename;
        this.filesize = filesize;
        this.filepath = filepath;
        this.hashedFileId = hashedFileId;
        this.username = username;
        this.body = body;
        this.actualChunk = actualChunk;
        this.totalNumChunks = totalNumChunks;
    }

    public long getFilesize() {
        return filesize;
    }

    public String getHashedFileId() {
        return hashedFileId;
    }

    public Object getBody() {
        return body;
    }

    public int getActualChunk() {
        return actualChunk;
    }

    public double getTotalNumChunks() {
        return totalNumChunks;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getFilename() {
        return filename;
    }

    public String getUsername() {
        return username;
    }
}
