package main.java.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.common.Message;
import main.java.common.MessageType;
import main.java.server.BlockchainNetworkServer;
import main.java.utils.Behavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsensusLoop.class);

    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private final List<Block> requests = new ArrayList<>();
    private final Map<Long, Block> blockchain = new HashMap<>();
    private final BlockchainNetworkServer server;

    //tests
    private final Behavior behavior;

    private final int N; // Total number of processes (fault tolerance threshold can be calculated by (N - 1) / 3)
    private long currIndex;
    private boolean inConsensus;

    public ConsensusLoop(BlockchainNetworkServer server, Behavior behavior) {
        this.currIndex = 0;
        this.inConsensus = false;
        this.server = server;
        this.behavior = behavior;
        this.N = server.getNetworkNodes().size();
    }

    @Override
    public void run() {
        logger.info("Consensus loop started");
        while (true) {
            this.doWork();
        }
    }

    /**
     * Process a READ message received from a server should be sent by leader.
     * Sends to leader the current STATE of consensus instance.
     *
     * @param message the message to be processed
     */
    synchronized public void processReadMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();
        int leaderId = message.getSender();

        Consensus consensus = getConsensusInstance(consensusIndex);
        State state = consensus.checkLeaderAndGetState(epochTS, leaderId);

        if (state != null) {
            if (this.behavior == Behavior.WRONG_READ_RESPONSE) {
                logger.debug("\n\nI am Byzantine and I will corrupt the STATE messages\n");
                Block curruptedBlock = new Block("Corrupted", 0);
                state.setValue(curruptedBlock);
            }
            Message response =
                    new Message(server.generateMessageId(), MessageType.STATE, server.getId(),
                            state.toJson(), consensusIndex, epochTS);
            server.sendConsensusResponse(response, leaderId);
        }
    }

     /**
     * Process a STATE message received from a server, done only by the LEADER.
     * It collects the state and if enough states are collected, it broadcasts the COLLECTED states.
     * 
     * @param message the message to be processed
     */
    synchronized public void processStateMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        String collectedStates = consensus.collectStateAndGetIfEnough(epochTS, message, server.getId());

        if (collectedStates != null) {
            server.broadcastConsensusResponse(consensusIndex, epochTS, MessageType.COLLECTED, collectedStates);
        }
    }

    /**
     * Process a COLLECTED message received from a server should be sent by leader.
     * If a value can be deterministically decided broadcasts it in a WRITE message.
     * 
     * @param message the message to be processed
     */
    synchronized public void processCollectedMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        Map<Integer, Message> collectedMessages = consensus.getCollectedMessages(epochTS, message.getSender(), message.getContent());
        if(collectedMessages == null) {
            return;
        }

        List<State> collectedStates = new ArrayList<>();
        State leaderState = null;

        // Check if the state was tampered and if not add it to the list of collected states
        int leaderId = message.getSender();
        for (Map.Entry<Integer, Message> entry : collectedMessages.entrySet()) {
            int serverId = entry.getKey();
            Message collectedMessage = entry.getValue();
            try {
                if (!server.getKeyManager().verifyMessage(collectedMessage, server.getNetworkNodes().get(serverId), leaderId)) {
                    return;
                }
                State state = State.fromJson(collectedMessage.getContent());
                collectedStates.add(state);
                if (serverId == message.getSender()) {
                    leaderState = state;
                }
            } catch (Exception e) {
                logger.error("Failed to verify signature for collected message", e);
                return;
            }
        }

        logger.debug("Verified all signatures and have enough STATE messages: {}", collectedStates.size());

        Block toWrite = consensus.determineValueToWrite(epochTS, collectedStates, leaderState, server.getNetworkClients().size());
        if (toWrite == null) {
            logger.error("ABORTED: consensus instance={}; consensus epoch={}; by message:\n{}", consensusIndex, epochTS, message); // FIXME: abort
        } else {
            server.broadcastConsensusResponse(consensusIndex, epochTS, MessageType.WRITE, toWrite.toJson());
        }
    }

    /**
     * Process a WRITE message received from a server.
     * It collects values and if enough values are the same, it broadcasts ACCEPT messages.
     *
     * @param message the message to be processed
     */
    synchronized public void processWriteMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        Block value = consensus.collectWriteAndGetIfEnough(epochTS, message.getSender(), message.getContent(), server.getId());
        if (value != null) {
            server.broadcastConsensusResponse(consensusIndex, epochTS, MessageType.ACCEPT, value.toJson());
        }
    }

    /**
     * Process a ACCEPT message received from a server.
     * It collects values and if enough values are the same, decide i.e. ends consensus instance FIXME: ? and write in the blockchain.
     *
     * @param message the message to be processed
     */
    synchronized public void processAcceptMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        Block value = consensus.collectAcceptAndGetIfEnough(epochTS, message.getSender(), message.getContent(), server.getId());
        if (value != null) {
            decide(consensusIndex, value);
            logger.info("DECIDED: consensus instance={}; consensus epoch={}; by message:\n{}", consensusIndex, epochTS, message);
        }
    }

    synchronized public void decide(long consensusIndex, Block block) {
        if (blockchain.putIfAbsent(consensusIndex, block) != null) return;
        requests.remove(block); // only removes first match
        inConsensus = false;
        currIndex++;
        Message response =
                new Message(server.generateMessageId(), MessageType.DECISION, server.getId(),
                        block.getValue(), consensusIndex, -1);
        server.sendReplyToClient(response, block.getClientId());
        wakeup();
    }

    /**
     * Waits for new requests from clients and for the end of previous consensus instance.
     * When conditions are met (including being leader for the specified epoch of consensus),
     * the leader starts (propose) a new consensus epoch for the current instance,
     * broadcasting READ messages.
     */
    synchronized public void doWork() {
        while (getWaitCondition()) { //is leader && not in other instance
            try {
                wait(); // wait until condition is met
            } catch (Exception e) {
                logger.error(e.getMessage());
                return;
            }
        }

        // Propose
        Consensus consensus = getConsensusInstance(currIndex);
        int epochTS = consensus.proposeToEpoch(requests.get(0));
        inConsensus = true;
        server.broadcastConsensusResponse(currIndex, epochTS, MessageType.READ, "");
    }

    /**
     * Check if this process is in a consensus instance,
     * or has no client requests to be processed,
     * or is not the leader for the current epoch of consensus instance
     *
     * @return true if conditions are met
     */
    private boolean getWaitCondition() {
        Consensus consensus = getConsensusInstance(currIndex);
        return inConsensus || requests.isEmpty()
            || consensus.getConsensusCurrentEpoch().getLeaderId() != server.getId();
    }

    synchronized void wakeup() {
        notify();
    }

    /**
     * Returns the consensus instance for the given index. Creates it if it does not exist.
     * 
     * @param index The index of consensus instance
     * @return The consensus instance that exists or was created
     */
    synchronized public Consensus getConsensusInstance(long index) {
        if (!consensusInstances.containsKey(index)) {
            Consensus consensus = new Consensus(N, behavior);
            consensusInstances.put(index, consensus);
            return consensus;
        }
        return consensusInstances.get(index);
    }

    /**
     * Adds a client request to be processed.
     * 
     * @param requestMessage The message containing the request to be processed
     */
    synchronized public void addRequest(Message requestMessage) {
        if (requestMessage.getContent().isBlank()) return;
        requests.add(new Block(requestMessage.getContent(), requestMessage.getSender()));
        wakeup();
    }

}
