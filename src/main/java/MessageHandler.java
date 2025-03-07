package main.java;

public interface MessageHandler {
    /**
     * Parses and processes a received message in a separate thread.
     * Verifies the message (i.e. uses authenticated reliable link) before processing it.
     *
     * @param message the received message
     */
    void parseReceivedMessage(Message message);

    /**
     * Processes incoming messages.
     *
     * @param message the received message
     * @param sender  the node that sent the message
     */
    void processMessage(Message message, Node sender);
}
