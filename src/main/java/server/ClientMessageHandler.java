package main.java.server;

import main.java.common.*;
import main.java.consensus.ConsensusLoop;
import main.java.signed_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles client incoming messages.
 */
public class ClientMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientMessageHandler.class);

    private final Map<Integer, NodeRegistry> clientNodes;
    private final NetworkManager networkManager;
    private final KeyManager keyManager;
    private final ConsensusLoop consensusLoop;

    /**
     * Constructor for the ClientMessageHandler class.
     *
     * @param clientNodes    keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     * @param consensusLoop  to request a block to be added to the blockchain
     */
    public ClientMessageHandler(Map<Integer, NodeRegistry> clientNodes, NetworkManager networkManager, KeyManager keyManager, ConsensusLoop consensusLoop) {
        this.clientNodes = clientNodes;
        this.networkManager = networkManager;
        this.keyManager = keyManager;
        this.consensusLoop = consensusLoop;
    }

    @Override
    public void parseReceivedMessage(Message message, int receiverId) {
        new Thread(() -> {
            NodeRegistry sender = clientNodes.get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, receiverId, keyManager)) {
                return;
            }
            processMessage(message, sender);
        }).start();
    }

    @Override
    public void processMessage(Message message, NodeRegistry sender) {
        logger.info("Processing message: {id:{}, content:\"{}\", type:{}, sender:{}{}}", message.getId(), message.getContent(), message.getType(), sender.getType(), sender.getId());
        boolean firstTime;
        switch (message.getType()) {
            case ACK:
                sender.ackMessage(message.getId()); // do not add the message since it does not have unique id
                break;
            case CLIENT_WRITE:
                firstTime = sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                if (firstTime) consensusLoop.addRequest(message);
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
