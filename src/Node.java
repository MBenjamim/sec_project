import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import utils.RSAKeyReader;

@Getter
@Setter
public class Node {
    private String ip;
    private int port;
    private int id;
    private PublicKey publicKey;

    Map<Long, Message> sentMessages = new HashMap<>();
    Map<Long, Message> receivedMessages = new HashMap<>();

    public Node(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.publicKey = null;
    }

    public PublicKey getPublicKey(String dir) {
        try {
            if (publicKey == null) {
                this.publicKey = RSAKeyReader.readPublicKey(dir + "server" + id + "_public.key");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public void addSentMessage(long id, Message message) {
        sentMessages.put(id, message);
    }

    public void addReceivedMessage(long id, Message message) {
        receivedMessages.putIfAbsent(id, message);
    }

    public void ackMessage(long id){
        sentMessages.get(id).setReceived(true);
    }
}