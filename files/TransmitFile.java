package files;

import communication.Message;
import communication.Multicast;
import communication.Node;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class TransmitFile {
    public static final String SendFile = "SendFile";
    public static final String SendFileAck = "SendFileAck";
    public static final String ResendFile = "ResendFile";

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    public static boolean deleteDirectory(File directory) {
        if(directory.isDirectory()) {
            File[] files = directory.listFiles();
            if(files!=null) {
                for(File file : files) {
                    if(file.isDirectory())
                        deleteDirectory(file);
                    else
                        file.delete();
                }
            }
            return directory.delete();
        }
        return true;
    }

    public static void sendFile(Multicast mc, String filepath, String username, BigDecimal... peer) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(filepath);

        File file = new File(filepath);
        String fileName = file.getName();

        int filesize = (int) file.length();
        String hashedFileId = username;

        int numBytesRead = 0;
        int actualChunk = 0;
        double totalNumChunks = Math.ceil((double)filesize/64000);

        while(numBytesRead < filesize) {
            byte[] body = new byte[64000];
            int numRead = fis.read(body);
            numBytesRead+=numRead;
            byte[] content = Arrays.copyOfRange(body, 0, numRead);

            FileData partialFile = new FileData(fileName, filesize, filepath, hashedFileId, username, content, actualChunk, totalNumChunks);

            Message msg = new Message(SendFile, mc.getThisPeer(), partialFile, peer);
            mc.send(msg);

            actualChunk++;
        }
    }

    public static void receiveFile(Message msg, Multicast mc) {
        /* Get data from message */
        FileData fileData = (FileData)msg.getBody();
        String folderName = fileData.getHashedFileId();
        int actualChunk = fileData.getActualChunk();
        double totalNumChunks = fileData.getTotalNumChunks();
        long filesize = fileData.getFilesize();
        Node sender = msg.getSender();
        String filename = fileData.getFilename();
        String peerId = sender.getHostName() + "_" + sender.getPort();

        String tempFolder = "database/temp/" + peerId + "/" + folderName;
        Path path = Paths.get(tempFolder + "/" + actualChunk);
        Path folderPath = Paths.get(tempFolder);

        byte[] data = (byte[])fileData.getBody();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(actualChunk == (totalNumChunks - 1)) {
            Message message;
            if (size(folderPath) == filesize) {
                String filePath = "database/" + peerId + "/" + folderName + "/" + filename;
                message = new Message(SendFileAck, mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
                Path pathfFinalFolder = Paths.get(filePath);
                try {
                    Files.createDirectories(pathfFinalFolder.getParent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for(int i = 0; i < totalNumChunks; i++) {
                    Path pathTemp = Paths.get(tempFolder + "/" + i);
                    if(!Files.exists(pathTemp)) {
                        message = new Message(ResendFile, mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
                        break;
                    }
                    else {
                        try {
                            File fileTemp = new File(tempFolder + "/" + i);
                            FileInputStream fis = new FileInputStream(fileTemp);

                            File file = new File(filePath);
                            FileOutputStream fos = new FileOutputStream(file, true);

                            byte[] content = new byte[64000];
                            int numBytes = fis.read(content);

                            byte[] body = Arrays.copyOfRange(content, 0, numBytes);

                            if (!file.exists())
                                file.createNewFile();

                            fos.write(body);

                            fos.flush();
                            fos.close();
							fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                File pathToDelete = new File(tempFolder);
                deleteDirectory(pathToDelete);
            }
            else
                message = new Message(ResendFile, mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
            mc.send(message);
        }
    }
}
