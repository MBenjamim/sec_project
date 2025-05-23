package main.java.common;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * Represents a message in the blockchain network.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private static final Logger logger = LoggerFactory.getLogger(Message.class);

    private long id;
    private int sender;
    private MessageType type;
    private String content;
    @JsonIgnore
    private boolean received = false;

    @JsonIgnore
    @ToString.Exclude
    private byte[] authenticationField; // hmac or signature

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long consensusIdx = null;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer epochTS = null;

    /**
     * Constructor for the Message class.
     *
     * @param id      the unique identifier for the message
     * @param type    the type of the message
     * @param sender  the unique identifier for the sender
     */
    public Message(long id, MessageType type, int sender) {
        this.id = id;
        this.type = type;
        this.sender = sender;
        this.content = "";
    }

    /**
     * Constructor for the Message class with content.
     *
     * @param id      the unique identifier for the message
     * @param type    the type of the message
     * @param sender  the unique identifier for the sender
     * @param content the content of the message
     */
    public Message(long id, MessageType type, int sender, String content) {
        this(id, type, sender);
        this.content = content;
    }

    /**
     * Constructor for the Message (consensus) class with content.
     *
     * @param id           the unique identifier for the message
     * @param type         the type of the message
     * @param sender       the unique identifier for the sender
     * @param content      the content of the message
     * @param consensusIdx the consensus instance identifier
     * @param epochTS      the epoch timestamp in the corresponding consensus instance
     */
    public Message(long id, MessageType type, int sender, String content, long consensusIdx, int epochTS) {
        this(id, type, sender, content);
        this.consensusIdx = consensusIdx;
        this.epochTS = epochTS;
    }

    /**
     * Retrieves the properties of the message to be authenticated.
     *
     * @return a string representation of the properties to be authenticated
     */
    @JsonIgnore
    public String getPropertiesToAuthenticate() {
        String propertiesToSign = id + "," + type + "," + content;
        if (consensusIdx != null && epochTS != null) {
            propertiesToSign += "," + consensusIdx + "," + epochTS;
        }
        return propertiesToSign;
    }

    /**
     * Converts the message to a JSON string.
     *
     * @return the JSON string representation of the message
     */
    public String toJson() {
        String json = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            json = objectMapper.writeValueAsString(this);
            if (json != null) {
                objectMapper.readValue(json, Message.class);
            }
            return json;
        } catch (Exception e) {
            logger.error("Failed to convert message to JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Creates a Message object from a JSON string.
     *
     * @param json the JSON string representation of the message
     * @return the Message object
     */
    public static Message fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to message: {}", json, e);
            return null;
        }
    }

    public static Message fromJson(String json, boolean ignoreError) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            //logger.error("Failed to convert JSON to message: {}", json, e);
            return null;
        }
    }

    /**
     * Retrieves the hmac or signature as a Base64 encoded string.
     *
     * @return the Base64 encoded string representation of the hmac or signature
     */
    @JsonProperty("authenticationField")
    public String getAuthenticationFieldBase64() {
        return Base64.getEncoder().encodeToString(authenticationField);
    }

    /**
     * Sets the hmac or signature from a Base64 encoded string.
     *
     * @param authenticationFieldBase64 the Base64 encoded string representation of the authentication field
     */
    @JsonProperty("authenticationField")
    public void setAuthenticationFieldBase64(String authenticationFieldBase64) {
        this.authenticationField = Base64.getDecoder().decode(authenticationFieldBase64);
    }
}
