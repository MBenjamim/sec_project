package main.java.common;
import lombok.Getter;

import java.io.*;
import java.net.*;
import java.util.*;

import main.java.authenticated_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the network communication of a node.
 */
@Getter
public class NetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);

    private final int id;
    private long sentMessages = 0;
    private final KeyManager keyManager;

    /**
     * Constructor for the NetworkManager class.
     *
     * @param id         the unique identifier for the server
     * @param keyManager for authenticated communication
     */
    public NetworkManager(int id, KeyManager keyManager) {
        this.id = id;
        this.keyManager = keyManager;
    }

    /**
     * Used for servers to start communications between each other.
     */
    public void startServerCommunications(int serverPort, int clientPort, MessageHandler handler1, MessageHandler handler2, Collection<NodeRegistry> nodes) {
        startListeningForUDP(serverPort, handler1);
        initiateBlockchainNetwork(nodes, false);
        startListeningForUDP(clientPort, handler2);
    }

    /**
     * Used for clients to start communication with servers.
     */
    public void startClientCommunications(int port, MessageHandler handler, Collection<NodeRegistry> nodes) {
        startListeningForUDP(port, handler);
        initiateBlockchainNetwork(nodes, true);
    }

    /**
     * Initiates the blockchain network by sending connect messages to other nodes.
     *
     * @param nodes the nodes to initiate a session with
     * @param twoWay if true indicates the session is two-way, otherwise means it is one-way
     */
    public void initiateBlockchainNetwork(Collection<NodeRegistry> nodes, boolean twoWay) {
        long messageId = generateMessageId();
        for (NodeRegistry node : nodes) {
            try {
                String encryptedKey = keyManager.generateSessionKey(node, twoWay);
                sendMessageThread(new Message(messageId, MessageType.CONNECT, id, encryptedKey), node);
            } catch (Exception e) {
                logger.error("Error while creating session key for node {}{}", node.getType(), node.getId(), e);
            }
        }
    }

    public void createOneWaySession(Message message, NodeRegistry sender) {
        try {
            sender.setSendSessionKey(keyManager.getSessionKey(message));
        } catch (Exception e) {
            logger.error("Error while creating one-way session for node {}{}", sender.getType(), sender.getId(), e);
        }
    }

    public void createTwoWaySession(Message message, NodeRegistry sender) {
        try {
            if (sender.getRecvSessionKey() == null) {
                sender.setRecvSessionKey(keyManager.getSessionKey(message));
            }
            sender.setSendSessionKey(keyManager.getSessionKey(message));
        } catch (Exception e) {
            logger.error("Error while creating two-way session for node {}{}", sender.getType(), sender.getId(), e);
        }
    }

    /**
     * Starts listening for UDP messages on the specified port.
     *
     * @param port    the port number to listen on
     * @param handler abstraction for message processing
     */
    public void startListeningForUDP(int port, MessageHandler handler) {
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(port)) {
                logger.debug("Listening for UDP messages on port {}...", port);

                while (true) {
                    Message receivedMessage = ReliableLink.receiveMessage(udpSocket);

                    if (receivedMessage != null) {
                        handler.parseReceivedMessage(receivedMessage, id);
                    }
                }
            } catch (IOException e) {
                logger.error("Error while listening for UDP messages", e);
            }
        }).start();
    }

    /**
     * Sends a message using authenticated reliable links abstraction in a separate thread.
     *
     * @param message the message to send
     * @param node    the node to send the message to
     */
    public void sendMessageThread(Message message, NodeRegistry node) {
        new Thread(() -> {
            logger.debug("Sending message: {id:{}, content:\"{}\", type:{}, receiver:{}{}}", message.getId(), message.getContent(), message.getType(), node.getType(), node.getId());
            ReliableLink.sendMessage(message, node, keyManager);
        }).start();
    }

    /**
     * Acknowledges a message in a separate thread.
     * @param message  the received message to ack
     * @param sender   the sender of original message (will be receiver of the ack)
     */
    public void acknowledgeMessage(Message message, NodeRegistry sender) {
        sendMessageThread(new Message(message.getId(), MessageType.ACK, id), sender);
    }

    /**
     * Generates a unique message ID.
     *
     * @return the generated message ID
     */
    synchronized public long generateMessageId() {
        return sentMessages++;
    }
}
