package main.java.common;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
    private long id;
    private int sender;
    private MessageType type;
    private String content;
    private boolean received;

    @JsonIgnore
    @ToString.Exclude
    private byte[] signature;

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
        this.received = false;
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
     * Retrieves the properties of the message to be signed.
     *
     * @return a string representation of the properties to be signed
     */
    @JsonIgnore
    public String getPropertiesToSign() {
        return id + "," + type + "," + content;
    }

    /**
     * Converts the message to a JSON string.
     *
     * @return the JSON string representation of the message
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
