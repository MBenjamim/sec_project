import java.io.*;
import java.net.*;

/**
 * Handles client connections and processes incoming messages.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final NetworkManager networkManager;

    /**
     * Constructor for the ClientHandler class.
     *
     * @param clientSocket   the socket for the client connection
     * @param networkManager the network manager instance
     */
    public ClientHandler(Socket clientSocket, NetworkManager networkManager) {
        this.clientSocket = clientSocket;
        this.networkManager = networkManager;
    }

    /**
     * Runs the client handler to process incoming messages.
     */
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}