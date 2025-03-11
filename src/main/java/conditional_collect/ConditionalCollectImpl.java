package main.java.conditional_collect;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.consensus.ConsensusMessage;

/**
 * Implementation of the ConditionalCollect interface.
 * This class is responsible for collecting proposed values from different processes
 * and filtering them based on a specified condition.
 */
public class ConditionalCollectImpl implements ConditionalCollect {
    private Map<Integer, ConsensusMessage> states = new HashMap<>();
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
    synchronized public void addState(int processId, ConsensusMessage value) {
        if (!collected) {
            states.put(processId, value);
        }
    }

    @Override
    synchronized public String collectValues() {
        if (collected) return null;
        if (this.states.size() >= N - F) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(states);
                this.states = new HashMap<>(); // reset collected states
                return jsonString;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
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
    /* synchronized public Map<Integer, ConsensusMessage> collectValues() {
        Map<Integer, ConsensusMessage> collectedValues = new HashMap<>();
        if (this.proposedValues.size() >= N - F) {
            for (Map.Entry<Integer, ConsensusMessage> entry : proposedValues.entrySet()) {
                if (checkCondition(entry.getValue())) {
                    collectedValues.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return collectedValues;
    } */

    /* @Override
    public boolean checkCondition(String value) {
        return value != null && !value.isEmpty();
    } */
}
