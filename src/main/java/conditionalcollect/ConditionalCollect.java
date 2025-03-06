package main.java.conditionalcollect;

import java.util.Map;

/**
 * Interface for collecting proposed values from different processes
 * and filtering them based on a specified condition.
 */
public interface ConditionalCollect {
    /**
     * Adds a proposed value from a process to the collection.
     *
     * @param processId The ID of the process proposing the value
     * @param value The proposed value
     */
    void addProposedValue(int processId, String value);

    /**
     * Collects values from the proposed values that meet the specified condition.
     *
     * @return A map of process IDs to their collected values
     */
    Map<Integer, String> collectValues();

    /**
     * Checks if a value meets the specified condition.
     *
     * @param value The value to check
     * @return True if the value meets the condition, false otherwise
     */
    boolean checkCondition(String value);
}
