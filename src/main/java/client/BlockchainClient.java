package main.java.client;

import main.java.common.ConfigLoader;
import main.java.common.MessageHandler;
import main.java.common.NetworkManager;
import main.java.common.Node;
import main.java.server.ClientHandler;
import main.java.server.NetworkServerHandler;

/**
 * Represents a client in the blockchain network.
 * Listens from command line and send messages to the blockchain.
 */
public class BlockchainClient {
    private final int serverPort;

    private final ClientState state;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId   the unique identifier for the client
     * @param serverPort the port number to listen from servers
     */
    public BlockchainClient(int clientId, int serverPort) {
        this.serverPort = serverPort;
        this.state = new ClientState(clientId);
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

        BlockchainClient server = new BlockchainClient(serverId, serverPort);
        server.start();
    }

    /**
     * Starts the client to listen for command line input and connections from blockchain members.
     */
    public void start() {
        int timeout = loadNodesFromConfig();
        NetworkManager networkManager = new NetworkManager(state.getId(), state.getKeyManager(), timeout);
        state.setNetworkManager(networkManager);
        // MessageHandler handler = new MessageHandler();
        // networkManager.startCommunications(serverPort);
    }

    /**
     * Loads the server nodes from the configuration file.
     *
     * @return timeout that will be used during communications
     */
    public int loadNodesFromConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int basePort = config.getIntProperty("BASE_PORT_CLIENT_TO_SERVER");
        int timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            state.putServerNode(i, new Node(i, "server", "localhost", port));
        }

        System.out.println("[CONFIG] Loaded nodes from config:");
        state.getNetworkNodes().values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));

        return timeout;
    }
}
