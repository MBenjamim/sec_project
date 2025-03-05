package main.java;
import java.security.*;

import main.java.utils.RSAAuthenticator;
import main.java.utils.RSAKeyReader;

/**
 * Manages the cryptographic keys and operations for the network.
 */
public class KeyManager {
    private final NetworkManager networkManager;
    private final String keyDir;
    private PrivateKey privateKey;
    // private PublicKey publicKey;

    /**
     * Constructor for the KeyManager class.
     *
     * @param networkManager the network manager instance
     */
    public KeyManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.keyDir = "server" + networkManager.getId() + "/";
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
        int senderId = networkManager.getId();
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
        int receiverId = networkManager.getId();
        byte[] messageBytes = message.getPropertiesToSign().getBytes();
        byte[] signature = message.getSignature();

        return RSAAuthenticator.verifySignature(node.getPublicKey(keyDir), senderId, receiverId, messageBytes, signature);
    }
}