package main.java.conditional_collect;

import java.util.Map;

public interface ConditionalCollect {
    void proposeValue(int processId, String value);
    Map<Integer, String> collectValues();
    boolean checkCondition(String value);
}
