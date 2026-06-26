package com.example.vupworld.dto;

import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.VupDtos.VupStateDTO;

import java.util.List;
import java.util.Map;

public final class DevDemoDtos {
    private DevDemoDtos() {
    }

    public record DemoResetRequest(String scenario, String runSeed) {
    }

    public record DemoResetResult(
            Long userId,
            String username,
            String runSeed,
            VupStateDTO vup,
            DaySessionDTO daySession
    ) {
    }

    public record DemoRunScriptRequest(String strategy, Integer targetDay, String runSeed, Boolean stopOnError) {
    }

    public record DemoFastForwardRequest(Integer targetDay) {
    }

    public record DemoRunScriptResult(
            String strategy,
            String runSeed,
            Long vupId,
            int currentDay,
            int day,
            String phase,
            int reportCount,
            List<Long> reportIds,
            int businessLogCount,
            int logCount,
            Long endingReviewId,
            String endingType,
            String summary,
            Map<String, Object> lastDaySnapshot,
            String endingReason
    ) {
    }

    public record DemoStatusResult(
            Long userId,
            String username,
            String runSeed,
            int currentDay,
            int day,
            String phase,
            int reportCount,
            int businessLogCount,
            int logCount,
            Long endingReviewId,
            String endingType,
            String summary,
            Map<String, Object> lastDaySnapshot,
            String endingReason,
            DemoLastErrorDTO lastError
    ) {
    }

    public record DemoLastErrorDTO(
            String operation,
            String code,
            String message,
            Boolean stopOnError
    ) {
    }
}
