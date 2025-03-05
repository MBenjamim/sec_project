package main.java;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Represents a server in the blockchain network.
 * Listens for client connections and handles them using the ClientHandler class.
 */
public class BlockchainNetworkServer {
    private final int serverId;
    private final int serverPort;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId   the unique identifier for the server
     * @param serverPort the port number for the server
     */
    public BlockchainNetworkServer(int serverId, int serverPort) {
        this.serverId = serverId;
        this.serverPort = serverPort;
    }

    /**
     * The main method to start the server.
     *
     * @param args command line arguments (serverId and serverPort)
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java BlockchainNetworkServer <serverId> <serverPort>");
            System.exit(1);
        }

        int serverId = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);

        BlockchainNetworkServer server = new BlockchainNetworkServer(serverId, serverPort);
        server.start();
    }

    /**
     * Starts the server to listen for client connections.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Server ID: " + serverId + " listening on port " + serverPort);

            NetworkManager networkManager = new NetworkManager(serverPort, serverId);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected to Server " + serverId + ": " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket, networkManager)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

