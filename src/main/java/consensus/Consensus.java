package main.java.consensus;

import main.java.common.Message;
import main.java.conditional_collect.ConditionalCollect;
import main.java.conditional_collect.ConditionalCollectImpl;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
@Getter
@Setter
public class Consensus {
    private final Map<Integer, ConsensusEpoch> epochs =  new HashMap<>();

    private final int N;      // Total number of processes
    private final int F;      // Fault tolerance threshold
    private final int leaderId;

    private final State state;
    private int timestamp;

    /**
     * Constructor to initialize the Consensus with the total number of processes (N)
     * and the fault tolerance threshold (F).
     *
     * @param N Total number of processes
     * @param F Fault tolerance threshold
     */
    public Consensus(int N, int F, int leaderId) {
        this.N = N;
        this.F = F;
        this.leaderId = leaderId;
        this.timestamp = 0;
        this.state = new State();
    }
    
    public void propose(Block value) { // or Message
        // send READ to every node (including this): use index to identify #decision and timestamp to identify #epoch
    }

    public String decide(Block value) {
        return "";
    }

    public void abort() {
        // send ABORT to every node (?including this?): use index to identify #decision and timestamp to identify #epoch
    }

    public void collectState(ConsensusMessage message) {
        // check if timestamp matches, if not ignore
        collector.addProposedValue(message.getSender(), message.getContent()); // FIXME
    }

    public void collectWrite(ConsensusMessage message) {
        // check if timestamp matches, if not ignore
        state.getWriteSet().put(message.getSender(), new Block(message.getContent(), message.getSender(), message.getSignature())); // FIXME
        // check condition and send ACCEPT message
    }

    public void collectAccept(ConsensusMessage message) {
        // check if timestamp matches, if not ignore
        accepted.put(message.getSender(), new Block(message.getContent(), message.getSender(), message.getSignature())); // FIXME
        // check condition and decide (end consensus instance)
    }

    synchronized public ConsensusEpoch getConsensusEpoch(int index) {
        if (!epochs.containsKey(index)) {
            ConsensusEpoch epoch = new ConsensusEpoch(N, F, leaderId);
            epochs.put(index, epoch);
            return epoch;
        }
        return epochs.get(index);
    } 
}
