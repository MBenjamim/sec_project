package utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

/**
 * Utility class for generating RSA key pairs and saving them to files.
 */
public class RSAKeyGenerator {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096;

    /**
     * Main method for generating and saving RSA key pairs.
     *
     * @param args command line arguments (private key file path and public key file path)
     * @throws Exception if an error occurs while generating or saving the keys
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: RSAKeyGenerator <priv-key-file> <pub-key-file>");
            return;
        }

        final String privKeyPath = args[0];
        final String pubKeyPath = args[1];

        KeyPair keys = generateKeyPair();
        saveKeys(privKeyPath, pubKeyPath, keys);

        System.out.println("Done.");
    }

    /**
     * Generates an RSA key pair.
     *
     * @return the generated RSA key pair
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        System.out.println("Generating " + ALGORITHM + " key ..." );
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        KeyPair keys = keyGen.generateKeyPair();
        System.out.println("Finish generating " + ALGORITHM + " keys");
        return keys;
    }

    /**
     * Saves the RSA key pair to files.
     *
     * @param privKeyPath the path to the private key file
     * @param pubKeyPath  the path to the public key file
     * @param keys        the RSA key pair to save
     * @throws IOException if an I/O error occurs
     */
    public static void saveKeys(String privKeyPath, String pubKeyPath, KeyPair keys) throws IOException {
        byte[] privKey = keys.getPrivate().getEncoded();
        byte[] pubKey = keys.getPublic().getEncoded();

        System.out.println("Writing Private key to '" + privKeyPath + "' ..." );
        try (FileOutputStream privFos = new FileOutputStream(privKeyPath)) {
            privFos.write(privKey);
        }
        System.out.println("Writing Public key to '" + pubKeyPath + "' ..." );
        try (FileOutputStream pubFos = new FileOutputStream(pubKeyPath)) {
            pubFos.write(pubKey);
        }
    }
}