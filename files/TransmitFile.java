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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by paulo on 18/05/2017.
 */
public class TransmitFile {
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

    public static void sendFile(Multicast mc, String filepath, BigDecimal... peer) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File file = new File(filepath);
        String fileName = file.getName();

        int filesize = (int) file.length();

        // Owner
        Path path = Paths.get(filepath);
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner = ownerAttributeView.getOwner();

        // Last modified
        long lastModified = file.lastModified();

        String fileId = fileName + owner.toString() + Long.toString(lastModified);

        // Applying SHA256 to fileId
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(fileId.getBytes("UTF-8"));
        byte[] digest = md.digest();
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String hashedFileId = sb.toString();

        int numBytesRead = 0;
        int actualChunk = 0;
        double totalNumChunks = Math.ceil((double)filesize/64000);

        while(numBytesRead < filesize) {
            byte[] body = new byte[64000];
            int numRead = fis.read(body);
            byte[] content = new byte[numRead];
            numBytesRead+=numRead;
            content = Arrays.copyOfRange(body, 0, numRead);

            FileData partialFile = new FileData(fileName, filesize, filepath, hashedFileId, content, actualChunk, totalNumChunks);

            Message msg = new Message("SendFile", mc.getThisPeer(), partialFile, peer);
            try {
                mc.send(msg);
            } catch (Exception e) {
                System.out.println("Error in sending chunk number " + actualChunk);
            }

            actualChunk++;
        }
    }

    public static void receiveFile(Message msg, Multicast mc) {
        /* Get data from message */
        String folderName = ((FileData)msg.getBody()).getHashedFileId();
        int actualChunk = ((FileData)msg.getBody()).getActualChunk();
        double totalNumChunks = ((FileData)msg.getBody()).getTotalNumChunks();
        long filesize = ((FileData)msg.getBody()).getFilesize();
        String filename = ((FileData)msg.getBody()).getFilename();
        String peerId = ((Node)msg.getSender()).getHostName() + "_" + ((Node)msg.getSender()).getPort();

        Path path = Paths.get("database/temp/" + peerId + "/" + folderName + "/" + actualChunk);
        Path folderPath = Paths.get("database/temp/" + peerId + "/" + folderName);

        byte[] data = (byte[])((FileData)msg.getBody()).getBody();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.write(path, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(actualChunk == (totalNumChunks - 1)) {
            Message ack;
            if (size(folderPath) == filesize) {
                ack = new Message("SendFileAck", mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
                Path pathfFinalFolder = Paths.get("database/" + peerId + "/" + folderName + "/" + filename);
                try {
                    Files.createDirectories(pathfFinalFolder.getParent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("totalNumChunks" + totalNumChunks);
                for(int i = 0; i < totalNumChunks; i++) {
                    Path pathTemp = Paths.get("database/temp/" + peerId + "/" + folderName + "/" + i);
                    if(!Files.exists(pathTemp)) {
                        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                        ack = new Message("ResendFile", mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
                        break;
                    }
                    else {
                        try {
                            File fileTemp = new File("database/temp/" + peerId + "/" + folderName + "/" + i);
                            FileInputStream fis = new FileInputStream(fileTemp);

                            File file = new File("database/" + peerId + "/" + folderName + "/" + filename);
                            FileOutputStream fos = new FileOutputStream(file, true);

                            byte[] content = new byte[64000];
                            int lol = fis.read(content);

                            byte[] body = new byte[lol];
                            body = Arrays.copyOfRange(content, 0, lol);

                            System.out.println("testing..." + lol);

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
                File pathToDelete = new File("database/temp/" + peerId + "/" + folderName);
                deleteDirectory(pathToDelete);
            }
            else
                ack = new Message("ResendFile", mc.getThisPeer(), msg.getBody(), msg.getSender().getId());
            try {
                mc.send(ack);
            } catch (Exception e) {
                System.out.println("Error in " + ack.getOperation());
            }
        }
    }
}
