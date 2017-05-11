package communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

// Follows the Banana Tree Protocol
public class Multicast {
    private int thisHostPort;
    private Node root;
    private Node thisPeer;
    private Node parent;

    // New group
    public Multicast(int thisHostPort) {
        this.thisHostPort = thisHostPort;
        this.thisPeer = new Node(1, "localhost", thisHostPort);
        this.root = this.thisPeer;
        this.parent = null;
        dispatcher();
    }

    // Join group
    public Multicast(int thisHostPort, String anotherHostName, int anotherHostPort) throws Exception {
        this.thisHostPort = thisHostPort;
        this.thisPeer = null;
        this.root = null;
        this.parent = null;
        Message message = new Message("NewChildRequest", this.thisPeer);
        send(anotherHostName, anotherHostPort, message);
        dispatcher();
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
                if (message.getReceivers().contains(thisPeer.getId())) {
                    // execute operations
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

    public void send(String hostName, int hostPort, Message message) throws Exception {
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
        if (!answer.getOperation().equals("NewChildAcceptedAck"))
            throw new Exception("Problem receiving the acknowledgment of the new child.");
    }

    private synchronized void newChildAccepted(Socket socket, Message message) throws Exception {
        root = (Node) message.getBody();
        thisPeer = root.getNode(message.getReceivers().get(0));
        parent = root.getParent(message.getSender());
        send(socket, new Message("NewChildAcceptedAck", thisPeer, parent.getId()));
    }

    /*
    Switch parent operation is missing. The peer must verify if the
    new parent is not his descendant and must ask for permission to
    the new parent before changing parent.

    If a node fails, the tree of which it is root partitions, and its
    children will become children of the root.
    Alternatively, to avoid overloading the root, they can
    become children of their grandparent, i.e. the parent of the
    faulty node.
    */
}
