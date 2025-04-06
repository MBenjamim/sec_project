package main.java.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.blockchain.Blockchain;
import main.java.blockchain.Transaction;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.common.NodeRegistry;
import main.java.server.BlockchainNetworkServer;
import main.java.utils.Behavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsensusLoop.class);

    private static final String genesisBlockPath = "genesis_block.json";

    // limit of transactions to be ordered during consensus and to be executed in a single block
    private static final int MAX_TRANSACTIONS_PER_BLOCK = 10;
    // wait after consensus to allow more effective batching
    private static final int BATCHING_DELAY = 1000;

    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private final List<Transaction> requests = new ArrayList<>();
    private final BlockchainNetworkServer server;
    private final Blockchain blockchain;
    private final Thread blockchainThread;

    //tests
    private final Behavior behavior;

    private final int N; // Total number of processes (fault tolerance threshold can be calculated by (N - 1) / 3)
    private long currIndex;
    private boolean inConsensus;

    public ConsensusLoop(BlockchainNetworkServer server, Behavior behavior) {
        this.currIndex = 1; // keep it the same as block indexes for simplicity
        this.inConsensus = false;
        this.server = server;
        this.behavior = behavior;
        this.N = server.getNetworkNodes().size();
        this.blockchain = new Blockchain(server, genesisBlockPath);
        this.blockchainThread = new Thread(blockchain);
    }

    @Override
    public void run() {
        logger.info("Consensus loop started");
        blockchainThread.start();
        while (true) {
            this.doWork();
            try {
                Thread.sleep(BATCHING_DELAY);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Process a READ message received from a server should be sent by leader.
     * Sends to leader the current STATE of consensus instance signed.
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
                state.setValue("corrupted");
            }
            try {
                String messageContent = server.getKeyManager().signState(state, server.getId(), consensusIndex, epochTS);
                Message response =
                        new Message(server.generateMessageId(), MessageType.STATE, server.getId(),
                                messageContent, consensusIndex, epochTS);
                server.sendConsensusResponse(response, leaderId);
            } catch (Exception e) {
                logger.error("Failed to sign state in response to read message from leaderId: {}", leaderId, e);
            }
        }
    }

     /**
     * Process a STATE message received from a server, done only by the LEADER, verifies the signature.
     * It collects the state and if enough states are collected, it broadcasts the COLLECTED states.
     * 
     * @param message the message to be processed
     */
    synchronized public void processStateMessage(Message message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();
        NodeRegistry senderNode = server.getNetworkNodes().get(message.getSender());

        Consensus consensus = getConsensusInstance(consensusIndex);
        String collectedStates = consensus.collectStateAndGetIfEnough(epochTS, message.getContent(), server.getId(), server.getKeyManager(), senderNode);

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
        Map<Integer, State> collectedStates = consensus.getCollectedStates(epochTS, message.getSender(), message.getContent());
        if(collectedStates == null) {
            return;
        }

        List<State> validStates = new ArrayList<>();
        State leaderState = null;

        // Check if the state was tampered and if not add it to the list of collected states
        int leaderId = message.getSender();
        for (Map.Entry<Integer, State> entry : collectedStates.entrySet()) {
            int serverId = entry.getKey();
            State collectedState = entry.getValue();
            NodeRegistry processNode = server.getNetworkNodes().get(serverId);
            try {
                if (!server.getKeyManager().verifyState(collectedState, processNode, consensusIndex, epochTS)) {
                    logger.debug("Invalid signature for process: {} with collected state: {} & signature={}", serverId, collectedState, collectedState.getSignatureBase64());
                    return;
                }
                validStates.add(collectedState);
                if (serverId == leaderId) {
                    leaderState = collectedState;
                }
            } catch (Exception e) {
                logger.error("Failed to verify signature for collected message", e);
                return;
            }
        }

        logger.debug("Verified all signatures and have enough STATE messages: {}", collectedStates.size());

        String transactions = consensus.determineValueToWrite(epochTS, validStates, leaderState, server.getKeyManager(), blockchain);
        if (transactions == null) {
            logger.error("ABORTED: consensus instance={}; consensus epoch={}; by message:\n{}", consensusIndex, epochTS, message); // FIXME: abort
        } else {
            server.broadcastConsensusResponse(consensusIndex, epochTS, MessageType.WRITE, transactions);
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
        String transactions = consensus.collectWriteAndGetIfEnough(epochTS, message.getSender(), message.getContent(), server.getId());
        if (transactions != null) {
            server.broadcastConsensusResponse(consensusIndex, epochTS, MessageType.ACCEPT, transactions);
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
        List<Transaction> transactions = consensus.collectAcceptAndGetIfEnough(epochTS, message.getSender(), message.getContent(), server.getId());
        if (transactions != null) {
            decide(consensusIndex, transactions);
            logger.info("DECIDED: consensus instance={}; consensus epoch={}; by message:\n{}", consensusIndex, epochTS, message);
        }
    }

    synchronized public void decide(long consensusIndex, List<Transaction> transactions) {
        if (!blockchain.addTransactionsForBlock(consensusIndex, transactions)) return; //returns if the block already exists
        transactions.forEach(requests::remove);
        currIndex++;
        inConsensus = false;
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
        List<Transaction> transactions = requests.stream().limit(MAX_TRANSACTIONS_PER_BLOCK).toList();
        Integer epochTS = consensus.proposeToEpoch(transactions);
        if (epochTS == null) return;
        inConsensus = true;
        server.broadcastConsensusResponse(currIndex, epochTS, MessageType.READ, "");
    }

    /**
     * Check if this process is in a consensus instance,
     * or has no client requests to be processed,
     * or is not the leader for the current epoch of consensus instance.
     *
     * FIXME - this condition simulates the average time to complete a consensus,
     *  since blockchains like Ethereum use a fixed time to let consensus complete
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
            Consensus consensus = new Consensus(index, N, behavior);
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
        Transaction transaction = Transaction.fromJson(requestMessage.getContent());
        if (this.behavior != Behavior.DONT_VERIFY_TRANSACTIONS) {
            if (transaction == null || requests.contains(transaction) || !transaction.isValid(blockchain, server.getKeyManager())) {
                logger.info("Invalid transaction: {}", requestMessage.getContent());
                return;
            }
        } else {
            logger.info("\n\nI am byzantine and I will not verify the transactions\n");
        }
        requests.add(transaction);
        wakeup();
    }
}
