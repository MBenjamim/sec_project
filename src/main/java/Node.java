package main.java;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import main.java.utils.RSAKeyReader;

@Getter
@Setter
public class Node {
    private String ip;
    private int port;
    private int id;
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
    public Node(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.publicKey = null;
    }

    /**
     * Retrieves the public key for the node.
     *
     * @param dir the directory where the public key file is located
     * @return the public key of the node
     */
    synchronized public PublicKey getPublicKey(String dir) {
        try {
            if (publicKey == null) {
                this.publicKey = RSAKeyReader.readPublicKey(dir + "server" + id + "_public.key");
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