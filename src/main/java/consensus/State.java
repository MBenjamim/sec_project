package main.java.consensus;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class State {
    private final Map<Integer, Block> writeSet = new HashMap<>();
    private Block value;
    private int valueTS = -1;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Converts the State to a JSON string.
     */
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a State object from a JSON string.
     */
    public static State fromJson(String json) {
        try {
            return objectMapper.readValue(json, State.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
