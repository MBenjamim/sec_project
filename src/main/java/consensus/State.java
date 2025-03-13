package main.java.consensus;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
@Getter
@NoArgsConstructor
public class State {
    private static final Logger logger = LoggerFactory.getLogger(State.class);

    private final Map<Integer, Block> writeSet = new HashMap<>();
    private Block value;
    private int valueTS = -1;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public boolean checkValid(int nrClients) {
        return value != null && value.checkValid(nrClients);
    }

    /**
     * Converts the State to a JSON string.
     */
    public String toJson() {
        try {
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
            return objectMapper.readValue(json, State.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert JSON to state", e);
            return null;
        }
    }
}
