package main.java;
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
    private final Map<Integer, Node> networkNodes = new HashMap<>();
    private final Map<Integer, Node> networkClients = new HashMap<>();

    private final int id;
    private long sentMessages = 0;
    private int timeout;

    private final KeyManager km;
    private ClientHandler clientHandler;
    private NetworkServerHandler nodeHandler;

    /**
     * Constructor for the NetworkManager class.
     *
     * @param id the unique identifier for the server
     */
    public NetworkManager(int id) {
        this.id = id;
        this.km = new KeyManager(id, "server"); // FIXME
        loadNodesFromConfig();
    }

    public void startCommunications(int serverPort, int clientPort) {
        startListeningForUDP(serverPort, nodeHandler);
        initiateBlockchainNetwork(serverPort);
        startListeningForUDP(clientPort, clientHandler);
    }

    /**
     * Loads the nodes and clients from the configuration file.
     */
    public void loadNodesFromConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePortServers = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");
        this.timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePortServers + i;
            networkNodes.put(i, new Node(i, "server", "localhost", port));
        }
        nodeHandler = new NetworkServerHandler(this, km);

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            networkClients.put(i, new Node(i, "client", "localhost", port));
        }
        clientHandler = new ClientHandler(this, km);

        System.out.println("[CONFIG] Loaded nodes and clients from config:");
        networkNodes.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
        networkClients.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
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
            ReliableLink.sendMessage(message, node, km, timeout);
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
