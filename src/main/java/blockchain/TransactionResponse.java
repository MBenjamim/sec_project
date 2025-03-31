package main.java.blockchain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionResponse {
    private final long transactionId;
    private final boolean status;
    private final String description;

    public TransactionResponse(long transactionId, boolean status, String description) {
        this.transactionId = transactionId;
        this.status = status;
        this.description = description;
    }
}
