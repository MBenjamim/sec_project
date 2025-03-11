package main.java.client;

import main.java.common.*;
import main.java.consensus.ConsensusLoop;
import main.java.signed_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles server incoming messages.
 */
public class ServerMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);

    private final Map<Integer, NodeRegistry> networkNodes;
    private final NetworkManager networkManager;
    private final KeyManager keyManager;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param networkNodes   keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     */
    public ServerMessageHandler(Map<Integer, NodeRegistry> networkNodes, NetworkManager networkManager, KeyManager keyManager) {
        this.networkNodes = networkNodes;
        this.networkManager = networkManager;
        this.keyManager = keyManager;
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            NodeRegistry sender = networkNodes.get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, keyManager)) {
                return;
            }
            processMessage(message, sender);
        }).start();
    }

    @Override
    public void processMessage(Message message, NodeRegistry sender) {
        logger.debug("Processing message: id:{} content:\"{}\" type:{} sender:{}{}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());
        switch (message.getType()) {
            case CONNECT:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                break;
            case ACK:
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            default:
                logger.debug("Unknown message type: {}", message.getType());
                break;
        }
    }
}
