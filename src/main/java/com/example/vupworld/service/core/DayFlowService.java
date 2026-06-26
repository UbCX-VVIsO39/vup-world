package com.example.vupworld.service.core;

import com.example.vupworld.service.ending.EndingService;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.DayPhaseStateMachine;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DailyReport;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一状态收尾入口。负责日报生成后的阶段收尾和路线分计算。
 * <p>
 * Gate 3 要求：DayFlowService 是唯一收尾入口。
 */
@Service
public class DayFlowService {

    private static final Logger log = LoggerFactory.getLogger(DayFlowService.class);

    private final EndingService endingService;
    private final JsonService jsonService;
    private final DaySessionMapper daySessionMapper;
    private final BalanceConfig balanceConfig;

    public DayFlowService(
            EndingService endingService,
            JsonService jsonService,
            DaySessionMapper daySessionMapper,
            BalanceConfig balanceConfig
    ) {
        this.endingService = endingService;
        this.jsonService = jsonService;
        this.daySessionMapper = daySessionMapper;
        this.balanceConfig = balanceConfig;
    }

    public DaySession requireCurrentSession(Vup vup) {
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (session == null) {
            throw new GameException("SYSTEM_ERROR", "当前天数状态丢失。");
        }
        return session;
    }

    /**
     * 主行动完成后持久化DaySession状态变更。
     */
    public void commitSessionAfterAction(DaySession daySession) {
        daySessionMapper.updateAfterAction(daySession);
    }

    public String actionLabel(String actionType) {
        if (actionType == null || actionType.isBlank()) return "当日行动";
        return switch (actionType) {
            case "TRAIN_SONG" -> "练歌";
            case "TRAIN_DANCE" -> "练舞";
            case "TRAIN_TALK" -> "杂谈复盘";
            case "STREAM_PLAN" -> "直播企划";
            case "PUBLISH_VIDEO" -> "发布视频";
            case "PUBLISH_CLIP" -> "发布切片";
            case "FAN_GROUP_MAINTAIN" -> "粉丝群维护";
            case "NPC_INTERACT" -> "同台互动";
            case "REST" -> "休息";
            case "RISK_TOOL" -> "米线工具";
            default -> "当日行动";
        };
    }

    public String routeLabel(String route) {
        if (route == null || route.isBlank()) route = "UNKNOWN";
        return switch (route) {
            case "SLICE_SAINT" -> "切片路线";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "SINGING_IDOL" -> "歌势路线";
            case "DANCE_MEME" -> "梗舞整活";
            default -> "路线仍在摇摆";
        };
    }

    /**
     * 日报生成后的收尾：最终日生成结局，其他天设为 REPORT_READY。
     *
     * @return true 如果进入 ENDING_READY
     */
    public boolean finishSessionAfterReport(Vup vup, DaySession session, DailyReport report, BusinessLog logEntry) {
        DayPhase nextPhase = DayPhaseStateMachine.afterReport(session.getDay(), balanceConfig.maxDay());
        if (nextPhase == DayPhase.ENDING_READY) {
            log.info("Day {} reached, generating ending review for vupId={}", session.getDay(), vup.getId());
            var endingReview = endingService.createEndingReview(vup, session, report, logEntry);
            session.setPhase(nextPhase.name());
            session.setEndingReviewId(endingReview.getId());
            return true;
        }
        session.setPhase(nextPhase.name());
        return false;
    }

    /**
     * 统一路线分变更。
     */
    public void applyRouteScoreChange(Vup vup, String routeKey, int scoreChange) {
        Map<String, Integer> routeScores = routeScores(vup);
        routeScores.put(routeKey, routeScores.getOrDefault(routeKey, 0) + scoreChange);
        vup.setRouteScoreJson(jsonService.write(routeScores));
        vup.setCurrentRoute(routeScores.entrySet().stream()
                .filter(entry -> !RouteType.UNKNOWN.name().equals(entry.getKey()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(RouteType.UNKNOWN.name()));
    }

    /**
     * 解析路线分 JSON。
     */
    public Map<String, Integer> routeScores(Vup vup) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), 0);
        }
        if (vup.getRouteScoreJson() == null || vup.getRouteScoreJson().isBlank()) {
            return scores;
        }
        jsonService.readMap(vup.getRouteScoreJson()).forEach((key, value) -> {
            if (value instanceof Number number) {
                scores.put(key, number.intValue());
            }
        });
        return scores;
    }
}
