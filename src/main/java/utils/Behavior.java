package main.java.utils;

public enum Behavior {
    // servers
    CORRECT,
    NO_RESPONSE_TO_ALL_SERVERS, //test1
    NO_RESPONSE_TO_LEADER, //test2
    WRONG_WRITE, //test3
    WRONG_READ_RESPONSE, //test4
    DELAY,
    BEGIN_WRONG_WRITE_AFTER_40_seconds, //test6
    DONT_VERIFY_TRANSACTIONS, //test5

    // clients
    REPLAY_ATTACK, //test5
}
