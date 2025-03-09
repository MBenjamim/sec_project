package main.java.client;

import main.java.common.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Represents a client in the blockchain network.
 * Listens from command line and send messages to the blockchain.
 */
public class BlockchainClient {
    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();

    private final int port;
    private final int id;
    private int timeout;

    private final KeyManager keyManager;
    private NetworkManager networkManager;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId   the unique identifier for the client
     * @param port the port number to communicate with servers
     */
    public BlockchainClient(int clientId, int port) {
        this.id = clientId;
        this.port = port;
        this.keyManager = new KeyManager(id, "client");
    }

    /**
     * The main method to start the client.
     *
     * @param args command line arguments (serverId and serverPort)
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java BlockchainClient <clientId> <serverPort>");
            System.exit(1);
        }

        int clientId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        System.out.println("Initing Client with serverId: " + clientId + " and serverPort: " + port);
        BlockchainClient client = new BlockchainClient(clientId, port);
        client.loadConfig();
        client.networkManager = new NetworkManager(client.id, client.keyManager, client.timeout);
        client.start();
    }

    /**
     * Starts the client to listen for command line input and connections from blockchain members.
     */
    public void start() {
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler(networkNodes, networkManager, keyManager);
        networkManager.startClientCommunications(port, serverMessageHandler, networkNodes.values());

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                System.exit(0);
                break;
            }
            // Process the received string
            processInput(input);
        }
    }

    private void processInput(String input) {
        System.out.println("Received input: " + input);

        // Create and send a message to each node with different IDs
        long messageId = networkManager.generateMessageId();
        networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(messageId, MessageType.CLIENT_WRITE, id, input), node));
    }

    /**
     * Loads the server nodes from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int basePort = config.getIntProperty("BASE_PORT_CLIENT_TO_SERVER");
        this.timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        System.out.println("[CONFIG] Loaded nodes from config:");
        networkNodes.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
    }
}
