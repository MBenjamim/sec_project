import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Base64;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private long id;
    private int sender;
    private String type;
    private String content;
    private boolean received;

    @JsonIgnore
    @ToString.Exclude
    private byte[] signature;

    public Message(long id, String type, int sender) {
        this.id = id;
        this.type = type;
        this.received = false;
        this.sender = sender;
        this.content = "";
    }

    public Message(long id, String type, int sender, String content) {
        this(id, type, sender);
        this.content = content;
    }

    @JsonIgnore
    public String getPropertiesToSign() {
        return id + "," + type + "," + content;
    }
    
    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }   

    public static Message fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @JsonProperty("signature") // Serialize as Base64 string
    public String getSignatureBase64() {
        return Base64.getEncoder().encodeToString(signature);
    }

    @JsonProperty("signature") // Deserialize from Base64 string
    public void setSignatureBase64(String signatureBase64) {
        this.signature = Base64.getDecoder().decode(signatureBase64);
    }
}