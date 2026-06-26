package com.example.vupworld.service.fan;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AudiencePressureService {
    private static final int FORMING_THRESHOLD = 6;
    private static final int LOCKED_THRESHOLD = 12;

    private final JsonService jsonService;
    private final DayFlowService dayFlowService;
    private final BusinessLogMapper businessLogMapper;

    public AudiencePressureService(JsonService jsonService, DayFlowService dayFlowService, BusinessLogMapper businessLogMapper) {
        this.jsonService = jsonService;
        this.dayFlowService = dayFlowService;
        this.businessLogMapper = businessLogMapper;
    }

    public AudiencePressure pressureForAction(Vup vup, int day, ActionType actionType, String targetRoute) {
        if (isBufferAction(actionType)) {
            return AudiencePressure.none();
        }
        return pressureForRoute(vup, day, targetRoute);
    }

    public AudiencePressure pressureForRoute(Vup vup, int day, String targetRoute) {
        String normalizedTarget = normalizeRoute(targetRoute);
        if (normalizedTarget == null || RouteType.UNKNOWN.name().equals(normalizedTarget)) {
            return AudiencePressure.none();
        }
        LeadExpectation lead = leadExpectation(vup);
        if (lead.score() < FORMING_THRESHOLD || normalizedTarget.equals(lead.routeType())) {
            return AudiencePressure.none();
        }

        int rawLevel = lead.score() >= LOCKED_THRESHOLD ? 2 : 1;
        int level = rawLevel;
        if (hasRecentBuffer(vup, day)) {
            level -= 1;
        }
        int stressMitigation = stressMitigationLevel(vup);
        if (stressMitigation > 0) {
            level -= stressMitigation;
        }
        if (level <= 0) {
            return AudiencePressure.none();
        }
        String pressureLabel = level == 2 ? "期待锁定" : "转型摩擦";
        String pressureHint = level == 2
                ? "观众已经把你按【%s】归档，今天强转【%s】会少转粉，还会被追问动机。"
                .formatted(dayFlowService.routeLabel(lead.routeType()), dayFlowService.routeLabel(normalizedTarget))
                : "观众开始按【%s】理解你，今天转向【%s】需要解释成本。"
                .formatted(dayFlowService.routeLabel(lead.routeType()), dayFlowService.routeLabel(normalizedTarget));
        if (stressMitigation > 0) {
            pressureHint = pressureHint + " 抗压属性抵消了" + stressMitigation + "级压力。";
        }
        return new AudiencePressure(
                level,
                rawLevel,
                stressMitigation,
                lead.routeType(),
                dayFlowService.routeLabel(lead.routeType()),
                normalizedTarget,
                dayFlowService.routeLabel(normalizedTarget),
                pressureLabel,
                pressureHint
        );
    }

    public int fanKeepPercent(AudiencePressure pressure) {
        if (pressure.level() >= 2) {
            return 70;
        }
        if (pressure.level() == 1) {
            return 85;
        }
        return 100;
    }

    public int reputationPenalty(AudiencePressure pressure) {
        if (pressure.level() >= 2) {
            return -2;
        }
        if (pressure.level() == 1) {
            return -1;
        }
        return 0;
    }

    public int watchHeatGain(AudiencePressure pressure) {
        if (pressure.level() >= 2) {
            return 3;
        }
        if (pressure.level() == 1) {
            return 1;
        }
        return 0;
    }

    public Map<String, Object> evidence(AudiencePressure pressure) {
        if (!pressure.active()) {
            return Map.of();
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("level", pressure.level());
        evidence.put("rawLevel", pressure.rawLevel());
        evidence.put("stressMitigation", pressure.stressMitigation());
        evidence.put("label", pressure.label());
        evidence.put("leadRoute", pressure.leadRoute());
        evidence.put("leadLabel", pressure.leadLabel());
        evidence.put("targetRoute", pressure.targetRoute());
        evidence.put("targetLabel", pressure.targetLabel());
        return evidence;
    }

    public String summarySuffix(AudiencePressure pressure) {
        if (!pressure.active()) {
            return "";
        }
        return " 观众期待落差触发：" + pressure.hint();
    }

    private boolean isBufferAction(ActionType actionType) {
        return actionType == ActionType.TRAIN_TALK
                || actionType == ActionType.FAN_GROUP_MAINTAIN
                || actionType == ActionType.REST;
    }

    private boolean hasRecentBuffer(Vup vup, int day) {
        if (day <= 1) {
            return false;
        }
        BusinessLog previous = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), day - 1);
        if (previous == null) {
            return false;
        }
        return ActionType.TRAIN_TALK.name().equals(previous.getAction())
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(previous.getAction());
    }

    private int stressMitigationLevel(Vup vup) {
        if (vup.getStressPower() >= 8) {
            return 2;
        }
        if (vup.getStressPower() >= 4) {
            return 1;
        }
        return 0;
    }

    private LeadExpectation leadExpectation(Vup vup) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), 0);
        }
        mergeScores(scores, vup.getRouteScoreJson());
        mergeScores(scores, vup.getExpectationJson());
        return scores.entrySet().stream()
                .filter(entry -> !RouteType.UNKNOWN.name().equals(entry.getKey()))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> new LeadExpectation(entry.getKey(), Math.max(0, entry.getValue())))
                .orElse(new LeadExpectation(RouteType.UNKNOWN.name(), 0));
    }

    private void mergeScores(Map<String, Integer> scores, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        jsonService.readMap(json).forEach((key, value) -> {
            if (value instanceof Number number && scores.containsKey(key)) {
                scores.put(key, Math.max(scores.get(key), number.intValue()));
            }
        });
    }

    private String normalizeRoute(String routeType) {
        if (routeType == null || routeType.isBlank()) {
            return null;
        }
        try {
            return RouteType.valueOf(routeType).name();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record LeadExpectation(String routeType, int score) {
    }

    public record AudiencePressure(
            int level,
            int rawLevel,
            int stressMitigation,
            String leadRoute,
            String leadLabel,
            String targetRoute,
            String targetLabel,
            String label,
            String hint
    ) {
        static AudiencePressure none() {
            return new AudiencePressure(0, 0, 0, null, null, null, null, "", "");
        }

        public boolean active() {
            return level > 0;
        }
    }
}
