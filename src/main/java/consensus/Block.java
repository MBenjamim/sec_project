package main.java.consensus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Block {
    private static final Logger logger = LoggerFactory.getLogger(Block.class);

    private String value;
    private Integer clientId;

    @JsonIgnore
    @ToString.Exclude
    private byte[] clientSignature;

    public Block(String value, int clientId) {
        this.value = value;
        this.clientId = clientId;
        //this.clientSignature = clientSignature;
    }

    public boolean checkValid(int nrClients) {
        return value != null && clientId != null && !value.isBlank() && clientId > -1 && clientId < nrClients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Block block = (Block) o;
        return value.equals(block.value) && Objects.equals(clientId, block.clientId);
    }

    /**
     * Converts the Block to a JSON string.
     *
     * @return the JSON string representation of the block
     */
    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("Failed to convert block to JSON", e);
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
            logger.error("Failed to convert JSON to block", e);
            return null;
        }
    }

    /**
     * Retrieves the signature as a Base64 encoded string.
     *
     * @return the Base64 encoded string representation of the signature
     */
    @JsonIgnore // for now
    @JsonProperty("signature")
    public String getSignatureBase64() {
        return Base64.getEncoder().encodeToString(clientSignature);
    }

    /**
     * Sets the signature from a Base64 encoded string.
     *
     * @param signatureBase64 the Base64 encoded string representation of the signature
     */
    @JsonIgnore // for now
    @JsonProperty("signature")
    public void setSignatureBase64(String signatureBase64) {
        this.clientSignature = Base64.getDecoder().decode(signatureBase64);
    }
}
