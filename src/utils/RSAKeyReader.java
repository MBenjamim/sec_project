package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility class for reading RSA keys from files.
 */
public class RSAKeyReader {

    /**
     * Main method for testing the RSAKeyReader.
     *
     * @param args command line arguments (private key file path and public key file path)
     * @throws Exception if an error occurs while reading the keys
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: RSAKeyGenerator <priv-key-file> <pub-key-file>");
            return;
        }

        final String privKeyPath = args[0];
        final String pubKeyPath = args[1];

        System.out.println("Private Key:");
        PrivateKey privKey = readPrivateKey(privKeyPath);
        System.out.println("Encoded type '" + privKey.getFormat() + "' ..." );
        System.out.println(DataUtils.bytesToHex(privKey.getEncoded()));

        System.out.println("Public Key:");
        PublicKey pubKey = readPublicKey(pubKeyPath);
        System.out.println("Encoded type '" + pubKey.getFormat() + "' ..." );
        System.out.println(DataUtils.bytesToHex(pubKey.getEncoded()));

        System.out.println("Done.");
    }

    /**
     * Reads a private key from a file.
     *
     * @param privKeyPath the path to the private key file
     * @return the private key
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public static PrivateKey readPrivateKey(String privKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = readKey(privKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
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
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Reads a key from a file.
     *
     * @param keyPath the path to the key file
     * @return the key as a byte array
     * @throws IOException if an I/O error occurs
     */
    private static byte[] readKey(String keyPath) throws IOException {
        System.out.println("Reading key from file " + keyPath + " ...");
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }
        return encoded;
    }
}
