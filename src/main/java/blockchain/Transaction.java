package main.java.blockchain;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
//@AllArgsConstructor
public class Transaction {
    long transactionId;

    public Transaction(long transactionId) {
        this.transactionId = transactionId;
    }
}
