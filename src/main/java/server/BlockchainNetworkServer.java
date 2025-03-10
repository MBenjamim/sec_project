package main.java.server;

import main.java.common.ConfigLoader;
import main.java.common.KeyManager;
import main.java.common.Message;
import main.java.common.NetworkManager;
import main.java.common.NodeRegistry;
import main.java.consensus.ConsensusEpoch;
import main.java.consensus.ConsensusLoop;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a server in the blockchain network.
 * Listens for connections from clients and other blockchain members.
 */
@Getter
public class BlockchainNetworkServer {
    private final Map<Integer, NodeRegistry> networkNodes = new HashMap<>();
    private final Map<Integer, NodeRegistry> networkClients = new HashMap<>();

    private final int serverPort;
    private final int clientPort;
    private final int id;
    private int timeout;

    private final KeyManager keyManager;
    private final ConsensusLoop consensusLoop;
    private NetworkManager networkManager;

    /**
     * Constructor for the BlockchainNetworkServer class.
     *
     * @param serverId   the unique identifier for the server
     * @param serverPort the port number to listen from servers
     * @param clientPort the port number to listen from clients
     */
    public BlockchainNetworkServer(int serverId, int serverPort, int clientPort) {
        this.id = serverId;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.keyManager = new KeyManager(id, "server");
        this.consensusLoop = new ConsensusLoop(this);
    }

    /**
     * The main method to start the server.
     *
     * @param args command line arguments (serverId, serverPort and clientPort)
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java BlockchainNetworkServer <serverId> <serverPort> <clientPort>");
            System.exit(1);
        }

        int serverId = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        int clientPort = Integer.parseInt(args[2]);

        BlockchainNetworkServer server = new BlockchainNetworkServer(serverId, serverPort, clientPort);
        server.loadConfig();
        server.networkManager = new NetworkManager(server.id, server.keyManager, server.timeout);

        server.start();
    }

    /**
     * Starts the server to listen for connections from clients and other blockchain members.
     */
    public void start() {
        NetworkServerMessageHandler networkServerMessageHandler = new NetworkServerMessageHandler(networkNodes, networkManager, keyManager, consensusLoop);
        ClientMessageHandler clientMessageHandler = new ClientMessageHandler(networkClients, networkManager, keyManager);
        networkManager.startServerCommunications(serverPort, clientPort, networkServerMessageHandler, clientMessageHandler, networkNodes.values());
        consensusLoop.run();
    }

    /**
     * Loads the nodes and clients from the configuration file.
     * Creates the NetworkManager.
     */
    public void loadConfig() {
        ConfigLoader config = new ConfigLoader();

        int numServers = config.getIntProperty("NUM_SERVERS");
        int numClients = config.getIntProperty("NUM_CLIENTS");
        int basePortServers = config.getIntProperty("BASE_PORT_SERVER_TO_SERVER");
        int basePortClients = config.getIntProperty("BASE_PORT_CLIENTS");

        this.timeout = config.getIntProperty("TIMEOUT");
        ConsensusEpoch.setLeaderId(config.getIntProperty("LEADER_ID"));

        for (int i = 0; i < numServers; i++) {
            int port = basePortServers + i;
            networkNodes.put(i, new NodeRegistry(i, "server", "localhost", port));
        }

        for (int i = 0; i < numClients; i++) {
            int port = basePortClients + i;
            networkClients.put(i, new NodeRegistry(i, "client", "localhost", port));
        }

        System.out.println("[CONFIG] Loaded nodes and clients from config:");
        networkNodes.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
        networkClients.values().forEach(node -> System.out.println("[CONFIG]" + node.getId() + ": " + node.getIp() + ":" + node.getPort()));
    }

    public void sendConsensusResponse(Message message, int receiverId){
        NodeRegistry receiver = networkNodes.get(receiverId);
        networkManager.sendMessageThread(message, receiver);
    }

    synchronized public long generateMessageId() {
        return networkManager.generateMessageId();
    }
}
