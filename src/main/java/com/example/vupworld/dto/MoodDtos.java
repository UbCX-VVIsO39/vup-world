package com.example.vupworld.dto;

import java.util.List;
import java.util.Map;

/**
 * VUP心情系统、叙事文本系统、术语词典、NPC关系系统、永久解锁系统、策略建议系统的DTO。
 */
public final class MoodDtos {
    private MoodDtos() {
    }

    /** 心情维度枚举 */
    public enum MoodType {
        ENERGIZED,  // 充能
        HAPPY,      // 开心
        NEUTRAL,    // 平静
        TIRED,      // 疲惫
        STRESSED,   // 压力
        EXCITED     // 兴奋
    }

    /** 心情计算结果 */
    public record MoodResult(
            MoodType mood,
            String moodLine,
            String effect
    ) {
    }

    /** 叙事文本结果 */
    public record NarrativeResult(
            String safeText,
            String trafficText,
            String memeText
    ) {
    }

    /** 术语DTO */
    public record TermDTO(
            String key,
            String displayName,
            String tooltipText,
            String example
    ) {
    }

    /** NPC互动结果 */
    public record NpcInteractionResult(
            String npcKey,
            String npcName,
            int affinityChange,
            int newAffinity,
            String dialogue
    ) {
    }

    /** 偷学结果 */
    public record StealLearnResult(
            String npcKey,
            String npcName,
            int affinityChange,
            int newAffinity,
            boolean discovered,
            int reputationChange,
            String buffDescription,
            String message
    ) {
    }

    /** 永久解锁DTO */
    public record PermanentUnlockDTO(
            String key,
            String label,
            String description,
            String sourceEnding,
            Map<String, Object> effect
    ) {
    }

    /** 策略建议DTO */
    public record StrategyAdviceDTO(
            String priority,
            String icon,
            String text,
            String reason
    ) {
    }
}
