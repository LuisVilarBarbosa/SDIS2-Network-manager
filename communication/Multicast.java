package communication;

import commands.Command;
import commands.CommandResponse;
import files.FileData;
import files.TransmitFile;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

// Follows the Banana Tree Protocol
public class Multicast {
    private static final String NewChildRequest = "NewChildRequest";
    private static final String NewChildAccepted = "NewChildAccepted";
    private static final String NewChildAcceptedAck = "NewChildAcceptedAck";
    private static final String NewChildAdded = "NewChildAdded";
    private static final String ChangeParentRequest = "ChangeParentRequest";
    private static final String ChangeParentConfirmation = "ChangeParentConfirmation";
    private static final String ChangeParentConfirmationAck = "ChangeParentConfirmationAck";
    private static final String ChangeNodeParent = "ChangeNodeParent";
    private static final String RemoveNode = "RemoveNode";

    private int thisHostPort;
    private Node root;
    private Node thisPeer;
    private Node parent;

    //If the peer is creating a new group, 'publicHostName' and 'publicHostPort' must be the reachable address of the peer that is starting.
    public Multicast(int thisHostPort, String publicHostName, int publicHostPort) {
        this.thisHostPort = thisHostPort;
        this.root = new Node(BigDecimal.ZERO, "localhost", 1500);    // fictitious root
        this.parent = this.root;
        this.thisPeer = new Node(BigDecimal.ONE, publicHostName, publicHostPort);
        this.parent.addChild(this.thisPeer);
        generateDispatcherThread();
    }

    //If the peer is joining a group, 'publicHostName' and 'publicHostPort' must be the reachable address of an already online peer.
    public Multicast(int thisHostPort, String publicHostName, int publicHostPort, String anotherHostName, int anotherHostPort) throws Exception {
        this.thisHostPort = thisHostPort;
        this.parent = null;
        this.thisPeer = null;
        this.root = null;
        newChild(publicHostName, publicHostPort, anotherHostName, anotherHostPort);
        generateDispatcherThread();
    }

    public Node getThisPeer() {
        return thisPeer;
    }

    public void showConnectedPeers() {
        StringBuilder stringBuilder = new StringBuilder();
        if(root != null)
            getConnectedPeers(root, stringBuilder, "");
        System.out.println(stringBuilder);
    }

    private synchronized void getConnectedPeers(Node root, StringBuilder stringBuilder, String indentation) {
        if(root != this.root)
            stringBuilder.append(indentation).append(root.getId()).append("\t").append(root.getHostName()).append("\t").append(root.getPort()).append("\n");
        for (Node child : root.getChildren())
                getConnectedPeers(child, stringBuilder, indentation + " ");
    }

