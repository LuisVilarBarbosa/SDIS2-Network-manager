package files;

import java.io.Serializable;
import java.nio.file.attribute.UserPrincipal;

public class FileData implements Serializable {
    private String filename;
    private long filesize;
    private String filepath;
    private String hashedFileId;
    private Object body;
    private int actualChunk;
    private double totalNumChunks;

    public FileData(String filename, long filesize, String filepath, String hashedFileId, Object body, int actualChunk, double totalNumChunks) {
        this.filename = filename;
        this.filesize = filesize;
        this.filepath = filepath;
        this.hashedFileId = hashedFileId;
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
}
