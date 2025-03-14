package main.java.common;

public interface MessageHandler {
    /**
     * Parses and processes a received message in a separate thread.
     * Verifies the message (i.e. uses authenticated reliable link) before processing it.
     *
     * @param message    the received message
     * @param receiverId the ID of the node that received the message
     */
    void parseReceivedMessage(Message message, int receiverId);

    /**
     * Processes incoming messages.
     *
     * @param message the received message
     * @param sender  the node that sent the message
     */
    void handleMessage(Message message, NodeRegistry sender);
}
