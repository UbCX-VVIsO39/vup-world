package com.example.vupworld.dto;

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
}
