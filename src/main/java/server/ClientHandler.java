package main.java.server;

import main.java.common.KeyManager;
import main.java.common.NetworkManager;
import main.java.common.Node;
import main.java.common.Message;
import main.java.common.MessageHandler;
import main.java.signed_reliable_links.ReliableLink;

/**
 * Handles client incoming messages.
 */
public class ClientHandler implements MessageHandler {
    private final ServerState state;
    private final NetworkManager manager;

    /**
     * Constructor for the ClientHandler class.
     *
     * @param serverState the state that keep track of nodes and their message history,
     *                    contains the key manager instance
     */
    public ClientHandler(ServerState serverState) {
        this.state = serverState;
        this.manager = serverState.getNetworkManager();
    }

    @Override
    public void parseReceivedMessage(Message message) {
        new Thread(() -> {
            Node sender = state.getNetworkClients().get(message.getSender());
            if (!ReliableLink.verifyMessage(message, sender, state.getKeyManager())) {
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
