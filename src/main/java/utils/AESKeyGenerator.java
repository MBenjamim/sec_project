package main.java.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * Utility class for generating and saving AES keys.
 */
public class AESKeyGenerator {

    /**
     * Main method for generating and saving or loading AES keys.
     *
     * @param args command line arguments (mode and key file path)
     * @throws Exception if an error occurs while generating or saving/loading the keys
     */
    public static void main(String[] args) throws Exception {

        // check args
        if (args.length != 2) {
            System.err.println("Usage: AESKeyGenerator [r|w] <key-file>");
            return;
        }

        final String mode = args[0];
        final String keyPath = args[1];

        if (mode.toLowerCase().startsWith("w")) {
            System.out.println("Generate and save keys");
            write(keyPath);
        } else {
            System.out.println("Load keys");
            read(keyPath);
        }

        System.out.println("Done.");
    }

    /**
     * Generates an AES key and saves it to a file.
     *
     * @param keyPath the path to the key file
     * @throws GeneralSecurityException if a security error occurs
     * @throws IOException if an I/O error occurs
     */
    public static void write(String keyPath) throws GeneralSecurityException, IOException {
        // get an AES private key
        System.out.println("Generating AES key ..." );
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        Key key = keyGen.generateKey();
        System.out.println( "Finish generating AES key" );
        byte[] encoded = key.getEncoded();
        System.out.println("Key:");
        System.out.println(DataUtils.bytesToHex(encoded));

        System.out.println("Writing key to '" + keyPath + "' ...");

        try (FileOutputStream fos = new FileOutputStream(keyPath)) {
            fos.write(encoded);
        }
    }

    /**
     * Reads an AES key from a file.
     *
     * @param keyPath the path to the key file
     * @return the AES key
     * @throws GeneralSecurityException if a security error occurs
     * @throws IOException if an I/O error occurs
     */
    public static Key read(String keyPath) throws GeneralSecurityException, IOException {
        System.out.println("Reading key from file " + keyPath + " ...");
        byte[] encoded;
        try (FileInputStream fis = new FileInputStream(keyPath)) {
            encoded = new byte[fis.available()];
            fis.read(encoded);
        }

        return new SecretKeySpec(encoded, 0, 16, "AES");
    }
}
