import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node {
    private String ip;
    private int port;
    private int id;

    Map<Integer, Message> sentMessages = new HashMap<>();
    Map<Integer, Message> receivedMessages = new HashMap<>();

    public Node(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public void addSentMessage(int id, Message message) {
        sentMessages.put(id, message);
    }

    public void addReceivedMessage(int id, Message message) {
        receivedMessages.putIfAbsent(id, message);
    }

    public void ackMessage(int id){
        sentMessages.get(id).setReceived(true);
    }
}