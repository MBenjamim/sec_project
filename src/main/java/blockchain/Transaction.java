package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import main.java.common.KeyManager;
import main.java.common.NodeRegistry;
import org.hyperledger.besu.datatypes.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    private long transactionId;
    private String functionSignature; // function signature of the transaction to be
    @JsonIgnore
    private Address senderAddress;

    // argument fields (optional depending on the function to call)
    @JsonIgnore
    private Address ownerAddress = null; // e.g. transferFrom involves 3 addresses
    @JsonIgnore
    private Address receiverAddress = null;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double amount = null;

    @JsonIgnore
    @ToString.Exclude
    private byte[] signature;

    public Transaction(long transactionId, Address senderAddress, Address receiverAddress, String functionSignature, Double amount) {
        this.transactionId = transactionId;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
        this.functionSignature = functionSignature;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Transaction t = (Transaction) o;
        return Objects.equals(transactionId, t.transactionId) && Objects.equals(getSignatureBase64(), t.getSignatureBase64());
    }

    /**
     * Converts the Transaction to a JSON string.
     */
    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert transaction to JSON", e);
            return null;
        }
    }

    /**
     * Creates a Transaction object from a JSON string.
     */
    public static Transaction fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Transaction.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert JSON to Transaction", e);
            return null;
        }
    }

    /**
     * Verifies if transaction only has one address (besides the sender one).
     */
    public boolean hasOnlyAddress() {
        return ownerAddress == null && receiverAddress != null && amount == null;
    }

    /**
     * Verifies if transaction only has one address (besides the sender one),
     * and has the amount field different from null.
     */
    public boolean hasAddressAndAmount() {
        return ownerAddress == null && receiverAddress != null && amount != null;
    }

    /**
     * Verify if transaction is correctly signed, is not repeated and is correctly formed.
     *
     * @param keyManager the key manager to verify the signature
     * @param blockchain to verify transaction signature and check replay attacks
     * @return true if transaction is valid, false otherwise
     */
    @JsonIgnore
    public boolean isValid(Blockchain blockchain, KeyManager keyManager) {
        // get the client that has the public key for the sender address
        NodeRegistry client = blockchain.getClients().get(senderAddress);
        if (client == null || signature == null) return false;

        // check if transaction is not a replay attack
        if (blockchain.checkRepeatedTransaction(getSignatureBase64())) return false;

        try {
            if (!keyManager.verifyTransaction(this, client)) {
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to verify signature for transaction from {}", senderAddress, e);
            return false;
        }

        // verify if function to be called exists
        TransactionType type = blockchain.getTransactionType(functionSignature);
        if (type == null) return false;

        // verify if arguments of the function are correct
        switch (type) { // FIXME - there are more functions
            case ADD_TO_BLACKLIST:
            case IS_BLACKLISTED:
            case REMOVE_FROM_BLACKLIST:
            case BALANCE_OF:
                if (!hasOnlyAddress()) return false;
            case TRANSFER:
                if (!hasAddressAndAmount()) return false;
        }

        return true;
    }

    @JsonProperty("senderAddress")
    public String getSenderAddressJson() {
        return this.senderAddress.toHexString();
    }

    @JsonProperty("senderAddress")
    public void setSenderAddressJson(String address) {
        this.senderAddress = Address.fromHexString(address);
    }

    @JsonProperty("receiverAddress")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getReceiverAddressJson() {
        return (receiverAddress == null) ? null : receiverAddress.toHexString();
    }

    @JsonProperty("receiverAddress")
    public void setReceiverAddressJson(String address) {
        this.receiverAddress = (address == null) ? null : Address.fromHexString(address);
    }

    @JsonProperty("ownerAddress")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getOwnerAddressJson() {
        return (ownerAddress == null) ? null : ownerAddress.toHexString();
    }

    @JsonProperty("ownerAddress")
    public void setOwnerAddressJson(String address) {
        this.ownerAddress = (address == null) ? null : Address.fromHexString(address);
    }

    /**
     * Retrieves the properties of the transaction to be signed.
     *
     * @return a byte array representing the properties to be signed
     */
    @JsonIgnore
    public byte[] getPropertiesToSign() {
        try {
            String propertiesString = transactionId + "," + functionSignature;
            propertiesString += (ownerAddress == null) ? "" : "," + ownerAddress.toHexString();
            propertiesString += (receiverAddress == null) ? "" : "," + receiverAddress.toHexString();
            propertiesString += (amount == null) ? "" : "," + amount;
            return propertiesString.getBytes();
        } catch (Exception e) {
            logger.error("Failed to get properties to sign transaction", e);
            return null;
        }
    }

    /**
     * Retrieves the signature as a Base64 encoded string.
     *
     * @return the Base64 encoded string representation of the signature
     */
    @JsonProperty("signature")
    public String getSignatureBase64() {
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Sets the signature from a Base64 encoded string.
     *
     * @param signatureBase64 the Base64 encoded string representation of the signature
     */
    @JsonProperty("signature")
    public void setSignatureBase64(String signatureBase64) {
        this.signature = Base64.getDecoder().decode(signatureBase64);
    }
}
