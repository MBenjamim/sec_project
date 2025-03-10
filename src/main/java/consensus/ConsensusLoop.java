package main.java.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.common.MessageType;
import main.java.server.BlockchainNetworkServer;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private final BlockchainNetworkServer server;

    private final int N; // Total number of processes (fault tolerance threshold can be calculated by (N - 1) / 3)
    private long currIndex;

    public ConsensusLoop(BlockchainNetworkServer server) {
        this.currIndex = 0;
        this.server = server;
        this.N = server.getNetworkNodes().size();
    }

    @Override
    public void run() {
        while (true) {
            this.doWork();
        }
    }


    /**
     * Process a read message received from a server
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

    synchronized public void processCollectedMessage(ConsensusMessage message) {
        long consensusIndex = message.getConsensusIdx();
        int epochTS = message.getEpochTS();

        Map<Integer, ConsensusMessage> collectedMessages = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            collectedMessages = objectMapper.readValue(message.getContent(), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Consensus consensus = getConsensusInstance(consensusIndex);
        if (collectedMessages == null || !consensus.verifyCollected(epochTS, collectedMessages))
            return;

        List<State> collectedStates = new ArrayList<>();
        for (int serverId = 0; serverId < N; serverId++) {
            ConsensusMessage collectedMessage = collectedMessages.get(serverId);
            if (collectedMessage != null) {
                try {
                    server.getKeyManager().verifyMessage(collectedMessage, server.getNetworkNodes().get(serverId));
                    collectedStates.add(State.fromJson(collectedMessage.getContent()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        consensus.determineValueToWrite(collectedStates);
        // write || abort?
    }

    /* TODO: Missing
        - pending requests from clients that notifies this loop
        - the blockchain i.e. what was written by each consensus instance
        (maybe in BlockchainNetworkServer instance that need to
         be accessible and shared between ConsensusLoop and ServerHandler)
     */

    public void doWork() {
        // check if I am leader and if I have values to propose and if I am not in other instance

        // then create a consensus instance for curr_index (if not already)
        // and use that instance to propose the received value, if other processes did not have a proposed a value already

        // else wait() until notify() when updating list/map of pending requests from clients
    }

    synchronized public Consensus getConsensusInstance(long index) {
        if (!consensusInstances.containsKey(index)) {
            Consensus consensus = new Consensus(N);
            consensusInstances.put(index, consensus);
            return consensus;
        }
        return consensusInstances.get(index);
    }
}
