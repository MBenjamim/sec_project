package main.java.crypto_utils;

import main.java.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility class for reading RSA keys from files.
 */
public class RSAKeyReader {
    private static final Logger logger = LoggerFactory.getLogger(RSAKeyReader.class);

    /**
     * Main method for testing the RSAKeyReader.
     *
     * @param args command line arguments (private key file path and public key file path)
     * @throws Exception if an error occurs while reading the keys
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            logger.error("Usage: RSAKeyGenerator <priv-key-file> <pub-key-file>");
            return;
        }

        final String privKeyPath = args[0];
        final String pubKeyPath = args[1];

        logger.info("Private Key:");
        PrivateKey privKey = readPrivateKey(privKeyPath);
        logger.info("Encoded type '{}' ...", privKey.getFormat());
        logger.info(DataUtils.bytesToHex(privKey.getEncoded()));

        logger.info("Public Key:");
        PublicKey pubKey = readPublicKey(pubKeyPath);
        logger.info("Encoded type '{}' ...", pubKey.getFormat());
        logger.info(DataUtils.bytesToHex(pubKey.getEncoded()));

        logger.info("Done.");
    }

    /**
     * Reads a private key from a file.
     *
     * @param privateKeyPath the path to the private key file
     * @return the private key
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public static PrivateKey readPrivateKey(String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = readKey(privateKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        logger.info("Private key successfully read from file {}", privateKeyPath);
        return privateKey;
    }

    /**
     * Reads a public key from a file.
     *
     * @param pubKeyPath the path to the public key file
     * @return the public key
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public static PublicKey readPublicKey(String pubKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = readKey(pubKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        logger.info("Public key successfully read from file {}", pubKeyPath);
        return pubKey;
    }

    /**
     * Reads a key from a file.
     *
     * @param keyPath the path to the key file
     * @return the key as a byte array
     * @throws IOException if an I/O error occurs
     */
    private static byte[] readKey(String keyPath) throws IOException {
        logger.info("Reading key from file {}...", keyPath);
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }
        return encoded;
    }
}
