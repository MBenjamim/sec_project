package utils;

import java.nio.ByteBuffer;
import java.security.*;

public class RSAAuthenticator {

    public static byte[] signMessage(PrivateKey privateKey, int senderId, int receiverId, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        signature.update(intToBytes(senderId));
        signature.update(intToBytes(receiverId));
        signature.update(message);

        return signature.sign();
    }

    public static boolean verifySignature(PublicKey publicKey, int senderId, int receiverId, byte[] message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);

        verifier.update(intToBytes(senderId));
        verifier.update(intToBytes(receiverId));
        verifier.update(message);

        return verifier.verify(signature);
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

}
