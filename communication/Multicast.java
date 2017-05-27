package communication;

import commands.Command;
import commands.CommandResponse;
import commands.ExecuteCommand;
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
    private static final String PingParentRequest = "PingParentRequest";
    private static final String PingParentConfirmation = "PingParentConfirmation";
    private static final String ChangeParentRequest = "ChangeParentRequest";
    private static final String ChangeParentConfirmation = "ChangeParentConfirmation";
    private static final String ChangeParentConfirmationAck = "ChangeParentConfirmationAck";
    private static final String ChangeNodeParent = "ChangeNodeParent";

    private int thisHostPort;
    private Node root;
    private Node thisPeer;
    private Node parent;

    //If the peer is creating a new group, 'publicHostName' and 'publicHostPort' must be the reachable address of the peer that is starting.
    public Multicast(int thisHostPort, String publicHostName, int publicHostPort) {
        this.thisHostPort = thisHostPort;
        this.parent = null;
        this.thisPeer = new Node(BigDecimal.ONE, publicHostName, publicHostPort);
        this.root = this.thisPeer;
        generateDispatcherThread();
        generatePingParentThread();
    }

    //If the peer is joining a group, 'publicHostName' and 'publicHostPort' must be the reachable address of an already online peer.
    public Multicast(int thisHostPort, String publicHostName, int publicHostPort, String anotherHostName, int anotherHostPort) throws Exception {
        this.thisHostPort = thisHostPort;
        this.parent = null;
        this.thisPeer = null;
        this.root = null;
        newChild(publicHostName, publicHostPort, anotherHostName, anotherHostPort);
        generateDispatcherThread();
        generatePingParentThread();
    }

    public Node getThisPeer() {
        return thisPeer;
    }

    public void showConnectedPeers() {
        HashMap<BigDecimal, Node> connectedPeers = getConnectedPeers(root);
        StringBuilder stringBuilder = new StringBuilder();
        for (Node n : connectedPeers.values())
            stringBuilder.append(n.getId()).append("\t").append(n.getHostName()).append("\t").append(n.getPort()).append("\n");
        System.out.println(stringBuilder);
    }

    private HashMap<BigDecimal, Node> getConnectedPeers(Node root) {
        HashMap<BigDecimal, Node> connectedPeers = new HashMap<>();
        connectedPeers.put(root.getId(), root);
        for (Node child : root.getChildren())
            connectedPeers.putAll(getConnectedPeers(child));
        return connectedPeers;
    }

    private void generatePingParentThread() {
        Timer timerReq = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                pingParentRequest();
            }
        };
        timerReq.schedule(t, 0);
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

                if (message.getOperation().equals(PingParentRequest))
                    pingParentConfirmation(connectionSocket, message);
                else if (message.getOperation().equals(ChangeParentRequest))
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
                        else if (message.getOperation().equals("SendFile"))
                            TransmitFile.receiveFile(message, this);
                        else if (message.getOperation().equals("ResendFile"))
                            TransmitFile.sendFile(this, ((FileData) message.getBody()).getFilepath(), message.getSender().getId());  // what if body, filepath or sender is null?
                        else if (message.getOperation().equals("SendCommand"))
                            Command.executeCommand(this, message);
                        else if(message.getOperation().equals("SendCommandAck") || message.getOperation().equals("TCPAck"))
                            ((CommandResponse)message.getBody()).print();
                        else if(message.getOperation().equals("TCP"))
                            Command.executeTCP(this, message);
                        else if(message.getOperation().equals("PORT"))
                            Command.executePort(this, message);
                    }
                }
            }

            connectionSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void propagateMessage(Message message) {
        Node lastSender = message.getLastSender();
        message.setLastSender(thisPeer);
        try {
            for (Node n : thisPeer.getChildren()) {
                if (!lastSender.equals(n) && thisPeer.isDescendant(root.getNode(n.getId())))
                    send(n.getHostName(), n.getPort(), message);
            }
            if (parent != null) {
                if (!lastSender.equals(parent))
                    send(parent.getHostName(), parent.getPort(), message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message message) throws Exception {
        if (thisPeer == null)
            throw new Exception("The multicast connection is not already established.");
        propagateMessage(message);
    }

    private void send(String hostName, int hostPort, Message message) throws Exception {
        Socket socket = SSL.generateSSLSocket(hostName, hostPort);
        send(socket, message);
        socket.close();
    }

    private void send(Socket socket, Message message) throws Exception {
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
        thisPeer = new Node(BigDecimal.ZERO, publicHostName, publicHostPort);  // 0 = id that is not used, except here
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

    private void pingParentRequest() {
        while (true) {
            try {
                if (parent != null) {
                    Socket socket = SSL.generateSSLSocket(parent.getHostName(), parent.getPort());
                    send(socket, new Message(PingParentRequest, thisPeer, parent.getId()));
                    Message answer = receive(socket);
                    socket.close();
                    if (!answer.getOperation().equals(PingParentConfirmation))
                        throw new Exception("Problem receiving the confirmation that the parent is alive.");
                }
                Thread.sleep(TCP.timeout);
            } catch (Exception e) {
                try {
                    changeParentRequest();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void pingParentConfirmation(Socket socket, Message message) throws Exception {
        send(socket, new Message(PingParentConfirmation, thisPeer, message.getSender().getId()));
    }

    private synchronized void changeParentRequest() throws Exception {
        if (!changeParentRequestAux(root, thisPeer))
            throw new Exception("Unable to send a change parent request to a new parent.");
    }

    private boolean changeParentRequestAux(Node root, Node thisPeer) {
        boolean parentChanged = false;
        try {
            Socket socket = SSL.generateSSLSocket(root.getHostName(), root.getPort());
            send(socket, new Message(ChangeParentRequest, thisPeer, root.getId()));
            Message answer = receive(socket);
            if (answer.getOperation().equals(ChangeParentConfirmation)) {
                boolean parentIsRoot = this.parent.equals(this.root);
                this.parent = root.getNode(answer.getSender().getId());
                if (parentIsRoot)
                    this.root = this.parent;
                send(socket, new Message(ChangeParentConfirmationAck, thisPeer, this.parent.getId()));
                parentChanged = true;
            }
            socket.close();
        } catch (Exception e) {
            for (Node child : root.getChildren()) {
                if (!thisPeer.equals(child) && !thisPeer.isDescendant(child))
                    if (parentChanged = changeParentRequestAux(child, thisPeer))
                        break;
            }
        }
        return parentChanged;
    }

    private synchronized void changeParentConfirmation(Socket socket, Message message) throws Exception {
        // It is automatically authorized to change because during the time where problems can occur this function is not initiated.
        Node newChild = root.getNode(message.getSender().getId());
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
        Node oldParentNode = root.getParent(childNode);
        oldParentNode.removeChild(childNode);
        Node newParent = root.getNode(parentNodeId);
        newParent.addChild(childNode);
    }
}
