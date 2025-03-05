package main.java.conditionalcollect;

import java.util.Map;

public interface ConditionalCollect {
    void proposeValue(int processId, String value);
    Map<Integer, String> collectValues();
    boolean checkCondition(String value);
}
