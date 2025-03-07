package main.java;

/**
 * Represents a server in the blockchain network.
 * Listens for client connections and handles them using the ClientHandler class.
 */
public class BlockchainNetworkServer {
    private final int serverId;
    private final int serverPort;
    private final int clientPort;

    private ClientHandler clientHandler;
    private NetworkServerHandler nodeHandler;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId   the unique identifier for the server
     * @param serverPort the port number to listen from servers
     * @param clientPort the port number to listen from clients
     */
    public BlockchainNetworkServer(int serverId, int serverPort, int clientPort) {
        this.serverId = serverId;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
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
        NetworkManager networkManager = new NetworkManager(serverId);
        networkManager.startCommunications(serverPort, clientPort);
    }
}
