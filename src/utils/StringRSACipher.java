package utils;

import java.io.IOException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

/**
 * Decrypts a string with the RSA algorithm in multiple modes, with a given, appropriate RSA key
 */

public class StringRSACipher {
    public static void main(String[] args) throws IOException {

        if(args.length != 2) {
            System.err.println("This program decrypts a string with RSA.");
            System.err.println("Usage: StringRSADecipher [inputString] [RSAKeyFile] [ECB|CBC|OFB]");
            return;
        }

        final String inputString = args[0];
        final String keyFile = args[1];
        final int opmode = Cipher.ENCRYPT_MODE;

        try{
            Key pubkey = RSAKeyGenerator.read(keyFile, "priv");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            System.out.println(cipher.getProvider().getInfo());

        
            cipher.init(opmode, pubkey);

            // get the bytes from the input string
            byte[] inputStringBytes = inputString.getBytes();

            // apply the manipulationFunction to the byte array
            byte[] outputBytes = cipher.doFinal(inputStringBytes);

            // write the output byte array to disk
            System.out.println(Arrays.toString(outputBytes));
            
        } catch (Exception e) {
            // Pokemon exception handling!
            e.printStackTrace();
        }
    }
}
