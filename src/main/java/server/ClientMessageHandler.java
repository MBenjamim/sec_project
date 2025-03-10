package main.java.server;

import main.java.common.*;
import main.java.signed_reliable_links.ReliableLink;

import java.util.Map;

/**
 * Handles client incoming messages.
 */
public class ClientMessageHandler implements MessageHandler {
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
        System.out.println("Processing message: id:" + message.getId() + " content:" + "\"" + message.getContent() + "\"" + " type:" + message.getType() + " sender:" + sender.getType() + sender.getId());
        switch (message.getType()) {
            case CONNECT:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                break;
            case ACK:
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            case CLIENT_WRITE:
                sender.addReceivedMessage(message.getId(), message);
                networkManager.acknowledgeMessage(message, sender);
                //TODO process client write
                break;
            default:
                System.out.println("Unknown message type: " + message.getType());
                break;
        }
    }
}
