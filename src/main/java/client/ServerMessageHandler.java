package main.java.client;

import main.java.common.*;
import main.java.authenticated_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles server incoming messages.
 */
public class ServerMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);

    private final Map<Integer, NodeRegistry> networkNodes; // keep track of nodes and their message history
    private final NetworkManager networkManager;           // to send back messages if needed
    private final KeyManager keyManager;                   // to verify authenticity and signatures
    private final BlockchainConfirmationCollector confirmationCollector;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param client needed for this class attributes
     */
    public ServerMessageHandler(BlockchainClient client) {
        this.networkNodes = client.getNetworkNodes();
        this.networkManager = client.getNetworkManager();
        this.keyManager = client.getKeyManager();
        this.confirmationCollector = client.getCollector();
    }

    @Override
    public void parseReceivedMessage(Message message, int receiverId) {
        new Thread(() -> {
            NodeRegistry sender = networkNodes.get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, receiverId, keyManager)) {
                return;
            }
            handleMessage(message, sender);
        }).start();
    }

    @Override
    public void handleMessage(Message message, NodeRegistry sender) {
        logger.debug("Processing message: id:{} content:\"{}\" type:{} sender:{}{}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());
        boolean firstTime;
        switch (message.getType()) {
            case ACK:
                sender.ackMessage(message.getId()); // do not add the message since it does not have unique id
                break;
            case CONNECT:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                if (firstTime) networkManager.createTwoWaySession(message, sender);
                if (sender.getSendSessionKey() != null) { // guarantee that session key is updated
                    networkManager.acknowledgeMessage(message, sender);
                }
                break;
            case DECISION:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) confirmationCollector.collectConfirmation(message);
                break;
            default:
                logger.debug("Unknown message type: {}", message.getType());
                break;
        }
    }
}
