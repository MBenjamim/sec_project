package main.java.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.hyperledger.besu.datatypes.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    private long transactionId;
    @JsonIgnore
    private Address senderAddress;
    @JsonIgnore
    private Address receiverAddress;

    @JsonProperty
    private String functionSignature; // function signature of the transaction to be

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

    @JsonProperty("senderAddress")
    public String getSenderAddressJson() {
        return this.senderAddress.toHexString();
    }

    @JsonProperty("receiverAddress")
    public String getReceiverAddressJson() {
        return this.receiverAddress.toHexString();
    }

    /**
     * Retrieves the properties of the transaction to be signed.
     *
     * @return a byte array representing the properties to be signed
     */
    @JsonIgnore
    public byte[] getPropertiesToSign() {
        try {
            String propertiesString =  transactionId + "," + receiverAddress.toHexString() + "," + functionSignature;
            return propertiesString.getBytes();
        } catch (Exception e) {
            logger.error("Failed to get properties to sign state", e);
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
