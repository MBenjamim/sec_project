import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private int id;
    private int sender;
    private String type;
    private String content;
    private boolean received; 

    public Message(int id, String type, int sender) {
        this.id = id;
        this.type = type;
        this.received = false;
        this.sender = sender;
    }

    public Message(int id, String type, int sender, String content){
        this(id, type, sender);
        this.content = content;    
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

    // Convert JSON back to object
    public static Message fromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}