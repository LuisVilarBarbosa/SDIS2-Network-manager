package communication;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

public class Message implements Serializable {
    private String operation;
    private Node sender;
    private Node lastSender;
    private ArrayList<BigDecimal> receivers;   // if empty = all
    private Object body;

    public Message(String operation, Node sender, BigDecimal... receivers) {
        this.operation = operation;
        this.sender = sender;
        this.lastSender = sender;
        this.receivers = new ArrayList<>();
        for (BigDecimal id : receivers)
            this.receivers.add(id);
    }

    public Message(String operation, Node sender, Object body, BigDecimal... receivers) {
        this.operation = operation;
        this.sender = sender;
        this.lastSender = sender;
        this.body = body;
        this.receivers = new ArrayList<>();
        for (BigDecimal id : receivers)
            this.receivers.add(id);
    }

    public String getOperation() {
        return operation;
    }

    public Node getSender() {
        return sender;
    }

    public Node getLastSender() {
        return lastSender;
    }

    public void setLastSender(Node lastSender) {
        this.lastSender = lastSender;
    }

    public ArrayList<BigDecimal> getReceivers() {
        return receivers;
    }

    public Object getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Message:\n");
        stringBuilder.append("  Operation: ").append(operation).append("\n");
        stringBuilder.append("  Sender: ");
        if (sender == null)
            stringBuilder.append("not known\n");
        else
            stringBuilder.append(sender.getId()).append(" ").append(sender.getHostName()).append(" ").append(sender.getPort()).append("\n");
        stringBuilder.append("  Receivers:");
        if (receivers.isEmpty())
            stringBuilder.append(" all");
        else {
            for (BigDecimal id : receivers)
                stringBuilder.append(" ").append(id);
        }
        stringBuilder.append("\n\n");
        return stringBuilder.toString();
    }

}
