package com.example.vupworld.service.progression;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.content.PlatformTrendService;
import com.example.vupworld.service.content.DailyFortuneService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.dto.StageDtos.RouteCandidateDTO;
import com.example.vupworld.dto.StageDtos.RouteTendencyDTO;
import com.example.vupworld.dto.StageDtos.StageBriefingDTO;
import com.example.vupworld.dto.StageDtos.StageRhythmDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class StageBriefingService {
    private final VupService vupService;
    private final PlatformTrendService platformTrendService;
    private final RiskDebtMapper riskDebtMapper;
    private final DaySessionMapper daySessionMapper;
    private final JsonService jsonService;
    private final DailyFortuneService dailyFortuneService;
    private final DayFlowService dayFlowService;
    private final StageObjectiveService stageObjectiveService;

    public StageBriefingService(
            VupService vupService,
            PlatformTrendService platformTrendService,
            RiskDebtMapper riskDebtMapper,
            DaySessionMapper daySessionMapper,
            JsonService jsonService,
            DailyFortuneService dailyFortuneService,
            DayFlowService dayFlowService,
            StageObjectiveService stageObjectiveService
    ) {
        this.vupService = vupService;
        this.platformTrendService = platformTrendService;
        this.riskDebtMapper = riskDebtMapper;
        this.daySessionMapper = daySessionMapper;
        this.jsonService = jsonService;
        this.dailyFortuneService = dailyFortuneService;
        this.dayFlowService = dayFlowService;
        this.stageObjectiveService = stageObjectiveService;
    }

    public StageBriefingDTO briefing(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        int day = vup.getDayCount();
        int nextMilestoneDay = nextMilestoneDay(day);
        boolean milestoneToday = day == nextMilestoneDay || day == 30;
        Map<String, Object> currentTrend = platformTrendService.currentTrendForDay(day);
        Map<String, Object> nextTrend = platformTrendService.trendFor(platformTrendService.trendIdForDay(Math.min(30, day + 1)));
        StageObjectiveService.StageObjectivePlan objectivePlan = stageObjectiveService.objectiveFor(vup);
        int objectiveStreak = stageObjectiveService.objectiveHitStreakBefore(vup, day);
        int objectiveNextStreak = objectiveStreak + 1;

        // 每日运势
        var fortune = dailyFortuneService.getFortune(day, vup.getFans(), vup.getReputation(), session);

        return new StageBriefingDTO(
                stageLabel(day, nextMilestoneDay),
                stageRhythm(day),
                day,
                nextMilestoneDay,
                Math.max(0, nextMilestoneDay - day),
                milestoneToday,
                currentTrend,
                nextTrend,
                nextTrendHint(day, nextTrend),
                routeSnapshot(vup),
                routeTendency(vup),
                fanSnapshot(vup),
                riskSnapshot(vup),
                actionHint(vup, day, nextTrend, fortune.luckyAction()),
                stageObjectiveService.objectiveActionTypeFor(vup, fortune.luckyAction()),
                objectivePlan.title(),
                objectivePlan.summary(),
                objectivePlan.progress(),
                objectivePlan.progressLabel(),
                objectiveStreak,
                objectiveNextStreak,
                objectiveRewardPreview(objectiveNextStreak),
                objectivePlan.items(),
                imageFor(day),
                fortune.fortune(),
                fortune.advice(),
                fortune.icon()
        );
    }

    private String objectiveRewardPreview(int nextStreak) {
        if (nextStreak >= 3) {
            return "命中后" + nextStreak + "连：真爱粉+2，人气额外+2，若贴合路线则路线分额外+1。";
        }
        if (nextStreak == 2) {
            return "命中后2连：真爱粉+2，人气额外+2，短线经营会更稳。";
        }
        return "命中今日委托：真爱粉+2，并把本手写进阶段复盘证据。";
    }

    private int nextMilestoneDay(int day) {
        if (day <= 7) {
            return 7;
        }
        if (day <= 14) {
            return 14;
        }
        if (day <= 21) {
            return 21;
        }
        if (day <= 28) {
            return 28;
        }
        return 30;
    }

    private String stageLabel(int day, int nextMilestoneDay) {
        return switch (day) {
            case 7 -> "第7天阶段复盘";
            case 14 -> "第14天路线成型";
            case 21 -> "第21天冲刺预警";
            case 28 -> "第28天毕业预演";
            case 30 -> "第30天最终收束";
            default -> "距第%d天阶段复盘还有%d天".formatted(nextMilestoneDay, Math.max(0, nextMilestoneDay - day));
        };
    }

    private StageRhythmDTO stageRhythm(int day) {
        if (day <= 7) {
            return new StageRhythmDTO(
                    "NEWBIE",
                    "新人期",
                    1,
                    7,
                    "第1-7天先建立基本盘，路线分通常还没拉开，行动重点是攒第一批可复盘证据。"
            );
        }
        if (day <= 14) {
            return new StageRhythmDTO(
                    "ROUTE_FORMING",
                    "路线成形",
                    8,
                    14,
                    "第8-14天开始把路线证据排出高低，当前最可能路线会影响后续目标和观众期待。"
            );
        }
        if (day <= 21) {
            return new StageRhythmDTO(
                    "CRISIS_EXPANSION",
                    "危机扩张",
                    15,
                    21,
                    "第15-21天流量和米线风险一起放大，竞争路线仍能追赶，但需要更明确的行动证据。"
            );
        }
        return new StageRhythmDTO(
                "FINAL_SPRINT",
                "收官冲刺",
                22,
                30,
                "第22-30天进入结局收束，主路线、风险债务和粉丝结构都会直接影响最终复盘。"
        );
    }

    private String nextTrendHint(int day, Map<String, Object> nextTrend) {
        String nextTrendLabel = String.valueOf(nextTrend.get("label"));
        if (day == 7 || day == 14 || day == 21 || day == 28) {
            return "下周口味：" + trendAdvice(nextTrendLabel);
        }
        return "下一日口味：" + trendAdvice(nextTrendLabel);
    }

    private String trendAdvice(String nextTrendLabel) {
        if ("歌回扶持周".equals(nextTrendLabel)) {
            return nextTrendLabel + "，练歌、低压歌回和投稿会更容易吃推荐。";
        }
        if ("抽象出圈周".equals(nextTrendLabel)) {
            return nextTrendLabel + "，切片和整活会更香，但米线压力也会跟着抬头。";
        }
        if ("商业复审周".equals(nextTrendLabel)) {
            return nextTrendLabel + "，稳定排班和高亮互动回应更好看，老粉会盯着商业味。";
        }
        return nextTrendLabel + "，稳健内容更适合兜底，标题组先别预判开庭。";
    }

    private String routeSnapshot(Vup vup) {
        Map<String, Object> routeScores = jsonMap(vup.getRouteScoreJson());
        String leader = routeScores.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Number)
                .max(Comparator.comparingInt((Map.Entry<String, Object> entry) -> ((Number) entry.getValue()).intValue()))
                .map(entry -> dayFlowService.routeLabel(entry.getKey()) + ((Number) entry.getValue()).intValue())
                .orElse("路线仍在摇摆");
        return "路线快照：" + dayFlowService.routeLabel(vup.getCurrentRoute()) + "；最高证据：" + leader + "。";
    }

    private RouteTendencyDTO routeTendency(Vup vup) {
        List<RouteScore> scoredRoutes = dayFlowService.routeScores(vup).entrySet().stream()
                .filter(entry -> !RouteType.UNKNOWN.name().equals(entry.getKey()))
                .map(entry -> new RouteScore(entry.getKey(), entry.getValue()))
                .sorted(Comparator.<RouteScore>comparingInt(RouteScore::score).reversed()
                        .thenComparingInt(score -> routeOrder(score.routeType())))
                .toList();

        RouteScore leadingScore = scoredRoutes.stream()
                .filter(score -> score.score() > 0)
                .findFirst()
                .orElse(null);
        int primaryScore = leadingScore == null ? 0 : leadingScore.score();
        String primaryRouteType = leadingScore == null ? RouteType.UNKNOWN.name() : leadingScore.routeType();
        RouteCandidateDTO primaryRoute = new RouteCandidateDTO(
                primaryRouteType,
                dayFlowService.routeLabel(primaryRouteType),
                primaryScore,
                0,
                primaryRouteReason(primaryRouteType, primaryScore, scoredRoutes)
        );

        List<RouteCandidateDTO> competingRoutes = scoredRoutes.stream()
                .filter(score -> !score.routeType().equals(primaryRouteType))
                .limit(2)
                .map(score -> new RouteCandidateDTO(
                        score.routeType(),
                        dayFlowService.routeLabel(score.routeType()),
                        score.score(),
                        Math.max(0, primaryScore - score.score()),
                        competingRouteReason(score, primaryRouteType, primaryScore)
                ))
                .toList();

        return new RouteTendencyDTO(
                primaryRoute,
                competingRoutes,
                routeTendencyReason(primaryRouteType, primaryScore)
        );
    }

    private String primaryRouteReason(String routeType, int score, List<RouteScore> scoredRoutes) {
        if (RouteType.UNKNOWN.name().equals(routeType) || score <= 0) {
            return "还没有任何核心路线拿到有效路线分，当前最可能路线暂时保持未定。";
        }
        int runnerUpScore = scoredRoutes.stream()
                .filter(candidate -> !candidate.routeType().equals(routeType))
                .findFirst()
                .map(RouteScore::score)
                .orElse(0);
        int lead = Math.max(0, score - runnerUpScore);
        if (lead == 0) {
            return dayFlowService.routeLabel(routeType) + "路线分并列最高，下一手行动会决定倾向。";
        }
        return dayFlowService.routeLabel(routeType) + "以" + score + "分排第一，领先第二路线" + lead + "分。";
    }

    private String competingRouteReason(RouteScore routeScore, String primaryRouteType, int primaryScore) {
        if (RouteType.UNKNOWN.name().equals(primaryRouteType) || primaryScore <= 0) {
            return dayFlowService.routeLabel(routeScore.routeType()) + "目前" + routeScore.score()
                    + "分，仍可用" + routeActionTheme(routeScore.routeType()) + "把路线推成主线。";
        }
        int gap = Math.max(0, primaryScore - routeScore.score());
        if (gap == 0) {
            return dayFlowService.routeLabel(routeScore.routeType()) + "与主路线同分，补一次明确证据就可能反超。";
        }
        if (gap <= 2) {
            return dayFlowService.routeLabel(routeScore.routeType()) + "只落后" + gap + "分，仍是近身竞争路线。";
        }
        return dayFlowService.routeLabel(routeScore.routeType()) + "落后" + gap + "分，适合作为备选倾向观察。";
    }

    private String routeTendencyReason(String primaryRouteType, int primaryScore) {
        if (RouteType.UNKNOWN.name().equals(primaryRouteType) || primaryScore <= 0) {
            return "判断来自路线分：所有核心路线仍未拉开，前端可显示为路线未定，并展示两条可争方向。";
        }
        return "判断来自路线分：" + dayFlowService.routeLabel(primaryRouteType)
                + "当前分数最高；竞争路线取剩余路线分前两名。";
    }

    private String routeActionTheme(String routeType) {
        return switch (routeType) {
            case "SINGING_IDOL" -> "练歌、歌回和稳定投稿";
            case "SLICE_SAINT" -> "发布视频、切片素材和投稿箱";
            case "SOCIAL_COLLAB" -> "同台互动和粉丝群维护";
            case "DANCE_MEME" -> "练舞、整活和二创传播";
            case "BLACK_RED_MAIN_STAGE" -> "强观点内容和高热度话题";
            case "ELECTRONIC_PICKLE" -> "低压杂谈和陪伴内容";
            default -> "稳定行动";
        };
    }

    private int routeOrder(String routeType) {
        try {
            return RouteType.valueOf(routeType).ordinal();
        } catch (IllegalArgumentException exception) {
            return RouteType.UNKNOWN.ordinal();
        }
    }

    private String fanSnapshot(Vup vup) {
        return "粉丝结构：真爱粉%d、乐子人%d、独角兽%d、DD%d，总粉丝%d，口碑%d。"
                .formatted(vup.getTrueFans(), vup.getFunFans(), vup.getUnicornFans(), vup.getDdFans(), vup.getFans(), vup.getReputation());
    }

    private String riskSnapshot(Vup vup) {
        var openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        if (openDebts.isEmpty()) {
            return "风险快照：暂无未结清债务，米线施工队今天不用加班。";
        }
        int maxSeverity = openDebts.stream().mapToInt(RiskDebt::getSeverity).max().orElse(0);
        return "风险快照：未结清债务%d笔，最高风险等级%d级，先看米线工具能不能提前拆弹。"
                .formatted(openDebts.size(), maxSeverity);
    }

    private String actionHint(Vup vup, int day, Map<String, Object> nextTrend, String luckyAction) {
        String fortuneHint = luckyAction == null || luckyAction.isBlank() ? "" : " 今日运势推荐：" + luckyAction + "。";
        String nextTrendLabel = String.valueOf(nextTrend.get("label"));
        if ("歌回扶持周".equals(nextTrendLabel)) {
            return "行动建议：提前练歌或发视频，别到第8天才临时抱佛脚。" + fortuneHint;
        }
        if ("抽象出圈周".equals(nextTrendLabel)) {
            return "行动建议：先准备素材库，切片组不吃空气，复读梗也会查重。" + fortuneHint;
        }
        if ("商业复审周".equals(nextTrendLabel)) {
            return "行动建议：稳住粉丝群和排班感，别把直播间做成老板打卡机。" + fortuneHint;
        }
        if (vup.getWatchHeat() >= 60) {
            return "行动建议：围观热度已经偏高，今天先确认口碑和债务别一起爆。" + fortuneHint;
        }
        String base = day <= 7
                ? "行动建议：新人保护期先定基本盘，别急着预支主会场。"
                : "行动建议：按当前路线补证据，让结局复盘有话可说。";
        return base + fortuneHint;
    }

    private String imageFor(int day) {
        if (day >= 22) {
            return "v4/backgrounds/day-05-before-final.png";
        }
        if (day >= 15) {
            return "v4/backgrounds/day-04-midgame-rush.png";
        }
        if (day >= 8) {
            return "v4/backgrounds/day-03-week-one.png";
        }
        return "v4/backgrounds/day-01-first-night.png";
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(json);
    }

    private record RouteScore(String routeType, int score) {
    }
}
