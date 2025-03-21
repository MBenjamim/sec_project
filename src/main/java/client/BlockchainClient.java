package main.java.client;

import lombok.Getter;
import main.java.common.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a client in the blockchain network.
 * Listens from command line and send messages to the blockchain.
 */
@Getter
public class BlockchainClient {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainClient.class);

    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();

    private final int id;
    private int port;
    private int timeout;

    private final KeyManager keyManager;
    private NetworkManager networkManager;
    private BlockchainConfirmationCollector collector;

    /**
     * Constructor for the BlockchainClient class.
     *
     * @param clientId the unique identifier for the client
     */
    public BlockchainClient(int clientId) {
        this.id = clientId;
        this.keyManager = new KeyManager(id, "client");
    }

    /**
     * The main method to start the client.
     *
     * @param args command line arguments (serverId and serverPort)
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            logger.error("Usage: java BlockchainClient <clientId> <configFile>");
            System.exit(1);
        }

        int clientId = Integer.parseInt(args[0]);
        String configFile = args[1];

        ConfigLoader.getProcessId();

        BlockchainClient client = new BlockchainClient(clientId);
        client.loadConfig(configFile);
        client.networkManager = new NetworkManager(client.id, client.keyManager, client.timeout);
        client.collector = new BlockchainConfirmationCollector(client.networkNodes.size());
        client.start();
    }

    /**
     * Starts the client to listen for command line input and connections from blockchain members.
     */
    public void start() {
        ServerMessageHandler serverMessageHandler = new ServerMessageHandler(this);
        networkManager.startClientCommunications(port, serverMessageHandler, networkNodes.values());

        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit")) {
                    logger.info("Exiting...");
                    System.exit(0);
                    break;
                }
                // Process the received string
                processInput(input);
            } catch (NoSuchElementException e) {
                // This exception is thrown when testing because there is no terminal
            } catch (Exception e) {
                logger.error("Error reading input: {}", e.getMessage());
            }
        }
    }

    private void processInput(String input) {
        if (input == null || input.isBlank()) return;
        logger.debug("Received input: {}", input);

        // Create and send a message to each node with different IDs
        long messageId = networkManager.generateMessageId();
        networkNodes.values().forEach(node -> networkManager.sendMessageThread(new Message(messageId, MessageType.CLIENT_WRITE, id, input), node));
        long timestamp = collector.waitForConfirmation(input);
        logger.info("Value '{}' appended to the blockchain with timestamp {}", input, timestamp);
    }

    /**
     * Loads the server nodes from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig(String configFile) {
        ConfigLoader config = new ConfigLoader(configFile);

        int numServers = config.getIntProperty("NUM_SERVERS");
        int basePort = config.getIntProperty("BASE_PORT_CLIENT_TO_SERVER");
        this.port = config.getIntProperty("BASE_PORT_CLIENTS") + id;
        this.timeout = config.getIntProperty("TIMEOUT");

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        logger.debug("[CONFIG] Loaded nodes from config:");
        networkNodes.values().forEach(node -> logger.debug("[CONFIG] {}{}: {}:{}", node.getType(), node.getId(), node.getIp(), node.getPort()));
    }
}
