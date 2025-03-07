package main.java;

import main.java.signed_reliable_links.ReliableLink;

/**
 * Handles client incoming messages.
 */
public class ClientHandler implements MessageHandler {
    private final NetworkManager networkManager;
    private final KeyManager km;


    /**
     * Constructor for the ClientHandler class.
     *
     * @param networkManager the network manager instance to receive messages
     * @param km the key manager instance to sign and verify messages
     */
    public ClientHandler(NetworkManager networkManager, KeyManager km) {
        this.networkManager = networkManager;
        this.km = km;
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            Node sender = networkManager.getNetworkClients().get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, km)) {
                return;
            }
            processMessage(message, sender);
        }).start();
    }

    @Override
    public void processMessage(Message message, Node sender) {
        // TODO
        System.err.println("[TODO]");
    }
}
