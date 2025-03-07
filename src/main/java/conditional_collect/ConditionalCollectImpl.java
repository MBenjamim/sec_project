package main.java.conditional_collect;

import java.util.HashMap;
import java.util.Map;

public class ConditionalCollectImpl implements ConditionalCollect {
    private final Map<Integer, String> proposedValues = new HashMap<>();

    @Override
    public void proposeValue(int processId, String value) {
        proposedValues.put(processId, value);
    }

    @Override
    public Map<Integer, String> collectValues() {
        Map<Integer, String> collectedValues = new HashMap<>();
        for (Map.Entry<Integer, String> entry : proposedValues.entrySet()) {
            if (checkCondition(entry.getValue())) {
                collectedValues.put(entry.getKey(), entry.getValue());
            }
        }
        return collectedValues;
    }

    @Override
    public boolean checkCondition(String value) {
        // Define condition here. For example, only collect values that are not null.
        return value != null && !value.isEmpty();
    }
}
