package main.java;

/**
 * Represents a server in the blockchain network.
 * Listens for client connections and handles them using the ClientHandler class.
 */
public class BlockchainClient {
    private final int clientId;
    private final int serverPort;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId   the unique identifier for the client
     * @param serverPort the port number to listen from servers
     */
    public BlockchainClient(int clientId, int serverPort) {
        this.clientId = clientId;
        this.serverPort = serverPort;
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
        // TODO
        System.err.println("[TODO]");
    }
}
