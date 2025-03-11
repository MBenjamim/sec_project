package main.java.consensus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Base64;

@Getter
@Setter
public class Block {
    private final String value;
    private final int clientId;

    @JsonIgnore
    @ToString.Exclude
    private byte[] clientSignature;

    public Block(String value, int clientId, byte[] clientSignature) {
        this.value = value;
        this.clientId = clientId;
        this.clientSignature = clientSignature;
    }

    public boolean checkValid(int nrClients) {
        return value != null && !value.isBlank() && clientId > 0 && clientId < nrClients;
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
        return value.equals(block.value) && clientId == block.clientId;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
        return Base64.getEncoder().encodeToString(clientSignature);
    }

    /**
     * Sets the signature from a Base64 encoded string.
     *
     * @param signatureBase64 the Base64 encoded string representation of the signature
     */
    @JsonProperty("signature")
    public void setSignatureBase64(String signatureBase64) {
        this.clientSignature = Base64.getDecoder().decode(signatureBase64);
    }
}
