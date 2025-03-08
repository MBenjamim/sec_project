package main.java.server;

import main.java.common.ConfigLoader;
import main.java.common.KeyManager;
import main.java.common.NetworkManager;
import main.java.common.NodeRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a server in the blockchain network.
 * Listens for connections from clients and other blockchain members.
 */
public class BlockchainNetworkServer {
    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();
    private final Map<Integer, NodeRegistry> networkClients = new HashMap<>();

    private final int serverPort;
    private final int clientPort;
    private final int id;

    private final KeyManager keyManager;
    private NetworkManager networkManager;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId   the unique identifier for the server
     * @param serverPort the port number to listen from servers
     * @param clientPort the port number to listen from clients
     */
    public BlockchainNetworkServer(int serverId, int serverPort, int clientPort) {
        this.id = serverId;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.keyManager = new KeyManager(id, "server");
    }

    /**
     * The main method to start the server.
     *
     * @param args command line arguments (serverId, serverPort and clientPort)
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java BlockchainNetworkServer <serverId> <serverPort> <clientPort>");
            System.exit(1);
        }

        int serverId = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        int clientPort = Integer.parseInt(args[2]);

        BlockchainNetworkServer server = new BlockchainNetworkServer(serverId, serverPort, clientPort);
        server.loadConfig();
        server.start();
    }

    /**
     * Starts the server to listen for connections from clients and other blockchain members.
     */
    public void start() {
        NetworkServerHandler serverHandler = new NetworkServerHandler(networkNodes, networkManager, keyManager);
        ClientHandler clientHandler = new ClientHandler(networkClients, networkManager, keyManager);
        networkManager.startCommunications(serverPort, clientPort, serverHandler, clientHandler, networkNodes.values());
    }

    /**
     * Loads the nodes and clients from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePortServers = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");
        int timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePortServers + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            networkClients.put(i, new NodeRegistry(i, "client", "localhost", port));
        }

        this.networkManager = new NetworkManager(id, keyManager, timeout);

        System.out.println("[CONFIG] Loaded nodes and clients from config:");
        networkNodes.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
        networkClients.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
    }
}
