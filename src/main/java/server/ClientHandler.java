package main.java.server;

import main.java.common.*;
import main.java.signed_reliable_links.ReliableLink;

import java.util.Map;

/**
 * Handles client incoming messages.
 */
public class ClientHandler implements MessageHandler {
    private final Map<Integer, NodeRegistry> clientNodes;
    private final NetworkManager networkManager;
    private final KeyManager keyManager;

    /**
     * Constructor for the ClientHandler class.
     *
     * @param clientNodes    keep track of nodes and their message history
     * @param networkManager to send back messages if needed
     * @param keyManager     to verify signatures
     */
    public ClientHandler(Map<Integer, NodeRegistry> clientNodes, NetworkManager networkManager, KeyManager keyManager) {
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
        // TODO
        System.err.println("[TODO]");
    }
}
