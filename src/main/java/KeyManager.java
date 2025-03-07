package main.java;
import java.security.*;

import main.java.crypto_utils.*;

/**
 * Manages the cryptographic keys and operations for the network.
 */
public class KeyManager {
    private final int id;
    private PrivateKey privateKey;
    // private PublicKey publicKey;

    /**
     * Constructor for the KeyManager class.
     *
     * @param id   identifier of the server / client
     * @param type can be either "server" or "client"
     *             The combination of `id` and `type` must be unique together
     */
    public KeyManager(int id, String type) {
        this.id = id;
        String keyDir = type + id + "/";
        try {
            this.privateKey = RSAKeyReader.readPrivateKey(keyDir + "private.key");
            // this.publicKey = RSAKeyReader.readPublicKey(keyDir + "public.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Signs a message using the private key.
     *
     * @param message the message to sign
     * @param node    the node to send the message to
     * @return the signed message as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during signing
     * @throws InvalidKeyException      if the key is invalid
     */
    public byte[] signMessage(Message message, Node node) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int senderId = id;
        int receiverId = node.getId();
        byte[] messageBytes = message.getPropertiesToSign().getBytes();
        byte[] signature = RSAAuthenticator.signMessage(privateKey, senderId, receiverId, messageBytes);

        message.setSignature(signature);
        return message.toJson().getBytes();
    }

    /**
     * Verifies the signature of a message.
     *
     * @param message the message to verify
     * @param node    the node that sent the message
     * @return true if the signature is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during verification
     * @throws InvalidKeyException      if the key is invalid
     */
    public boolean verifyMessage(Message message, Node node) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int senderId = node.getId();
        int receiverId = id;
        byte[] messageBytes = message.getPropertiesToSign().getBytes();
        byte[] signature = message.getSignature();

        return RSAAuthenticator.verifySignature(node.getPublicKey(), senderId, receiverId, messageBytes, signature);
    }
}
