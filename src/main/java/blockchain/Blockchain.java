package main.java.blockchain;

import lombok.Getter;
import main.java.common.NodeRegistry;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Blockchain {
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    private SmartContractExecutor executor;
    private SimpleWorld world;
    private final Map<Address, NodeRegistry> clients = new HashMap<>();
    private final Map<Long, String> blocks = new HashMap<>();
    private final Map<Long, List<Transaction>> pendingTransactions = new HashMap<>();
    private long currentBlock = 0;

    private final Map<Long, main.java.consensus.Block> deprecatedBlocks = new HashMap<>(); // FIXME

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
        this.executor = new SmartContractExecutor(world);
        this.executor.setTokenContract(world.getAccount(genesisBlock.getBlacklistAddress()));
        this.executor.setTokenContract(world.getAccount(genesisBlock.getTokenAddress()));
        blocks.put(currentBlock, genesisBlock.toJson());
        currentBlock++;
        logger.info("Blockchain initialized");
    }

    /**
     * Add transaction to be performed at a certain block.
     * Useful to perform transactions in parallel with future consensus.
     *
     * @param blockIndex   the index of the block where transactions should be performed
     * @param transactions the list of transactions to be performed at the block
     */
    synchronized public void addTransactionsForBlock(long blockIndex, List<Transaction> transactions) {
        pendingTransactions.put(blockIndex, transactions);
    }

    /**
     * Remove the pending transactions when the block is added to the blockchain.
     *
     * @param blockIndex the index of the block where transactions were performed
     */
    synchronized public void removeTransactionsForBlock(long blockIndex) {
        pendingTransactions.remove(blockIndex);
    }
}
