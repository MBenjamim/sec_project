package main.java.blockchain;

import lombok.Getter;
import main.java.utils.DataUtils;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
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
import java.util.HashMap;
import java.util.Map;

@Getter
public class SmartContractExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SmartContractExecutor.class);

    private final EVMExecutor executor;
    private Bytes tokenBytecode = null;
    private Bytes blacklistBytecode = null;
    private Address tokenAddress = null;
    private Address blacklistAddress = null;
    private final ByteArrayOutputStream outputStream;
    private final Map<String, TransactionType> signatureToType = new HashMap<>();
    private final Map<TransactionType, String> typeToSignature = new HashMap<>();

    public SmartContractExecutor(SimpleWorld world) {
        this.outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
                .tracer(tracer)
                .worldUpdater(world.updater())
                .commitWorldState();

        // AccessControl.sol functions (commented are not implemented)
        addFunctionSignature("addToBlacklist(address)", TransactionType.ADD_TO_BLACKLIST);
        //addFunctionSignature("authorizedParties(address)", TransactionType.AUTHORIZED_PARTIES);
        addFunctionSignature("isBlacklisted(address)", TransactionType.IS_BLACKLISTED);
        //addFunctionSignature("owner()", TransactionType.OWNER);
        addFunctionSignature("removeFromBlacklist(address)", TransactionType.REMOVE_FROM_BLACKLIST);
        //addFunctionSignature("setAuthorizedParty(address,bool)", TransactionType.SET_AUTHORIZED_PARTIES);

        // ISTCoin.sol functions (commented are not implemented)
        //addFunctionSignature("accessControl()", TransactionType.ACCESS_CONTROL);
        addFunctionSignature("allowance(address,address)", TransactionType.ALLOWANCE);
        addFunctionSignature("approve(address,uint256)", TransactionType.APPROVE);
        addFunctionSignature("balanceOf(address)", TransactionType.BALANCE_OF);
        //addFunctionSignature("decimals()", TransactionType.DECIMALS);
        //addFunctionSignature("name()", TransactionType.NAME);
        //addFunctionSignature("symbol()", TransactionType.SYMBOL);
        addFunctionSignature("totalSupply()", TransactionType.TOTAL_SUPPLY);
        addFunctionSignature("transfer(address,uint256)", TransactionType.TRANSFER);
        addFunctionSignature("transferFrom(address,address,uint256)", TransactionType.TRANSFER_FROM);
    }

    public SmartContractExecutor(SimpleWorld world, Address blacklist, Address token) {
        this(world);
        setBlacklistContract(world.getAccount(blacklist));
        setTokenContract(world.getAccount(token));
    }

    private void addFunctionSignature(String function, TransactionType type) {
        String functionSignature = DataUtils.getFunctionSignature(function);
        signatureToType.put(functionSignature, type);
        typeToSignature.put(type, functionSignature);
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
                .callData(Bytes.EMPTY)
                .sender(ownerAddr)
                .receiver(contractAddr)
                .execute();
        outputStream.reset();
        return contractAddr;
    }

    /**
     * Executes a smart contract, updating the world state.
     *
     * @param caller          the address of the user calling the smart contract
     * @param bytecode        the bytecode of the smart contract to be executed
     * @param contractAddress the address of smart contract to be executed
     * @param input           the input arguments including function signature
     */
    private void executeContract(Address caller, Bytes bytecode, Address contractAddress, Bytes input) {
        outputStream.reset(); // keep only info about this execution
        executor.messageFrameType(MessageFrame.Type.MESSAGE_CALL)
                .code(bytecode)
                .callData(input)
                .sender(caller)
                .receiver(contractAddress)
                .execute();
    }

    /**
     * Executes addToBlacklist(address) method from AccessControl smart contract.
     */
    public TransactionResponse addToBlacklist(Address caller, Address address) {
        String functionSignature = typeToSignature.get(TransactionType.ADD_TO_BLACKLIST);
        String paddedAddress = DataUtils.padHexString(address.toHexString());

        executeContract(caller, blacklistBytecode, blacklistAddress,
                Bytes.fromHexString(functionSignature + paddedAddress));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    /**
     * Executes isBlacklisted(address) method from AccessControl smart contract.
     */
    public TransactionResponse isBlacklisted(Address caller, Address address) {
        String functionSignature = typeToSignature.get(TransactionType.IS_BLACKLISTED);
        String paddedAddress = DataUtils.padHexString(address.toHexString());

        executeContract(caller, blacklistBytecode, blacklistAddress,
                Bytes.fromHexString(functionSignature + paddedAddress));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    /**
     * Executes removeFromBlacklist(address) method from AccessControl smart contract.
     */
    public TransactionResponse removeFromBlacklist(Address caller, Address address) {
        String functionSignature = typeToSignature.get(TransactionType.REMOVE_FROM_BLACKLIST);
        String paddedAddress = DataUtils.padHexString(address.toHexString());

        executeContract(caller, blacklistBytecode, blacklistAddress,
                Bytes.fromHexString(functionSignature + paddedAddress));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    /**
     * Executes "allowance(address,address)" method from ISTCoin smart contract.
     */
    public TransactionResponse allowance(Address caller, Address owner, Address spender) {
        String functionSignature = typeToSignature.get(TransactionType.ALLOWANCE);
        String paddedAddress1 = DataUtils.padHexString(owner.toHexString());
        String paddedAddress2 = DataUtils.padHexString(spender.toHexString());

        executeContract(caller, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature + paddedAddress1 + paddedAddress2));

        return ReturnDataParser.getResult(outputStream, ReturnType.UINT256);
    }

    /**
     * Executes "approve(address,uint256)" method from ISTCoin smart contract.
     */
    public TransactionResponse approve(Address owner, Address spender, double amount) {
        String functionSignature = typeToSignature.get(TransactionType.APPROVE);
        String paddedAddress = DataUtils.padHexString(spender.toHexString());
        String value = DataUtils.convertNumberToHex256Bit(DataUtils.convertAmountToLong(amount));

        executeContract(owner, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature + paddedAddress + value));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    /**
     * Executes "balanceOf(address)" method from ISTCoin smart contract.
     */
    public TransactionResponse balanceOf(Address caller, Address address) {
        String functionSignature = typeToSignature.get(TransactionType.BALANCE_OF);
        String paddedAddress = DataUtils.padHexString(address.toHexString());

        executeContract(caller, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature + paddedAddress));

        return ReturnDataParser.getResult(outputStream, ReturnType.UINT256);
    }

    /**
     * Executes totalSupply() method from ISTCoin smart contract.
     */
    public TransactionResponse totalSupply(Address caller) {
        String functionSignature = typeToSignature.get(TransactionType.TOTAL_SUPPLY);

        executeContract(caller, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature));

        return ReturnDataParser.getResult(outputStream, ReturnType.UINT256);
    }

    /**
     * Executes transfer(address,uint256) method from ISTCoin smart contract.
     */
    public TransactionResponse transfer(Address from, Address to, double amount) {
        String functionSignature = typeToSignature.get(TransactionType.TRANSFER);
        String paddedReceiver = DataUtils.padHexString(to.toHexString());
        String value = DataUtils.convertNumberToHex256Bit(DataUtils.convertAmountToLong(amount));

        executeContract(from, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature + paddedReceiver + value));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    /**
     * Executes transferFrom(address,address,uint256) method from ISTCoin smart contract.
     */
    public TransactionResponse transferFrom(Address caller, Address from, Address to, double amount) {
        String functionSignature = typeToSignature.get(TransactionType.TRANSFER_FROM);
        String paddedSender = DataUtils.padHexString(from.toHexString());
        String paddedReceiver = DataUtils.padHexString(to.toHexString());
        String value = DataUtils.convertNumberToHex256Bit(DataUtils.convertAmountToLong(amount));

        executeContract(caller, tokenBytecode, tokenAddress,
                Bytes.fromHexString(functionSignature + paddedSender + paddedReceiver + value));

        return ReturnDataParser.getResult(outputStream, ReturnType.BOOL);
    }

    public TransactionResponse execute(Transaction transaction) {
        String functionSignature = transaction.getFunctionSignature();
        TransactionType type = signatureToType.get(functionSignature);
        Address msgSender = transaction.getSenderAddress();
        TransactionResponse response;
        switch (type) {
            case ADD_TO_BLACKLIST:
                response = addToBlacklist(msgSender, transaction.getReceiverAddress());
                break;
            case IS_BLACKLISTED:
                response = isBlacklisted(msgSender, transaction.getReceiverAddress());
                break;
            case REMOVE_FROM_BLACKLIST:
                response = removeFromBlacklist(msgSender, transaction.getReceiverAddress());
                break;
            case APPROVE:
                response = approve(msgSender, transaction.getReceiverAddress(), transaction.getAmount());
                break;
            case ALLOWANCE:
                response = allowance(msgSender, transaction.getOwnerAddress(), transaction.getReceiverAddress());
                break;
            case BALANCE_OF:
                response = balanceOf(msgSender, transaction.getReceiverAddress());
                break;
            case TOTAL_SUPPLY:
                response = totalSupply(msgSender);
                break;
            case TRANSFER:
                response = transfer(msgSender, transaction.getReceiverAddress(), transaction.getAmount());
                break;
            case TRANSFER_FROM:
                response = transferFrom(msgSender, transaction.getOwnerAddress(), transaction.getReceiverAddress(), transaction.getAmount());
                break;
            default:
                logger.error("Function not implemented: {}", type);
                return null;
        }
        response.setTransactionInfo(transaction.getTransactionId(), transaction.getSignature(), type);
        return response;
    }

}
