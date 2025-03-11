package main.java.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import main.java.common.MessageType;
import main.java.server.BlockchainNetworkServer;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private final List<Block> requests = new ArrayList<>();
    private final BlockchainNetworkServer server;

    private final int N; // Total number of processes (fault tolerance threshold can be calculated by (N - 1) / 3)
    private long currIndex;
    private boolean inConsensus;

    public ConsensusLoop(BlockchainNetworkServer server) {
        this.currIndex = 0;
        this.inConsensus = false;
        this.server = server;
        this.N = server.getNetworkNodes().size();
    }

    @Override
    public void run() {
        System.out.println("[CONSENSUS]: Loop Started");
        while (true) {
            this.doWork();
        }
    }


    /**
     * Process a READ message received from a server (it should be the leader)
     *
     * @param message the message to be processed
     */
    synchronized public void processReadMessage(ConsensusMessage message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();
        int leaderId = message.getSender();

        Consensus consensus = getConsensusInstance(consensusIndex);
        State state = consensus.checkLeaderAndGetState(epochTS, leaderId);

        if (state != null) {
            ConsensusMessage response =
                    new ConsensusMessage(server.generateMessageId(), MessageType.STATE, server.getId(),
                            state.toJson(), consensusIndex, epochTS);
            server.sendConsensusResponse(response, leaderId);
        }
    }

     /**
     * Process a STATE message received from a server, done only by the leader.
     * It collects the state and if enough states are collected, it broadcasts the collected states.
     * 
     * @param message the message to be processed
     */
    synchronized public void processStateMessage(ConsensusMessage message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        String collectedStates = consensus.collectStateAndGetIfEnough(epochTS, message, server.getId());

        if (collectedStates != null) {
            for (int serverId = 0; serverId < N; serverId++) {
                ConsensusMessage broadcast =
                        new ConsensusMessage(server.generateMessageId(), MessageType.COLLECTED, server.getId(),
                                collectedStates, consensusIndex, epochTS);
                server.sendConsensusResponse(broadcast, serverId);
            }
        }
    }
    /**
     * Process a COLLECTED message received from a server should be send by leader.
     * 
     * @param message
     */
    synchronized public void processCollectedMessage(ConsensusMessage message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Consensus consensus = getConsensusInstance(consensusIndex);
        Map<Integer, ConsensusMessage> collectedMessages = consensus.getCollectedMessages(epochTS, message.getSender(), message.getContent());
        if(collectedMessages == null) {
            return;
        }

        List<State> collectedStates = new ArrayList<>();
        State leaderState = null;

        // Check if the state was tampered and if not add it to the list of collected states
        for (Map.Entry<Integer, ConsensusMessage> entry : collectedMessages.entrySet()) {
            int serverId = entry.getKey();
            ConsensusMessage collectedMessage = entry.getValue();
            try {
                if (!server.getKeyManager().verifyMessage(collectedMessage, server.getNetworkNodes().get(serverId))) {
                    return;
                }
                State state = State.fromJson(collectedMessage.getContent());
                collectedStates.add(state);
                if (serverId == message.getSender()) {
                    leaderState = state;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        Block toWrite = consensus.determineValueToWrite(epochTS, collectedStates, leaderState, server.getNetworkClients().size());
        if (toWrite == null) {
            //FIXME: abort
        } else {
            server.broadcastConsenusResponse(consensusIndex, epochTS, MessageType.WRITE, toWrite.toJson());
        }
    }

    /* FIXME: Missing
        - pending requests from clients that notifies this loop
        - the blockchain i.e. what was written by each consensus instance
        (maybe in BlockchainNetworkServer instance that need to
         be accessible and shared between ConsensusLoop and ServerHandler)
     */

    /**
     * TODO
     * things that never happen given that I am the leader:
     *  - cannot receive STATE (and future messages) before sending READs
     *  - cannot receive WRITE / ACCEPT before sending COLLECTED
     */

    synchronized public void doWork() {
        while (getWaitCondition()) { //is leader && not in other instance
            try {
                requests.wait(); // Wait until a task is added
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // Propose
        Consensus consensus = getConsensusInstance(currIndex);
        int epochTS = consensus.proposeToEpoch(requests.get(0));
        inConsensus = true;
        server.broadcastConsenusResponse(currIndex, epochTS, MessageType.READ, "");
    }

    // check if I am leader and if I have values to propose and if I am not in other instance
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
     * @param index
     * @return
     */
    synchronized public Consensus getConsensusInstance(long index) {
        if (!consensusInstances.containsKey(index)) {
            Consensus consensus = new Consensus(N);
            consensusInstances.put(index, consensus);
            return consensus;
        }
        return consensusInstances.get(index);
    }

    /**
     * Adds a request to be processed.
     * 
     * @param request
     */
    synchronized public void addRequest(Block request) {
        requests.add(request);
        wakeup();
    }

    
}
