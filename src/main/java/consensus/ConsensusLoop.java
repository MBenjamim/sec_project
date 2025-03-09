package main.java.consensus;

import java.util.HashMap;
import java.util.Map;

import lombok.Synchronized;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.server.BlockchainNetworkServer;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private final BlockchainNetworkServer server;
    private long curr_index;

    public ConsensusLoop(BlockchainNetworkServer server) {
        this.curr_index = 0;
        this.server = server;
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
        Consensus consensus = getConsensusInstance(message.getConsensusIdx());
        State state = consensus.getState();
        ConsensusMessage response = new ConsensusMessage(server.generateMessageId(), MessageType.STATE, server.getId(),  message.getConsensusIdx(), message.getEpochTS());
        response.setContent(state.toJson());
        server.sendConsensusResponse(message, message.getSender());
    }

    /* TODO: Missing
        - pending requests from clients that notifies this loop
        - the blockchain i.e. what was written by each consensus instance
        (maybe in BlockchainNetworkServer instance that need to
         be accessible and shared between ConsensusLoop and ServerHandler)
     */

    public void doWork() {
        // check if I am leader and if I have values to propose

        // then create a consensus instance for curr_index (if not already)
        // and use that instance to propose the received value, if other processes did not have a proposed a value already

        // else wait() until notify() when updating list/map of pending requests from clients
    }

    synchronized public Consensus getConsensusInstance(long index) {
        if (!consensusInstances.containsKey(index)) {
            Consensus consensus = new Consensus(3, 1, server.getLeaderId()); // FIXME
            consensusInstances.put(index, consensus);
            return consensus;
        }
        return consensusInstances.get(index);
    } 
    
    synchronized public void sendResponse(ConsensusMessage message, int receiverId) {
        server.sendConsensusResponse(message, receiverId);
    }
}
