package main.java.blockchain;

import lombok.Getter;
import main.java.common.Message;
import main.java.common.MessageType;
import main.java.common.NodeRegistry;
import main.java.server.BlockchainNetworkServer;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;

@Getter
public class Blockchain implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    private final BlockchainNetworkServer server;
    private SmartContractExecutor executor;
    private NativeExecutor nativeExecutor;
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
     * @param server             to associate blockchain address of EOAs to their public keys
     * @param pathToGenesisBlock the path to the genesis block file
     */
    public Blockchain(BlockchainNetworkServer server, String pathToGenesisBlock) {
        Block genesisBlock = Block.loadFromFile(pathToGenesisBlock);
        this.server = server;
        if (genesisBlock == null) return;
        for (NodeRegistry registry : server.getNetworkClients().values()) {
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
        this.nativeExecutor = new NativeExecutor(world);
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
     * Waits for transactions to be ordered for the current block.
     * When conditions are met, transactions are executed
     * and a new the block (with updated state) is added to the blockchain.
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

        // Execute transactions
        logger.debug("Executing transactions for block {}", currentBlock);
        List<Transaction> transactions = pendingTransactions.get(currentBlock);
        List<TransactionResponse> responses = new ArrayList<>();
        int transactionCount = transactions.size();
        for (Transaction transaction : transactions) {
            TransactionResponse response = executeTransaction(transaction);
            response.setClientAddress(transaction.getSenderAddress());
            responses.add(response);
        }

        // Create and append the block
        Block newBlock = new Block(world, executor.getBlacklistAddress(), executor.getTokenAddress(), previousBlockHash);
        newBlock.setTransactions(transactions);
        blocks.put(currentBlock, newBlock.toJson());
        newBlock.debugToFile("server" + server.getId() + "_block" + currentBlock + ".json"); // DEBUG

        // Respond to clients
        for (TransactionResponse response : responses) {
            response.setBlockHash(newBlock.getBlockHash());
            Message msg = new Message(server.generateMessageId(), MessageType.DECISION, server.getId(), response.toJson());
            server.sendReplyToClient(msg, clients.get(response.getClientAddress()).getId());
        }

        removeTransactionsForBlock(currentBlock);
        previousBlockHash = newBlock.getBlockHash();
        currentBlock++;
        logger.info("APPENDED NEW BLOCK: {} with hash {}, {} transactions were executed", currentBlock, newBlock.getBlockHash(), transactionCount);
    }

    /**
     * Check if the transactions for current block
     * were ordered in a consensus instance.
     *
     * @return true if conditions are not met
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

    synchronized public TransactionResponse executeTransaction(Transaction transaction) {
        logger.debug("Executing transaction {}", transaction.getTransactionId());
        TransactionResponse transactionResponse;
        if (transaction.getFunctionSignature() == null) {
            transactionResponse = nativeExecutor.performTransaction(transaction);
            executor.getExecutor().worldUpdater(world.updater()).commitWorldState();
        } else {
            transactionResponse = executor.execute(transaction);
        }
        if (transactionResponse.isStatus()) {
            logger.info("Transaction {} executed successfully", transaction.getTransactionId());
        } else {
            logger.info("Transaction {} failed: {}", transaction.getTransactionId(), transactionResponse.getDescription());
        }

        return transactionResponse;
    }
}
