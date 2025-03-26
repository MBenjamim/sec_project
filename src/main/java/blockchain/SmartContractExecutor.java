package main.java.blockchain;

import main.java.utils.DataUtils;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;

public class SmartContractExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SmartContractExecutor.class);

    private static final int DECIMALS = 2;
    private final EVMExecutor executor;
    private Bytes tokenBytecode = null;
    private Bytes blacklistBytecode = null;
    private Address tokenAddress = null;
    private Address blacklistAddress = null;
    private final ByteArrayOutputStream outputStream;

    public SmartContractExecutor(SimpleWorld world) {
        this.outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
                .tracer(tracer)
                .worldUpdater(world.updater())
                .commitWorldState();
    }

    /**
     * Set fields to be able to execute "ISTCoin.sol" methods.
     *
     * @param contract the ISTCoin smart contract account
     */
    public void setTokenContract(MutableAccount contract) {
        this.tokenBytecode = contract.getCode();
        this.tokenAddress = contract.getAddress();
    }

    /**
     * Set fields to be able to execute "AccessControl.sol" methods.
     *
     * @param contract the AccessControl smart contract account
     */
    public void setBlacklistContract(MutableAccount contract) {
        this.blacklistBytecode = contract.getCode();
        this.blacklistAddress = contract.getAddress();
    }

    /**
     * Deploys a smart contract in the world, creating a contract account.
     *
     * @param bytecode  the compiled bytecode of the smart contract to deploy
     * @param ownerAddr the "msg.sender" that will be the owner of the deployed contract
     * @param nonce     the unique value for address generation
     * @return the address of the deployed contract
     */
    public Address deployContract(Bytes bytecode, Address ownerAddr, long nonce) {
        Address contractAddr = Address.contractAddress(ownerAddr, nonce);
        executor.messageFrameType(MessageFrame.Type.CONTRACT_CREATION)
                .contract(contractAddr)
                .code(bytecode)
                .sender(ownerAddr)
                .receiver(contractAddr)
                .execute();
        return contractAddr;
    }

    /**
     * Methods from AccessControl.sol are implemented below:
     *  - addToBlacklist(address)          implemented
     *  - authorizedParties(address)       TODO
     *  - isBlacklisted(address)           implemented
     *  - owner()                          TODO
     *  - removeFromBlacklist(address)     implemented
     *  - setAuthorizedParty(address,bool) TODO
     */

    /**
     * Executes addToBlacklist(address) method from AccessControl smart contract.
     */
    public boolean addToBlacklist(Address caller, Address address) {
        String functionSignature = DataUtils.getFunctionSignature("addToBlacklist(address)");
        String paddedAddress = DataUtils.padHexString(address.toHexString());
        executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                .code(blacklistBytecode)
                .callData(Bytes.fromHexString(functionSignature + paddedAddress))
                .sender(caller)
                .receiver(blacklistAddress)
                .execute();
        return true; // FIXME - real return value
    }

    /**
     * Executes isBlacklisted(address) method from AccessControl smart contract.
     */
    public boolean isBlacklisted(Address caller, Address address) {
        String functionSignature = DataUtils.getFunctionSignature("isBlacklisted(address)");
        String paddedAddress = DataUtils.padHexString(address.toHexString());
        executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                .code(blacklistBytecode)
                .callData(Bytes.fromHexString(functionSignature + paddedAddress))
                .sender(caller)
                .receiver(blacklistAddress)
                .execute();
        return true; // FIXME - real return value
    }

    /**
     * Executes removeFromBlacklist(address) method from AccessControl smart contract.
     */
    public boolean removeFromBlacklist(Address caller, Address address) {
        String functionSignature = DataUtils.getFunctionSignature("removeFromBlacklist(address)");
        String paddedAddress = DataUtils.padHexString(address.toHexString());
        executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                .code(blacklistBytecode)
                .callData(Bytes.fromHexString(functionSignature + paddedAddress))
                .sender(caller)
                .receiver(blacklistAddress)
                .execute();
        return true; // FIXME - real return value
    }

    /**
     * Methods from ISTCoin.sol are implemented below:
     *  - accessControl()                       TODO
     *  - allowance(address,address)            TODO
     *  - approve(address,uint256)              TODO
     *  - balanceOf(address)                    TODO
     *  - decimals()                            TODO
     *  - name()                                TODO
     *  - symbol()                              TODO
     *  - totalSupply()                         TODO
     *  - transfer(address,uint256)             implemented
     *  - transferFrom(address,address,uint256) TODO
     */

    /**
     * Executes transfer(address,uint256) method from ISTCoin smart contract.
     */
    public void transfer(Address from, Address to, double amount) {
        String functionSignature = DataUtils.getFunctionSignature("transfer(address,uint256)");
        String paddedReceiver = DataUtils.padHexString(to.toHexString());
        String value = DataUtils.convertNumberToHex256Bit(getAmount(amount));
        executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                .code(tokenBytecode)
                .callData(Bytes.fromHexString(functionSignature + paddedReceiver + value))
                .sender(from)
                .receiver(tokenAddress)
                .execute();
    }

    /**
     * FIXME - for some reason balances are not updated directly by Transfer() event
     */
    public void fixBalancesFromStorage(SimpleWorld world) {
        String functionSignature = DataUtils.getFunctionSignature("balanceOf(address)");
        Collection<MutableAccount> list = (Collection<MutableAccount>) world.getTouchedAccounts();
        for (MutableAccount account : list) {
            String paddedAddr = DataUtils.padHexString(account.getAddress().toHexString());
            executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                    .code(tokenBytecode)
                    .callData(Bytes.fromHexString(functionSignature + paddedAddr))
                    .sender(tokenAddress)
                    .receiver(tokenAddress)
                    .execute();
            // update balance manually
            account.setBalance(Wei.fromHexString(Long.toHexString(DataUtils.extractLongFromReturnData(outputStream))));
            executor.worldUpdater(world.updater()).commitWorldState();
        }
    }

    /**
     * Gets the amount including decimals.
     *
     * @param amount the real amount of the token
     * @return the amount understandable by solidity contracts
     */
    public static long getAmount(double amount) {
        long decimals = BigInteger.TEN.pow(DECIMALS).longValue();
        double roundedAmount = Math.floor(amount * decimals) / (double) decimals;
        return (long) (roundedAmount * decimals);
    }
}
