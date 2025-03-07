package main.java.server;

import main.java.common.ConfigLoader;
import main.java.common.NetworkManager;
import main.java.common.Node;

/**
 * Represents a server in the blockchain network.
 * Listens for connections from clients and other blockchain members.
 */
public class BlockchainNetworkServer {
    private final int serverPort;
    private final int clientPort;

    private final ServerState state;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId   the unique identifier for the server
     * @param serverPort the port number to listen from servers
     * @param clientPort the port number to listen from clients
     */
    public BlockchainNetworkServer(int serverId, int serverPort, int clientPort) {
        this.serverPort = serverPort;
        this.clientPort = clientPort;

        this.state = new ServerState(serverId);
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
        server.start();
    }

    /**
     * Starts the server to listen for connections from clients and other blockchain members.
     */
    public void start() {
        int timeout = loadNodesFromConfig();
        NetworkManager networkManager = new NetworkManager(state.getId(), state.getKeyManager(), timeout);
        state.setNetworkManager(networkManager);
        NetworkServerHandler serverHandler = new NetworkServerHandler(state);
        ClientHandler clientHandler = new ClientHandler(state);
        networkManager.startCommunications(serverPort, clientPort, serverHandler, clientHandler, state.getNetworkNodes().values());
    }

    /**
     * Loads the nodes and clients from the configuration file.
     *
     * @return timeout that will be used during communications
     */
    public int loadNodesFromConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePortServers = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");
        int timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePortServers + i;
            state.putServerNode(i, new Node(i, "server", "localhost", port));
        }

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            state.putClientNode(i, new Node(i, "client", "localhost", port));
        }

        System.out.println("[CONFIG] Loaded nodes and clients from config:");
        state.getNetworkNodes().values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
        state.getNetworkClients().values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));

        return timeout;
    }
}
