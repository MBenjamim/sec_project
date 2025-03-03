package utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class RSAKeyGenerator {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096;

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

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        System.out.println("Generating " + ALGORITHM + " key ..." );
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        KeyPair keys = keyGen.generateKeyPair();
        System.out.println("Finish generating " + ALGORITHM + " keys");
        return keys;
    }

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