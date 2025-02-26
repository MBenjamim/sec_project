import java.io.*;
import java.net.*;
import java.util.*;

public class NetworkManager {
    private final List<Node> networkNodes = new ArrayList<>();

    private static final String CONFIG_FILE = "config.cfg";

    public void loadNodesFromConfig() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load configuration file: " + CONFIG_FILE);
            e.printStackTrace();
            return;
        }

        int numServers = Integer.parseInt(config.getProperty("NUM_SERVERS", "3"));
        int basePort = Integer.parseInt(config.getProperty("BASE_PORT", "5000"));

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            networkNodes.add(new Node("localhost", port));
        }

        System.out.println("Loaded nodes from config:");
        networkNodes.forEach(node -> System.out.println(node.getIp() + ":" + node.getPort()));
    }

    public void initiateBlockchainNetwork(int port) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] message = "Blockchain network initialized".getBytes();
            for (Node node : networkNodes) {
                if(node.port == port) 
                    continue;
                InetAddress address = InetAddress.getByName(node.ip);
                DatagramPacket packet = new DatagramPacket(message, message.length, address, node.port);
                udpSocket.send(packet);
                System.out.println("Sent blockchain network message to " + node.ip + ":" + node.port);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startListeningForUDP(int port) {
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(port)) {
                byte[] buffer = new byte[1024];
    
                System.out.println("Listening for UDP messages on port " + port + "...");
    
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet); // Wait for incoming messages
    
                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received UDP message on port " + port + ": " + receivedMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public NetworkManager(int port){
        loadNodesFromConfig();
        startListeningForUDP(port);    
        initiateBlockchainNetwork(port);
    }

    public List<Node> getNetworkNodes() {
        return this.networkNodes;
    }
}