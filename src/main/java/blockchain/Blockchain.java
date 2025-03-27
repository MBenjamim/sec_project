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
public class Blockchain {
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    private SmartContractExecutor executor;
    private SimpleWorld world;
    private final Map<Address, NodeRegistry> clients = new HashMap<>();
    private final Map<Long, String> blocks = new HashMap<>();
    private final Map<Long, List<Transaction>> pendingTransactions = new HashMap<>();
    private final Set<String> decidedTransactions = new HashSet<>();
    private long currentBlock = 0;

    // private final Map<Long, main.java.consensus.Block> deprecatedBlocks = new HashMap<>(); // FIXME - remove this

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
        logger.info("Blockchain initialized");
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
        if (pendingTransactions.putIfAbsent(blockIndex, transactions) == null) return false;
        transactions.forEach(transaction -> decidedTransactions.add(transaction.getSignatureBase64()));
        return true;
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
}
