package main.java.client;

import main.java.common.ConfigLoader;
import main.java.common.KeyManager;
import main.java.common.NetworkManager;
import main.java.common.NodeRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a client in the blockchain network.
 * Listens from command line and send messages to the blockchain.
 */
public class BlockchainClient {
    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();

    private final int serverPort;
    private final int id;
    private int timeout;

    private final KeyManager keyManager;
    private NetworkManager networkManager;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId   the unique identifier for the client
     * @param serverPort the port number to communicate with servers
     */
    public BlockchainClient(int clientId, int serverPort) {
        this.id = clientId;
        this.serverPort = serverPort;
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

        int serverId = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);

        BlockchainClient client = new BlockchainClient(serverId, serverPort);
        client.loadConfig();
        client.networkManager = new NetworkManager(client.id, client.keyManager, client.timeout);
        client.start();
    }

    /**
     * Starts the client to listen for command line input and connections from blockchain members.
     */
    public void start() {
        // MessageHandler handler = new MessageHandler();
        // networkManager.startCommunications(serverPort);
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
