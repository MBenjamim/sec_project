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
public class TransactionResponse {
    private static final Logger logger = LoggerFactory.getLogger(TransactionResponse.class);

    private long transactionId; // field for client to confirm which of its transaction was processed
    private String blockHash;

    private boolean status;
    private String description;

    private TransactionType transactionType;
    private ReturnType returnType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object result = null;

    @JsonIgnore
    @ToString.Exclude
    private byte[] signature; // field for client to confirm which of its transaction was processed

    @JsonIgnore
    Address clientAddress; // to send back to client (ignore for sending)

    public TransactionResponse(boolean status, ReturnType returnType) {
        this.status = status;
        this.returnType = returnType;
    }

    /**
     * Converts the TransactionResponse to a JSON string.
     */
    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert transaction response to JSON", e);
            return null;
        }
    }

    /**
     * Creates a TransactionResponse object from a JSON string.
     */
    public static TransactionResponse fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, TransactionResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert JSON to TransactionResponse", e);
            return null;
        }
    }

    @JsonIgnore
    public void setTransactionInfo(long transactionId, byte[] signature, TransactionType transactionType) {
        this.transactionId = transactionId;
        this.signature = signature;
        this.transactionType = transactionType;
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
