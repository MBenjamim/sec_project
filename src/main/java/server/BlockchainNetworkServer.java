package main.java.server;

import main.java.common.ConfigLoader;
import main.java.common.KeyManager;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.common.NetworkManager;
import main.java.common.NodeRegistry;
import main.java.consensus.ConsensusEpoch;
import main.java.consensus.ConsensusLoop;
import lombok.Getter;
import main.java.utils.Behavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.exit;
/**
 * Represents a server in the blockchain network.
 * Listens for connections from clients and other blockchain members.
 */
@Getter
public class BlockchainNetworkServer {
    private static final Logger logger = LoggerFactory.getLogger(BlockchainNetworkServer.class);

    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();
    private final Map<Integer, NodeRegistry> networkClients = new HashMap<>();

    private final int id;
    private int serverPort;
    private int clientPort;

    //tests
    private final Behavior behavior;

    private final KeyManager keyManager;
    // the following is set based on config
    private ConsensusLoop consensusLoop;
    private Thread consensusThread;
    private NetworkManager networkManager;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId the unique identifier for the server
     */
    public BlockchainNetworkServer(int serverId, Behavior behavior) {
        this.id = serverId;
        this.keyManager = new KeyManager(id, "server");
        this.behavior = behavior;
    }

    /**
     * The main method to start the server.
     *
     * @param args command line arguments (serverId, serverPort and clientPort)
     */
    public static void main(String[] args) {
        if (args.length > 3 || args.length < 2) {
            logger.error("Usage: java BlockchainNetworkServer <serverId> <configFile> optional: <behavior>");
            exit(1);
        }

        int serverId = Integer.parseInt(args[0]);
        String configFile = args[1];
        Behavior behavior = Behavior.CORRECT; // default behavior

        if (args.length == 3) {
            String behaviorStr = args[2];
            logger.debug("Behavior: {}", behaviorStr);

            try {
                behavior = Behavior.valueOf(behaviorStr);
            } catch (Exception e) {
                logger.error("Invalid behavior: {}", behaviorStr);
                exit(1);
            }
        }

        ConfigLoader.getProcessId();

        BlockchainNetworkServer server = new BlockchainNetworkServer(serverId, behavior);
        server.loadConfig(configFile);
        server.consensusLoop = new ConsensusLoop(server, behavior);
        server.consensusThread = new Thread(server.consensusLoop);
        server.networkManager = new NetworkManager(server.id, server.keyManager);
        server.start();
    }

    /**
     * Starts the server to listen for connections from clients and other blockchain members.
     */
    public void start() {
        NetworkServerMessageHandler networkServerMessageHandler = new NetworkServerMessageHandler(this);
        ClientMessageHandler clientMessageHandler = new ClientMessageHandler(this);
        networkManager.startServerCommunications(serverPort, clientPort, networkServerMessageHandler, clientMessageHandler, networkNodes.values());
        consensusThread.start();
    }

    /**
     * Loads the nodes and clients from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig(String configFile) {
        ConfigLoader config = new ConfigLoader(configFile);

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePortServers = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");

        this.serverPort = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER") + id;
        this.clientPort = config.getIntProperty("BASE_PORT_CLIENT_TO_SERVER") + id;
        ConsensusEpoch.setLeaderId(config.getIntProperty("LEADER_ID"));

        for (int i = 0; i < numServers; i++) {
            int port = basePortServers + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            networkClients.put(i, new NodeRegistry(i, "client", "localhost", port));
        }

        logger.debug("[CONFIG] Loaded nodes and clients from config:");
        networkNodes.values().forEach(node -> logger.debug("[CONFIG] server{}: {}:{}", node.getId(), node.getIp(), node.getPort()));
        networkClients.values().forEach(node -> logger.debug("[CONFIG] client{}: {}:{}", node.getId(), node.getIp(), node.getPort()));
    }

    public void sendConsensusResponse(Message message, int receiverId) {
        NodeRegistry receiver = networkNodes.get(receiverId);
        networkManager.sendMessageThread(message, receiver);
    }

    public void sendReplyToClient(Message message, int clientId) {
        NodeRegistry receiver = networkClients.get(clientId);
        networkManager.sendMessageThread(message, receiver);
    }

    public void broadcastConsensusResponse(long consensusIdx, int epochTS, MessageType type, String content) {
        long messageId = generateMessageId();
        for (NodeRegistry node : networkNodes.values()) {
            Message message = new Message(messageId, type, id, content, consensusIdx, epochTS);
            networkManager.sendMessageThread(message, node);
        }
    }

    synchronized public long generateMessageId() {
        return networkManager.generateMessageId();
    }
}
