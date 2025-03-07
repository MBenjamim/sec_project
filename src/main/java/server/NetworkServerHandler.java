package main.java.server;

import main.java.common.*;
import main.java.signed_reliable_links.ReliableLink;

/**
 * Handles server incoming messages.
 */
public class NetworkServerHandler implements MessageHandler {
    private final ServerState server;
    private final NetworkManager manager;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param serverState the state that keep track of nodes and their message history,
     *                    contains the key manager instance
     */
    public NetworkServerHandler(ServerState serverState) {
        this.server = serverState;
        this.manager = serverState.getNetworkManager();
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            Node sender = server.getNetworkNodes().get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, server.getKeyManager())) {
                return;
            }
            processMessage(message, sender);
        }).start();
    }

    @Override
    public void processMessage(Message message, Node sender) {
        System.out.println("Processing message: " + message);
        switch (message.getType()) {
            case "CONNECT":
                sender.addReceivedMessage(message.getId(), message);
                manager.sendMessageThread(new Message(message.getId(), "ACK", server.getId()), sender);
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
