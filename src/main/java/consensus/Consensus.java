package main.java.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    
    /**
     * Used before send READs phase.
     * This method sets the value for the decision if no value has been assigned yet 
     * and returns the timestamp of the current epoch.
     * 
     * @param value The block containing the value requested by a client and its client ID
     * @return The timestamp of the current epoch
     */
    public int proposeToEpoch(Block value) {
        if (state.getValueTS() < 0) {
            state.setValue(value);
        }
        ConsensusEpoch epoch = getConsensusCurrentEpoch();
        epoch.setSentRead(true);
        return currTS;
    }

    public String decide(Block value) {
        return "";
    }

    public void abort() {
        // send ABORT to every node (?including this?): use index to identify #decision and timestamp to identify #epoch
    }

    /**
     * Check if the leader ID corresponds to the leader of a given epoch.
     * 
     * @param epochTS The timestamp of the epoch to check.
     * @param leaderId The ID of the leader to verify.
     * @return true if the leader ID matches the leader of the given epoch, false otherwise.
     */
    public boolean checkLeader(int epochTS, int leaderId) {
        if (epochTS < currTS) return false;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        return epoch.getLeaderId() == leaderId;
    }

    /**
     * Used upon receiving READ message and to send STATE message.
     * Check if the leader ID corresponds to the leader of a given epoch,
     * and returns the current state of consensus from this process.
     * 
     * @param epochTS The timestamp of the epoch to check.
     * @param leaderId The ID of the leader to verify.
     * @return The current state if the leader ID matches the leader of the given epoch, false otherwise.
     */
    public State checkLeaderAndGetState(int epochTS, int leaderId) {
        return checkLeader(epochTS, leaderId) ? state : null;
    }

    /**
     * Used only by LEADER upon receiving STATE message and to send COLLECTED message.
     * Check if this process is the leader that already broadcast READ messages,
     * if so adds this state to the collected ones.
     * Then checks if have received enough STATE messages,
     * if so returns the map of collected states.
     * 
     * @param epochTS     The timestamp of the epoch to receive the STATE message
     * @param stateSigned The state of some process including its signature
     * @param serverId    This process ID to check if is the leader
     */
    public String collectStateAndGetIfEnough(int epochTS, ConsensusMessage stateSigned, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() == serverId && epoch.isSentRead()) {  // only if this server is leader for that epoch
            epoch.addToCollector(stateSigned.getSender(), stateSigned);
            return epoch.getCollector().collectValues();
        }
        return null;
    }

    /**
     * 
     * Parsing of a COLLECTOR message
     * 
     * @param epochTS
     * @param leaderId
     * @param jsonString collect message content
     * @return ConsensusMessage map
     */

    public Map<Integer, ConsensusMessage> getCollectedMessages(int epochTS, int leaderId, String jsonString) {
        if (epochTS < currTS || !checkLeader(epochTS, leaderId)) return null;

        // Get map of collected messages from json string
        Map<Integer, ConsensusMessage> collectedMessages = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            collectedMessages = objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (collectedMessages != null && !epoch.getCollector().isCollected() && collectedMessages.size() >= N - F) {
            return collectedMessages;
        }
        return null;
    }

    /**
     * 
     * Deterministic process to determin the value to assigne to a WRITE message,
     * given a collection of states
     * 
     * @param epochTS
     * @param collectedStates
     * @param leaderState
     * @param nrClients
     * @return Block
     */

    public Block determineValueToWrite(int epochTS, List<State> collectedStates, State leaderState, int nrClients) {
        // if (epochTS < currTS) return null; // verified before using checkLeader()
        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        epoch.getCollector().markAsCollected();

        for (State state : collectedStates) {
            Block value = state.getValue();
            int valueTS = state.getValueTS();
            Block tmpval = null;

            if (!value.checkValid(nrClients)) continue;

            int count = 0;
            if (state.getValueTS() >= 0) {
                for (State otherState : collectedStates) {
                    if (otherState.getValue().equals(value) && otherState.getValueTS() == valueTS) {
                        // value is bound
                        if (++count > F) {
                            tmpval = value;
                            break;
                        }
                    }
                }
            }

            // value is unbound
            if (count < F + 1 && leaderState.getValue().equals(value)) {
                tmpval = value;
            }

            if (tmpval != null) {
                Map<Integer, Block> writeSet = this.state.getWriteSet();
                Block aux = writeSet.get(valueTS);
                if (aux != null && aux.equals(value)) {
                    writeSet.remove(valueTS);
                }
                currTS = epochTS;
                writeSet.put(epochTS, value);
                return value;
            }
        }
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

    synchronized public ConsensusEpoch getConsensusCurrentEpoch() {
        return getConsensusEpoch(currTS);
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
