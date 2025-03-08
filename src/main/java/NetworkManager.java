package main.java;
import lombok.Getter;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Manages the network of nodes in the blockchain network.
 */
@Getter
public class NetworkManager {
    private final Map<Integer, Node> networkNodes = new HashMap<>();

    private static final String CONFIG_FILE = "config.cfg";
    private final int id;
    private long sentMessages = 0;
    private final KeyManager km;

    /**
     * Constructor for the NetworkManager class.
     *
     * @param port     the port number for the server
     * @param serverId the unique identifier for the server
     */
    public NetworkManager(int port, int serverId) {
        this.id = serverId;
        this.km = new KeyManager(this);
        loadNodesFromConfig();
        startListeningForUDP(port);
        initiateBlockchainNetwork(port);
    }

    /**
     * Loads the nodes from the configuration file.
     */
    public void loadNodesFromConfig() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load configuration file: " + CONFIG_FILE);
            e.printStackTrace();
            return;
        }

        int numServers = Integer.parseInt(config.getProperty("NUM_SERVERS", "3"));
        int basePort = Integer.parseInt(config.getProperty("BASE_PORT", "5000"));

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            networkNodes.put(i, new Node(i, "localhost", port));
        }

        System.out.println("Loaded nodes from config:");
        networkNodes.values().forEach(node -> System.out.println(node.getId() + ": " + node.getIp() + ":" + node.getPort()));
    }

    /**
     * Initiates the blockchain network by sending connect messages to other nodes.
     *
     * @param port the port number for the server
     */
    public void initiateBlockchainNetwork(int port) {
        long messageId = generateMessageId();
        for (Node node : networkNodes.values()) {
            if (node.getPort() == port)
                continue;
            sendMessageThread(new Message(messageId, "CONNECT", id), node);
        }
    }

    /**
     * Starts listening for UDP messages on the specified port.
     *
     * @param port the port number to listen on
     */
    public void startListeningForUDP(int port) {
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(port)) {
                System.out.println("Listening for UDP messages on port " + port + "...");

                while (true) {
                    Message receivedMessage = ReliableLink.receiveMessage(udpSocket);

                    if (receivedMessage != null) {
                        parseReceivedMessage(receivedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Parses and processes a received message in a separate thread.
     * Verifies the message (authenticated reliable link) before processing it.
     * Only processes messages from other nodes in a separate thread (for now).
     *
     * @param message the received message
     */
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            Node sender = networkNodes.get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, km)) {
                return;
            }
            processMessage(message, sender);
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
            ReliableLink.sendMessage(message, node, km);
        }).start();
    }

    /**
     * Processes a received message.
     *
     * @param message the message to process
     */
    public void processMessage(Message message, Node sender) {
        System.out.println("Processing message: " + message);
        switch (message.getType()) {
            case "CONNECT":
                sender.addReceivedMessage(message.getId(), message);
                sendMessageThread(new Message(message.getId(), "ACK", id), sender);
                break;
            case "ACK":
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            default:
                System.out.println("Unknown message type: " + message.getType());
                break;
        }
    }

    /**
     * Retrieves the list of network nodes.
     *
     * @return the list of network nodes
     */
    public List<Node> getNetworkNodes() {
        return new ArrayList<>(this.networkNodes.values());
    }

    /**
     * Generates a unique message ID.
     *
     * @return the generated message ID
     */
    public long generateMessageId() {
        return sentMessages++;
    }
}