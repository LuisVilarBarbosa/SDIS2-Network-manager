package communication;

import java.io.Serializable;
import java.util.ArrayList;

class Node implements Serializable {
    private int id;
    private String hostName;
    private int port;
    private ArrayList<Node> children;

    public Node(int id, String hostName, int port) {
        if (hostName == null)
            throw new NullPointerException();
        this.id = id;
        this.hostName = hostName;
        this.port = port;
        this.children = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public void addChild(Node n) {
        children.add(n);
    }

    public void removeChild(Node node) {
        children.remove(node);
    }

    public Node getParent(Node node) {
        for (Node n : children) {
            if (n.equals(node))
                return this;
        }
        for (Node n : children) {
            Node parent = n.getParent(node);
            if (parent != null)
                return parent;
        }
        return null;
    }

    public boolean isDescendant(Node node) {
        for (Node n : children) {
            if (n.equals(node))
                return true;
        }
        for (Node n : children) {
            boolean isDescendant = n.isDescendant(node);
            if (isDescendant == true)
                return isDescendant;
        }
        return false;
    }

    public Node getNode(int id) {
        if (this.id == id)
            return this;
        for (Node n : children) {
            Node n1 = n.getNode(id);
            if (n1 != null)
                return n1;
        }
        return null;
    }


    public int getMaxId() {
        int maxId = id;
        for (Node n : children) {
            int childId = n.getMaxId();
            maxId = Math.max(maxId, childId);
        }
        return maxId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        if (id != node.id) return false;
        if (port != node.port) return false;
        if (hostName != null ? !hostName.equals(node.hostName) : node.hostName != null) return false;
        return children != null ? children.equals(node.children) : node.children == null;
    }

}
