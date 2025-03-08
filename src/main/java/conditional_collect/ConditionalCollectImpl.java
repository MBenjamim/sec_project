package main.java.conditional_collect;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the ConditionalCollect interface.
 * This class is responsible for collecting proposed values from different processes
 * and filtering them based on a specified condition.
 */
public class ConditionalCollectImpl implements ConditionalCollect {
    private final Map<Integer, String> proposedValues = new HashMap<>();
    private int N; // Total number of processes
    private int F; // Fault tolerance threshold

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
    }

    /**
     * Adds a proposed value from a process to the collection.
     *
     * @param processId The ID of the process proposing the value
     * @param value The proposed value
     */
    @Override
    public void addProposedValue(int processId, String value) {
        proposedValues.put(processId, value);
    }

    /**
     * Collects values from the proposed values that meet the specified condition.
     * Only collects values if the number of proposed values is greater than (N - F).
     *
     * @return A map of process IDs to their collected values
     */
    @Override
    public Map<Integer, String> collectValues() {
        Map<Integer, String> collectedValues = new HashMap<>();
        if (this.proposedValues.size() >= N - F) {
            for (Map.Entry<Integer, String> entry : proposedValues.entrySet()) {
                if (checkCondition(entry.getValue())) {
                    collectedValues.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return collectedValues;
    }

    /**
     * Checks if a value meets the specified condition.
     * In this implementation, the condition is that the value should not be null or empty.
     *
     * @param value The value to check
     * @return True if the value meets the condition, false otherwise
     */
    @Override
    public boolean checkCondition(String value) {
        return value != null && !value.isEmpty();
    }
}
