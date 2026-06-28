package com.example.vupworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class VupDtos {
    private VupDtos() {
    }

    public record CreateVupRequest(
            @NotBlank(message = "VUP名称不能为空") @Size(min = 1, max = 64, message = "名称长度1-64") String name,
            @NotBlank(message = "人设不能为空") @Size(min = 1, max = 255, message = "人设长度1-255") String persona,
            String difficulty
    ) {
    }

    public record VupStateDTO(
            Long id,
            Long vupId,
            int slotNumber,
            String name,
            String persona,
            int day,
            int maxDay,
            String phase,
            String currentRoute,
            Map<String, Object> route,
            Map<String, Object> expectations,
            ResourceDTO resources,
            AttributeDTO attributes,
            FanStructureDTO fanStructure,
            OpinionDTO opinion,
            List<DebtDTO> debts,
            Map<String, Object> funProfile,
            Map<String, Object> platformTrend,
            List<TutorialHintDTO> tutorialHints,
            String mood,
            String moodLine,
            String moodEffect,
            List<Map<String, Object>> strategyAdvice,
            List<Map<String, Object>> permanentUnlocks
    ) {
    }

    public record TutorialHintDTO(String key, String text) {
    }

    public record ResourceDTO(int stamina, int maxStamina, int coin, int inspiration) {
    }

    public record AttributeDTO(
            int songPower,
            int dancePower,
            int talkPower,
            int memePower,
            int planPower,
            int stressPower
    ) {
    }

    public record FanStructureDTO(
            int fans,
            int trueFans,
            int funFans,
            int unicornFans,
            int ddFans
    ) {
    }

    public record OpinionDTO(
            int popularity,
            int watchHeat,
            int reputation,
            int memeLevel,
            int commercialLevel
    ) {
    }

    public record DebtDTO(
            Long id,
            String debtType,
            String status,
            int severity,
            int createDay,
            int dueDay,
            String summary,
            String debtLabel,
            int remainingDays,
            String crisisLevelLabel,
            String consequencePreview,
            String recommendedAction,
            boolean urgent
    ) {
    }
}
