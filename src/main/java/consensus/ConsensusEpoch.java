package main.java.consensus;

import lombok.Getter;
import lombok.Setter;
import main.java.conditional_collect.ConditionalCollect;
import main.java.conditional_collect.ConditionalCollectImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
@Getter
@Setter
public class ConsensusEpoch {
    private final Map<Integer, Block> written = new HashMap<>();
    private final Map<Integer, Block> accepted = new HashMap<>();

    private static int leaderId; // Set from config and is always the same in this project

    private final int N; // Total number of processes
    private final int F; // Fault tolerance threshold
    private ConditionalCollect collector;

    public ConsensusEpoch(int N, int F) {
        this.N = N;
        this.F = F;
        this.collector = new ConditionalCollectImpl(N, F);
    }

    public void addToCollector(int sender, ConsensusMessage state) {
        collector.addState(sender, state);
    }

    // Lombok does not directly support generating static getter and setter methods for static fields
    public int getLeaderId() {
        return leaderId;
    }

    // Lombok does not directly support generating static getter and setter methods for static fields
    public static void setLeaderId(int leaderId) {
        ConsensusEpoch.leaderId = leaderId;
    }
}
