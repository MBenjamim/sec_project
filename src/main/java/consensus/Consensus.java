package main.java.consensus;

import main.java.common.Message;
import main.java.conditional_collect.ConditionalCollect;
import main.java.conditional_collect.ConditionalCollectImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class Consensus {
    private final Map<Integer, Block> writeSet = new HashMap<>();
    private final int N;      // Total number of processes
    private final int F;      // Fault tolerance threshold

    private int timestamp;
    private Block value;
    private int valueTS;
    private Map<Integer, Block> written;
    private Map<Integer, Block> accepted;
    private ConditionalCollect collector;

    /**
     * Constructor to initialize the Consensus with the total number of processes (N)
     * and the fault tolerance threshold (F).
     *
     * @param N Total number of processes
     * @param F Fault tolerance threshold
     */
    public Consensus(int N, int F) {
        this.N = N;
        this.F = F;
        this.timestamp = -1;
        this.value = null;
        this.valueTS = -1;
    }

    synchronized public void newEpoch() {
        timestamp++;
        written = new HashMap<>();
        accepted = new HashMap<>();
        collector = new ConditionalCollectImpl(N, F);
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

    public void collectRead(Message message) {
        // check if timestamp matches, if not ignore
        collector.addProposedValue(message.getSender(), message.getContent()); // FIXME
    }

    public void collectWrite(Message message) {
        // check if timestamp matches, if not ignore
        written.put(message.getSender(), new Block(message.getContent(), message.getSender(), message.getSignature())); // FIXME
        // check condition and send ACCEPT message
    }

    public void collectAccept(Message message) {
        // check if timestamp matches, if not ignore
        accepted.put(message.getSender(), new Block(message.getContent(), message.getSender(), message.getSignature())); // FIXME
        // check condition and decide (end consensus instance)
    }
}
