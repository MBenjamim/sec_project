package main.java.conditional_collect;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.consensus.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the ConditionalCollect interface.
 * This class is responsible for collecting proposed values from different processes
 * and filtering them based on a specified condition.
 */
public class ConditionalCollectImpl implements ConditionalCollect {
    private static final Logger logger = LoggerFactory.getLogger(ConditionalCollectImpl.class);

    private Map<Integer, State> states = new HashMap<>();
    private final int N; // Total number of processes
    private final int F; // Fault tolerance threshold
    private boolean collected;

    /**
     * Constructor to initialize the ConditionalCollectImpl with the total number of processes (N)
     * and the fault tolerance threshold (F).
     *
     * @param N Total number of processes
     * @param F Fault tolerance threshold
     */
    public ConditionalCollectImpl(int N, int F) {
        this.N = N;
        this.F = F;
        collected = false;
    }

    @Override
    synchronized public void addValue(int processId, State value) {
        if (!collected) {
            states.put(processId, value);
        }
    }

    @Override
    synchronized public String collectValues(int myId) {
        if (collected) return null;
        if (states.size() >= N - F && states.get(myId) != null) {
            String collectedStates = collectionOfStatesToJson(states);
            this.states = new HashMap<>();
            return collectedStates;
        }
        return null;
    }

    @Override
    synchronized public boolean isCollected() {
        return collected;
    }

    @Override
    synchronized public void markAsCollected() {
        collected = true;
    }

    private static String collectionOfStatesToJson(Map<Integer, State> states) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(states);
        } catch (Exception e) {
            logger.error("Failed to convert collected messages map to JSON", e);
            return null;
        }
    }
}
