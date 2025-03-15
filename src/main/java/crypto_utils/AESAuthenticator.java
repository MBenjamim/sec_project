package main.java.crypto_utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.util.Arrays;
import javax.crypto.*;

/**
 * Utility class for authenticating and verifying messages using AES.
 */
public class AESAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(AESAuthenticator.class);

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Generates HMAC of a message using the secret key.
     *
     * @param key        the secret key to compute the HMAC with
     * @param senderId   the unique identifier for the sender
     * @param receiverId the unique identifier for the receiver
     * @param message    the message to authenticate
     * @return the computed HMAC as a byte array
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the key is invalid
     */
    public static byte[] generateHmac(SecretKey key, int senderId, int receiverId, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(key);

        mac.update(DataUtils.intToBytes(senderId));
        mac.update(DataUtils.intToBytes(receiverId));
        mac.update(message);

        return mac.doFinal();
    }

    /**
     * Verifies the HMAC of a message by generating the HMAC of the message
     * and comparing it with the expected HMAC.
     *
     * @param key        the secret key to compute the HMAC with
     * @param senderId   the unique identifier for the sender
     * @param receiverId the unique identifier for the receiver
     * @param message    the message to verify
     * @param hmac       the expected HMAC to compare against
     * @return true if the HMAC is valid, false otherwise
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the key is invalid
     */
    public static boolean verifyHmac(SecretKey key, int senderId, int receiverId, byte[] message, byte[] hmac) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] generatedHmac = generateHmac(key, senderId, receiverId, message);
        return Arrays.equals(hmac, generatedHmac);
    }
}
