package communication;

import java.io.IOException;
import java.net.ServerSocket;
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
        this.thisPeer = new Node(1, publicHostName, publicHostPort);
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
        HashMap<Integer, Node> connectedPeers = getConnectedPeers(root);
        StringBuilder stringBuilder = new StringBuilder();
        for (Node n : connectedPeers.values())
            stringBuilder.append(n.getId()).append("\t").append(n.getHostName()).append("\t").append(n.getPort()).append("\n");
        System.out.println(stringBuilder);
    }

    private HashMap<Integer, Node> getConnectedPeers(Node root) {
        HashMap<Integer, Node> connectedPeers = new HashMap<>();
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
            ServerSocket receivingSocket = new ServerSocket(thisHostPort);
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
        } catch (IOException e) {
            e.printStackTrace();
            // Prevent process lock if a TimerTask is running
            System.exit(1);
        }
    }

    private void requestsProcessor(Socket connectionSocket) {
        try {
            Message message = receive(connectionSocket);

            if (message != null && Message.class.isInstance(message)) {
                String operation = message.getOperation();

                if (message.getOperation().equals(PingParentRequest))
                    pingParentConfirmation(connectionSocket, message);
                else if (message.getOperation().equals(ChangeParentRequest))
                    changeParentConfirmation(connectionSocket, message);
                else if (operation.equals(NewChildRequest))
                    newChildRequest(connectionSocket, message);
                else {
                    propagateMessage(message);
                    ArrayList<Integer> receivers = message.getReceivers();
                    if (receivers.isEmpty() || receivers.contains(thisPeer.getId())) {
                        // execute operations
                        if (message.getOperation().equals(ChangeNodeParent))
                            changeNodeParent(message);
                    }
                }
            }

            connectionSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void propagateMessage(Message message) {
        Node lastSender = message.getLastSender();
        message.setLastSender(thisPeer);
        try {
            for (Node n : thisPeer.getChildren()) {
                if (!lastSender.equals(n))
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
        Socket socket = new Socket(hostName, hostPort);
        send(socket, message);
        socket.close();
    }

    private void send(Socket socket, Message message) throws Exception {
        TCP.send(socket, message);
        System.out.println("< " + message);
    }

    private Message receive(Socket socket) throws IOException {
        Message message = (Message) TCP.receive(socket);
        System.out.println("> " + message);
        return message;
    }

    private synchronized void newChildRequest(Socket socket, Message message) throws Exception {
        Node sender = message.getSender();
        String hostName = sender.getHostName();
        int port = sender.getPort();
        int newId = root.getMaxId() + 1;
        if (newId == 0) {  // overflow occurred
            System.err.println("The maximum number of ids has been reached.");
            System.exit(1);
        }
        Node node = new Node(newId, hostName, port);
        thisPeer.addChild(node);
        Message confirmationMessage = new Message(NewChildAccepted, thisPeer, root, node.getId());
        send(socket, confirmationMessage);
        /* Wait for an ACK so this Multicast object does not change its state until the new child
        is updated (pay attention that the functions that change the state are synchronized). */
        Message answer = receive(socket);
        if (answer == null || !answer.getOperation().equals(NewChildAcceptedAck))
            throw new Exception("Problem receiving the acknowledgment of the new child.");
    }

    private synchronized void newChild(String publicHostName, int publicHostPort, String anotherHostName, int anotherHostPort) throws Exception {
        this.thisPeer = new Node(0, publicHostName, publicHostPort);  // 0 = id that is not used, except here
        Socket socket = new Socket(anotherHostName, anotherHostPort);
        Message message = new Message(NewChildRequest, this.thisPeer);
        send(socket, message);
        Message answer = receive(socket);
        root = (Node) answer.getBody();
        thisPeer = root.getNode(answer.getReceivers().get(0));
        parent = root.getParent(thisPeer);
        send(socket, new Message(NewChildAcceptedAck, thisPeer, parent.getId()));
    }

    private void pingParentRequest() {
        while (true) {
            try {
                if (parent != null) {
                    Socket socket = new Socket(parent.getHostName(), parent.getPort());
                    send(socket, new Message(PingParentRequest, thisPeer, parent.getId()));
                    Message answer = receive(socket);
                    if (answer == null)
                        changeParentRequest();
                    else if (!answer.getOperation().equals(PingParentConfirmation))
                        throw new Exception("Problem receiving the confirmation that the parent is alive.");
                }
                Thread.sleep(TCP.timeout);
            } catch (Exception e) {
                e.printStackTrace();
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

    private synchronized boolean changeParentRequestAux(Node root, Node thisPeer) throws Exception {
        boolean parentChanged = false;
        Socket socket = new Socket(root.getHostName(), root.getPort());
        send(socket, new Message(ChangeParentRequest, thisPeer, root.getId()));
        Message answer = receive(socket);
        if (answer == null) {
            for (Node child : root.getChildren()) {
                if (!thisPeer.isDescendant(child))
                    if (changeParentRequestAux(child, thisPeer))
                        break;
            }
        } else if (answer.getOperation().equals(ChangeParentConfirmation)) {
            boolean parentIsRoot = this.parent.equals(this.root);
            this.parent = root.getNode(answer.getSender().getId());
            if (parentIsRoot)
                this.root = this.parent;
            send(socket, new Message(ChangeParentConfirmationAck, thisPeer, this.parent.getId()));
            parentChanged = true;
        }
        socket.close();
        return parentChanged;
    }

    private synchronized void changeParentConfirmation(Socket socket, Message message) throws Exception {
        // It is automatically authorized to change because during the time where problems can occur this function is not initiated.
        Node newChild = root.getNode(message.getSender().getId());
        send(socket, new Message(ChangeParentConfirmation, thisPeer, newChild.getId()));
        send(new Message(ChangeNodeParent, thisPeer, newChild));
        Message answer = receive(socket);
        if (answer == null || !answer.equals(ChangeParentConfirmationAck))
            throw new Exception("Problem receiving the acknowledgment of the new child parent has changed.");
    }

    private synchronized void changeNodeParent(Message message) {
        // The old parent will automatically detect that its child is no longer its child and deletes it from the 'children' array.
        int parentNodeId = message.getSender().getId();
        int childNodeId = ((Node) message.getBody()).getId();
        Node childNode = root.getNode(childNodeId);
        Node oldParentNode = root.getParent(childNode);
        oldParentNode.removeChild(childNode);
        Node newParent = root.getNode(parentNodeId);
        newParent.addChild(childNode);
    }
}
