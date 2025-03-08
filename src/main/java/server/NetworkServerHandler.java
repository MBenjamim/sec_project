package main.java.server;

import main.java.common.*;
import main.java.signed_reliable_links.ReliableLink;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles server incoming messages.
 */
public class NetworkServerHandler implements MessageHandler {
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
    public NetworkServerHandler(Map<Integer, NodeRegistry> networkNodes, NetworkManager networkManager, KeyManager keyManager) {
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
        System.out.println("Processing message: " + message);
        switch (message.getType()) {
            case "CONNECT":
                sender.addReceivedMessage(message.getId(), message);
                networkManager.sendMessageThread(new Message(message.getId(), "ACK", networkManager.getId()), sender);
                break;
            case "ACK":
                sender.addReceivedMessage(message.getId(), message);
                sender.ackMessage(message.getId());
                break;
            default:
                System.out.println("Unknown message type: " + message.getType());
                break;
        }
    }
}
