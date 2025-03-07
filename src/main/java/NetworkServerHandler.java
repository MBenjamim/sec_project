package main.java;

import main.java.signed_reliable_links.ReliableLink;

/**
 * Handles server incoming messages.
 */
public class NetworkServerHandler implements MessageHandler {
    private final NetworkManager networkManager;
    private final KeyManager km;

    /**
     * Constructor for the NodeHandler class.
     *
     * @param networkManager the network manager instance to receive messages
     * @param km the key manager instance to sign and verify messages
     */
    public NetworkServerHandler(NetworkManager networkManager, KeyManager km) {
        this.networkManager = networkManager;
        this.km = km;
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            Node sender = networkManager.getNetworkNodes().get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, km)) {
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
