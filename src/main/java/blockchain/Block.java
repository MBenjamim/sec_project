package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import main.java.utils.DataUtils;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Block {
    private static final Logger logger = LoggerFactory.getLogger(Block.class);

    public static final String ALGORITHM = "SHA-256";

    private String blockHash;
    private String previousBlockHash;
    private List<Transaction> transactions = new ArrayList<>();
    @JsonIgnore
    private Address blacklistAddress;
    @JsonIgnore
    private Address tokenAddress;
    @JsonIgnore
    private SimpleWorld world;

    public Block(String previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    @JsonCreator
    public Block() {
        this.world = new SimpleWorld();
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

    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("Failed to convert block to JSON: ", e);
            return null;
        }
    }

    public static Block fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Block.class);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to block: {}", json, e);
            return null;
        }
    }

    @JsonIgnore
    public Collection<MutableAccount> getAccounts() {
        return (Collection<MutableAccount>) world.getTouchedAccounts();
    }

    @JsonIgnore
    public void setAddresses(Address blacklistAddress, Address tokenAddress) {
        this.blacklistAddress = blacklistAddress;
        this.tokenAddress = tokenAddress;
    }

    @JsonProperty("blacklistAddress")
    public String getBlacklistAddressJson() {
        return blacklistAddress.toHexString();
    }

    @JsonProperty("blacklistAddress")
    public void setBlacklistAddressJson(String blacklistAddress) {
        this.blacklistAddress = Address.fromHexString(blacklistAddress);
    }

    @JsonProperty("tokenAddress")
    public String getTokenAddressJson() {
        return tokenAddress.toHexString();
    }

    @JsonProperty("tokenAddress")
    public void setTokenAddressJson(String tokenAddress) {
        this.tokenAddress = Address.fromHexString(tokenAddress);
    }

    @JsonProperty("state")
    public Map<String, Account> getStateJson() {
        Map<String, Account> state =  new HashMap<>();
        for (MutableAccount account : getAccounts()) {
            //logger.info("{}\t{}", account.getAddress().toHexString(), new Account(account));
            state.put(account.getAddress().toHexString(), new Account(account, (account.getCode().equals(Bytes.fromHexString(""))) ? "EOA" : "contract"));
        }
        logger.info("{}\t{}", getAccounts().size(), state.size());
        return state;
    }

    @JsonProperty("state")
    public void setStateJson(Map<String, Account> stateJson) {
        for (Map.Entry<String, Account> entry : stateJson.entrySet()) {
            Address address = Address.fromHexString(entry.getKey());
            MutableAccount account = world.createAccount(address);
            account.setBalance(entry.getValue().getBalance());
            account.setCode(entry.getValue().getCode());
            account.getUpdatedStorage().putAll(entry.getValue().getStorage());
        }
        this.world.updater().commit();
    }
}
