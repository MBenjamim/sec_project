package main.java.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import main.java.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Block {
    private static final Logger logger = LoggerFactory.getLogger(Block.class);

    public static final String ALGORITHM = "SHA-256";

    private String blockHash;
    private String previousBlockHash;
    private List<Transaction> transactions = new ArrayList<>();
    private Map<String, Account> state =  new HashMap<>();

    public Block(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    // FIXME: use more parameters for hash (make it less predictable)
    public void hashBlock() {
        StringBuilder txnData = new StringBuilder();

        for (Transaction txn : transactions) {
            txnData.append(txn.toString());
        }
        byte[] bytes = txnData.toString().getBytes();

        try {
            MessageDigest sha256 = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = sha256.digest(bytes);
            this.blockHash = DataUtils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to hash block", e);
        }
    }

    /**
     * Converts the block to a JSON string.
     *
     * @return the JSON string representation of the block
     */
    public String toJson() {
        String json = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            json = objectMapper.writeValueAsString(this);
            if (json != null) {
                objectMapper.readValue(json, Block.class);
            }
            return json;
        } catch (Exception e) {
            logger.error("Failed to convert block to JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Creates a Block object from a JSON string.
     *
     * @param json the JSON string representation of the block
     * @return the Block object
     */
    public static Block fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Block.class);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to block: {}", json, e);
            return null;
        }
    }
}
