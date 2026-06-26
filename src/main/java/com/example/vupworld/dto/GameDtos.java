package com.example.vupworld.dto;

import com.example.vupworld.dto.AuthDtos.UserDTO;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.VupDtos.VupStateDTO;

import java.util.List;
import java.util.Map;

public final class GameDtos {
    private GameDtos() {
    }

    public record GameBootstrapDTO(
            boolean hasSave,
            UserDTO user,
            VupStateDTO activeRun,
            DaySessionDTO daySession,
            List<SaveSlotDTO> saveSlots
    ) {
    }

    public record GameStartDTO(
            UserDTO user,
            VupStateDTO activeRun,
            DaySessionDTO daySession,
            List<SaveSlotDTO> saveSlots
    ) {
    }

    public record SaveSlotDTO(
            int slotNumber,
            boolean occupied,
            Long vupId,
            String name,
            int day,
            String phase,
            String status,
            String currentRoute,
            boolean canContinue,
            String summary,
            String endingType,
            String endingTitle,
            String lastSavedAt,
            String lastSavedSummary
    ) {
    }

    public record RenameSaveSlotRequest(
            String name
    ) {
    }

    public record SaveArchiveDTO(
            String saveVersion,
            String exportedAt,
            int sourceSlotNumber,
            SaveSlotDTO slot,
            Map<String, Object> player,
            Map<String, Object> saveSlot,
            Map<String, Object> manualSave,
            Map<String, Object> vup,
            List<Map<String, Object>> daySessions,
            List<Map<String, Object>> businessLogs,
            List<Map<String, Object>> dailyReports,
            List<Map<String, Object>> endingReviews,
            List<Map<String, Object>> riskDebts,
            List<Map<String, Object>> unlocks,
            List<Map<String, Object>> statsHistory
    ) {
    }
}
