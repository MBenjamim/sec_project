package test.java.conditional_collect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import main.java.conditional_collect.ConditionalCollectImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionalCollectImplTest {

    private ConditionalCollectImpl conditionalCollect;

    @BeforeEach
    public void setUp() {
        conditionalCollect = new ConditionalCollectImpl(4, 1);
    }

    @Test
    public void testAddProposedValue() {
        conditionalCollect.addProposedValue(1, "Value1");
        conditionalCollect.addProposedValue(2, "Value2");
        conditionalCollect.addProposedValue(3, "Value3");
        Map<Integer, String> proposedValues = conditionalCollect.collectValues();
        assertEquals(3, proposedValues.size());
        assertEquals("Value1", proposedValues.get(1));
    }

    @Test
    public void testCollectValues() {
        conditionalCollect.addProposedValue(1, "Value1");
        conditionalCollect.addProposedValue(2, "Value2");
        conditionalCollect.addProposedValue(3, "");

        Map<Integer, String> collectedValues = conditionalCollect.collectValues();
        assertEquals(2, collectedValues.size());
        assertTrue(collectedValues.containsKey(1));
        assertTrue(collectedValues.containsKey(2));
        assertFalse(collectedValues.containsKey(3));
    }

    @Test
    public void testCheckCondition() {
        assertTrue(conditionalCollect.checkCondition("Value1"));
        assertFalse(conditionalCollect.checkCondition(""));
        assertFalse(conditionalCollect.checkCondition(null));
    }
}