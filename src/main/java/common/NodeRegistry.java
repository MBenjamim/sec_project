package main.java.common;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import main.java.crypto_utils.RSAKeyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class NodeRegistry {
    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

    private static final String publicKeysDir = "public_keys/";
    private String ip;
    private int port;
    private int id;
    private String type;
    private PublicKey publicKey;

    Map<Long, Message> sentMessages = new HashMap<>();
    Map<Long, Message> receivedMessages = new HashMap<>();

    /**
     * Constructor for the Node class.
     *
     * @param id    the unique identifier for the node
     * @param ip    the IP address of the node
     * @param port  the port number of the node
     */
    public NodeRegistry(int id, String type, String ip, int port) {
        this.id = id;
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.publicKey = null;
    }

    /**
     * Retrieves the public key for the node.
     *
     * @return the public key of the node
     */
    synchronized public PublicKey getPublicKey() {
        try {
            if (publicKey == null) {
                this.publicKey = RSAKeyReader.readPublicKey(publicKeysDir + type + id + "_public.key");
            }
        } catch (Exception e) {
            logger.error("Failed to read public key", e);
        }
        return publicKey;
    }

    /**
     * Adds a sent message to the sentMessages map.
     *
     * @param id      the unique identifier for the message
     * @param message the message to be added
     */
    synchronized public void addSentMessage(long id, Message message) {
        sentMessages.put(id, message);
    }

     /**
      * Adds a received message to the receivedMessages map.
      * Check if the message is being received for the first time.
      *
      * @param id      the unique identifier for the message
      * @param message the message to be added
      * @return true if the message is being received for the first time
     */
    synchronized public boolean addReceivedMessage(long id, Message message) {
        return receivedMessages.putIfAbsent(id, message) == null;
    }

    /**
     * Acknowledges a message by setting its received status to true.
     * Only ack messages that were sent.
     *
     * @param id the unique identifier for the message
     */
    synchronized public void ackMessage(long id) {
        Message message = sentMessages.get(id);
        if (message != null) {
            message.setReceived(true);
        }
    }

    /**
     * Check if message was acknowledged by verifying received status
     *
     * @param id the unique identifier for the message
     */
    synchronized public boolean checkAckedMessage(long id) {
        return sentMessages.get(id).isReceived();
    }
}
