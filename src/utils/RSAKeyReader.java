package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAKeyReader {

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

    public static PrivateKey readPrivateKey(String privKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = readKey(privKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey readPublicKey(String privKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = readKey(privKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

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
