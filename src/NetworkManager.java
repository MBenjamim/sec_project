import lombok.Getter;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Manages the network of nodes in the blockchain network.
 */
@Getter
public class NetworkManager {
    private final Map<Integer, Node> networkNodes = new HashMap<>();

    private static final String CONFIG_FILE = "config.cfg";
    private final int id;
    private long sentMessages = -1;
    private final KeyManager km;

    /**
     * Constructor for the NetworkManager class.
     *
     * @param port     the port number for the server
     * @param serverId the unique identifier for the server
     */
    public NetworkManager(int port, int serverId) {
        this.id = serverId;
        this.km = new KeyManager(this);
        loadNodesFromConfig();
        startListeningForUDP(port);
        initiateBlockchainNetwork(port);
    }

    /**
     * Loads the nodes from the configuration file.
     */
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

    /**
     * Initiates the blockchain network by sending connect messages to other nodes.
     *
     * @param port the port number for the server
     */
    public void initiateBlockchainNetwork(int port) {
        long messageId = generateMessageId();
        for (Node node : networkNodes.values()) {
            if (node.getPort() == port)
                continue;
            sendConnectMessage(messageId, node);
        }
    }

    /**
     * Starts listening for UDP messages on the specified port.
     *
     * @param port the port number to listen on
     */
    public void startListeningForUDP(int port) {
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(port)) {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                System.out.println("Listening for UDP messages on port " + port + "...");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    if (packet.getLength() > bufferSize) {
                        bufferSize = packet.getLength();
                        buffer = new byte[bufferSize];
                        packet.setData(buffer);
                        udpSocket.receive(packet);
                    }

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

    /**
     * Sends a connect message to the specified node.
     *
     * @param messageId the unique identifier for the message
     * @param node      the node to send the message to
     */
    public void sendConnectMessage(long messageId, Node node) {
        Message connectMessage = new Message(messageId, "CONNECT", id);
        sendAndAcknowledgeMessageThread(connectMessage, node);
    }

    /**
     * Sends a message and waits for an acknowledgment in a separate thread.
     *
     * @param message the message to send
     * @param node    the node to send the message to
     */
    public void sendAndAcknowledgeMessageThread(Message message, Node node) {
        new Thread(() -> {
            System.out.println("Sending CONNECT message to " + node.getIp() + ":" + node.getPort());
            sendAndAcknowledgeMessage(message, node);
        }).start();
    }

    /**
     * Sends a message and waits for an acknowledgment.
     *
     * @param message the message to send
     * @param node    the node to send the message to
     */
    public void sendAndAcknowledgeMessage(Message message, Node node) {
        int relay = 0;
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(node.getIp());
            byte[] messageBytes = km.signMessage(message, node);

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, node.getPort());
            node.addSentMessage(message.getId(), message);

            do {
                udpSocket.send(packet);
                System.out.println("Sent blockchain network message to " + node.getIp() + ":" + node.getPort());
                System.out.println("Message: " + message);

                try {
                    Thread.sleep(200L * relay);
                } catch (InterruptedException e) {
                    System.out.println("Wait interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                relay++;
            } while (!message.isReceived());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to sign message:");
            e.printStackTrace();
        }
    }

    /**
     * Processes a received message.
     *
     * @param message the message to process
     */
    public void processMessage(Message message) {
        System.out.println("Processing message: " + message);
        Node sender = networkNodes.get(message.getSender());
        try {
            if (sender == null || !km.verifyMessage(message, sender)) {
                System.err.println("Invalid message: " + message);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        switch (message.getType()) {
            case "CONNECT":
                sender.addReceivedMessage(message.getId(), message);
                sendMessage(new Message(message.getId(), "ACK", id), sender);
                break;
            case "ACK":
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            default:
                System.out.println("Unknown message type: " + message.getType());
                break;
        }
    }

    /**
     * Sends a message to the specified node.
     *
     * @param message the message to send
     * @param node    the node to send the message to
     */
    public void sendMessage(Message message, Node node) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] messageBytes = km.signMessage(message, node);

            InetAddress address = InetAddress.getByName(node.getIp());
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, node.getPort());
            udpSocket.send(packet);

            System.out.println("Sent message: " + message.getType() + " to: " + node.getIp() + ":" + node.getPort());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to sign message:");
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the list of network nodes.
     *
     * @return the list of network nodes
     */
    public List<Node> getNetworkNodes() {
        return new ArrayList<>(this.networkNodes.values());
    }

    /**
     * Generates a unique message ID.
     *
     * @return the generated message ID
     */
    public long generateMessageId() {
        return sentMessages++;
    }
}