package main.java.signed_reliable_links;

import main.java.common.KeyManager;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.common.NodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Manages an authenticated communication link over UDP (simulating fair loss links),
 * ensuring reliable message delivery.
 */
public class ReliableLink {
    private static final Logger logger = LoggerFactory.getLogger(ReliableLink.class);

    private static final int BASE_BUFFER = 4096;
    private static final int MAX_BUFFER = 65536;

    /**
     * Receives a message from a UDP socket and converts the received data into a Message object.
     *
     * @param udpSocket the UDP socket to receive the message from
     * @return the received Message object
     * @throws IOException if an error occurs during packet reception
     */
    public static Message receiveMessage(DatagramSocket udpSocket) throws IOException {
        int bufferSize = BASE_BUFFER;
        byte[] buffer = new byte[bufferSize];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);

        Message message;
        while ((message = Message.fromJson(new String(packet.getData(), 0, packet.getLength()), true)) == null
                && bufferSize <= MAX_BUFFER) {
            bufferSize += BASE_BUFFER;
            buffer = new byte[bufferSize];
            packet = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(packet);
        }

        return message;
    }

    /**
     * Verifies the authenticity of a received message using the KeyManager.
     *
     * @param message the message to verify
     * @param sender  the node that sent the message
     * @param recvId  the ID of the node that received the message
     * @param km      the KeyManager used for verification
     * @return true if the message is valid, false otherwise
     */
    public static boolean verifyMessage(Message message, NodeRegistry sender, int recvId, KeyManager km) {
        try {
            if (sender == null || !km.verifyMessage(message, sender, recvId)) {
                logger.error("Invalid message: {}", message);
                return false;
            }
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            logger.error("Failed to verify message", e);
            return false;
        }
        return true;
    }

    /**
     * Send message and wait for acknowledgment (except for ACK messages).
     *
     * @param message the message to send
     * @param node    the node to send the message to
     * @param km      required KeyManager to sign the message
     * @param timeout number of tries to send the message
     */
    public static void sendMessage(Message message, NodeRegistry node, KeyManager km, int timeout) {
        int relay = 0;
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(node.getIp());
            byte[] messageBytes = km.signMessage(message, node);

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, node.getPort());

            // skip loop if ACK
            if (message.getType().equals(MessageType.ACK)) {
                udpSocket.send(packet);
                logger.debug("Sent {} message to {}:{}", message.getType(), node.getIp(), node.getPort());
                return;
            }

            // ack are only added to received
            node.addSentMessage(message.getId(), message);

            do {
                if (relay > timeout) {
                    logger.error("Timed out waiting for ack for message to {}:{}", node.getIp(), node.getPort());
                    return;
                }

                udpSocket.send(packet);
                logger.debug("Sent {} message to {}:{}\nMessage: {}", message.getType(), node.getIp(), node.getPort(), message);

                try {
                    relay++;
                    Thread.sleep(200L * relay);
                } catch (InterruptedException e) {
                    logger.error("Wait interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } while (!node.checkAckedMessage(message.getId()));

        } catch (IOException e) {
            logger.error("Failed to send message to {}:{}", node.getIp(), node.getPort(), e);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            logger.error("Failed to sign message", e);
        }
    }
}
