package main.java.blockchain;

import lombok.Getter;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class NativeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(NativeExecutor.class);

    SimpleWorld world;

    public NativeExecutor(SimpleWorld world) {
        this.world = world;
    }

    /**
     * Get the native currency balance of an account.
     */
    public TransactionResponse balanceOf(Address address) {
        MutableAccount account = world.getAccount(address);
        boolean status = (account != null);
        TransactionResponse result = new TransactionResponse(status, ReturnType.WEI);
        result.setDescription((status) ? "Transaction performed successfully." : "Account not found for" + address);
        return result;
    }

    /**
     * Transfer native currency from an account to another.
     */
    public TransactionResponse transfer(Address from, Address to, Wei amount) {
        MutableAccount accountFrom = world.getAccount(from);
        MutableAccount accountTo = world.getAccount(to);
        if (accountTo == null) {
            TransactionResponse result = new TransactionResponse(false, ReturnType.BOOL);
            result.setDescription("Account not found for " + from);
            return result;
        }

        // Check if from account has sufficient amount in Wei
        if (accountFrom.getBalance().compareTo(amount) < 0) {
            TransactionResponse result = new TransactionResponse(false, ReturnType.BOOL);
            result.setDescription("Insufficient balance");
            return result;
        }

        accountFrom.decrementBalance(amount);
        accountTo.incrementBalance(amount);

        TransactionResponse result = new TransactionResponse(true, ReturnType.BOOL);
        result.setDescription("Transaction performed successfully");
        return result;
    }

    public TransactionResponse performTransaction(Transaction transaction) {
        TransactionResponse response;
        TransactionType type = transaction.getNativeOperation();
        switch (type) {
            case NATIVE_BALANCE:
                response = balanceOf(transaction.getReceiverAddress());
                break;
            case NATIVE_TRANSFER:
                response = transfer(transaction.getSenderAddress(), transaction.getReceiverAddress(), transaction.getWeiAmount());
                break;
            default:
                logger.error("Operation not implemented: {}", type);
                return null;
        }
        response.setTransactionInfo(transaction.getTransactionId(), transaction.getSignature(), type);
        return response;
    }
}
