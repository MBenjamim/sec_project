import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockchainNetworkServer {
    private int serverId;
    private int serverPort;

    public BlockchainNetworkServer(int serverId, int serverPort) {
        this.serverId = serverId;
        this.serverPort = serverPort;
    }

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

