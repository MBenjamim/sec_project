package main.java.server;

import main.java.common.*;
import main.java.consensus.ConsensusLoop;
import main.java.authenticated_reliable_links.ReliableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles client incoming messages.
 */
public class ClientMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientMessageHandler.class);

    private final Map<Integer, NodeRegistry> clientNodes; // keep track of nodes and their message history
    private final NetworkManager networkManager;          // to send back messages if needed
    private final KeyManager keyManager;                  // to verify authenticity and signatures
    private final ConsensusLoop consensusLoop;            // to request a block to be added to the blockchain

    /**
     * Constructor for the ClientMessageHandler class.
     *
     * @param server needed for this class attributes
     */
    public ClientMessageHandler(BlockchainNetworkServer server) {
        this.clientNodes = server.getNetworkClients();
        this.networkManager = server.getNetworkManager();
        this.keyManager = server.getKeyManager();
        this.consensusLoop = server.getConsensusLoop();
    }

    @Override
    public void parseReceivedMessage(Message message, int receiverId) {
        new Thread(() -> {
            NodeRegistry sender = clientNodes.get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, receiverId, keyManager)) {
                return;
            }
            handleMessage(message, sender);
        }).start();
    }

    @Override
    public void handleMessage(Message message, NodeRegistry sender) {
        logger.info("Handling message: {id:{}, type:{}, sender:{}{}}", message.getId(), message.getType(), sender.getType(), sender.getId());
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
                firstTime = sender.addReceivedMessage(message.getId(), message);
                if (firstTime) networkManager.createTwoWaySession(message, sender);
                if (sender.getSendSessionKey() != null) { // guarantee that session key is updated
                    networkManager.acknowledgeMessage(message, sender);
                }
                break;
            default:
                logger.error("Unknown message type: {}", message.getType());
                break;
        }
    }
}
