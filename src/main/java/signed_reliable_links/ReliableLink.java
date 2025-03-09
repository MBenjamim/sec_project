package main.java.signed_reliable_links;

import main.java.common.KeyManager;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.common.NodeRegistry;

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
    /**
     * Receives a message from a UDP socket and converts the received data into a Message object.
     *
     * @param udpSocket the UDP socket to receive the message from
     * @return the received Message object
     * @throws IOException if an error occurs during packet reception
     */
    public static Message receiveMessage(DatagramSocket udpSocket) throws IOException {
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);

        if (packet.getLength() > bufferSize) {
            bufferSize = packet.getLength();
            buffer = new byte[bufferSize];
            packet.setData(buffer);
            udpSocket.receive(packet);
        }

        return Message.fromJson(new String(packet.getData(), 0, packet.getLength()));
    }

    /**
     * Verifies the authenticity of a received message using the KeyManager.
     *
     * @param message the message to verify
     * @param sender  the node that sent the message
     * @param km      the KeyManager used for verification
     * @return true if the message is valid, false otherwise
     */
    public static boolean verifyMessage(Message message, NodeRegistry sender, KeyManager km) {
        try {
            if (sender == null || !km.verifyMessage(message, sender)) {
                System.err.println("[ERROR] Invalid message: " + message);
                return false;
            }
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            System.err.println("[ERROR] Failed to verify message");
            e.printStackTrace();
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
                System.out.println("Sent " + message.getType() + " message to " + node.getIp() + ":" + node.getPort());
                return;
            }

            // ack are only added to received
            node.addSentMessage(message.getId(), message);

            do {
                if (relay > timeout) {
                    System.err.println("[ERROR] Timed out waiting for ack for message to " + node.getIp() + ":" + node.getPort());
                }

                udpSocket.send(packet);
                System.out.println("Sent " + message.getType() +
                        " message to " + node.getIp() + ":" + node.getPort() + "\n" + "Message: " + message);

                try {
                    Thread.sleep(200L * relay);
                } catch (InterruptedException e) {
                    System.out.println("[ERROR] Wait interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                relay++;
            } while (!node.checkAckedMessage(message.getId()));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            System.err.println("[ERROR] Failed to sign message...");
            e.printStackTrace();
        }
    }
}
