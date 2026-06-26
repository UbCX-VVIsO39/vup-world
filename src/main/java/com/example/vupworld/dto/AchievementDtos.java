package com.example.vupworld.dto;

import java.util.List;

public class AchievementDtos {
    public record AchievementDTO(
            String id,
            String name,
            String description,
            String icon,
            int target,
            int progress,
            boolean unlocked,
            String progressText
    ) {}

    public record AchievementProgressDTO(
            List<AchievementDTO> achievements,
            long unlockedCount,
            int totalCount,
            int totalPoints,
            String title,
            AchievementTitleDTO featuredTitle,
            List<AchievementTitleDTO> earnedTitles,
            AchievementTitleDTO nextTitle,
            EndingCollectionDTO endingCollection
    ) {}

    public record AchievementTitleDTO(
            String titleKey,
            String label,
            String tier,
            String description,
            boolean unlocked,
            String progressText
    ) {}

    public record EndingAtlasSlotDTO(
            String id,
            String type,
            String name,
            String description,
            String icon,
            boolean unlocked,
            String progressText,
            int bestScore,
            String bestGrade,
            String bestGradeLabel,
            String nextGrade,
            int nextGradeScore,
            String nextGradeHint,
            String routeRecipe,
            String gateHint,
            String trapHint
    ) {}

    public record EndingCollectionDTO(
            List<EndingAtlasSlotDTO> endings,
            long unlockedCount,
            int totalCount,
            int completedRuns,
            String nextTargetHint,
            String nextTargetType,
            String nextTargetLabel,
            String nextRunGoal
    ) {}
}
