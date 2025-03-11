package main.java.server;

import main.java.common.*;
import main.java.consensus.ConsensusLoop;
import main.java.consensus.ConsensusMessage;
import main.java.signed_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles server incoming messages.
 */
public class NetworkServerMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(NetworkServerMessageHandler.class);

    private final Map<Integer, NodeRegistry> networkNodes;
    private final NetworkManager networkManager;
    private final KeyManager keyManager;
    private final ConsensusLoop consensusLoop;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param networkNodes   keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     * @param consensusLoop  to eventually agree on a block to be added to the blockchain
     */
    public NetworkServerMessageHandler(Map<Integer, NodeRegistry> networkNodes, NetworkManager networkManager, KeyManager keyManager, ConsensusLoop consensusLoop) {
        this.networkNodes = networkNodes;
        this.networkManager = networkManager;
        this.keyManager = keyManager;
        this.consensusLoop = consensusLoop;
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
        logger.info("Processing message: {id:{}, content:\"{}\", type:{}, sender:{}{}}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());
        switch (message.getType()) {
            case CONNECT:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                break;
            case READ:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                consensusLoop.processReadMessage((ConsensusMessage) message);
                break;
            case STATE:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                consensusLoop.processStateMessage((ConsensusMessage) message);
                break;
            case ACK:
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            default:
                logger.error("Unknown message type: {}", message.getType());
                break;
        }
    }
}
