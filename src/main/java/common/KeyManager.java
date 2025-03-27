package main.java.common;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;

import main.java.blockchain.Transaction;
import main.java.consensus.State;
import main.java.crypto_utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Manages the cryptographic keys and operations for the network.
 */
public class KeyManager {
    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);

    private final int id;
    private PrivateKey privateKey;

    /**
     * Constructor for the KeyManager class.
     *
     * @param id   identifier of the server / client
     * @param type can be either "server" or "client"
     *             The combination of `id` and `type` must be unique together
     */
    public KeyManager(int id, String type) {
        this.id = id;
        try {
            this.privateKey = RSAKeyReader.readPrivateKey(type + id + "/" + "private.key");
        } catch (Exception e) {
            logger.error("Failed to read key files", e);
        }
    }

    /**
     * Authenticates a message using the secret key defined for session;
     * or Signs a message using the private key.
     *
     * @param message the message to authenticate / sign
     * @param node    the node to send the message to
     * @return the authenticated / signed message as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during signing
     * @throws InvalidKeyException      if the key is invalid
     */
    public byte[] authenticateMessage(Message message, NodeRegistry node) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int receiverId = node.getId();
        byte[] messageBytes = message.getPropertiesToAuthenticate().getBytes();

        byte[] authentication;
        if (MessageType.CONNECT.equals(message.getType())) { // only CONNECT messages are signed
            authentication = RSAAuthenticator.signMessage(privateKey, this.id, receiverId, messageBytes);
        } else {
            authentication = AESAuthenticator.generateHmac(node.getSendSessionKey(), this.id, receiverId, messageBytes);
        }

        message.setAuthenticationField(authentication);
        return message.toJson().getBytes();
    }

    /**
     * Verifies the authenticity of a message.
     *
     * @param message    the message to verify
     * @param senderNode the node that sent the message
     * @param receiverId the ID of the node that received the message
     * @return true if the message is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during verification
     * @throws InvalidKeyException      if the key is invalid
     */
    public boolean verifyMessage(Message message, NodeRegistry senderNode, int receiverId) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int senderId = senderNode.getId();
        byte[] messageBytes = message.getPropertiesToAuthenticate().getBytes();
        byte[] authenticationField = message.getAuthenticationField();

        if (MessageType.CONNECT.equals(message.getType())) { // only CONNECT messages are signed
            return RSAAuthenticator.verifySignature(senderNode.getPublicKey(), senderId, receiverId, messageBytes, authenticationField);
        }
        return AESAuthenticator.verifyHmac(senderNode.getRecvSessionKey(), senderId, receiverId, messageBytes, authenticationField);
    }

    /**
     * Signs a state using the private key.
     *
     * @param state        the state to sign
     * @param processId    the ID of the process signing the state
     * @param consensusIdx the index of the consensus instance
     * @param epochTS      the timestamp of the epoch
     * @return the signed state as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during signing
     * @throws InvalidKeyException      if the key is invalid
     */
    public byte[] signState(State state, int processId, long consensusIdx, int epochTS) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] stateBytes = state.getPropertiesToSign().getBytes();

        byte[] signature = RSAAuthenticator.signState(privateKey, processId, consensusIdx, epochTS, stateBytes);

        state.setSignature(signature);
        return state.toJson().getBytes();
    }

    /**
     * Verifies the signature of a state.
     *
     * @param state        the state to verify
     * @param process      the process that signed the state
     * @param consensusIdx the index of the consensus instance
     * @param epochTS      the timestamp of the epoch
     * @return true if the state is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during verification
     * @throws InvalidKeyException      if the key is invalid
     */
    public boolean verifyState(State state, NodeRegistry process, long consensusIdx, int epochTS) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int processId = process.getId();
        byte[] stateBytes = state.getPropertiesToSign().getBytes();
        byte[] signature = state.getSignature();

        return RSAAuthenticator.verifyState(process.getPublicKey(), processId, consensusIdx, epochTS, stateBytes, signature);
    }


    /**
     * Signs a transaction using the private key.
     *
     * @param transaction        the transaction to sign
     * @return the signed transaction as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during signing
     * @throws InvalidKeyException      if the key is invalid
     */
    public byte[] signTransaction(Transaction transaction) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] transactionBytes = transaction.getPropertiesToSign();

        byte[] signature = RSAAuthenticator.signTransaction(privateKey, transactionBytes);

        transaction.setSignature(signature);
        return transaction.toJson().getBytes();
    }

    /**
     * Verifies the signature of a transaction.
     *
     * @param transaction        the transaction to verify
     * @return true if the transaction is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws SignatureException       if an error occurs during verification
     * @throws InvalidKeyException      if the key is invalid
     */
    public boolean verifyTransaction(Transaction transaction, NodeRegistry process) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] transactionBytes = transaction.getPropertiesToSign();
        byte[] signature = transaction.getSignature();

        return RSAAuthenticator.verifyTransaction(process.getPublicKey(), transactionBytes, signature);
    }


    /**
     * Generates a session key and encrypts it using the receiver's public key.
     * Set the session key for the specified node (the receiver).
     *
     * @param node   the node to which the session key will be sent
     * @param twoWay if true indicates the session is two-way, otherwise means it is one-way
     * @return encrypted session key as a Base64-encoded string
     * @throws NoSuchAlgorithmException  if the algorithm is not available
     * @throws NoSuchPaddingException    if the padding scheme is unavailable
     * @throws IllegalBlockSizeException if the size of the data is incorrect for RSA encryption
     * @throws BadPaddingException       if the padding scheme used in the decryption is invalid
     * @throws InvalidKeyException       if the provided key is invalid
     */
    public String generateSessionKey(NodeRegistry node, boolean twoWay) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SecretKey sessionKey = AESKeyGenerator.generateKey();
        node.setRecvSessionKey(sessionKey);
        if (twoWay) node.setSendSessionKey(sessionKey);
        byte[] encryptedKey = RSAKeyProtector.encryptSecretKey(node.getPublicKey(), sessionKey);
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Retrieves the session key from the encrypted message content.
     *
     * @param message the message containing the encrypted key
     * @return session key generated by the node that sent it encrypted
     * @throws NoSuchAlgorithmException  if the algorithm is not available
     * @throws NoSuchPaddingException    if the padding scheme is unavailable
     * @throws IllegalBlockSizeException if the size of the data is incorrect for RSA decryption
     * @throws BadPaddingException       if the padding scheme used in the encryption is invalid
     * @throws InvalidKeyException       if the provided key is invalid
     */
    public SecretKey getSessionKey(Message message) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] encryptedKey = Base64.getDecoder().decode(message.getContent());
        return RSAKeyProtector.decryptSecretKey(privateKey, encryptedKey);
    }
}
