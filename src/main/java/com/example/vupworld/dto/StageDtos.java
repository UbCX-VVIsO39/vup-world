package com.example.vupworld.dto;

import java.util.List;
import java.util.Map;

public final class StageDtos {
    private StageDtos() {
    }

    public record StageObjectiveItemDTO(
            String label,
            String state,
            String hint,
            boolean achieved,
            String objectiveKey,
            String recommendedActionType,
            String targetRouteType
    ) {
        public StageObjectiveItemDTO(String label, String state, String hint, boolean achieved) {
            this(label, state, hint, achieved, "", "", "");
        }
    }

    public record StageBriefingDTO(
            String stageLabel,
            StageRhythmDTO stageRhythm,
            int currentDay,
            int nextMilestoneDay,
            int daysUntilMilestone,
            boolean milestoneToday,
            Map<String, Object> currentTrend,
            Map<String, Object> nextTrend,
            String nextTrendHint,
            String routeSnapshot,
            RouteTendencyDTO routeTendency,
            String fanSnapshot,
            String riskSnapshot,
            String actionHint,
            String objectiveActionType,
            String objectiveTitle,
            String objectiveSummary,
            int objectiveProgress,
            String objectiveProgressLabel,
            int objectiveStreak,
            int objectiveNextStreak,
            String objectiveRewardPreview,
            List<StageObjectiveItemDTO> objectiveItems,
            String image,
            String dailyFortune,
            String dailyFortuneAdvice,
            String dailyFortuneIcon
    ) {
    }

    public record StageRhythmDTO(
            String phaseKey,
            String phaseName,
            int startDay,
            int endDay,
            String reason
    ) {
    }

    public record RouteTendencyDTO(
            RouteCandidateDTO primaryRoute,
            List<RouteCandidateDTO> competingRoutes,
            String reason
    ) {
    }

    public record RouteCandidateDTO(
            String routeType,
            String label,
            int score,
            int scoreGapToPrimary,
            String reason
    ) {
    }
}
