package main.java.conditional_collect;

import main.java.consensus.State;

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
    void addValue(int processId, State value);

    /**
     * Gets collected states if the number of states is greater than (N - F).
     *
     * @param myId Process ID to make a deterministic choice when leader is correct
     * @return A json representation of a map containing states
     */
    String collectValues(int myId);

    /**
     * Checks if collector received enough and valid states
     *
     * @return true if collected enough states
     */
    boolean isCollected();

    /**
     * Mark as collected after received enough and valid states.
     */
    void markAsCollected();
}
