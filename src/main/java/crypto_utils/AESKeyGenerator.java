package main.java.crypto_utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import javax.crypto.*;

/**
 * Utility class for generating AES keys and IVs.
 */
public class AESKeyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AESKeyGenerator.class);

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;
    private static final int IV_SIZE = 16;

    /**
     * Main method for generating AES keys or IVs.
     *
     * @param args command line arguments (mode)
     * @throws Exception if an error occurs while generating keys
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.error("Usage: AESKeyGenerator <key|iv>");
            return;
        }

        final String mode = args[0].toLowerCase();
        switch (mode) {
            case "key":
                logger.info("Secret Key");
                SecretKey key = generateKey();
                logger.info("Encoded type '{}'", key.getFormat());
                logger.info(DataUtils.bytesToHex(key.getEncoded()));
                break;
            case "iv":
                logger.info("Initialization Vector");
                byte[] iv = generateIV();
                logger.info(DataUtils.bytesToHex(iv));
                break;
            default:
                logger.error("Unrecognized mode: {}", mode);
                logger.error("Usage: AESKeyGenerator <key|iv>");
        }
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        logger.info("Generating " + ALGORITHM + " key ..." );
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        SecretKey key = keyGen.generateKey();
        logger.info("Finish generating " + ALGORITHM + " keys");
        return key;
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }
}
