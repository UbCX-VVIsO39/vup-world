package com.example.vupworld.service.progression;

import com.example.vupworld.dto.DailyHighlightDtos.DailyHighlightDTO;
import com.example.vupworld.dto.DailyHighlightDtos.HighlightListDTO;
import com.example.vupworld.mapper.DailyReportMapper;
import com.example.vupworld.model.DailyReport;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DailyHighlightService {

    private final DailyReportMapper dailyReportMapper;
    private final JsonService jsonService;

    public DailyHighlightService(DailyReportMapper dailyReportMapper, JsonService jsonService) {
        this.dailyReportMapper = dailyReportMapper;
        this.jsonService = jsonService;
    }

    public DailyHighlightDTO generateHighlight(Long vupId, int day, String actionType, String summary) {
        String title;
        String description;
        String icon;
        String highlightType;

        if (actionType.equals("STREAM_PLAN") && summary.contains("首播")) {
            title = "首播成功！";
            description = "你完成了第一次直播，弹幕说新人加油。";
            icon = "🎬";
            highlightType = "MILESTONE";
        } else if (actionType.equals("PUBLISH_VIDEO") && (summary.contains("爆火") || summary.contains("暴涨"))) {
            title = "视频爆火！";
            description = "视频爆火，粉丝暴涨，切片组开始调时间轴。";
            icon = "🔥";
            highlightType = "VIRAL";
        } else if (actionType.equals("PUBLISH_CLIP") && summary.contains("复读")) {
            title = "梗被复读！";
            description = "切片被复读，梗开始被查重，楼友开始翻旧账。";
            icon = "🔄";
            highlightType = "MEME";
        } else if (summary.contains("债务") || summary.contains("回旋镖")) {
            title = "债务产生！";
            description = "高风险标题产生债务，楼友开始等回旋镖。";
            icon = "💸";
            highlightType = "DEBT";
        } else if (summary.contains("涨粉") || summary.contains("粉丝")) {
            title = "粉丝增长！";
            description = "粉丝数量增长，DD顺手坐了一站。";
            icon = "👥";
            highlightType = "FAN";
        } else {
            title = "今日记录";
            description = summary.length() > 50 ? summary.substring(0, 50) + "..." : summary;
            icon = "📝";
            highlightType = "DAILY";
        }

        // Load existing highlights for this vupId+day from daily_report
        List<DailyHighlightDTO> existing = loadHighlightsForDay(vupId, day);

        DailyHighlightDTO highlight = new DailyHighlightDTO(
                (long) (existing.size() + 1),
                vupId,
                day,
                title,
                description,
                icon,
                highlightType,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        // Append and persist
        List<DailyHighlightDTO> updated = new ArrayList<>(existing);
        updated.add(highlight);
        saveHighlightsForDay(vupId, day, updated);

        return highlight;
    }

    public HighlightListDTO getHighlights(Long vupId) {
        List<DailyHighlightDTO> allHighlights = loadAllHighlights(vupId);
        String lastTitle = allHighlights.isEmpty() ? null : allHighlights.get(allHighlights.size() - 1).title();
        return new HighlightListDTO(allHighlights, allHighlights.size(), lastTitle);
    }

    @SuppressWarnings("unchecked")
    private List<DailyHighlightDTO> loadHighlightsForDay(Long vupId, int day) {
        DailyReport report = dailyReportMapper.findByVupIdAndDay(vupId, day);
        if (report == null) {
            return List.of();
        }
        return parseHighlights(report.getStrategyPanelJson());
    }

    @SuppressWarnings("unchecked")
    private List<DailyHighlightDTO> loadAllHighlights(Long vupId) {
        List<DailyReport> reports = dailyReportMapper.findAllByVupId(vupId);
        List<DailyHighlightDTO> all = new ArrayList<>();
        for (DailyReport report : reports) {
            all.addAll(parseHighlights(report.getStrategyPanelJson()));
        }
        return all;
    }

    private List<DailyHighlightDTO> parseHighlights(String strategyPanelJson) {
        if (strategyPanelJson == null || strategyPanelJson.isBlank() || "{}".equals(strategyPanelJson)) {
            return List.of();
        }
        try {
            Map<String, Object> map = jsonService.readMap(strategyPanelJson);
            Object raw = map.get("highlights");
            if (!(raw instanceof List<?> rawList)) {
                return List.of();
            }
            List<DailyHighlightDTO> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?>) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    result.add(new DailyHighlightDTO(
                            toLong(m.get("id")),
                            toLong(m.get("vupId")),
                            toInt(m.get("day")),
                            (String) m.get("title"),
                            (String) m.get("description"),
                            (String) m.get("icon"),
                            (String) m.get("highlightType"),
                            (String) m.get("createdAt")
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void saveHighlightsForDay(Long vupId, int day, List<DailyHighlightDTO> highlights) {
        DailyReport report = dailyReportMapper.findByVupIdAndDay(vupId, day);
        if (report != null) {
            dailyReportMapper.updateStrategyPanelJson(report.getId(),
                    jsonService.write(Map.of("highlights", highlights)));
        } else {
            DailyReport newReport = new DailyReport();
            newReport.setVupId(vupId);
            newReport.setDay(day);
            newReport.setSummary("");
            newReport.setReportTone("neutral");
            newReport.setRiskHint("");
            newReport.setVisibleItemsJson("[]");
            newReport.setEvidenceRefsJson("[]");
            newReport.setTemplateRefsJson("[]");
            newReport.setRenderVersion("highlight-v1");
            newReport.setStrategyPanelJson(jsonService.write(Map.of("highlights", highlights)));
            dailyReportMapper.insert(newReport);
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        return null;
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) return Integer.parseInt(s);
        return 0;
    }
}
