package communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// Follows the Banana Tree Protocol
public class Multicast {
    public static final int CREATE_NEW_GROUP_MODE = 1;
    public static final int JOIN_GROUP_MODE = 2;
    private int thisHostPort;
    private Node root;
    private Node thisPeer;
    private Node parent;

    /*
    If the peer is creating a new group, 'publicHostName' and 'publicHostPort' must be the reachable address of the peer that is starting.
    If the peer is joining a group, 'publicHostName' and 'publicHostPort' must be the reachable address of an already online peer.
     */
    public Multicast(int mode, int thisHostPort, String publicHostName, int publicHostPort) throws Exception {
        this.thisHostPort = thisHostPort;
        this.parent = null;

        switch (mode) {
            case CREATE_NEW_GROUP_MODE:
                this.thisPeer = new Node(1, publicHostName, publicHostPort);
                this.root = this.thisPeer;
                break;
            case JOIN_GROUP_MODE:
                this.thisPeer = null;
                this.root = null;
                Message message = new Message("NewChildRequest", this.thisPeer);
                send(publicHostName, publicHostPort, message);
                break;
        }

        dispatcher();
        generatePingParentThread();
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
            Object object = TCP.receive(connectionSocket);

            Message message = (Message) object;
            System.out.println(message);

            String operation = message.getOperation();
            if (operation.equals("NewChildRequest"))
                newChildRequest(connectionSocket);
            else if (operation.equals("NewChildAccepted"))
                newChildAccepted(connectionSocket, message);
            else {
                propagateMessage(connectionSocket, message);
                ArrayList<Integer> receivers = message.getReceivers();
                if (receivers.isEmpty() || receivers.contains(thisPeer.getId())) {
                    // execute operations
                    if (message.getOperation().equals("PingParentRequest"))
                        pingParentConfirmation(connectionSocket, message);
                    else if (message.getOperation().equals("ChangeParentRequest"))
                        changeParentConfirmation(connectionSocket, message);
                    else if (message.getOperation().equals("ChangeNodeParent"))
                        changeNodeParent(message);
                }
            }

            connectionSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void propagateMessage(Socket socket, Message message) {
        String socketHostName = socket.getInetAddress().getHostName();
        int socketHostPort = socket.getPort();
        try {
            for (Node n : thisPeer.getChildren()) {
                String nodeHostName = n.getHostName();
                int nodeHostPort = n.getPort();
                if (!(socketHostName.equals(nodeHostName) && socketHostPort == nodeHostPort))
                    send(nodeHostName, nodeHostPort, message);
            }
            if (parent != null) {
                String parentHostName = parent.getHostName();
                int parentHostPort = parent.getPort();
                if (!(socketHostName.equals(parentHostName) && socketHostPort == parentHostPort))
                    send(parentHostName, parentHostPort, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message message) throws Exception {
        if (thisPeer == null)
            throw new Exception("The multicast connection is not already established.");

        Socket socket = new Socket(thisPeer.getHostName(), thisPeer.getPort());
        propagateMessage(socket, message);
        socket.close();
    }

    private void send(String hostName, int hostPort, Message message) throws Exception {
        Socket socket = new Socket(hostName, hostPort);
        send(socket, message);
        socket.close();
    }

    private void send(Socket socket, Message message) throws Exception {
        TCP.send(socket, message);
    }

    private Message receive(Socket socket) throws IOException {
        return (Message) TCP.receive(socket);
    }

    private synchronized void newChildRequest(Socket socket) throws Exception {
        String hostName = socket.getInetAddress().getHostName();
        int port = socket.getPort();
        int newId = root.getMaxId() + 1;
        if (newId == 0) {  // overflow occurred
            System.err.println("The maximum number of ids has been reached.");
            System.exit(1);
        }
        Node node = new Node(newId, hostName, port);
        thisPeer.addChild(node);
        Message message = new Message("NewChildAccepted", thisPeer, root, node.getId());
        send(socket, message);
        /* Wait for an ACK so this Multicast object does not change its state until the new child
        is updated (pay attention that the functions that change the state are synchronized). */
        Message answer = receive(socket);
        if (answer == null || !answer.getOperation().equals("NewChildAcceptedAck"))
            throw new Exception("Problem receiving the acknowledgment of the new child.");
    }

    private synchronized void newChildAccepted(Socket socket, Message message) throws Exception {
        root = (Node) message.getBody();
        thisPeer = root.getNode(message.getReceivers().get(0));
        parent = root.getParent(message.getSender());
        send(socket, new Message("NewChildAcceptedAck", thisPeer, parent.getId()));
    }

    private void pingParentRequest() {
        while (true) {
            try {
                if (parent != null) {
                    Socket socket = new Socket(parent.getHostName(), parent.getPort());
                    send(socket, new Message("PingParentRequest", thisPeer, parent.getId()));
                    Message answer = receive(socket);
                    if (answer == null)
                        changeParentRequest();
                    else if (!answer.getOperation().equals("PingParentConfirmation"))
                        throw new Exception("Problem receiving the confirmation that the parent is alive.");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pingParentConfirmation(Socket socket, Message message) throws Exception {
        send(socket, new Message("PingParentConfirmation", thisPeer, message.getSender().getId()));
    }

    private synchronized void changeParentRequest() throws Exception {
        if (!changeParentRequestAux(root, thisPeer))
            throw new Exception("Unable to send a change parent request to a new parent.");
    }

    private synchronized boolean changeParentRequestAux(Node root, Node thisPeer) throws Exception {
        boolean parentChanged = false;
        Socket socket = new Socket(root.getHostName(), root.getPort());
        send(socket, new Message("ChangeParentRequest", thisPeer, root.getId()));
        Message answer = receive(socket);
        if (answer == null) {
            for (Node child : root.getChildren()) {
                if (!thisPeer.isDescendant(child))
                    if (changeParentRequestAux(child, thisPeer))
                        break;
            }
        } else if (answer.getOperation().equals("ChangeParentConfirmation")) {
            boolean parentIsRoot = this.parent.equals(this.root);
            this.parent = root.getNode(answer.getSender().getId());
            if (parentIsRoot)
                this.root = this.parent;
            send(socket, new Message("ChangeParentConfirmationAck", thisPeer, this.parent.getId()));
            parentChanged = true;
        }
        socket.close();
        return parentChanged;
    }

    private synchronized void changeParentConfirmation(Socket socket, Message message) throws Exception {
        // It is automatically authorized to change because during the time where problems can occur this function is not initiated.
        Node newChild = root.getNode(message.getSender().getId());
        send(socket, new Message("ChangeParentConfirmation", thisPeer, newChild.getId()));
        send(new Message("ChangeNodeParent", thisPeer, newChild));
        Message answer = receive(socket);
        if (answer == null || !answer.equals("ChangeParentConfirmationAck"))
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
