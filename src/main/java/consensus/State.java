package main.java.consensus;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private static final Logger logger = LoggerFactory.getLogger(State.class);

    private final Map<Integer, String> writeSet = new HashMap<>();
    private String value;
    private int valueTS = -1;

    @JsonIgnore
    @ToString.Exclude
    private byte[] signature;

    /**
     * Retrieves the properties of the state to be signed.
     *
     * @return a string representation of the properties to be signed
     */
    @JsonIgnore
    public String getPropertiesToSign() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return valueTS + "," + value + "," + objectMapper.writeValueAsString(writeSet);
        } catch (JsonProcessingException e) {
            logger.error("Failed to get properties to sign state", e);
            return null;
        }
    }

    /**
     * Converts the State to a JSON string.
     */
    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert state to JSON", e);
            return null;
        }
    }

    /**
     * Creates a State object from a JSON string.
     */
    public static State fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, State.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert JSON to state", e);
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
