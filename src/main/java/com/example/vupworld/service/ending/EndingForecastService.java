package com.example.vupworld.service.ending;

import com.example.vupworld.service.risk.RiskDebtLabels;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.EndingDtos.EndingForecastDTO;
import com.example.vupworld.dto.EndingDtos.EndingForecastRequirementDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EndingForecastService {
    private final VupService vupService;
    private final RiskDebtMapper riskDebtMapper;
    private final BusinessLogMapper businessLogMapper;
    private final JsonService jsonService;
    private final DayFlowService dayFlowService;
    private final EndingRuleEngine endingRuleEngine;
    private final BalanceConfig balanceConfig;

    public EndingForecastService(
            VupService vupService,
            RiskDebtMapper riskDebtMapper,
            BusinessLogMapper businessLogMapper,
            JsonService jsonService,
            DayFlowService dayFlowService,
            EndingRuleEngine endingRuleEngine,
            BalanceConfig balanceConfig
    ) {
        this.vupService = vupService;
        this.riskDebtMapper = riskDebtMapper;
        this.businessLogMapper = businessLogMapper;
        this.jsonService = jsonService;
        this.dayFlowService = dayFlowService;
        this.endingRuleEngine = endingRuleEngine;
        this.balanceConfig = balanceConfig;
    }

    public EndingForecastDTO forecast(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        List<BusinessLog> evidenceLogs = businessLogMapper.findEndingReferenceLogs(vup.getId());
        EndingRuleEngine.EndingResult ruleResult = endingRuleEngine.evaluate(
                vup,
                null,
                null,
                null,
                openDebts,
                evidenceLogs,
                false
        );
        ForecastTarget target = forecastTarget(vup, ruleResult, openDebts);
        String targetEndingType = target.endingType();
        List<EndingForecastRequirementDTO> requirements = requirements(vup, openDebts, evidenceLogs, targetEndingType, ruleResult);
        int confidence = confidence(vup, openDebts, targetEndingType, requirements);
        return new EndingForecastDTO(
                headline(targetEndingType, confidence, ruleResult),
                targetEndingType,
                finalTitle(targetEndingType),
                ruleResult.endingType(),
                finalTitle(ruleResult.endingType()),
                target.reason(),
                confidence,
                requirements,
                riskLine(openDebts),
                sprintHint(vup, targetEndingType, openDebts),
                imageFor(targetEndingType)
        );
    }

    private ForecastTarget forecastTarget(Vup vup, EndingRuleEngine.EndingResult ruleResult, List<RiskDebt> openDebts) {
        String restartTargetType = restartTargetType(vup);
        if (restartTargetType != null && restartTargetApplies(vup, openDebts, restartTargetType)) {
            return new ForecastTarget(restartTargetType, "复活赛目标仍有效，预演按本周目冲刺目标计算。");
        }
        String routeTarget = targetEndingForRoute(vup.getCurrentRoute());
        if (!"UNKNOWN".equals(routeTarget)) {
            return new ForecastTarget(routeTarget, "按当前主路线预演，避免默认安全结局劫持行动推荐。");
        }
        String scoreTarget = targetEndingForRoute(leadingRouteType(vup));
        if (!"UNKNOWN".equals(scoreTarget)) {
            return new ForecastTarget(scoreTarget, "按最高路线分预演，先把已有证据打成结局方向。");
        }
        return new ForecastTarget(ruleResult.endingType(), "路线尚未成型，暂按当前规则原判预演。");
    }

    private boolean restartTargetApplies(Vup vup, List<RiskDebt> openDebts, String restartTargetType) {
        if ("UNKNOWN".equals(restartTargetType) || vup.getDayCount() > 14) {
            return false;
        }
        int severeDebtCount = (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 4).count();
        return severeDebtCount == 0 && vup.getReputation() >= 35 && vup.getWatchHeat() < 80;
    }

    private String restartTargetType(Vup vup) {
        if (vup.getExpectationJson() == null || vup.getExpectationJson().isBlank()) {
            return null;
        }
        Object raw;
        try {
            raw = jsonService.readMap(vup.getExpectationJson()).get("restartTargetType");
        } catch (IllegalStateException ignored) {
            return null;
        }
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING",
                 "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE", "DD_BUS_STOP",
                 "SOCIAL_COLLAB", "DANCE_MEME", "UNKNOWN" -> value;
            default -> null;
        };
    }

    private List<EndingForecastRequirementDTO> requirements(
            Vup vup,
            List<RiskDebt> openDebts,
            List<BusinessLog> evidenceLogs,
            String likelyEndingType,
            EndingRuleEngine.EndingResult ruleResult
    ) {
        int routeScore = routeScoreForEnding(vup, likelyEndingType);
        String routeLabel = dayFlowService.routeLabel(routeTypeForEnding(likelyEndingType));
        int requiredRouteScore = requiredRouteScoreFor(likelyEndingType);
        int recentEvidence = recentRouteEvidenceCount(evidenceLogs, vup.getDayCount(), likelyEndingType);
        int requiredRecentEvidence = requiredRecentEvidenceFor(likelyEndingType);
        int severeDebtCount = (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 4).count();
        String targetRoute = routeTypeForEnding(likelyEndingType);
        List<String> routeActions = suggestedRouteActions(likelyEndingType);
        List<EndingForecastRequirementDTO> items = new ArrayList<>();
        items.add(requirement("ROUTE", "路线分", routeScore, requiredRouteScore, targetRoute, routeActions, 10,
                routeScore >= requiredRouteScore
                        ? "目标路线%s，路线分%d/%d，已经够结局组归档。".formatted(routeLabel, routeScore, requiredRouteScore)
                        : "目标路线%s分%d/%d，还需要连续行动把路线钉牢。".formatted(routeLabel, routeScore, requiredRouteScore)));
        items.add(requirement("RECENT_EVIDENCE", "近7天证据", recentEvidence, requiredRecentEvidence, targetRoute, routeActions, 20,
                recentEvidence >= requiredRecentEvidence
                        ? "近7天对应证据%d/%d，冲刺方向仍然清晰。".formatted(recentEvidence, requiredRecentEvidence)
                        : "近7天对应证据%d/%d，临门转线会被结局组判成证据不足。".formatted(recentEvidence, requiredRecentEvidence)));
        items.add(requirement("FANS", "粉丝门槛", vup.getFans(), 130, targetRoute, suggestedFanActions(likelyEndingType), 30,
                vup.getFans() >= 130
                        ? "总粉丝%d，已越过查无此V的基础门槛。".formatted(vup.getFans())
                        : "总粉丝%d，距离130基础门槛还差%d。".formatted(vup.getFans(), Math.max(0, 130 - vup.getFans()))));
        int debtSafety = severeDebtCount == 0 ? 1 : 0;
        items.add(requirement("DEBT_RISK", "债务风险", debtSafety, 1, targetRoute, List.of("FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"), 5,
                openDebts.isEmpty()
                        ? "暂无未结清债务，毕业照暂时没有法槌阴影。"
                        : "未结清债务%d笔，其中高压债务%d笔，先看米线工具能不能拆。".formatted(openDebts.size(), severeDebtCount)));
        int specificCurrent = routeSpecificCurrent(vup, likelyEndingType, routeScore, recentEvidence);
        int specificTarget = routeSpecificTarget(likelyEndingType);
        items.add(requirement("ROUTE_SPECIFIC", "路线专项", specificCurrent, specificTarget, targetRoute, routeActions, 40,
                routeSpecificLine(vup, likelyEndingType, routeScore, recentEvidence)));
        items.add(ruleTraceRequirement(vup, likelyEndingType, ruleResult));
        return items;
    }

    private EndingForecastRequirementDTO requirement(
            String key,
            String label,
            int currentValue,
            int targetValue,
            String targetRoute,
            List<String> suggestedActionTypes,
            int priority,
            String line
    ) {
        boolean pass = currentValue >= targetValue;
        return new EndingForecastRequirementDTO(
                key,
                label,
                pass ? "PASS" : "WARN",
                line,
                Math.max(0, currentValue),
                Math.max(0, targetValue),
                Math.max(0, targetValue - currentValue),
                targetRoute == null ? "" : targetRoute,
                suggestedActionTypes == null ? List.of() : suggestedActionTypes,
                priority,
                !pass && priority <= 30
        );
    }

    private EndingForecastRequirementDTO ruleTraceRequirement(
            Vup vup,
            String likelyEndingType,
            EndingRuleEngine.EndingResult ruleResult
    ) {
        boolean sameRule = likelyEndingType.equals(ruleResult.endingType());
        String ruleLine = sameRule
                ? "统一规则命中%s：%s。".formatted(finalTitle(ruleResult.endingType()), ruleResult.rule())
                : "复活赛目标暂时覆盖预演：当前目标%s，规则原判%s。"
                        .formatted(finalTitle(likelyEndingType), finalTitle(ruleResult.endingType()));
        Object route = ruleResult.conditions().getOrDefault("currentRoute", vup.getCurrentRoute());
        Object fans = ruleResult.conditions().getOrDefault("fans", vup.getFans());
        Object debtCount = ruleResult.conditions().getOrDefault("openDebtCount", 0);
        Object unknownReason = ruleResult.conditions().getOrDefault("unknownFailureReason", "");
        String unknownLine = "UNKNOWN".equals(ruleResult.endingType())
                && unknownReason instanceof String text
                && !text.isBlank()
                ? " 查无此V原因：" + text
                : "";
        return new EndingForecastRequirementDTO(
                "RULE_TRACE",
                "规则来源",
                sameRule ? "PASS" : "WARN",
                ruleLine + " 当前路线%s，粉丝%s，未结债务%s。%s".formatted(route, fans, debtCount, unknownLine),
                sameRule ? 1 : 0,
                1,
                sameRule ? 0 : 1,
                routeTypeForEnding(likelyEndingType),
                sameRule ? List.of() : suggestedRouteActions(likelyEndingType),
                60,
                false
        );
    }

    private int confidence(
            Vup vup,
            List<RiskDebt> openDebts,
            String likelyEndingType,
            List<EndingForecastRequirementDTO> requirements
    ) {
        int passed = (int) requirements.stream().filter(item -> "PASS".equals(item.status())).count();
        int routeScore = routeScoreForEnding(vup, likelyEndingType);
        int base = passed * 18 + Math.min(24, routeScore * 2);
        if ("UNKNOWN".equals(likelyEndingType)) {
            base = Math.min(base, 45);
        }
        if (!openDebts.isEmpty()) {
            base -= openDebts.stream().mapToInt(RiskDebt::getSeverity).max().orElse(0) * 3;
        }
        return Math.max(20, Math.min(95, base));
    }

    private boolean routeSpecificPass(Vup vup, String likelyEndingType, int routeScore, int recentEvidence) {
        return switch (likelyEndingType) {
            case "SINGING_IDOL" -> routeScore >= 16 && recentEvidence >= 2 && vup.getSongPower() >= 11;
            case "SLICE_SAINT" -> routeScore >= 16 && recentEvidence >= 2 && (vup.getMemeLevel() >= 35 || vup.getFunFans() >= 45);
            case "BLACK_RED_MAIN_STAGE" -> routeScore >= 15 && recentEvidence >= 1 && vup.getWatchHeat() >= 45;
            case "DD_BUS_STOP" -> routeScore >= 14 && recentEvidence >= 1 && ddRatio(vup) >= balanceConfig.ddBusStopDdRatioRequired();
            case "CYBER_GIRLFRIEND" -> routeScore >= 12
                    && recentEvidence >= 1
                    && cyberRelationshipReady(vup);
            case "MAIN_STAGE_KING" -> vup.getMemeLevel() >= 70 && vup.getReputation() < 40;
            case "ELECTRONIC_PICKLE" -> routeScore >= 12 && recentEvidence >= 2 && vup.getReputation() >= 55;
            default -> false;
        };
    }

    private String routeSpecificLine(Vup vup, String likelyEndingType, int routeScore, int recentEvidence) {
        return switch (likelyEndingType) {
            case "SINGING_IDOL" -> "歌力%d，路线分%d，近7天歌势证据%d；练歌、歌回或投稿至少要持续露面。".formatted(vup.getSongPower(), routeScore, recentEvidence);
            case "SLICE_SAINT" -> "串味%d、乐子人%d，路线分%d，近7天素材证据%d；切片结局需要供货不断。".formatted(vup.getMemeLevel(), vup.getFunFans(), routeScore, recentEvidence);
            case "BLACK_RED_MAIN_STAGE" -> "围观%d、口碑%d，路线分%d，近7天主会场证据%d；热度够也要有代表事件。".formatted(vup.getWatchHeat(), vup.getReputation(), routeScore, recentEvidence);
            case "DD_BUS_STOP" -> "DD占比%d%%/%d%%，路线分%d，近7天联动证据%d；社交结局要持续接车。".formatted(ddRatio(vup), balanceConfig.ddBusStopDdRatioRequired(), routeScore, recentEvidence);
            case "CYBER_GIRLFRIEND" -> "独角兽%d%%/%d%% 或独角兽%d/%d，商业化%d/%d，路线分%d，近7天陪伴/边界证据%d。".formatted(
                    unicornRatio(vup),
                    balanceConfig.cyberUnicornRatioRequired(),
                    vup.getUnicornFans(),
                    balanceConfig.cyberUnicornFansRequired(),
                    vup.getCommercialLevel(),
                    balanceConfig.cyberCommercialRequired(),
                    routeScore,
                    recentEvidence
            );
            case "MAIN_STAGE_KING" -> "串味%d、口碑%d，主会场之王需要大量事故素材支撑。".formatted(vup.getMemeLevel(), vup.getReputation());
            case "ELECTRONIC_PICKLE" -> "口碑%d，路线分%d，近7天稳健证据%d；杂谈复盘和粉丝群维护不能断档。".formatted(vup.getReputation(), routeScore, recentEvidence);
            default -> "路线还没成型，先把任一方向连续做出证据。";
        };
    }

    private int routeSpecificCurrent(Vup vup, String likelyEndingType, int routeScore, int recentEvidence) {
        return switch (likelyEndingType) {
            case "SINGING_IDOL" -> Math.min(routeScore, 16) + Math.min(recentEvidence, 2) * 3 + Math.min(vup.getSongPower(), 11);
            case "SLICE_SAINT" -> Math.min(routeScore, 16) + Math.min(recentEvidence, 2) * 3 + Math.min(Math.max(vup.getMemeLevel(), vup.getFunFans()), 45);
            case "BLACK_RED_MAIN_STAGE" -> Math.min(routeScore, 15) + Math.min(recentEvidence, 1) * 4 + Math.min(vup.getWatchHeat(), 45);
            case "DD_BUS_STOP" -> Math.min(routeScore, 14) + Math.min(recentEvidence, 1) * 4
                    + Math.min(ddRatio(vup), balanceConfig.ddBusStopDdRatioRequired());
            case "CYBER_GIRLFRIEND" -> Math.min(routeScore, 12) + Math.min(recentEvidence, 1) * 4
                    + Math.min(cyberRelationshipProgress(vup), balanceConfig.cyberCommercialRequired());
            case "MAIN_STAGE_KING" -> Math.min(vup.getMemeLevel(), 70) + Math.max(0, 40 - vup.getReputation());
            case "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> Math.min(routeScore, 12) + Math.min(recentEvidence, 2) * 3 + Math.min(vup.getReputation(), 70);
            default -> Math.min(routeScore, 8);
        };
    }

    private int routeSpecificTarget(String likelyEndingType) {
        return switch (likelyEndingType) {
            case "SINGING_IDOL" -> 33;
            case "SLICE_SAINT" -> 67;
            case "BLACK_RED_MAIN_STAGE" -> 64;
            case "DD_BUS_STOP" -> 14 + 4 + balanceConfig.ddBusStopDdRatioRequired();
            case "CYBER_GIRLFRIEND" -> 12 + 4 + balanceConfig.cyberCommercialRequired();
            case "MAIN_STAGE_KING" -> 100;
            case "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> 88;
            default -> 8;
        };
    }

    private List<String> suggestedRouteActions(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> List.of("TRAIN_SONG", "STREAM_PLAN", "PUBLISH_VIDEO");
            case "SLICE_SAINT", "DANCE_MEME" -> List.of("PUBLISH_VIDEO", "PUBLISH_CLIP", "STREAM_PLAN");
            case "DD_BUS_STOP", "SOCIAL_COLLAB" -> List.of("NPC_INTERACT", "FAN_GROUP_MAINTAIN", "STREAM_PLAN");
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> List.of("FAN_GROUP_MAINTAIN", "TRAIN_TALK", "PUBLISH_CLIP");
            case "CYBER_GIRLFRIEND" -> List.of("TRAIN_TALK", "FAN_GROUP_MAINTAIN", "NPC_INTERACT");
            case "GLORIOUS_GRADUATION" -> List.of("FAN_GROUP_MAINTAIN", "REST", "TRAIN_TALK");
            case "ELECTRONIC_PICKLE" -> List.of("TRAIN_TALK", "FAN_GROUP_MAINTAIN", "STREAM_PLAN");
            default -> List.of("PUBLISH_VIDEO", "STREAM_PLAN", "TRAIN_TALK");
        };
    }

    private List<String> suggestedFanActions(String endingType) {
        List<String> actions = new ArrayList<>();
        actions.add("STREAM_PLAN");
        actions.add("PUBLISH_VIDEO");
        actions.add("PUBLISH_CLIP");
        for (String action : suggestedRouteActions(endingType)) {
            if (!actions.contains(action)) {
                actions.add(action);
            }
        }
        return actions;
    }

    private String headline(String likelyEndingType, int confidence, EndingRuleEngine.EndingResult ruleResult) {
        if ("UNKNOWN".equals(likelyEndingType)) {
            return "结局预演：毕业照还没对上焦，先把粉丝和路线证据补起来。";
        }
        if (!likelyEndingType.equals(ruleResult.endingType())) {
            return "目标预演：本轮正在冲【%s】，如果现在结算会偏【%s】；先补目标缺口。"
                    .formatted(finalTitle(likelyEndingType), finalTitle(ruleResult.endingType()));
        }
        return "结局预演：当前最像【%s】，置信度%d%%，结局组已经开始找代表事件。"
                .formatted(finalTitle(likelyEndingType), confidence);
    }

    private String riskLine(List<RiskDebt> openDebts) {
        if (openDebts.isEmpty()) {
            return "债务风险：暂无未结清债务，楼友暂时没有把毕业照拿去开庭。";
        }
        RiskDebt top = openDebts.stream()
                .max(Comparator.comparingInt(RiskDebt::getSeverity))
                .orElse(openDebts.get(0));
        return "债务风险：未结清债务%d笔，最高风险是%s，先拆它再冲刺。"
                .formatted(openDebts.size(), RiskDebtLabels.debtTypeLabel(top.getDebtType()));
    }

    private String sprintHint(Vup vup, String likelyEndingType, List<RiskDebt> openDebts) {
        if (!openDebts.isEmpty()) {
            return "冲刺建议：先用米线工具或低压复盘拆旧账，不然结局复盘会先写欠账清单。";
        }
        return switch (likelyEndingType) {
            case "SINGING_IDOL" -> "冲刺建议：继续练歌、开低压歌回或投稿，别把歌势证据停在练习室。";
            case "SLICE_SAINT" -> "冲刺建议：先补视频素材再切，切片组要新料，不要只复读同一梗。";
            case "BLACK_RED_MAIN_STAGE" -> "冲刺建议：热度够时先保口碑，标题组别把毕业照写成庭审记录。";
            case "DD_BUS_STOP" -> "冲刺建议：同台互动可以继续，但穿插粉丝群维护稳住老粉。";
            case "CYBER_GIRLFRIEND" -> "冲刺建议：先用低压陪伴开局，再把高亮互动回应、边界处理和粉丝服务写进证据链。";
            case "MAIN_STAGE_KING" -> "冲刺建议：先接热度和事故素材，再用米线工具控制旧账别失控。";
            case "GLORIOUS_GRADUATION" -> "冲刺建议：粉丝群维护、低压复盘和少预支，比临门一脚冲热搜更重要。";
            case "ELECTRONIC_PICKLE" -> "冲刺建议：稳健路线要有代表行动，杂谈复盘和粉丝群维护别全像复制粘贴。";
            default -> vup.getFans() < 130
                    ? "冲刺建议：先补粉丝基本盘，查无此V是所有路线的硬门槛。"
                    : "冲刺建议：选一条路线连续补证据，让结局组有东西可写。";
        };
    }

    private String finalTitle(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "歌势遗珠";
            case "SLICE_SAINT" -> "切片圣体";
            case "DANCE_MEME" -> "梗舞整活";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "DD_BUS_STOP" -> "DD公交站";
            default -> "查无此V";
        };
    }

    private String imageFor(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "v4/routes/singing-idol/ending-highlight.png";
            case "DANCE_MEME" -> "v4/routes/dance-meme/ending-highlight.png";
            case "SLICE_SAINT" -> "v4/routes/slice-saint/ending-highlight.png";
            case "SOCIAL_COLLAB" -> "v4/routes/social-collab/ending-highlight.png";
            case "BLACK_RED_MAIN_STAGE" -> "v4/routes/black-red-main-stage/ending-highlight.png";
            case "MAIN_STAGE_KING" -> "v4/routes/main-stage-king/ending-highlight.png";
            case "CYBER_GIRLFRIEND" -> "v4/routes/cyber-girlfriend/ending-highlight.png";
            case "GLORIOUS_GRADUATION" -> "v4/routes/glorious-graduation/ending-highlight.png";
            case "ELECTRONIC_PICKLE" -> "v4/routes/electronic-pickle/ending-highlight.png";
            case "DD_BUS_STOP" -> "v4/routes/dd-bus-stop/ending-highlight.png";
            default -> "v4/routes/unknown/ending-highlight.png";
        };
    }

    private int currentRouteScore(Vup vup) {
        Map<String, Object> scores = jsonService.readMap(vup.getRouteScoreJson());
        Object raw = scores.get(vup.getCurrentRoute());
        return raw instanceof Number number ? number.intValue() : 0;
    }

    private int routeScoreForEnding(Vup vup, String endingType) {
        Map<String, Object> scores = jsonService.readMap(vup.getRouteScoreJson());
        if ("SLICE_SAINT".equals(endingType) || "DANCE_MEME".equals(endingType)) {
            return Math.max(scoreValue(scores.get("SLICE_SAINT")), scoreValue(scores.get("DANCE_MEME")));
        }
        Object raw = scores.get(routeTypeForEnding(endingType));
        return scoreValue(raw);
    }

    private int scoreValue(Object raw) {
        return raw instanceof Number number ? number.intValue() : 0;
    }

    private boolean cyberRelationshipReady(Vup vup) {
        return (unicornRatio(vup) >= balanceConfig.cyberUnicornRatioRequired()
                || vup.getUnicornFans() >= balanceConfig.cyberUnicornFansRequired())
                && vup.getCommercialLevel() >= balanceConfig.cyberCommercialRequired();
    }

    private int cyberRelationshipProgress(Vup vup) {
        int unicornRatioProgress = vup.getCommercialLevel() * Math.min(100, unicornRatio(vup))
                / Math.max(1, balanceConfig.cyberUnicornRatioRequired());
        int unicornFanProgress = vup.getCommercialLevel() * Math.min(vup.getUnicornFans(), balanceConfig.cyberUnicornFansRequired())
                / Math.max(1, balanceConfig.cyberUnicornFansRequired());
        return Math.max(unicornRatioProgress, unicornFanProgress);
    }

    private String leadingRouteType(Vup vup) {
        return jsonService.readMap(vup.getRouteScoreJson()).entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Number)
                .filter(entry -> !"UNKNOWN".equals(entry.getKey()))
                .max(Comparator.comparingInt(entry -> ((Number) entry.getValue()).intValue()))
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    private String targetEndingForRoute(String routeType) {
        return switch (routeType == null ? "UNKNOWN" : routeType) {
            case "SINGING_IDOL" -> "SINGING_IDOL";
            case "SLICE_SAINT", "DANCE_MEME" -> "SLICE_SAINT";
            case "SOCIAL_COLLAB", "DD_BUS_STOP" -> "DD_BUS_STOP";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_GIRLFRIEND" -> "CYBER_GIRLFRIEND";
            case "GLORIOUS_GRADUATION" -> "GLORIOUS_GRADUATION";
            case "ELECTRONIC_PICKLE" -> "ELECTRONIC_PICKLE";
            default -> "UNKNOWN";
        };
    }

    private int requiredRouteScoreFor(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL", "SLICE_SAINT" -> 16;
            case "BLACK_RED_MAIN_STAGE" -> 15;
            case "DD_BUS_STOP" -> 14;
            case "CYBER_GIRLFRIEND", "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> 12;
            case "MAIN_STAGE_KING" -> 10;
            default -> 8;
        };
    }

    private int requiredRecentEvidenceFor(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL", "SLICE_SAINT", "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> 2;
            case "BLACK_RED_MAIN_STAGE", "DD_BUS_STOP", "CYBER_GIRLFRIEND", "MAIN_STAGE_KING" -> 1;
            default -> 1;
        };
    }

    private int recentRouteEvidenceCount(List<BusinessLog> logs, int currentDay, String endingType) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        int anchorDay = currentDay > 0
                ? currentDay
                : logs.stream().mapToInt(BusinessLog::getDay).max().orElse(0);
        int startDay = Math.max(1, anchorDay - 6);
        return (int) logs.stream()
                .filter(log -> log != null && log.getDay() >= startDay)
                .filter(log -> isRouteEvidence(log, endingType))
                .count();
    }

    private boolean isRouteEvidence(BusinessLog log, String endingType) {
        String action = log.getAction();
        String routeScoreChange = log.getRouteScoreChange();
        String weightDetail = log.getWeightDetail();
        String result = log.getResult();
        String routeType = routeTypeForEnding(endingType);
        if (containsMarker(routeScoreChange, routeType) || containsMarker(weightDetail, routeType)) {
            return true;
        }
        return switch (endingType) {
            case "SINGING_IDOL" -> "TRAIN_SONG".equals(action)
                    || containsMarker(result, "歌")
                    || containsMarker(weightDetail, "singingEvidence");
            case "SLICE_SAINT" -> "PUBLISH_VIDEO".equals(action)
                    || "PUBLISH_CLIP".equals(action)
                    || "TRAIN_DANCE".equals(action)
                    || containsMarker(weightDetail, "memeMaterial")
                    || containsMarker(weightDetail, "clipMomentum");
            case "DD_BUS_STOP" -> "NPC_INTERACT".equals(action)
                    || containsMarker(weightDetail, "collabEvidence")
                    || containsMarker(result, "联动");
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "PUBLISH_CLIP".equals(action)
                    || containsMarker(weightDetail, "trafficSpike")
                    || containsMarker(weightDetail, "archivePressure")
                    || containsMarker(routeScoreChange, "BLACK_RED_MAIN_STAGE");
            case "CYBER_GIRLFRIEND", "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> "FAN_GROUP_MAINTAIN".equals(action)
                    || "TRAIN_TALK".equals(action)
                    || "REST".equals(action)
                    || containsMarker(weightDetail, "stableCommunity")
                    || containsMarker(weightDetail, "riskCooldown");
            default -> false;
        };
    }

    private String routeTypeForEnding(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "SINGING_IDOL";
            case "SLICE_SAINT", "DANCE_MEME" -> "SLICE_SAINT";
            case "DD_BUS_STOP" -> "SOCIAL_COLLAB";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_GIRLFRIEND", "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> "ELECTRONIC_PICKLE";
            default -> "";
        };
    }

    private boolean containsMarker(String text, String marker) {
        return text != null && marker != null && !marker.isBlank() && text.contains(marker);
    }

    private int ddRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getDdFans() * 100 / vup.getFans();
    }

    private int unicornRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getUnicornFans() * 100 / vup.getFans();
    }

    private record ForecastTarget(String endingType, String reason) {
    }
}
