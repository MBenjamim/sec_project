package main.java.utils;

public enum Behavior {
    // servers
    CORRECT,
    NO_RESPONSE_TO_ALL_SERVERS,
    NO_RESPONSE_TO_LEADER,
    WRONG_WRITE,
    WRONG_READ_RESPONSE,
    DELAY,
    BEGIN_WRONG_WRITE_AFTER_40_seconds,

    // clients
    REPLAY_ATTACK,
}
