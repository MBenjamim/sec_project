package main.java.client;

import lombok.Getter;
import main.java.blockchain.TransactionResponse;
import main.java.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Getter
public class BlockchainConfirmationCollector {
    private final Logger logger = LoggerFactory.getLogger(BlockchainConfirmationCollector.class);
    private Map<Integer, String> collectedValues = new HashMap<>();
    private final Set<String> collectedConfirmations = new HashSet<>();
    private final Map<Long, String> collectedTransactions = new HashMap<>();
    private final int N; // total number of servers
    private final int F; // maximum faulty servers

    public BlockchainConfirmationCollector(int nServers) {
        this.N = nServers;
        this.F = (nServers - 1) / 3;
    }

    synchronized public void collectConfirmation(Message message) {
        TransactionResponse response = TransactionResponse.fromJson(message.getContent());
        if (response != null && response.getSignature() != null
                && !collectedConfirmations.contains(response.getSignatureBase64())) {
            int senderId = message.getSender();
            collectedValues.put(senderId, message.getContent());
            notify();
        }
    }

    /**
     * Waits for blockchain confirmation for the operation.
     *
     * @return The timestamp of when decision was done
     */
    synchronized public TransactionResponse waitForConfirmation() {
        TransactionResponse response;
        while ((response = condition()) == null) {
            try {
                wait(); // wait until condition is met
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        }
        return response;
    }

    /**
     * Condition to keep waiting.
     *
     * @return null to keep waiting, timestamp of decision otherwise
     */
    private TransactionResponse condition() {
        int requiredCount = F + 1;
        if (collectedValues.size() < requiredCount) return null;

        for (int i = 0; i < N; i++) {
            String value = collectedValues.get(i);
            if (value == null || value.isBlank()) continue;

            int matches = (int) collectedValues.values().stream()
                    .filter(v -> v.equals(value)).count();

            if (matches >= requiredCount) { // enough good results & reset to wait again
                TransactionResponse response = TransactionResponse.fromJson(value);
                collectedValues = new HashMap<>();
                collectedConfirmations.add(response.getSignatureBase64());
                collectedTransactions.put(response.getTransactionId(), value);
                return response;
            }
        }
        return null;
    }
}
