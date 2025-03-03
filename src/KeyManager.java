import utils.RSAAuthenticator;
import utils.RSAKeyReader;

import java.security.*;

public class KeyManager {
    private final NetworkManager networkManager;
    private final String keyDir;
    private PrivateKey privateKey;
    // private PublicKey publicKey;

    public KeyManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.keyDir = "server" + networkManager.getId() + "/";
        try {
            this.privateKey = RSAKeyReader.readPrivateKey(keyDir + "private.key");
            // this.publicKey = RSAKeyReader.readPublicKey(keyDir + "public.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] signMessage(Message message, Node node) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int senderId = networkManager.getId();
        int receiverId = node.getId();
        byte[] messageBytes = message.getPropertiesToSign().getBytes();
        byte[] signature = RSAAuthenticator.signMessage(privateKey, senderId, receiverId, messageBytes);

        message.setSignature(signature);
        return message.toJson().getBytes();
    }

    public boolean verifyMessage(Message message, Node node) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        int senderId = node.getId();
        int receiverId = networkManager.getId();
        byte[] messageBytes = message.getPropertiesToSign().getBytes();
        byte[] signature = message.getSignature();

        return RSAAuthenticator.verifySignature(node.getPublicKey(keyDir), senderId, receiverId, messageBytes, signature);
    }
}