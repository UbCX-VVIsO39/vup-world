package com.example.vupworld.dto;

import java.util.Map;

public final class StressTestDtos {
    private StressTestDtos() {
    }

    public record StressRunRequest(
            String strategy,
            Integer rounds,
            String runSeed,
            Boolean useReal
    ) {
    }

    public record StressRunResult(
            String strategy,
            String runSeed,
            int rounds,
            int daysPerRound,
            Map<String, Integer> endingDistribution,
            Map<String, Integer> routeScoreDistribution,
            Map<String, Integer> endingReasonDistribution,
            double avgFinalFans,
            double avgReputation,
            double avgMemeLevel,
            double avgUnresolvedDebts,
            int maxSingleDayFanGain,
            int negativeEventStreakMax,
            double topEventFrequency,
            int stageCapHitCount,
            int memeFatigueTriggerCount,
            int boomerangPhaseTriggerCount,
            int debtDelayCount,
            Map<String, Object> qualification
    ) {
    }
}
