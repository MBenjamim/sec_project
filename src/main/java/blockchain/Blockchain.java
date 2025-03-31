package main.java.blockchain;

import lombok.Getter;
import main.java.common.NodeRegistry;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;

@Getter
public class Blockchain implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    private SmartContractExecutor executor;
    private SimpleWorld world;
    private final Map<Address, NodeRegistry> clients = new HashMap<>();
    private final Map<Long, String> blocks = new HashMap<>();
    private final Map<Long, List<Transaction>> pendingTransactions = new HashMap<>();
    private final Set<String> decidedTransactions = new HashSet<>();
    private long currentBlock = 0;
    private String previousBlockHash;

    /**
     * Creates the blockchain, initializing every account from genesis block file.
     *
     * @param clientNodes        the node registries to associate blockchain address of EOAs to their public keys
     * @param pathToGenesisBlock the path to the genesis block file
     */
    public Blockchain(Map<Integer, NodeRegistry> clientNodes, String pathToGenesisBlock) {
        Block genesisBlock = Block.loadFromFile(pathToGenesisBlock);
        if (genesisBlock == null) return;
        for (NodeRegistry registry : clientNodes.values()) {
            try {
                PublicKey publicKey = registry.getPublicKey();
                Address address = AddressGenerator.generateAddress(publicKey);
                clients.put(address, registry);
            } catch (Exception e) {
                logger.error("Failed to get client address: ", e);
            }
        }
        this.world = genesisBlock.getWorld();
        this.executor = new SmartContractExecutor(world, genesisBlock.getBlacklistAddress(), genesisBlock.getTokenAddress());
        blocks.put(currentBlock, genesisBlock.toJson());
        currentBlock++;
        this.previousBlockHash = genesisBlock.getBlockHash();
        logger.info("Blockchain initialized");
    }

    @Override
    public void run() {
        logger.info("Blockchain started");
        while (true) {
            this.doWork();
        }
    }

    /**
     * Waits for new transaction and for the end of previous consensus instance.
     * When conditions are met (including being leader for the specified epoch of consensus),
     * the leader starts (propose) a new consensus epoch for the current instance,
     * broadcasting READ messages.
     */
    synchronized public void doWork() {
        while (getWaitCondition()) {
            try {
                logger.debug("Waiting for transactions");
                wait(); // wait until condition is met
            } catch (Exception e) {
                logger.error(e.getMessage());
                return;
            }
        }


        logger.debug("Executing transactions for block {}", currentBlock);
        for (Transaction transaction : pendingTransactions.remove(currentBlock)) { // removes pending transactions for the block
            executeTransaction(transaction);
        }


        String previousBlockHash =this.previousBlockHash;
        Block newBlock = new Block(this.world, this.executor.getBlacklistAddress(), this.executor.getTokenAddress(), previousBlockHash);
        blocks.put(currentBlock, newBlock.toJson());
        currentBlock++;
    }

    /**
     * Check if this process is there are transactions
     * to execute
     *
     * @return true if conditions are met
     */
    private boolean getWaitCondition() {
        return !pendingTransactions.containsKey(currentBlock) || pendingTransactions.get(currentBlock).isEmpty();
    }

    synchronized void wakeup() {
        logger.debug("Waking up");
        notify();
    }

    public TransactionType getTransactionType(String functionSignature) {
        return executor.getSignatureToType().get(functionSignature);
    }

    /**
     * Add transaction to be performed at a certain block.
     * Also add the unique identifier of the transaction to avoid replay attacks.
     * Useful to perform transactions in parallel with future consensus.
     *
     * @param blockIndex   the index of the block where transactions should be performed
     * @param transactions the list of transactions to be performed at the block
     * @return true if it is the first time adding transactions to the block
     */
    synchronized public boolean addTransactionsForBlock(long blockIndex, List<Transaction> transactions) {
        logger.debug("Adding transactions {} for block {}", transactions, blockIndex);
        if (pendingTransactions.putIfAbsent(blockIndex, transactions) == null) {
            transactions.forEach(transaction -> decidedTransactions.add(transaction.getSignatureBase64()));
            logger.debug("Transactions successfully added");
            wakeup();
            return true;
        }
        logger.debug("Transactions already exist for block {}", blockIndex);
        return false;
    }

    /**
     * Remove the pending transactions when the block is added to the blockchain.
     *
     * @param blockIndex the index of the block where transactions were performed
     */
    synchronized public void removeTransactionsForBlock(long blockIndex) {
        pendingTransactions.remove(blockIndex);
    }

    /**
     * Verifies if unique identifier of the transaction was already decided to avoid replay attacks.
     *
     * @param signature the unique identifier of the transaction i.e. base64 representation of transaction signature
     * @return true if is a duplicate transaction, false otherwise
     */
    synchronized public boolean checkRepeatedTransaction(String signature) {
        return decidedTransactions.contains(signature);
    }

    synchronized public TransactionResponse executeTransaction(Transaction transaction){
        logger.debug("Executing transaction {}", transaction.getTransactionId());
        TransactionResponse transactionResponse = executor.execute(transaction);
        if (transactionResponse.isStatus()) {
            logger.info("Transaction {} executed successfully", transaction.getSignatureBase64());
        } else {
            logger.info("Transaction {} failed: {}", transaction.getTransactionId(), transactionResponse.getDescription());
        }

        return transactionResponse;
    }
}
