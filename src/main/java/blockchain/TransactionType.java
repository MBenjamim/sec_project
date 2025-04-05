package main.java.blockchain;

public enum TransactionType {
    // AccessControl.sol functions
    ADD_TO_BLACKLIST,
    AUTHORIZED_PARTIES,
    IS_BLACKLISTED,
    OWNER,
    REMOVE_FROM_BLACKLIST,
    SET_AUTHORIZED_PARTIES,

    // ISTCoin.sol functions
    ACCESS_CONTROL,
    ALLOWANCE,
    APPROVE,
    BALANCE_OF,
    DECIMALS,
    NAME,
    SYMBOL,
    TOTAL_SUPPLY,
    TRANSFER,
    TRANSFER_FROM,

    // Native currency functions
    NATIVE_BALANCE,
    NATIVE_TRANSFER,
}
