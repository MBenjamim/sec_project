package main.java.crypto_utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;

/**
 * Utility class for signing and verifying messages using RSA.
 */
public class RSAAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(RSAAuthenticator.class);

    private static final String ALGORITHM = "SHA256withRSA";

    /**
     * Signs a message using the private key.
     *
     * @param privateKey the private key to sign the message with
     * @param senderId   the unique identifier for the sender
     * @param receiverId the unique identifier for the receiver
     * @param message    the message to sign
     * @return the signed message as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the key is invalid
     * @throws SignatureException       if an error occurs during signing
     */
    public static byte[] signMessage(PrivateKey privateKey, int senderId, int receiverId, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initSign(privateKey);

        signature.update(DataUtils.intToBytes(senderId));
        signature.update(DataUtils.intToBytes(receiverId));
        signature.update(message);

        return signature.sign();
    }

    /**
     * Verifies the signature of a message using the public key.
     *
     * @param publicKey  the public key to verify the signature with
     * @param senderId   the unique identifier for the sender
     * @param receiverId the unique identifier for the receiver
     * @param message    the message to verify
     * @param signature  the signature to verify
     * @return true if the signature is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the key is invalid
     * @throws SignatureException       if an error occurs during verification
     */
    public static boolean verifySignature(PublicKey publicKey, int senderId, int receiverId, byte[] message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature verifier = Signature.getInstance(ALGORITHM);
        verifier.initVerify(publicKey);

        verifier.update(DataUtils.intToBytes(senderId));
        verifier.update(DataUtils.intToBytes(receiverId));
        verifier.update(message);

        return verifier.verify(signature);
    }
}
