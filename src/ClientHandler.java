import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final NetworkManager networkManager;

    public ClientHandler(Socket clientSocket, NetworkManager networkManager) {
        this.clientSocket = clientSocket;
        this.networkManager = networkManager;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}