package main.java.consensus;

import main.java.common.Message;
import main.java.conditional_collect.ConditionalCollect;
import main.java.conditional_collect.ConditionalCollectImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusEpoch {
    private final Map<Integer, Block> written = new HashMap<>();
    private final Map<Integer, Block> accepted = new HashMap<>();

    private final int leaderId;
    private ConditionalCollect collector;

    public ConsensusEpoch(int N, int F, int leaderId) {
        this.leaderId = leaderId;
        this.collector = new ConditionalCollectImpl(N, F);
    }
}
