package com.velorise.emc.common;

public final class InteractionRuleProfileTest {
    public static void main(String[] args) {
        assertEquals(4, InteractionRuleProfile.stepLevel(4, 1, 4, false));
        assertEquals(1, InteractionRuleProfile.stepLevel(1, 1, 4, true));
        assertEquals(3, InteractionRuleProfile.stepCycle(0, 4, true));
        assertEquals(0, InteractionRuleProfile.stepCycle(3, 4, false));
        assertEquals(8_096L, PurificationProfile.operationCost(1));
        assertEquals(8_096L, PurificationProfile.operationCost(4));
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
