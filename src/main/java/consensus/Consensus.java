package main.java.consensus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import main.java.blockchain.Blockchain;
import main.java.blockchain.Transaction;
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
    private Behavior behavior;
    private static final long startTime = System.currentTimeMillis();

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
     * This method sets the list of transaction to be decided if no order has been assigned yet
     * and returns the timestamp of the current epoch.
     * 
     * @param transactions The list of transactions to be ordered
     * @return The timestamp of the current epoch, null if json conversion fails
     */
    public Integer proposeToEpoch(List<Transaction> transactions) {
        if (state.getValueTS() < 0) {
            String transactionsJson = transactionsToJson(transactions);
            if (transactionsJson == null) return null;
            state.setValue(transactionsJson);
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

            String collectedStates = epoch.getCollector().collectValues(serverId);
            if (collectedStates != null) {
                epoch.setSentCollected(true);
            }
            return collectedStates;
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
        Map<Integer, State> collectedStates = collectionOfStatesFromJson(jsonString);
        if (collectedStates == null) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (!epoch.getCollector().isCollected() && collectedStates.size() >= N - F) {
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
     * @param km              The key manager to verify transaction signatures
     * @param blockchain      To verify transaction signatures and check replay attacks
     * @return Block containing the decided value and ID of the client that proposed the value,
     * or null if no value can be decided
     */
    public String determineValueToWrite(int epochTS, List<State> collectedStates, State leaderState, KeyManager km, Blockchain blockchain) {
        // if (epochTS < currTS) return null; // verified before using checkLeader()
        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        epoch.getCollector().markAsCollected();

        // Check all states for a value stopping when a deterministic value is found (skip invalid values)
        for (State state : collectedStates) {
            if (state == null || !checkValidTransactions(state.getValue(), km, blockchain)) continue;

            String transactions = determineValueFromState(state, collectedStates, leaderState);

            if (transactions != null) {
                if (this.behavior == Behavior.BEGIN_WRONG_WRITE_AFTER_40_seconds) {
                    long currentTime = System.currentTimeMillis();
                    long timeStillToWait = startTime + 40000 - currentTime;
                    if (timeStillToWait > 0) {
                        logger.info("FIRST {}\tCURRENT {}", startTime/1000, currentTime/1000);
                        logger.info("\nI will become byzantine in {} seconds\n", timeStillToWait / 1000);
                    } else {
                        this.behavior = Behavior.WRONG_WRITE;
                        logger.info("\nNow, I am byzantine and I will send wrong write sets\n");
                    }
                }
                if (this.behavior == Behavior.WRONG_WRITE) {
                    logger.info("\nI am byzantine and I will send a wrong write set\n");
                    transactions = transactions + "WRONG";
                }
                updateStateAndEpochTS(epochTS, transactions, false);
                return transactions;
            }
        }
        return null;
    }

    /**
     * Verify if transactions are correctly signed, are not repeated and are correctly formed.
     *
     * @param transactionsJson json representation of the list of transactions
     * @param km               the key manager to verify signatures
     * @param blockchain       to verify transaction signatures and check replay attacks
     * @return true if transaction is valid, false otherwise
     */
    private boolean checkValidTransactions(String transactionsJson, KeyManager km, Blockchain blockchain) {
        List<Transaction> transactions = transactionsFromJson(transactionsJson);
        if (transactions == null) return false;

        for (Transaction transaction : transactions) {
            if (transaction == null) return false;
            if (!transaction.isValid(blockchain, km)) return false;
        }
        return true;
    }

    /**
     * Given a state from collection of states determine if some value (order of transactions) can be decided:
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
     * @return String containing the decided order of transactions, or null if no value can be decided
     */
    private String determineValueFromState(State state, List<State> collectedStates, State leaderState) {
        String value = state.getValue();
        int valueTS = state.getValueTS();
        String tmpval = null;

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
     */
    public String collectWriteAndGetIfEnough(int epochTS, int senderId, String transactions, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() != serverId || (epoch.isSentRead() && epoch.isSentCollected())) {
            epoch.addWritten(senderId, transactions);
            if (epoch.enoughWritten(transactions)) {
                updateStateAndEpochTS(epochTS, transactions, true);
                return transactions;
            }
        }
        return null;
    }

    /**
     * Used upon receiving ACCEPT message and to decide (finish consensus instance).
     */
    public List<Transaction> collectAcceptAndGetIfEnough(int epochTS, int senderId, String transactions, int serverId) {
        if (epochTS < currTS) return null;

        ConsensusEpoch epoch = getConsensusEpoch(epochTS);
        if (epoch.getLeaderId() != serverId || (epoch.isSentRead() && epoch.isSentCollected())) {
            epoch.addAccepted(senderId, transactions);
            if (epoch.enoughAccepted(transactions)) {
                updateStateAndEpochTS(epochTS, transactions, true);
                return transactionsFromJson(transactions);
            }
        }
        return null;
    }

    private void updateStateAndEpochTS(int epochTS, String value, boolean toUpdatePair) {
        currTS = epochTS;
        if (toUpdatePair) {
            state.setValueTS(currTS);
            state.setValue(value);
        }
        // update write set
        Map<Integer, String> writeSet = this.state.getWriteSet();
        String valueInWriteSet = writeSet.get(currTS);
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

    public static Map<Integer, State> collectionOfStatesFromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Failed to convert JSON to collected states map", e);
            return null;
        }
    }

    public static String transactionsToJson(List<Transaction> transactions) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(transactions);
        } catch (Exception e) {
            logger.error("Failed to convert list of transactions to JSON", e);
            return null;
        }
    }

    public static List<Transaction> transactionsFromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Failed to convert JSON to list of transactions", e);
            return null;
        }
    }
}
