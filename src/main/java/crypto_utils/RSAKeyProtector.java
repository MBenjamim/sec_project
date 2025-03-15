package main.java.crypto_utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for encrypting and decrypting secret keys using RSA encryption.
 */
public class RSAKeyProtector {
    private static final Logger logger = LoggerFactory.getLogger(RSAKeyProtector.class);

    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    /**
     * Encrypts a symmetric secret key using the public key.
     *
     * @param publicKey the public key used for encryption
     * @param key       the symmetric secret key to encrypt
     * @return the encrypted secret key as a byte array
     * @throws NoSuchPaddingException    if the padding scheme is unavailable
     * @throws NoSuchAlgorithmException  if the RSA algorithm is not available
     * @throws InvalidKeyException       if the provided key is invalid
     * @throws IllegalBlockSizeException if the size of the data is incorrect for RSA encryption
     * @throws BadPaddingException       if the padding scheme used in the decryption is invalid
     */
    public static byte[] encryptSecretKey(PublicKey publicKey, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(key.getEncoded());
    }

    /**
     * Decrypts an encrypted secret key using the private key.
     *
     * @param privateKey   the private key used for decryption
     * @param encryptedKey the encrypted secret key as a byte array
     * @return the decrypted secret key as a SecretKey object
     * @throws NoSuchPaddingException    if the padding scheme is unavailable
     * @throws NoSuchAlgorithmException  if the RSA algorithm is not available
     * @throws InvalidKeyException       if the provided key is invalid
     * @throws IllegalBlockSizeException if the size of the data is incorrect for RSA decryption
     * @throws BadPaddingException       if the padding scheme used in the encryption is invalid
     */
    public static SecretKey decryptSecretKey(PrivateKey privateKey, byte[] encryptedKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new SecretKeySpec(cipher.doFinal(encryptedKey), SECRET_KEY_ALGORITHM);
    }
}
