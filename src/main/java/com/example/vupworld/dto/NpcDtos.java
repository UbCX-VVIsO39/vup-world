package com.example.vupworld.dto;

import java.util.List;

public final class NpcDtos {
    private NpcDtos() {
    }

    public record NpcSpotlightDTO(
            String npcKey,
            String displayName,
            String role,
            String image,
            String moodLine,
            String advice,
            String relevance
    ) {
    }

    /** NPC亲密度DTO */
    public record NpcRelationshipDTO(
            String key,
            String name,
            int affinity,
            String label
    ) {
    }

    /** 平台主播DTO：包含亲密度和偷学状态 */
    public record PlatformNPCDTO(
            String key,
            String name,
            int affinity,
            String label,
            boolean canStealLearn,
            boolean stealUnlocked
    ) {
    }

    public record RivalDTO(
            String name,
            String route,
            int fans,
            int growthRate,
            String threatLevel
    ) {
    }

    public record RivalProgressDTO(
            List<RivalDTO> rivals,
            boolean overtaken
    ) {
    }

    public record LeaderboardItem(
            String name,
            int score,
            String avatar,
            boolean isPlayer
    ) {
    }

    public record LeaderboardDTO(
            List<LeaderboardItem> fans,
            List<LeaderboardItem> gifts,
            List<LeaderboardItem> danmaku
    ) {
    }
}
