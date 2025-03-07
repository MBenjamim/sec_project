package main.java.common;
import lombok.Getter;

import java.io.*;
import java.net.*;
import java.util.*;

import main.java.signed_reliable_links.ReliableLink;

/**
 * Manages the network of nodes in the blockchain network.
 */
@Getter
public class NetworkManager {
    private final int id;
    private long sentMessages = 0;
    private final int timeout;
    private final KeyManager keyManager;

    /**
     * Constructor for the NetworkManager class.
     *
     * @param id         the unique identifier for the server
     * @param keyManager for authenticated communication
     */
    public NetworkManager(int id, KeyManager keyManager, int timeout) {
        this.id = id;
        this.keyManager = keyManager;
        this.timeout = timeout;
    }

    public void startCommunications(int serverPort, int clientPort, MessageHandler handler1, MessageHandler handler2, Collection<Node> nodes) {
        startListeningForUDP(serverPort, handler1);
        initiateBlockchainNetwork(serverPort, nodes);
        startListeningForUDP(clientPort, handler2);
    }

    /**
     * Initiates the blockchain network by sending connect messages to other nodes.
     *
     * @param port the port number for the server
     */
    public void initiateBlockchainNetwork(int port, Collection<Node> nodes) {
        long messageId = generateMessageId();
        for (Node node : nodes) {
            if (node.getPort() == port)
                continue;
            sendMessageThread(new Message(messageId, "CONNECT", id), node);
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
                System.out.println("Listening for UDP messages on port " + port + "...");

                while (true) {
                    Message receivedMessage = ReliableLink.receiveMessage(udpSocket);

                    if (receivedMessage != null) {
                        handler.parseReceivedMessage(receivedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Sends a message using authenticated reliable links abstraction
     * in a separate thread.
     *
     * @param message the message to send
     * @param node    the node to send the message to
     */
    public void sendMessageThread(Message message, Node node) {
        new Thread(() -> {
            System.out.println("Sending " + message.getType() + " message to " + node.getIp() + ":" + node.getPort());
            ReliableLink.sendMessage(message, node, keyManager, timeout);
        }).start();
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
