package com.example.vupworld.dto;

import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record DailyReportDTO(
            Long id,
            int day,
            String summary,
            String selectedTitle,
            Map<String, Integer> dataDelta,
            Map<String, Object> platformTrend,
            List<String> highlights,
            List<String> visibleItems,
            List<Map<String, Object>> evidenceRefs,
            String debtSummary,
            String riskHint,
            boolean nextDayEnabled
    ) {
    }
}
