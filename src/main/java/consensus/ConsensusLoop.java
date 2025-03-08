package main.java.consensus;

import java.util.HashMap;
import java.util.Map;

/**
 * Decide the value to be appended to the blockchain across consensus epochs.
 */
public class ConsensusLoop implements Runnable {
    private final Map<Long, Consensus> consensusInstances = new HashMap<>();
    private long curr_index;

    public ConsensusLoop() {
        this.curr_index = 0;
    }

    @Override
    public void run() {
        while (true) {
            this.doWork();
        }
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
}
