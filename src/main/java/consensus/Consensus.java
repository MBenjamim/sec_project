package main.java.consensus;

import java.util.HashMap;
import java.util.List;
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

    private final State state;
    private final int N; // Total number of processes
    private final int F; // Fault tolerance threshold
    private int currTS;

    /**
     * Constructor to initialize the Consensus with the total number of processes and leader identifier.
     *
     * @param N Total number of processes
     */
    public Consensus(int N) {
        this.N = N;
        this.F = (N - 1) / 3;
        this.currTS = 0;
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

    public State checkLeaderAndGetState(int epochTS, int leaderId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        return (epoch.getLeaderId() == leaderId) ? state : null;
    }

    public String collectStateAndGetIfEnough(int epochTS, ConsensusMessage stateSigned, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() == serverId) {  // only if this server is leader for that epoch
            epoch.addToCollector(stateSigned.getSender(), stateSigned);
            return epoch.getCollector().collectValues();
        }
        return null;
    }

    public boolean verifyCollected(int epochTS, Map<Integer, ConsensusMessage> collectedMessages) {
        if (epochTS < currTS) return false;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        return (!epoch.getCollector().isCollected() && collectedMessages.size() >= N - F);
    }

    public Block determineValueToWrite(List<State> collectedStates) {
        // TODO: deterministic choice of value
        return null;
    }

    public void collectWrite(ConsensusMessage message) {
        // check if timestamp matches, if not ignore
        state.getWriteSet().put(message.getSender(), new Block(message.getContent(), message.getSender(), message.getSignature())); // FIXME
        // check condition and send ACCEPT message
    }

    public void collectAccept(ConsensusMessage message) {
        ConsensusEpoch epoch = getConsensusEpoch(message.getEpochTS());
        epoch.addAccepted(message);
        // check condition and decide (end consensus instance)
    }

    synchronized public ConsensusEpoch getConsensusEpoch(int index) {
        if (!epochs.containsKey(index)) {
            ConsensusEpoch epoch = new ConsensusEpoch(N, F);
            epochs.put(index, epoch);
            return epoch;
        }
        return epochs.get(index);
    } 
}
