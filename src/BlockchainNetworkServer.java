import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockchainNetworkServer {
    public static void main(String[] args) {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        NetworkManager networkManager;
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Blockchain network server listening on port " + serverPort);
            
            networkManager = new NetworkManager(serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket, networkManager)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
