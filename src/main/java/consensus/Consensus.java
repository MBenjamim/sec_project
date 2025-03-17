package main.java.consensus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import main.java.common.KeyManager;
import main.java.common.NodeRegistry;
import main.java.utils.Behavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
@Getter
@Setter
public class Consensus {
    private static final Logger logger = LoggerFactory.getLogger(Consensus.class);
    private final Map<Integer, ConsensusEpoch> epochs =  new HashMap<>();

    private final long index;
    private final State state;
    private final int N; // Total number of processes
    private final int F; // Fault tolerance threshold
    private int currTS;

    //tests
    private final Behavior behavior;

    /**
     * Constructor to initialize the Consensus with the total number of processes and leader identifier.
     *
     * @param N Total number of processes
     */
    public Consensus(long index, int N, Behavior behavior) {
        this.index = index;
        this.N = N;
        this.F = (N - 1) / 3;
        this.currTS = 0;
        this.state = new State();
        this.behavior = behavior;
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

    /**
     * Check if the leader ID corresponds to the leader of a given epoch.
     * 
     * @param epochTS The timestamp of the epoch to check
     * @param leaderId The ID of the leader to verify
     * @return true if the leader ID matches the leader of the given epoch, false otherwise
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
     * @param epochTS  The timestamp of the epoch to check
     * @param leaderId The ID of the leader to verify
     * @return The current state if the leader ID matches the leader of the given epoch, null otherwise
     */
    public State checkLeaderAndGetState(int epochTS, int leaderId) {
        return checkLeader(epochTS, leaderId) ? state : null;
    }

    /**
     * Used only by LEADER upon receiving STATE message and to send COLLECTED message.
     * Check if this process is the leader that already broadcast read messages,
     * if so verifies the signature and adds this state to the collected ones.
     * Then checks if collector has received enough STATE messages,
     * if so returns the map of collected states.
     *
     * @param epochTS         The timestamp of the epoch to receive the STATE message
     * @param signedStateJson The json string representation of the state and its signature
     * @param serverId        This process ID to check if is the leader
     * @param km              This process key manager to verify the signature
     * @param senderNode      The node that sent the message to verify if state matches the signature
     * @return Collection of states and signatures if conditions are verified, null otherwise
     */
    public String collectStateAndGetIfEnough(int epochTS, String signedStateJson, int serverId, KeyManager km, NodeRegistry senderNode) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() == serverId && epoch.isSentRead()) {
            State signedState = State.fromJson(signedStateJson);

            try {
                if (signedState == null || !km.verifyState(signedState, senderNode, index, epochTS)) {
                    return null;
                }
                epoch.addToCollector(senderNode.getId(), signedState);
            } catch (Exception e) {
                logger.error("Failed to verify signature collecting state from {}{}", senderNode.getType(), senderNode.getId(), e);
            }

            String toSend = epoch.getCollector().collectValues(serverId);
            if (toSend != null) {
                epoch.setSentCollected(true);
            }
            return toSend;
        }
        return null;
    }

    /**
     * Used upon receiving COLLECTED message.
     * Check if the leader ID corresponds to the leader of a given epoch,
     * and returns the map of states from collected message.
     *
     * @param epochTS    The timestamp of the epoch to check
     * @param leaderId   The ID of the leader to verify
     * @param jsonString Collected message content i.e. JSON string
     * @return Collection of states from collected message
     */
    public Map<Integer, State> getCollectedStates(int epochTS, int leaderId, String jsonString) {
        if (epochTS < currTS || !checkLeader(epochTS, leaderId)) return null;

        // Get map of collected states from json string
        Map<Integer, State> collectedStates;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            collectedStates = objectMapper.readValue(jsonString, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Failed to convert JSON to collected states map", e);
            return null;
        }

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (collectedStates != null && !epoch.getCollector().isCollected() && collectedStates.size() >= N - F) {
            return collectedStates;
        }
        return null;
    }

    /**
     * Used upon receiving COLLECTED message after leader and signature verification, and to send WRITE message.
     * Uses a deterministic process to determinate the value to assign to a WRITE message, given a collection of states.
     * 
     * @param epochTS         The timestamp of the epoch to receive the COLLECTED message
     * @param collectedStates The collection of states
     * @param leaderState     The state of the leader for unbound decisions
     * @param nrClients       Number of clients to check is value is valid
     * @return Block containing the decided value and ID of the client that proposed the value,
     * or null if no value can be decided
     */
    public Block determineValueToWrite(int epochTS, List<State> collectedStates, State leaderState, int nrClients) {
        // if (epochTS < currTS) return null; // verified before using checkLeader()
        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        epoch.getCollector().markAsCollected();

        // Check all states for a value stopping when a deterministic value is found (skip invalid values)
        for (State state : collectedStates) {
            if (state == null || !state.checkValid(nrClients)) continue;

            Block block = determineValueFromState(state, collectedStates, leaderState);

            if (block != null) {
                //tests
                if (this.behavior == Behavior.WRONG_WRITE) {
                    logger.info("\nI am byzantine and I will write a wrong block\n");
                    String originalValue = block.getValue();
                    String newValue = originalValue + "WRONG";
                    block.setValue(newValue);
                }
                updateStateAndEpochTS(epochTS, block, false);
                return block;
            }
        }
        return null;
    }

    /**
     * Given a state from collection of states determine if some value can be decided:
     *      - Starts by verifying if the value was written in some epoch (verify timestamp),
     *      and check if this pair (valTS, val) is in a minimum number of write sets.
     *      If both conditions are verified the value is bound and can be decided.
     *      - If value is unbound check if it matches what leader proposed (is present in its state).
     *      If this is confirmed the value can be decided, otherwise any value can be decided.
     * If some value can be decided the current epoch timestamp is updated,
     * and adds the value to the write set with that timestamp (removing the entry with the old timestamp if present).
     *
     * @param state           The state from collection to check
     * @param collectedStates The collection of states
     * @param leaderState     The state of the leader for unbound decisions
     * @return Block containing the decided value and ID of the client that proposed the value,
     * or null if no value can be decided
     */
    public Block determineValueFromState(State state, List<State> collectedStates, State leaderState) {
        Block value = state.getValue();
        int valueTS = state.getValueTS();
        Block tmpval = null;

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

        return tmpval;
    }

    /**
     * Used upon receiving WRITE message and to send ACCEPT message.
     * @param epochTS
     * @param senderId
     * @param json
     * @param serverId
     * @return
     */
    public Block collectWriteAndGetIfEnough(int epochTS, int senderId, String json, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() != serverId || (epoch.isSentRead() && epoch.isSentCollected())) {
            Block value;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                value = objectMapper.readValue(json, Block.class);
            } catch (Exception e) {
                logger.error("Failed to convert JSON to block", e);
                return null;
            }

            epoch.addWritten(senderId, value);
            if (epoch.enoughWritten(value)) {
                updateStateAndEpochTS(epochTS, value, true);
                return value;
            }
        }
        return null;
    }

    /**
     * Used upon receiving ACCEPT message and to decide (finish consensus instance).
     * @param epochTS
     * @param senderId
     * @param json
     * @param serverId
     * @return
     */
    public Block collectAcceptAndGetIfEnough(int epochTS, int senderId, String json, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() != serverId || (epoch.isSentRead() && epoch.isSentCollected())) {
            Block value;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                value = objectMapper.readValue(json, Block.class);
            } catch (Exception e) {
                logger.error("Failed to convert JSON to block", e);
                return null;
            }

            epoch.addAccepted(senderId, value);
            if (epoch.enoughAccepted(value)) {
                updateStateAndEpochTS(epochTS, value, true);
                return value;
            }
        }
        return null;
    }

    private void updateStateAndEpochTS(int epochTS, Block value, boolean toUpdatePair) {
        currTS = epochTS;
        if (toUpdatePair) {
            state.setValueTS(currTS);
            state.setValue(value);
        }
        // update write set
        Map<Integer, Block> writeSet = this.state.getWriteSet();
        Block valueInWriteSet = writeSet.get(currTS);
        if (valueInWriteSet != null && valueInWriteSet.equals(value)) {
            writeSet.remove(currTS);
        }
        writeSet.put(epochTS, value);
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
