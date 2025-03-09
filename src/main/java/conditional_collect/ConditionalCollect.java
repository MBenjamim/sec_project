package main.java.conditional_collect;

import java.util.Map;

import main.java.consensus.ConsensusMessage;

/**
 * Interface for collecting proposed values from different processes
 * and filtering them based on a specified condition.
 */
public interface ConditionalCollect {
    /**
     * Adds a process state and its signature to the collection.
     *
     * @param processId The ID of the process
     * @param value     The process state and signature
     */
    void addState(int processId, ConsensusMessage value);

    /**
     * Gets collected states if the number of states is greater than (N - F).
     *
     * @return A json representation of a map conatining states
     */
    String collectValues();

    /**
     * Checks if a value meets the specified condition.
     *
     * @param value The value to check
     * @return True if the value meets the condition, false otherwise
     */
    boolean checkCondition(String value);
}
