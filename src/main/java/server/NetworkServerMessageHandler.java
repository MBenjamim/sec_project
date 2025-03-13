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
            processMessage(message, sender);
        }).start();
    }

    @Override
    public void processMessage(Message message, NodeRegistry sender) {
        logger.info("Processing message: {id:{}, content:\"{}\", type:{}, sender:{}{}}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());

        if (this.behavior == Behavior.CORRECT) {
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
        }else {
            switch (this.behavior){
                case NO_RESPONSE_TO_ALL_SERVERS:
                    logger.info("I am Byzantine and I will not respond to the message");
                    return;
                case DELAY:
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Error while delaying message in byzantine process", e);
                    }
                    break;
            }
        }
    }
}