    private void generateDispatcherThread() {
        Timer timerReq = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                dispatcher();
            }
        };
        timerReq.schedule(t, 0);
    }

    private void dispatcher() {
        try {
            SSLServerSocket receivingSocket = SSL.generateSSLServerSocket(thisHostPort);
            while (true) {
                Socket connectionSocket = receivingSocket.accept();
                Timer timerReq = new Timer();
                TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        requestsProcessor(connectionSocket);
                    }
                };
                timerReq.schedule(t, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Prevent process lock if a TimerTask is running
            System.exit(1);
        }
    }

    private void requestsProcessor(Socket connectionSocket) {
        try {
            Message message = receive(connectionSocket);

            if (Message.class.isInstance(message)) {
                String operation = message.getOperation();

                if (message.getOperation().equals(ChangeParentRequest))
                    changeParentConfirmation(connectionSocket, message);
                else if (operation.equals(NewChildRequest))
                    newChildRequest(connectionSocket, message);
                else {
                    propagateMessage(message);
                    ArrayList<BigDecimal> receivers = message.getReceivers();
                    if (receivers.isEmpty() || receivers.contains(thisPeer.getId())) {
                        // execute operations
                        if (message.getOperation().equals(ChangeNodeParent))
                            changeNodeParent(message);
                        else if (message.getOperation().equals(NewChildAdded))
                            newChildAdded(message);
                        else if (message.getOperation().equals(RemoveNode))
                            removeNode(message);
                        else if (message.getOperation().equals(TransmitFile.SendFile))
                            TransmitFile.receiveFile(message, this);
                        else if (message.getOperation().equals(TransmitFile.ResendFile))
                            TransmitFile.sendFile(this, ((FileData) message.getBody()).getFilepath(), message.getSender().getId());  // what if body, filepath or sender is null?
                        else if (message.getOperation().equals(Command.SEND_COMMAND))
                            Command.executeCommand(this, message);
                        else if(message.getOperation().equals(Command.SEND_COMMAND_ACK) || message.getOperation().equals(Command.TCP_ACK) || message.getOperation().equals(Command.PORT_ACK))
                            ((CommandResponse)message.getBody()).print();
                        else if(message.getOperation().equals(Command.TCP))
                            Command.executeTCP(this, message);
                        else if(message.getOperation().equals(Command.PORT))
                            Command.executePort(this, message);
                        else if(message.getOperation().equals(Command.HTTP) || message.getOperation().equals(Command.HTTPS) || message.getOperation().equals(Command.FTP))
                            Command.executeProtocol(this, message);
                    }
                }
            }

            connectionSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void propagateMessage(Message message) {
        Node lastSender = message.getLastSender();
        message.setLastSender(thisPeer);
        ArrayList<BigDecimal> receivers = message.getReceivers();

        if(descendantIsReceiver(receivers)) {
            for (Node n : thisPeer.getChildren()) {
                if (!lastSender.equals(n)) {
                    try {
                        send(n.getHostName(), n.getPort(), message);
                    } catch (Exception e) {
                        removeNode(n);
                    }
                }
            }
        }
        if (parent != null && !parent.equals(root)) {
            if (!lastSender.equals(parent)) {
                try {
                    send(parent.getHostName(), parent.getPort(), message);
                } catch (Exception e) {
                    removeNode(parent);
                }
            }
        }
    }

    private boolean descendantIsReceiver(ArrayList<BigDecimal> receivers) {
        if(receivers.isEmpty())
            return true;
        for (BigDecimal receiverId : receivers) {
            Node receiver = root.getNode(receiverId);
            if(thisPeer.isDescendant(receiver))
                return true;
        }
        return false;
    }

    public void send(Message message) {
        if (thisPeer == null) {
            System.err.println("The multicast connection is not already established.");
            return;
        }
        propagateMessage(message);
    }

    private void send(String hostName, int hostPort, Message message) throws Exception {
        Socket socket = SSL.generateSSLSocket(hostName, hostPort);
        send(socket, message);
        socket.close();
    }

    private void send(Socket socket, Message message) throws IOException {
        TCP.send(socket, message);
        //System.out.println("< " + message);
    }

    private Message receive(Socket socket) throws IOException {
        Message message = (Message) TCP.receive(socket);
        //System.out.println("> " + message);
        return message;
    }

    private synchronized void newChildRequest(Socket socket, Message message) throws Exception {
        Node sender = message.getSender();
        String hostName = sender.getHostName();
        int port = sender.getPort();
        BigDecimal newId = root.getMaxId().add(BigDecimal.ONE);
        Node node = new Node(newId, hostName, port);
        thisPeer.addChild(node);
        Message confirmationMessage = new Message(NewChildAccepted, thisPeer, root, node.getId());
        send(socket, confirmationMessage);
        /* Wait for an ACK so this Multicast object does not change its state until the new child
        is updated (pay attention that the functions that change the state are synchronized). */
        Message answer = receive(socket);
        if (!answer.getOperation().equals(NewChildAcceptedAck))
            throw new Exception("Problem receiving the acknowledgment of the new child.");
        send(new Message(NewChildAdded, thisPeer, node));
    }

    private synchronized void newChild(String publicHostName, int publicHostPort, String anotherHostName, int anotherHostPort) throws Exception {
        thisPeer = new Node(BigDecimal.ZERO, publicHostName, publicHostPort);  // 0 = id that is not used, except here and on the fictitious root
        Socket socket = SSL.generateSSLSocket(anotherHostName, anotherHostPort);
        Message message = new Message(NewChildRequest, thisPeer);
        send(socket, message);
        Message answer = receive(socket);
        root = (Node) answer.getBody();
        thisPeer = root.getNode(answer.getReceivers().get(0));
        parent = root.getParent(thisPeer);
        send(socket, new Message(NewChildAcceptedAck, thisPeer, parent.getId()));
        socket.close();
    }

    private synchronized void newChildAdded(Message message) {
        Node parent = root.getNode(message.getSender().getId());
        Node child = (Node) message.getBody();
        if (!parent.getChildren().contains(child))
            parent.addChild(child);
    }

    private synchronized void changeParentRequest() {
        if (!changeParentRequestAux(root)) {
            parent = this.root;
            send(new Message(ChangeNodeParent, root, thisPeer));    // simulating that root is informing that added a new child
            if(thisPeer.getChildren().isEmpty())
                System.err.println("Unable to send a change parent request to a new parent that is not my descendant. I am the only one connected.");
        }
    }

    private boolean changeParentRequestAux(Node root) {
        boolean parentChanged = false;
        try {
            Socket socket = SSL.generateSSLSocket(root.getHostName(), root.getPort());
            send(socket, new Message(ChangeParentRequest, thisPeer, root.getId()));
            Message answer = receive(socket);
            if (answer.getOperation().equals(ChangeParentConfirmation)) {
                this.parent = root.getNode(answer.getSender().getId());
                send(socket, new Message(ChangeParentConfirmationAck, thisPeer, this.parent.getId()));
                parentChanged = true;
            }
            socket.close();
        } catch (Exception e) {
            for (Node child : root.getChildren()) {
                if (!thisPeer.equals(child) && !thisPeer.isDescendant(child))
                    if (parentChanged = changeParentRequestAux(child))
                        break;
            }
        }
        return parentChanged;
    }

    private synchronized void changeParentConfirmation(Socket socket, Message message) throws Exception {
        // It is automatically authorized to change because during the time where problems can occur this function is not initiated.
        Node newChild = root.getNode(message.getSender().getId());
        root.removeDescendant(newChild);
        thisPeer.addChild(newChild);
        send(socket, new Message(ChangeParentConfirmation, thisPeer, newChild.getId()));
        send(new Message(ChangeNodeParent, thisPeer, newChild));
        Message answer = receive(socket);
        if (!answer.getOperation().equals(ChangeParentConfirmationAck))
            throw new Exception("Problem receiving the acknowledgment of the new child parent has changed.");
    }

    private synchronized void changeNodeParent(Message message) {
        // The old parent will automatically detect that its child is no longer its child and deletes it from the 'children' array.
        BigDecimal parentNodeId = message.getSender().getId();
        BigDecimal childNodeId = ((Node) message.getBody()).getId();
        Node childNode = root.getNode(childNodeId);
        root.removeDescendant(childNode);
        Node newParent = root.getNode(parentNodeId);
        newParent.addChild(childNode);
    }

    private void removeNode(Message message) {
        removeNodeAux((Node)message.getBody());
    }

    private void removeNode(Node node) {
        removeNodeAux(node);
        send(new Message(RemoveNode, thisPeer, node));
    }

    private synchronized void removeNodeAux(Node node) {
        if(parent != null && parent.equals(node)) {
            parent = null;
            changeParentRequest();
        }
        else
            root.removeDescendant(node);
    }
}
