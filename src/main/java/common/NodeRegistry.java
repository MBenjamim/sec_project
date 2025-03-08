package main.java.common;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import main.java.crypto_utils.RSAKeyReader;

@Getter
@Setter
public class NodeRegistry {
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
            e.printStackTrace();
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
     *
     * @param id      the unique identifier for the message
     * @param message the message to be added
     */
    synchronized public void addReceivedMessage(long id, Message message) {
        receivedMessages.putIfAbsent(id, message);
    }

    /**
     * Acknowledges a message by setting its received status to true.
     *
     * @param id the unique identifier for the message
     */
    synchronized public void ackMessage(long id) {
        sentMessages.get(id).setReceived(true);
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
