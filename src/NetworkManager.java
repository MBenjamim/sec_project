import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class NetworkManager {
    private final Map<Integer, Node> networkNodes = new HashMap<>();

    private static final String CONFIG_FILE = "config.cfg";
    private int serverId;
    private int sentMessages = -1; 

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
            networkNodes.put(i, new Node(i, "localhost", port));
        }

        System.out.println("Loaded nodes from config:");
        networkNodes.values().forEach(node -> System.out.println(node.getId() + ": " + node.getIp() + ":" + node.getPort()));
    }

    public void initiateBlockchainNetwork(int port) {
        int messageId = generateMessageId();
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            for (Node node : networkNodes.values()) {
                if(node.getPort() == port) 
                    continue;
                Message message = new Message(messageId, "CONNECT", serverId,"");
                System.out.println("Sending CONNECT message to " + node.getIp() + ":" + node.getPort());
                sendAndAcknowledgeMessageThread(message, node);
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
                    udpSocket.receive(packet);

                    Message receivedMessage = Message.fromJson(new String(packet.getData(), 0, packet.getLength()));
                    
                    if (receivedMessage != null) {
                        processMessage(receivedMessage);
                        System.out.println("Received UDP message on port " + port + ": " + receivedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendAndAcknowledgeMessageThread(Message message, Node node) {
        new Thread(() -> sendAndAcknowledgeMessage(message, node)).start();
    }

    public void sendAndAcknowledgeMessage(Message message, Node node) {
        int relay = 0;
        node.addSentMessage(relay, message);
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(node.getIp());
            byte[] messageBytes = message.toJson().getBytes();
            
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, node.getPort());
            node.addSentMessage(message.getId(), message);

            do{
                udpSocket.send(packet);
                System.out.println("Sent blockchain network message to " + node.getIp() + ":" + node.getPort());
                System.out.println("Message: " + message);

                try {
                    Thread.sleep(200*relay);
                }catch (InterruptedException e) {
                    System.out.println("Wait interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                relay++;
            } while (!message.isReceived()); 

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public NetworkManager(int port, int serverId){
        this.serverId = serverId;
        loadNodesFromConfig();
        startListeningForUDP(port);    
        initiateBlockchainNetwork(port);
    }

    public void processMessage(Message message) {
        System.out.println("Processing message: " + message);
        Node sender = networkNodes.get(message.getSender());
        switch (message.getType()) {
            case "CONNECT":
                if (sender != null) {
                    sender.addReceivedMessage(message.getId(), message);
                    sendMessage(new Message(message.getId(), "ACK", serverId), sender);
                } else {
                    System.out.println("Sender node not found for CONNECT message.");
                }
                break;
            case "ACK":
                if (sender != null) {
                    sender.addReceivedMessage(message.getId(), message);
                    sender.ackMessage(message.getId());
                } else {
                    System.out.println("Sender node not found for ACK message.");
                }
                break;
            default:
                System.out.println("Unknown message type: " + message.getType());
                break;
        }
    }

    public void sendMessage(Message message, Node node) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] messageJson = message.toJson().getBytes();

            InetAddress address = InetAddress.getByName(node.getIp());
            DatagramPacket packet = new DatagramPacket(messageJson, messageJson.length, address, node.getPort());
            udpSocket.send(packet);

            System.out.println("Sent message: " + message.getType() + " to: "+ node.getIp() + ":" + node.getPort());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Node> getNetworkNodes() {
        return this.networkNodes.values().stream().collect(Collectors.toList());
    }

    public int generateMessageId(){
        return sentMessages++;
    }
}