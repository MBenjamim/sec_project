package main.java.server;

import main.java.common.*;
import main.java.consensus.ConsensusLoop;
import main.java.signed_reliable_links.ReliableLink;
import main.java.utils.Behavior;
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

    private final Behavior behavior;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param networkNodes   keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     * @param consensusLoop  to eventually agree on a block to be added to the blockchain
     */
    public NetworkServerMessageHandler(Map<Integer, NodeRegistry> networkNodes, NetworkManager networkManager, KeyManager keyManager, ConsensusLoop consensusLoop, Behavior behavior) {
        this.networkNodes = networkNodes;
        this.networkManager = networkManager;
        this.keyManager = keyManager;
        this.consensusLoop = consensusLoop;

        //tests
        this.behavior = behavior;
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
        logger.info("Handling message: {id:{}, content:\"{}\", type:{}, sender:{}{}}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());

        switch (this.behavior){
            case CORRECT:
                logger.debug("\n\nI am Correct and I will respond to all server messages\n");
                processMessage(message, sender);
                break;
            case NO_RESPONSE_TO_ALL_SERVERS:
                logger.info("\n\nI am Byzantine and I will not respond to any server message\n");
                return;
            case NO_RESPONSE_TO_LEADER:
                if (sender.getId() == 1){
                    logger.info("\n\nI am Byzantine and I will not respond to the server{} message\n", sender.getId());
                    return;
                }
                processMessage(message, sender);
                break;
            case DELAY:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("Error while delaying message in byzantine process", e);
                }
                break;
            default:
                processMessage(message, sender);
                break;
        }
    }

    public void processMessage(Message message, NodeRegistry sender) {
        boolean firstTime;
        switch (message.getType()) {
            case ACK:
                sender.ackMessage(message.getId()); // do not add the message since it does not have unique id
                break;
            case READ:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) consensusLoop.processReadMessage(message);
                break;
            case STATE:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                consensusLoop.processStateMessage(message);
                break;
            case COLLECTED:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) consensusLoop.processCollectedMessage(message);
                break;
            case WRITE:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) consensusLoop.processWriteMessage(message);
                break;
            case ACCEPT:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) consensusLoop.processAcceptMessage(message);
                break;
            case CONNECT:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                break;
            default:
                logger.error("Unknown message type: {}", message.getType());
                break;
        }
    }
}
