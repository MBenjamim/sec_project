package main.java.consensus;

import lombok.Getter;
import lombok.Setter;
import main.java.common.Message;
import main.java.conditional_collect.ConditionalCollect;
import main.java.conditional_collect.ConditionalCollectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
@Getter
@Setter
public class ConsensusEpoch {
    private static final Logger logger = LoggerFactory.getLogger(ConsensusEpoch.class);
    private Map<Integer, Block> written = new HashMap<>();
    private Map<Integer, Block> accepted = new HashMap<>();

    private static int leaderId; // Set from config and is always the same in this project

    private final int N; // Total number of processes
    private final int F; // Fault tolerance threshold
    private ConditionalCollect collector;

    // the following fields are only used by leader of that epoch for messages that can be sent by any process
    private boolean sentRead = false;       // leader avoid receiving any message if didn't start the consensus
    private boolean sentCollected = false;  // leader avoid receiving WRITE or ACCEPT messages before sending collection of states

    public ConsensusEpoch(int N, int F) {
        this.N = N;
        this.F = F;
        this.collector = new ConditionalCollectImpl(N, F);
    }

    public void addToCollector(int sender, Message state) {
        collector.addValue(sender, state);
    }

    public void addWritten(int sender, Block block) {
        written.put(sender, block);
    }

    public void addAccepted(int sender, Block block) {
        accepted.put(sender, block);
    }

    // Lombok does not directly support generating static getter and setter methods for static fields
    public int getLeaderId() {
        return leaderId;
    }

    // Lombok does not directly support generating static getter and setter methods for static fields
    public static void setLeaderId(int leaderId) {
        ConsensusEpoch.leaderId = leaderId;
    }

    public boolean enoughWritten(Block block) {
        if (enoughForMap(written, block)) {
            written = new HashMap<>();
            return true;
        }
        return false;
    }

    public boolean enoughAccepted(Block block) {
        if (enoughForMap(accepted, block)) {
            accepted = new HashMap<>();
            return true;
        }
        return false;
    }

    private boolean enoughForMap(Map<?, Block> map, Block block) {
        int requiredCount = 2 * F + 1;
        if (map.size() < requiredCount) return false;
        return map.values().stream()
                .filter(value -> value.equals(block))
                .count() == requiredCount;
    }
}
