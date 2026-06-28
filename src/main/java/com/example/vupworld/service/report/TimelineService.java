package com.example.vupworld.service.report;

import com.example.vupworld.dto.ReportDtos.TimelineDayDTO;
import com.example.vupworld.dto.ReportDtos.TimelineDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.BusinessLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 回放服务：聚合 BusinessLog 时间线为 TimelineDTO。
 */
@Service
public class TimelineService {

    private final BusinessLogMapper businessLogMapper;

    public TimelineService(BusinessLogMapper businessLogMapper) {
        this.businessLogMapper = businessLogMapper;
    }

    public TimelineDTO timeline(Long vupId) {
        List<BusinessLog> logs = businessLogMapper.findTimelineByVupId(vupId);
        List<TimelineDayDTO> days = new ArrayList<>();
        for (BusinessLog log : logs) {
            int routeScore = parseRouteScore(log.getRouteScoreChange());
            String highlight = log.getResult() != null ? log.getResult() : "";
            days.add(new TimelineDayDTO(
                    log.getDay(),
                    log.getAction(),
                    log.getFanChange(),
                    routeScore,
                    highlight
            ));
        }
        return new TimelineDTO(List.copyOf(days));
    }

    private int parseRouteScore(String routeScoreChange) {
        if (routeScoreChange == null || routeScoreChange.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(routeScoreChange.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
