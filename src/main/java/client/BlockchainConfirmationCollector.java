package main.java.client;

import lombok.Getter;
import main.java.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BlockchainConfirmationCollector {
    private final Logger logger = LoggerFactory.getLogger(BlockchainConfirmationCollector.class);
    private Map<Integer, String> collectedValues = new HashMap<>();
    private Map<Integer, Long> collectedTimestamps = new HashMap<>();
    private final List<Long> collectedConfirmations = new ArrayList<>();
    private final Map<Long, String> collectedTransactions = new HashMap<>();
    private final int N; // total number of servers
    private final int F; // maximum faulty servers

    public BlockchainConfirmationCollector(int nServers) {
        this.N = nServers;
        this.F = (nServers - 1) / 3;
    }

    synchronized public void collectConfirmation(Message message) {
        Long consensusIndex = message.getConsensusIdx();
        if (consensusIndex != null && !collectedConfirmations.contains(consensusIndex)) {
            int senderId = message.getSender();
            collectedValues.put(senderId, message.getContent());
            collectedTimestamps.put(senderId, message.getConsensusIdx());
            notify();
        }
    }

    /**
     * Waits for blockchain confirmation for the operation.
     *
     * @return The timestamp of when decision was done
     */
    synchronized public long waitForConfirmation() {
        Long timestamp;
        while ((timestamp = condition()) == null) {
            try {
                wait(); // wait until condition is met
            } catch (Exception e) {
                logger.error(e.getMessage());
                return -1;
            }
        }
        return timestamp;
    }

    /**
     * Condition to keep waiting.
     *
     * @return null to keep waiting, timestamp of decision otherwise
     */
    private Long condition() {
        int requiredCount = F + 1;
        if (collectedValues.size() < requiredCount) return null;
        int count = 0;
        for (int i = 0; i < N; i++) {
            String value = collectedValues.get(i);
            Long timestamp = collectedTimestamps.get(i);
            if (value == null || timestamp == null || value.isBlank()) continue;

            if (++count == requiredCount) { // enough good results & reset to wait again
                collectedValues = new HashMap<>();
                collectedTimestamps = new HashMap<>();
                collectedConfirmations.add(timestamp);
                collectedTransactions.put(timestamp, value);
                return timestamp;
            }
        }
        return null;
    }
}
