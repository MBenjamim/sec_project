package main.java.blockchain;

import main.java.crypto_utils.DataUtils;

import java.security.*;
import java.util.Arrays;

import org.hyperledger.besu.datatypes.Address;

public class AddressGenerator {

    public static final String ALGORITHM = "SHA-256";

    /**
     * Uses the public key to generate the address to use in the blockchain.
     *
     * @param publicKey the public key to generate the address from
     * @return the generated address
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static Address generateAddress(PublicKey publicKey) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance(ALGORITHM);
        byte[] hash = sha256.digest(publicKey.getEncoded());

        // take the first 20 bytes for the address
        byte[] addressBytes = Arrays.copyOfRange(hash, 0, 20);

        return Address.fromHexString(DataUtils.bytesToHex(addressBytes));
    }
}
