package main.java.server;

import main.java.common.*;
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

    /**
     * Constructor for the ClientMessageHandler class.
     *
     * @param clientNodes    keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     */
    public ClientMessageHandler(Map<Integer, NodeRegistry> clientNodes, NetworkManager networkManager, KeyManager keyManager) {
        this.clientNodes = clientNodes;
        this.networkManager = networkManager;
        this.keyManager = keyManager;
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            NodeRegistry sender = clientNodes.get(message.getSender());
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
                networkManager.sendMessageThread(new Message(message.getId(), MessageType.ACK, networkManager.getId()), sender);
                break;
            case ACK:
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            case CLIENT_WRITE:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.sendMessageThread(new Message(message.getId(), MessageType.ACK, networkManager.getId()), sender);
                //TODO process client write
                break;
            default:
                logger.error("Unknown message type: {}", message.getType());
                break;
        }
    }
}
