package com.example.vupworld.service.progression;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.StageDtos.StageObjectiveItemDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class StageObjectiveService {
    private final BusinessLogMapper businessLogMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final JsonService jsonService;
    private final BalanceConfig balanceConfig;

    public StageObjectiveService(
            BusinessLogMapper businessLogMapper,
            RiskDebtMapper riskDebtMapper,
            JsonService jsonService,
            BalanceConfig balanceConfig
    ) {
        this.businessLogMapper = businessLogMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.jsonService = jsonService;
        this.balanceConfig = balanceConfig;
    }

    public StageObjectivePlan objectiveFor(Vup vup) {
        int day = vup.getDayCount();
        if (day <= 7) {
            return objectivePlanFor(vup, 1, 7);
        }
        if (day <= 14) {
            return objectivePlanFor(vup, 8, 14);
        }
        if (day <= 21) {
            return objectivePlanFor(vup, 15, 21);
        }
        if (day <= 28) {
            return objectivePlanFor(vup, 22, 28);
        }
        return objectivePlanFor(vup, 29, 30);
    }

    public String reviewLineFor(Vup vup) {
        StageObjectivePlan plan = objectiveFor(vup);
        String missed = plan.items().stream()
                .filter(item -> !item.achieved())
                .findFirst()
                .map(StageObjectiveItemDTO::label)
                .orElse("全部目标已达成");
        return "阶段目标复盘：" + plan.title()
                + "，完成" + plan.achievedCount() + "/" + plan.totalCount()
                + "项，进度" + plan.progressLabel()
                + "；下一手：" + missed + "。";
    }

    public String objectiveActionTypeFor(Vup vup, String luckyAction) {
        String objectiveAction = objectiveActionTypesFor(vup).stream().findFirst().orElse(null);
        if (objectiveAction != null) {
            return objectiveAction;
        }
        int day = vup.getDayCount();
        if (day <= 7) {
            return "TRAIN_TALK";
        }
        if (day <= 14) {
            return "PUBLISH_VIDEO";
        }
        if (day <= 21) {
            return vup.getWatchHeat() >= 60 ? "FAN_GROUP_MAINTAIN" : "TRAIN_TALK";
        }
        if (day <= 28) {
            return routeActionType(preferredRouteForObjective(vup));
        }
        return "FAN_GROUP_MAINTAIN";
    }

    public List<StageObjectiveTarget> objectiveTargetsFor(Vup vup) {
        return objectiveFor(vup).items().stream()
                .filter(item -> !item.achieved())
                .map(item -> objectiveTargetForItem(vup, item))
                .filter(StageObjectiveTarget::active)
                .distinct()
                .toList();
    }

    public List<String> objectiveActionTypesFor(Vup vup) {
        return objectiveTargetsFor(vup).stream()
                .map(StageObjectiveTarget::actionType)
                .distinct()
                .toList();
    }

    private StageObjectiveTarget objectiveTargetForItem(Vup vup, StageObjectiveItemDTO item) {
        String actionType = objectiveActionForItem(vup, item);
        String targetRoute = item.targetRouteType();
        if ("ROUTE_SCORE".equals(item.objectiveKey()) && (targetRoute == null || targetRoute.isBlank())) {
            targetRoute = preferredRouteForObjective(vup);
        }
        return new StageObjectiveTarget(
                actionType == null ? "" : actionType,
                targetRoute == null ? "" : targetRoute,
                item.label(),
                item.objectiveKey()
        );
    }

    private String objectiveActionForItem(Vup vup, StageObjectiveItemDTO item) {
        if (item.recommendedActionType() != null && !item.recommendedActionType().isBlank()) {
            return item.recommendedActionType();
        }
        if ("ROUTE_SCORE".equals(item.objectiveKey())) {
            return routeActionType(preferredRouteForObjective(vup));
        }
        return "";
    }

    private String preferredRouteForObjective(Vup vup) {
        String restartTarget = restartTargetType(vup);
        if (restartTarget != null && vup.getDayCount() <= 14 && !"UNKNOWN".equals(restartTarget)) {
            return baseRouteTypeForTarget(restartTarget);
        }
        return vup.getCurrentRoute();
    }

    private String restartTargetType(Vup vup) {
        if (vup.getExpectationJson() == null || vup.getExpectationJson().isBlank()) {
            return null;
        }
        try {
            Object raw = jsonService.readMap(vup.getExpectationJson()).get("restartTargetType");
            if (!(raw instanceof String value) || value.isBlank()) {
                return null;
            }
            return switch (value) {
                case "SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING",
                     "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE", "DD_BUS_STOP",
                     "SOCIAL_COLLAB", "DANCE_MEME", "UNKNOWN" -> value;
                default -> null;
            };
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public int objectiveHitStreakBefore(Vup vup, int currentDay) {
        List<BusinessLog> logs = new ArrayList<>(businessLogMapper.findRecentByVupId(vup.getId(), 14));
        logs.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId).reversed());
        int expectedDay = currentDay - 1;
        int streak = 0;
        boolean sawExpectedDay = false;
        boolean hitExpectedDay = false;
        for (BusinessLog log : logs) {
            if (log.getDay() > expectedDay) {
                continue;
            }
            while (log.getDay() < expectedDay) {
                if (!sawExpectedDay || !hitExpectedDay) {
                    return streak;
                }
                streak++;
                expectedDay--;
                sawExpectedDay = false;
                hitExpectedDay = false;
            }
            sawExpectedDay = true;
            hitExpectedDay = hitExpectedDay || hasStageObjectiveEvidence(log);
        }
        return sawExpectedDay && hitExpectedDay ? streak + 1 : streak;
    }

    private boolean hasStageObjectiveEvidence(BusinessLog log) {
        if (log.getMultiplierDetail() == null || log.getMultiplierDetail().isBlank()) {
            return false;
        }
        try {
            return jsonService.readMap(log.getMultiplierDetail()).containsKey("stageObjectiveBonus");
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public StageMomentum stageMomentumFor(Vup vup) {
        int day = vup.getDayCount();
        if (day <= 7) {
            return StageMomentum.empty();
        }

        int previousStageEndDay = previousStageEndDay(day);
        StageObjectivePlan previousPlan = objectivePlanFor(vup, previousStageWindowStart(day), previousStageEndDay);
        if (previousPlan.totalCount() == 0) {
            return StageMomentum.empty();
        }

        int achievedCount = previousPlan.achievedCount();
        int totalCount = previousPlan.totalCount();
        int progress = achievedCount * 100 / totalCount;

        if (achievedCount == totalCount) {
            return new StageMomentum(
                    "full",
                    4,
                    10,
                    1,
                    0,
                    1,
                    "上阶段全清，今天能顺手吃到一点阶段红利。",
                    previousPlan.title(),
                    previousPlan.summary(),
                    previousPlan.progressLabel()
            );
        }
        if (progress <= 33) {
            return new StageMomentum(
                    "weak",
                    0,
                    0,
                    0,
                    2,
                    1,
                    "上阶段漏得有点多，今天高风险动作会更吃力。",
                    previousPlan.title(),
                    previousPlan.summary(),
                    previousPlan.progressLabel()
            );
        }
        if (progress >= 66) {
            return new StageMomentum(
                    "good",
                    2,
                    5,
                    1,
                    0,
                    1,
                    "上阶段做得不错，今天可以更稳地吃路线红利。",
                    previousPlan.title(),
                    previousPlan.summary(),
                    previousPlan.progressLabel()
            );
        }
        return new StageMomentum(
                "mid",
                1,
                3,
                0,
                1,
                0,
                "上阶段完成度一般，今天有一点小回报，但别贪。",
                previousPlan.title(),
                previousPlan.summary(),
                previousPlan.progressLabel()
        );
    }

    private StageObjectivePlan objectivePlanFor(Vup vup, int startDay, int endDay) {
        List<BusinessLog> logs = businessLogMapper.findByVupIdBetweenDays(vup.getId(), startDay, endDay);
        int recentStreams = (int) logs.stream().filter(log -> "STREAM_PLAN".equals(log.getAction())).count();
        int recentVideos = (int) logs.stream().filter(log -> "PUBLISH_VIDEO".equals(log.getAction())).count();
        int recentClips = (int) logs.stream().filter(log -> "PUBLISH_CLIP".equals(log.getAction())).count();
        int recentFanOps = (int) logs.stream().filter(log -> "FAN_GROUP_MAINTAIN".equals(log.getAction())).count();
        int recentNpcOps = (int) logs.stream().filter(log -> "NPC_INTERACT".equals(log.getAction())).count();
        int recentRests = (int) logs.stream().filter(log -> "REST".equals(log.getAction())).count();
        int recentTrainSong = (int) logs.stream().filter(log -> "TRAIN_SONG".equals(log.getAction())).count();
        int recentTrainDance = (int) logs.stream().filter(log -> "TRAIN_DANCE".equals(log.getAction())).count();
        int recentTrainTalk = (int) logs.stream().filter(log -> "TRAIN_TALK".equals(log.getAction())).count();
        int recentPractice = (int) logs.stream().filter(log -> "TRAIN_SONG".equals(log.getAction())
                || "TRAIN_DANCE".equals(log.getAction())
                || "TRAIN_TALK".equals(log.getAction())).count();
        int recentBasics = recentPractice + recentStreams + recentVideos + recentFanOps;
        int routeLeaderScore = routeLeaderScore(vup);
        String routeKey = vup.getCurrentRoute();
        List<com.example.vupworld.model.RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        int openDebtCount = openDebts.size();
        int severeDebtCount = (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 4).count();
        int day = endDay;
        Map<String, Object> gradeContract = restartGradeContract(vup);
        if (!gradeContract.isEmpty() && day <= 7) {
            return restartContractPlan(
                    vup,
                    gradeContract,
                    true,
                    recentStreams,
                    recentVideos,
                    recentClips,
                    recentFanOps,
                    recentNpcOps,
                    recentRests,
                    recentTrainSong,
                    recentTrainDance,
                    recentTrainTalk,
                    recentBasics,
                    routeLeaderScore,
                    openDebtCount,
                    severeDebtCount
            );
        }
        if (!gradeContract.isEmpty() && day <= 14) {
            return restartContractPlan(
                    vup,
                    gradeContract,
                    false,
                    recentStreams,
                    recentVideos,
                    recentClips,
                    recentFanOps,
                    recentNpcOps,
                    recentRests,
                    recentTrainSong,
                    recentTrainDance,
                    recentTrainTalk,
                    recentBasics,
                    routeLeaderScore,
                    openDebtCount,
                    severeDebtCount
            );
        }

        if (day <= 7) {
            return plan(
                    "第1周先立住",
                    "先把基本盘打稳，别急着追爆点。",
                    recentBasics,
                    3,
                    List.of(
                            item("完成3次基础动作", recentBasics >= 3,
                                    "练歌、练舞、杂谈复盘、直播、发视频、粉丝群维护都算基本盘。"),
                            item("路线不摇摆", !"UNKNOWN".equals(routeKey),
                                    "先让当前路线有证据，不要今天一条明天一条。"),
                            item("债务不失控", openDebtCount <= 1,
                                    "前几天只要没把旧账堆爆，就算稳住。")
                    )
            );
        }

        if (day <= 14) {
            return plan(
                    "第2周补路线",
                    "把主要路线拉出来，开始向平台口味对齐。",
                    routeLeaderScore,
                    12,
                    List.of(
                            item("路线证据到12分", routeLeaderScore >= 12,
                                    "当日行为越贴路线，阶段复盘越容易定调。",
                                    "ROUTE_SCORE", "STREAM_PLAN", ""),
                            item("至少1次视频或切片", recentVideos + recentClips >= 1,
                                    "中段得让内容资产开始滚起来。"),
                            item("债务压在2笔以内", openDebtCount <= 2,
                                    "别让未清旧账把中段节奏拖歪。")
                    )
            );
        }

        if (day <= 21) {
            return plan(
                    "第3周控风险",
                    "冲刺前先把口碑和债务收住。",
                    vup.getReputation(),
                    70,
                    List.of(
                            item("口碑冲到70", vup.getReputation() >= 70,
                                    "高热阶段至少得有可用口碑兜底。"),
                            item("热度和路线别打架", vup.getWatchHeat() < 60 || !"UNKNOWN".equals(routeKey),
                                    "热度上来时，路线不能还是飘的。"),
                            item("保住可用体力", vup.getStamina() >= 3,
                                    "冲刺周别把体力清空。")
                    )
            );
        }

        if (day <= 28) {
            return routeSprintPlan(
                    vup,
                    sprintRouteFor(vup, routeKey, openDebtCount, severeDebtCount),
                    false,
                    recentStreams,
                    recentVideos,
                    recentClips,
                    recentFanOps,
                    recentNpcOps,
                    recentRests,
                    recentTrainSong,
                    recentTrainDance,
                    recentTrainTalk,
                    routeLeaderScore,
                    openDebtCount,
                    severeDebtCount
            );
        }

        return routeSprintPlan(
                vup,
                sprintRouteFor(vup, routeKey, openDebtCount, severeDebtCount),
                true,
                recentStreams,
                recentVideos,
                recentClips,
                recentFanOps,
                recentNpcOps,
                recentRests,
                recentTrainSong,
                recentTrainDance,
                recentTrainTalk,
                routeLeaderScore,
                openDebtCount,
                severeDebtCount
        );
    }

    private StageObjectivePlan restartContractPlan(
            Vup vup,
            Map<String, Object> contract,
            boolean firstWeek,
            int recentStreams,
            int recentVideos,
            int recentClips,
            int recentFanOps,
            int recentNpcOps,
            int recentRests,
            int recentTrainSong,
            int recentTrainDance,
            int recentTrainTalk,
            int recentBasics,
            int routeLeaderScore,
            int openDebtCount,
            int severeDebtCount
    ) {
        String targetType = canonicalEndingType(stringValue(contract.get("targetType"), restartTargetType(vup)));
        String baseRouteType = baseRouteTypeForTarget(targetType);
        String targetLabel = stringValue(contract.get("targetLabel"), routeLabel(targetType));
        String nextGrade = stringValue(contract.get("nextGrade"), "A");
        String weakness = stringValue(contract.get("primaryWeakness"), "ROUTE_FOCUS");
        String stageGoal = stringValue(contract.get(firstWeek ? "stageOneGoal" : "stageTwoGoal"),
                firstWeek ? "第一周先把复活赛目标定调。" : "第二周把路线证据补到冲档门槛。");
        String routeAction = routeActionType(baseRouteType);
        int routeTouches = routeTouchCount(targetType, recentStreams, recentVideos, recentClips, recentFanOps, recentNpcOps,
                recentTrainSong, recentTrainDance, recentTrainTalk);
        int evidenceTouches = recentStreams + recentVideos + recentClips + recentNpcOps;
        int routeTarget = "S".equals(nextGrade) || "A".equals(nextGrade) ? 14 : 12;
        String weaknessAction = contractActionType(targetType, weakness);
        List<StageObjectiveItemDTO> items = firstWeek
                ? List.of(
                        item("冲档契约：" + stageGoal, routeTouches >= 2 || routeLeaderScore >= 8,
                                "这是上轮评分卡给本周目的短线目标，先走「" + routeLabel(baseRouteType)
                                        + "」基础路线，再冲「" + targetLabel + "」。",
                                "GRADE_CONTRACT", weaknessAction, baseRouteType),
                        item("前7天同路证据2次", routeTouches >= 2,
                                "连续点同类基础路线行动，避免复活赛第一周继续摇摆。",
                                "ROUTE_SCORE", routeAction, baseRouteType),
                        item("短板先别扩大", weaknessPass(weakness, recentBasics, evidenceTouches, recentFanOps, recentTrainTalk, recentRests, openDebtCount, severeDebtCount),
                                weaknessHint(weakness),
                                weaknessObjectiveKey(weakness), weaknessAction, baseRouteType)
                )
                : List.of(
                        item("路线分冲到" + routeTarget, routeLeaderScore >= routeTarget,
                                "第2周要把「" + routeLabel(baseRouteType) + "」打成可结算主轴，再看是否派生为「" + targetLabel + "」。",
                                "ROUTE_SCORE", routeAction, baseRouteType),
                        item("证据链至少2条", evidenceTouches >= 2,
                                "直播、投稿、切片或同台要能被结局复盘引用。",
                                "CONTENT_ASSET", contractEvidenceActionType(targetType), baseRouteType),
                        item("旧账压在2笔以内", openDebtCount <= 2 && severeDebtCount == 0,
                                "冲" + nextGrade + "档不能背着高危旧账进中段。",
                                "RISK_CONTROL", "FAN_GROUP_MAINTAIN", baseRouteType)
                );
        String title = firstWeek ? "复活赛第1周 · 冲档定调" : "复活赛第2周 · 冲档加固";
        String summary = "目标「" + targetLabel + "」冲" + nextGrade + "档；主短板："
                + weaknessLabel(weakness) + "。基础路线先按「" + routeLabel(baseRouteType) + "」推进。";
        return routePlan(title, summary, items);
    }

    private StageObjectivePlan routeSprintPlan(
            Vup vup,
            String routeKey,
            boolean finalStage,
            int recentStreams,
            int recentVideos,
            int recentClips,
            int recentFanOps,
            int recentNpcOps,
            int recentRests,
            int recentTrainSong,
            int recentTrainDance,
            int recentTrainTalk,
            int routeLeaderScore,
            int openDebtCount,
            int severeDebtCount
    ) {
        String route = routeKey == null || routeKey.isBlank() ? "UNKNOWN" : routeKey;
        String title = finalStage ? "第30天收束" : "第4周路线冲刺";
        return switch (route) {
            case "SINGING_IDOL" -> routePlan(
                    title + " · 歌势",
                    "把练习室里的歌势证据兑现成观众能记住的舞台。",
                    List.of(
                            item("歌力达到11", vup.getSongPower() >= 11,
                                    "歌势结局硬看歌力，最后几天别只练不播。"),
                            item("歌势证据上桌", recentTrainSong + recentStreams + recentVideos >= 2,
                                    "练歌、低压歌回或投稿至少再留下两条证据。"),
                            item("收官口碑不塌", vup.getReputation() >= 60 && severeDebtCount == 0,
                                    "歌势遗珠需要遗憾，不需要毕业照被开庭。")
                    )
            );
            case "SLICE_SAINT", "DANCE_MEME" -> routePlan(
                    title + " · 切片",
                    "给切片组新料，同时别让同一个梗被复读到糊。",
                    List.of(
                            item("梗浓度到40", vup.getMemeLevel() >= 40,
                                    "切片圣体至少要有足够可剪的记忆点。"),
                            item("素材链不断", recentVideos + recentClips + recentTrainDance >= 2,
                                    "投稿、切片或练舞都能给二创组供货。"),
                            item("回旋风险可控", openDebtCount <= 3 && severeDebtCount <= 1,
                                    "可以有回旋镖，但不能让所有素材都变事故。")
                    )
            );
            case "SOCIAL_COLLAB" -> routePlan(
                    title + " · DD",
                    "让同台流量留下来，同时给老粉一个解释入口。",
                    List.of(
                            item("DD占比达标", ddRatio(vup) >= balanceConfig.ddBusStopDdRatioRequired(),
                                    "DD公交站需要足够多换乘乘客。"),
                            item("同台证据继续出现", recentNpcOps + recentStreams >= 1,
                                    "NPC互动或联动直播要在收官周留下记录。"),
                            item("老粉有被安抚", recentFanOps + recentTrainTalk >= 1,
                                    "只接DD不安抚老粉，容易像路过站台。")
                    )
            );
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> routePlan(
                    title + " · 主会场",
                    "热度要响，但旧账要能被你拿在手里。",
                    List.of(
                            item("围观热度到60", vup.getWatchHeat() >= 60,
                                    "黑红和主会场路线需要足够大的现场。"),
                            item("事故素材够硬", vup.getMemeLevel() >= 55 || recentClips >= 1,
                                    "梗浓度、切片或事件选择要能撑住复盘。"),
                            item("别炸到失控", openDebtCount <= 4 && severeDebtCount <= 1,
                                    "主会场可以乱，但不能全是未结清高危旧账。")
                    )
            );
            case "CYBER_GIRLFRIEND" -> routePlan(
                    title + " · 陪伴",
                    "陪伴营业要写进证据链，同时把边界留清楚。",
                    List.of(
                            item("独角兽和商业成型", cyberRelationshipReady(vup),
                                    "赛博女友看陪伴关系浓度，也看商业化是否成型。"),
                            item("粉丝服务留证据", recentFanOps + recentStreams >= 1,
                                    "粉丝群维护、高亮互动回应或陪伴回要在最后阶段出现。"),
                            item("边界没有糊掉", vup.getReputation() >= 55 && severeDebtCount == 0,
                                    "陪伴路线最怕高危关系债把结局带歪。")
                    )
            );
            case "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> routePlan(
                    title + " · 稳健",
                    "把低压、口碑和告别排班收成一条体面证据链。",
                    List.of(
                            item("口碑达到70", vup.getReputation() >= 70,
                                    "稳健收官靠口碑，不靠临门一脚开庭。"),
                            item("低压证据足够", recentFanOps + recentTrainTalk + recentRests >= 2,
                                    "粉丝群维护、杂谈复盘或休息调整要有连续证据。"),
                            item("高危旧账清干净", severeDebtCount == 0,
                                    "光荣毕业和电子榨菜都不适合背着高危债收尾。")
                    )
            );
            default -> routePlan(
                    title + " · 定型",
                    "先选一条最像的路线连续补证据，别让结局组无从归档。",
                    List.of(
                            item("路线证据到18分", routeLeaderScore >= 18,
                                    "任一主路线分数够厚，结局才不会散。"),
                            item("粉丝基本盘过线", vup.getFans() >= 130,
                                    "查无此V的第一道硬门槛是粉丝基本盘。"),
                            item("最后别背高危债", severeDebtCount == 0,
                                    "未定型时背高危旧账，结局组只会先写事故。")
                    )
            );
        };
    }

    private StageObjectivePlan routePlan(String title, String summary, List<StageObjectiveItemDTO> items) {
        int achievedCount = (int) items.stream().filter(StageObjectiveItemDTO::achieved).count();
        return plan(title, summary, achievedCount, Math.max(1, items.size()), items);
    }

    private String sprintRouteFor(Vup vup, String currentRoute, int openDebtCount, int severeDebtCount) {
        if (vup.getMemeLevel() >= 75 && vup.getReputation() < 45 && openDebtCount > 0) {
            return "MAIN_STAGE_KING";
        }
        if ((unicornRatio(vup) >= balanceConfig.cyberUnicornRatioRequired()
                || vup.getUnicornFans() >= balanceConfig.cyberUnicornFansRequired())
                && vup.getCommercialLevel() >= balanceConfig.cyberCommercialRequired()) {
            return "CYBER_GIRLFRIEND";
        }
        if (vup.getReputation() >= 75 && vup.getWatchHeat() <= 45 && severeDebtCount == 0) {
            return "GLORIOUS_GRADUATION";
        }
        if (currentRoute == null || currentRoute.isBlank()) {
            return "UNKNOWN";
        }
        return currentRoute;
    }

    private StageObjectivePlan objectiveFor(Vup vup, int startDay, int endDay) {
        return objectivePlanFor(vup, startDay, endDay);
    }

    private StageObjectivePlan plan(String title, String summary, int progress, int target, List<StageObjectiveItemDTO> items) {
        int achievedCount = (int) items.stream().filter(StageObjectiveItemDTO::achieved).count();
        return new StageObjectivePlan(
                title,
                summary,
                Math.max(0, Math.min(100, target <= 0 ? 100 : progress * 100 / target)),
                Math.min(progress, target) + "/" + target,
                items,
                achievedCount,
                items.size()
        );
    }

    private StageObjectiveItemDTO item(String label, boolean achieved, String hint) {
        ObjectiveMarker marker = objectiveMarkerFor(label);
        return item(label, achieved, hint, marker.objectiveKey(), marker.recommendedActionType(), marker.targetRouteType());
    }

    private StageObjectiveItemDTO item(
            String label,
            boolean achieved,
            String hint,
            String objectiveKey,
            String recommendedActionType,
            String targetRouteType
    ) {
        return new StageObjectiveItemDTO(
                label,
                achieved ? "达成" : "推进中",
                hint,
                achieved,
                objectiveKey == null ? "" : objectiveKey,
                recommendedActionType == null ? "" : recommendedActionType,
                targetRouteType == null ? "" : targetRouteType
        );
    }

    private Map<String, Object> restartGradeContract(Vup vup) {
        if (vup.getExpectationJson() == null || vup.getExpectationJson().isBlank()) {
            return Map.of();
        }
        try {
            Object raw = jsonService.readMap(vup.getExpectationJson()).get("restartGradeContract");
            if (raw instanceof Map<?, ?> rawMap) {
                java.util.LinkedHashMap<String, Object> contract = new java.util.LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        contract.put(key, entry.getValue());
                    }
                }
                return contract;
            }
        } catch (IllegalStateException ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private int routeTouchCount(
            String targetType,
            int recentStreams,
            int recentVideos,
            int recentClips,
            int recentFanOps,
            int recentNpcOps,
            int recentTrainSong,
            int recentTrainDance,
            int recentTrainTalk
    ) {
        return switch (canonicalEndingType(targetType)) {
            case "SINGING_IDOL" -> recentTrainSong + recentStreams + recentVideos;
            case "DANCE_MEME" -> recentTrainDance + recentVideos + recentClips;
            case "SLICE_SAINT" -> recentVideos + recentClips;
            case "SOCIAL_COLLAB", "DD_BUS_STOP" -> recentNpcOps + recentStreams;
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> recentStreams + recentClips + recentFanOps;
            case "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> recentTrainTalk + recentFanOps;
            default -> recentStreams + recentVideos + recentFanOps;
        };
    }

    private String contractActionType(String targetType, String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "FAN_GROUP_MAINTAIN";
            case "EVIDENCE" -> contractEvidenceActionType(targetType);
            default -> routeActionType(targetType);
        };
    }

    private String contractEvidenceActionType(String targetType) {
        return switch (canonicalEndingType(targetType)) {
            case "SINGING_IDOL" -> "STREAM_PLAN";
            case "SLICE_SAINT", "DANCE_MEME" -> "PUBLISH_VIDEO";
            case "SOCIAL_COLLAB", "DD_BUS_STOP" -> "NPC_INTERACT";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "PUBLISH_CLIP";
            case "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> "FAN_GROUP_MAINTAIN";
            default -> "PUBLISH_VIDEO";
        };
    }

    private boolean weaknessPass(
            String weakness,
            int recentBasics,
            int evidenceTouches,
            int recentFanOps,
            int recentTrainTalk,
            int recentRests,
            int openDebtCount,
            int severeDebtCount
    ) {
        return switch (weakness) {
            case "RISK_CONTROL" -> severeDebtCount == 0 && (openDebtCount == 0 || recentFanOps + recentTrainTalk + recentRests >= 1);
            case "EVIDENCE" -> evidenceTouches >= 2 || recentBasics >= 3;
            default -> recentBasics >= 3;
        };
    }

    private String weaknessHint(String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "上轮主要扣在风险控制，先用粉丝群维护、杂谈复盘或休息把旧账压住。";
            case "EVIDENCE" -> "上轮主要扣在证据链，优先留下能进结局复盘的直播、投稿、切片或同台。";
            default -> "上轮主要扣在路线专注，第一周别频繁转线。";
        };
    }

    private String weaknessObjectiveKey(String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "RISK_CONTROL";
            case "EVIDENCE" -> "RECENT_EVIDENCE";
            default -> "ROUTE_SCORE";
        };
    }

    private String weaknessLabel(String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "风险控制";
            case "EVIDENCE" -> "证据链";
            default -> "路线专注";
        };
    }

    private String routeLabel(String routeType) {
        return switch (canonicalEndingType(routeType)) {
            case "SINGING_IDOL" -> "歌势遗珠";
            case "SLICE_SAINT" -> "切片圣体";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "DD_BUS_STOP" -> "DD公交站";
            case "DANCE_MEME" -> "梗舞整活";
            case "SOCIAL_COLLAB" -> "社交联动";
            default -> "查无此V";
        };
    }

    private String canonicalEndingType(String endingType) {
        if (endingType == null || endingType.isBlank()) {
            return "UNKNOWN";
        }
        return switch (endingType.trim().toUpperCase()) {
            case "LEGEND" -> "MAIN_STAGE_KING";
            case "GRADUATION" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE" -> "CYBER_GIRLFRIEND";
            case "DANCE_MEME" -> "DANCE_MEME";
            case "SOCIAL_COLLAB" -> "SOCIAL_COLLAB";
            default -> endingType.trim().toUpperCase();
        };
    }

    private String baseRouteTypeForTarget(String targetType) {
        return switch (canonicalEndingType(targetType)) {
            case "SINGING_IDOL" -> "SINGING_IDOL";
            case "SLICE_SAINT" -> "SLICE_SAINT";
            case "DANCE_MEME" -> "DANCE_MEME";
            case "SOCIAL_COLLAB", "DD_BUS_STOP" -> "SOCIAL_COLLAB";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> "ELECTRONIC_PICKLE";
            default -> "UNKNOWN";
        };
    }

    private ObjectiveMarker objectiveMarkerFor(String label) {
        if (label == null || label.isBlank()) {
            return ObjectiveMarker.empty();
        }
        if (label.contains("低压证据")) {
            return new ObjectiveMarker("LOW_PRESSURE_PROOF", "TRAIN_TALK", "ELECTRONIC_PICKLE");
        }
        if (label.contains("粉丝服务")) {
            return new ObjectiveMarker("FAN_SERVICE_PROOF", "FAN_GROUP_MAINTAIN", "CYBER_GIRLFRIEND");
        }
        if (label.contains("独角兽") || label.contains("商业成型")) {
            return new ObjectiveMarker("RELATION_BUSINESS_PROOF", "FAN_GROUP_MAINTAIN", "CYBER_GIRLFRIEND");
        }
        if (label.contains("围观热度")) {
            return new ObjectiveMarker("WATCH_HEAT", "STREAM_PLAN", "BLACK_RED_MAIN_STAGE");
        }
        if (label.contains("梗浓度")) {
            return new ObjectiveMarker("MEME_DENSITY", "PUBLISH_CLIP", "SLICE_SAINT");
        }
        if (label.contains("债务") || label.contains("旧账") || label.contains("米线")) {
            return new ObjectiveMarker("RISK_CONTROL", "FAN_GROUP_MAINTAIN", "");
        }
        if (label.contains("口碑") || label.contains("杂谈") || label.contains("边界")) {
            return new ObjectiveMarker("REPUTATION", "TRAIN_TALK", "");
        }
        if (label.contains("体力") || label.contains("休息") || label.contains("低压")) {
            return new ObjectiveMarker("RECOVERY", "REST", "");
        }
        if (label.contains("视频") || label.contains("投稿") || label.contains("素材链")) {
            return new ObjectiveMarker("CONTENT_ASSET", "PUBLISH_VIDEO", "SLICE_SAINT");
        }
        if (label.contains("切片") || label.contains("梗浓度") || label.contains("事故素材")) {
            return new ObjectiveMarker("CLIP_OUTPUT", "PUBLISH_CLIP", "SLICE_SAINT");
        }
        if (label.contains("同台") || label.contains("DD")) {
            return new ObjectiveMarker("SOCIAL_PROOF", "NPC_INTERACT", "SOCIAL_COLLAB");
        }
        if (label.contains("歌力") || label.contains("歌势")) {
            return new ObjectiveMarker("SINGING_PROOF", "TRAIN_SONG", "SINGING_IDOL");
        }
        if (label.contains("路线")) {
            return new ObjectiveMarker("ROUTE_SCORE", "", "");
        }
        if (label.contains("粉丝") || label.contains("老粉")) {
            return new ObjectiveMarker("FAN_BASE", "FAN_GROUP_MAINTAIN", "");
        }
        return ObjectiveMarker.empty();
    }

    private int routeLeaderScore(Vup vup) {
        return jsonMap(vup.getRouteScoreJson()).entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Number)
                .mapToInt(entry -> ((Number) entry.getValue()).intValue())
                .max()
                .orElse(0);
    }

    private int ddRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getDdFans() * 100 / vup.getFans();
    }

    private int unicornRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getUnicornFans() * 100 / vup.getFans();
    }

    private boolean cyberRelationshipReady(Vup vup) {
        return (unicornRatio(vup) >= balanceConfig.cyberUnicornRatioRequired()
                || vup.getUnicornFans() >= balanceConfig.cyberUnicornFansRequired())
                && vup.getCommercialLevel() >= balanceConfig.cyberCommercialRequired();
    }

    private String routeActionType(String routeType) {
        routeType = baseRouteTypeForTarget(routeType);
        if ("SINGING_IDOL".equals(routeType)) {
            return "TRAIN_SONG";
        }
        if ("DANCE_MEME".equals(routeType)) {
            return "TRAIN_DANCE";
        }
        if ("SLICE_SAINT".equals(routeType)) {
            return "PUBLISH_VIDEO";
        }
        if ("SOCIAL_COLLAB".equals(routeType) || "DD_BUS_STOP".equals(routeType)) {
            return "NPC_INTERACT";
        }
        if ("BLACK_RED_MAIN_STAGE".equals(routeType) || "MAIN_STAGE_KING".equals(routeType)) {
            return "FAN_GROUP_MAINTAIN";
        }
        if ("CYBER_GIRLFRIEND".equals(routeType)) {
            return "TRAIN_TALK";
        }
        if ("ELECTRONIC_PICKLE".equals(routeType)) {
            return "TRAIN_TALK";
        }
        if ("GLORIOUS_GRADUATION".equals(routeType)) {
            return "REST";
        }
        return "STREAM_PLAN";
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(json);
    }

    private int previousStageEndDay(int day) {
        if (day <= 14) {
            return 7;
        }
        if (day <= 21) {
            return 14;
        }
        if (day <= 28) {
            return 21;
        }
        return 28;
    }

    private int previousStageWindowStart(int day) {
        if (day <= 14) {
            return 1;
        }
        if (day <= 21) {
            return 8;
        }
        if (day <= 28) {
            return 15;
        }
        return 22;
    }

    public record StageObjectivePlan(
            String title,
            String summary,
            int progress,
            String progressLabel,
            List<StageObjectiveItemDTO> items,
            int achievedCount,
            int totalCount
    ) {
    }

    public record StageObjectiveTarget(
            String actionType,
            String targetRouteType,
            String label,
            String objectiveKey
    ) {
        boolean active() {
            return actionType != null && !actionType.isBlank();
        }
    }

    private record ObjectiveMarker(
            String objectiveKey,
            String recommendedActionType,
            String targetRouteType
    ) {
        static ObjectiveMarker empty() {
            return new ObjectiveMarker("", "", "");
        }
    }

    public record StageMomentum(
            String tier,
            int trueFanBonus,
            int popularityBonus,
            int routeBonus,
            int watchHeatBonus,
            int reputationBonus,
            String hint,
            String previousStageTitle,
            String previousStageSummary,
            String previousStageProgressLabel
    ) {
        static StageMomentum empty() {
            return new StageMomentum("none", 0, 0, 0, 0, 0, "", "", "", "");
        }
    }
}
