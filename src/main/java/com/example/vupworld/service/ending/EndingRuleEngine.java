package com.example.vupworld.service.ending;

import com.example.vupworld.service.risk.RiskDebtLabels;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一结局规则引擎。正式结局和预演结局共用同一套规则。
 * Gate 6 要求：预演和正式结局规则一致，GLORIOUS_GRADUATION 是唯一毕业类枚举。
 */
@Component
public class EndingRuleEngine {

    private final BalanceConfig balanceConfig;
    private final JsonService jsonService;

    public EndingRuleEngine(BalanceConfig balanceConfig, JsonService jsonService) {
        this.balanceConfig = balanceConfig;
        this.jsonService = jsonService;
    }

    /**
     * 评估结局，返回匹配的结局类型。
     */
    public EndingResult evaluate(
            Vup vup,
            DaySession session,
            com.example.vupworld.model.DailyReport report,
            BusinessLog log,
            List<RiskDebt> openDebts,
            List<BusinessLog> businessLogs,
            boolean isFinal
    ) {
        List<RiskDebt> debts = openDebts == null ? List.of() : openDebts;
        List<BusinessLog> logs = businessLogs == null ? List.of() : businessLogs;
        int fans = vup.getFans();
        int ddRatio = fans > 0 ? vup.getDdFans() * 100 / fans : 0;
        int unicornRatio = fans > 0 ? vup.getUnicornFans() * 100 / fans : 0;
        int songPower = vup.getSongPower();
        String currentRoute = vup.getCurrentRoute() == null ? "UNKNOWN" : vup.getCurrentRoute();
        int severeDebtCount = (int) debts.stream().filter(d -> d.getSeverity() >= 4).count();
        long fanServiceEvidenceCount = logs.stream()
                .filter(this::hasFanServiceEvidence)
                .count();
        long graduationEvidenceCount = logs.stream()
                .filter(this::hasGraduationEvidence)
                .count();
        long blackRedEvidenceCount = logs.stream()
                .filter(this::hasBlackRedEvidence)
                .count();
        int routeScore = currentRouteScore(vup, currentRoute);
        int singingRouteScore = currentRouteScore(vup, "SINGING_IDOL");
        int sliceRouteScore = Math.max(
                currentRouteScore(vup, "SLICE_SAINT"),
                currentRouteScore(vup, "DANCE_MEME")
        );
        int collabRouteScore = currentRouteScore(vup, "SOCIAL_COLLAB");
        int blackRedRouteScore = currentRouteScore(vup, "BLACK_RED_MAIN_STAGE");
        int stableRouteScore = currentRouteScore(vup, "ELECTRONIC_PICKLE");
        int recentSingingEvidenceCount = recentRouteEvidenceCount(logs, vup.getDayCount(), "SINGING_IDOL");
        int recentSliceEvidenceCount = recentRouteEvidenceCount(logs, vup.getDayCount(), "SLICE_SAINT");
        int recentCollabEvidenceCount = recentRouteEvidenceCount(logs, vup.getDayCount(), "SOCIAL_COLLAB");
        int recentBlackRedEvidenceCount = recentRouteEvidenceCount(logs, vup.getDayCount(), "BLACK_RED_MAIN_STAGE");
        int recentStableEvidenceCount = recentRouteEvidenceCount(logs, vup.getDayCount(), "ELECTRONIC_PICKLE");
        boolean blackRedIntentLocked = ("BLACK_RED_MAIN_STAGE".equals(currentRoute) || "MAIN_STAGE_KING".equals(currentRoute))
                && routeScore >= 24
                && blackRedEvidenceCount >= balanceConfig.blackRedEvidenceRequired();
        boolean singingCurrentRoute = "SINGING_IDOL".equals(currentRoute);
        boolean sliceCurrentRoute = "SLICE_SAINT".equals(currentRoute) || "DANCE_MEME".equals(currentRoute);
        boolean collabCurrentRoute = "SOCIAL_COLLAB".equals(currentRoute);
        boolean singingRouteReady = singingRouteScore >= (singingCurrentRoute ? 16 : 18)
                && recentSingingEvidenceCount >= 2;
        boolean sliceRouteReady = sliceRouteScore >= (sliceCurrentRoute ? 16 : 18)
                && recentSliceEvidenceCount >= 2;
        boolean collabRouteReady = collabRouteScore >= (collabCurrentRoute ? 14 : 16)
                && recentCollabEvidenceCount >= 1;
        boolean stableRouteReady = stableRouteScore >= 12
                && recentStableEvidenceCount >= 2;
        boolean isGraduationAction = log != null && "FAN_GROUP_MAINTAIN".equals(log.getAction());
        // 体面收束结局：路线分达标 + 无严重旧账 + 口碑达标 + 低压收官 + 稳健证据充足 + 最后一天主动维护粉丝群
        // 保留 isGraduationAction 作为毕业意图门槛，避免稳健线（最后一天 TRAIN_TALK）误判为毕业。
        // glorious_graduation 演示脚本在最后一天优先 FAN_GROUP_MAINTAIN，firstAvailableDemoAction 会处理压力锁回退。
        boolean graduationReady = vup.getReputation() >= balanceConfig.graduationReputationRequired()
                && vup.getWatchHeat() <= balanceConfig.graduationWatchHeatMax()
                && severeDebtCount == 0
                && graduationEvidenceCount >= balanceConfig.graduationEvidenceActionsRequired()
                && stableRouteReady
                && isGraduationAction;
        boolean cyberReady = (unicornRatio >= balanceConfig.cyberUnicornRatioRequired()
                || vup.getUnicornFans() >= balanceConfig.cyberUnicornFansRequired())
                && vup.getCommercialLevel() >= balanceConfig.cyberCommercialRequired()
                && fanServiceEvidenceCount >= balanceConfig.cyberFanServiceEvidenceRequired()
                && stableRouteScore >= 12
                && recentStableEvidenceCount + recentBlackRedEvidenceCount >= 1;
        boolean blackRedRouteReady = blackRedRouteScore >= 15
                && (recentBlackRedEvidenceCount >= 1 || blackRedIntentLocked)
                && (blackRedEvidenceCount >= balanceConfig.blackRedEvidenceRequired()
                || "BLACK_RED_MAIN_STAGE".equals(currentRoute))
                && vup.getWatchHeat() >= balanceConfig.blackRedWatchHeatRequired()
                && vup.getReputation() < balanceConfig.blackRedReputationMax();
        boolean blackRedCrisisReady = vup.getWatchHeat() >= balanceConfig.blackRedCrisisWatchHeatRequired()
                && vup.getReputation() < balanceConfig.blackRedCrisisReputationMax()
                && !debts.isEmpty();
        long proactiveEvidenceCount = logs.stream()
                .filter(this::isProactiveRouteEvidence)
                .count();
        boolean passiveIdleArchive = fans < 180
                && proactiveEvidenceCount == 0
                && routeScore <= 12
                && vup.getWatchHeat() <= 15
                && vup.getMemeLevel() <= 15
                && vup.getCommercialLevel() <= 15;

        List<String> matchedRules = new ArrayList<>();
        String endingType;
        String rule;

        if (passiveIdleArchive) {
            endingType = "UNKNOWN";
            rule = "UNKNOWN_PASSIVE_IDLE_ARCHIVE";
        } else if (fans < 130 && "UNKNOWN".equals(currentRoute)) {
            endingType = "UNKNOWN";
            rule = "UNKNOWN_LOW_FANS_AND_UNFORMED_ROUTE";
        } else if (fans < 130) {
            endingType = "UNKNOWN";
            rule = "UNKNOWN_MINIMUM_FANS";
        } else if ("UNKNOWN".equals(currentRoute)) {
            endingType = "UNKNOWN";
            rule = "UNKNOWN_ROUTE_UNFORMED";
        } else if (graduationReady) {
            endingType = "GLORIOUS_GRADUATION";
            rule = "GRADUATION_ACTION_HIGH_REPUTATION_CLEAN_RECORD";
        } else if (("MAIN_STAGE_KING".equals(currentRoute) && routeScore >= 32 && vup.getMemeLevel() >= 75 && vup.getWatchHeat() >= 85)
                || (blackRedIntentLocked && vup.getMemeLevel() > 90 && vup.getWatchHeat() >= 100 && vup.getReputation() < 25 && !debts.isEmpty())) {
            endingType = "MAIN_STAGE_KING";
            rule = "HIGH_MEME_LOW_REPUTATION_WITH_DEBT";
        } else if (cyberReady) {
            endingType = "CYBER_GIRLFRIEND";
            rule = "HIGH_UNICORN_COMMERCIAL_AND_FAN_SERVICE";
        } else if (collabRouteReady && ddRatio >= balanceConfig.ddBusStopDdRatioRequired()) {
            endingType = "DD_BUS_STOP";
            rule = "SOCIAL_COLLAB_ROUTE_AND_HIGH_DD_RATIO";
        } else if (singingRouteReady && songPower >= 11) {
            endingType = "SINGING_IDOL";
            rule = "ROUTE_SINGING_SCORE_AND_RECENT_EVIDENCE";
        } else if (sliceRouteReady && (vup.getMemeLevel() >= 40 || vup.getFunFans() >= 45)) {
            endingType = "SLICE_SAINT";
            rule = "ROUTE_SLICE_SCORE_AND_RECENT_EVIDENCE";
        } else if (blackRedRouteReady || blackRedCrisisReady) {
            endingType = "BLACK_RED_MAIN_STAGE";
            rule = "BLACK_RED_EVIDENCE_OR_HEAT_CRISIS";
        } else {
            endingType = "ELECTRONIC_PICKLE";
            rule = "DEFAULT_SAFE_ROUTE";
        }
        matchedRules.add(rule);

        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("fans", fans);
        conditions.put("fansThreshold", 130);
        conditions.put("ddRatio", ddRatio);
        conditions.put("ddRatioThreshold", balanceConfig.ddBusStopDdRatioRequired());
        conditions.put("unicornRatio", unicornRatio);
        conditions.put("unicornRatioThreshold", balanceConfig.cyberUnicornRatioRequired());
        conditions.put("unicornFans", vup.getUnicornFans());
        conditions.put("unicornFansThreshold", balanceConfig.cyberUnicornFansRequired());
        conditions.put("memeLevel", vup.getMemeLevel());
        conditions.put("reputation", vup.getReputation());
        conditions.put("watchHeat", vup.getWatchHeat());
        conditions.put("commercialLevel", vup.getCommercialLevel());
        conditions.put("songPower", songPower);
        conditions.put("singingSongPowerThreshold", 11);
        conditions.put("sliceMemeThreshold", 40);
        conditions.put("routeScore", routeScore);
        conditions.put("singingRouteScore", singingRouteScore);
        conditions.put("sliceRouteScore", sliceRouteScore);
        conditions.put("collabRouteScore", collabRouteScore);
        conditions.put("blackRedRouteScore", blackRedRouteScore);
        conditions.put("stableRouteScore", stableRouteScore);
        conditions.put("routeScoreStableThreshold", 12);
        conditions.put("routeScoreCollabThreshold", 14);
        conditions.put("routeScoreHighThreshold", 16);
        conditions.put("recentSingingEvidenceCount", recentSingingEvidenceCount);
        conditions.put("recentSliceEvidenceCount", recentSliceEvidenceCount);
        conditions.put("recentCollabEvidenceCount", recentCollabEvidenceCount);
        conditions.put("recentBlackRedEvidenceCount", recentBlackRedEvidenceCount);
        conditions.put("recentStableEvidenceCount", recentStableEvidenceCount);
        conditions.put("blackRedIntentLocked", blackRedIntentLocked);
        conditions.put("graduationEvidenceCount", graduationEvidenceCount);
        conditions.put("graduationEvidenceRequired", balanceConfig.graduationEvidenceActionsRequired());
        conditions.put("graduationWatchHeatMax", balanceConfig.graduationWatchHeatMax());
        conditions.put("blackRedEvidenceCount", blackRedEvidenceCount);
        conditions.put("blackRedEvidenceRequired", balanceConfig.blackRedEvidenceRequired());
        conditions.put("currentRoute", currentRoute);
        conditions.put("openDebtCount", debts.size());
        conditions.put("severeDebtCount", severeDebtCount);
        conditions.put("fanServiceEvidenceCount", fanServiceEvidenceCount);
        conditions.put("fanServiceEvidenceRequired", balanceConfig.cyberFanServiceEvidenceRequired());
        conditions.put("isGraduationAction", isGraduationAction);
        conditions.put("graduationReady", graduationReady);
        conditions.put("cyberReady", cyberReady);
        conditions.put("blackRedRouteReady", blackRedRouteReady);
        conditions.put("blackRedCrisisReady", blackRedCrisisReady);
        conditions.put("proactiveEvidenceCount", proactiveEvidenceCount);
        conditions.put("passiveIdleArchive", passiveIdleArchive);
        conditions.put("singingRouteReady", singingRouteReady);
        conditions.put("sliceRouteReady", sliceRouteReady);
        conditions.put("collabRouteReady", collabRouteReady);
        conditions.put("stableRouteReady", stableRouteReady);
        conditions.put("isFinal", isFinal);
        conditions.put("unknownFailureReason", unknownFailureReason(rule));

        return new EndingResult(endingType, rule, matchedRules, conditions);
    }

    private int currentRouteScore(Vup vup, String routeType) {
        if (routeType == null || routeType.isBlank() || vup.getRouteScoreJson() == null || vup.getRouteScoreJson().isBlank()) {
            return 0;
        }
        Object raw = jsonService.readMap(vup.getRouteScoreJson()).get(routeType);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int recentRouteEvidenceCount(List<BusinessLog> logs, int currentDay, String routeType) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        int anchorDay = currentDay > 0
                ? currentDay
                : logs.stream().mapToInt(BusinessLog::getDay).max().orElse(0);
        int startDay = Math.max(1, anchorDay - 6);
        return (int) logs.stream()
                .filter(log -> log != null && log.getDay() >= startDay)
                .filter(log -> isRouteEvidence(log, routeType))
                .count();
    }

    private boolean isRouteEvidence(BusinessLog log, String routeType) {
        String action = log.getAction();
        String routeScoreChange = log.getRouteScoreChange();
        String weightDetail = log.getWeightDetail();
        String result = log.getResult();
        if (containsMarker(routeScoreChange, routeType) || containsMarker(weightDetail, routeType)) {
            return true;
        }
        return switch (routeType) {
            case "SINGING_IDOL" -> "TRAIN_SONG".equals(action)
                    || ("STREAM_PLAN".equals(action) && containsMarker(routeScoreChange, "SINGING_IDOL"))
                    || containsMarker(weightDetail, "singingEvidence")
                    || (result != null && (result.contains("练歌") || result.contains("歌势") || result.contains("唱歌")));
            case "SLICE_SAINT" -> "PUBLISH_VIDEO".equals(action)
                    || "PUBLISH_CLIP".equals(action)
                    || "TRAIN_DANCE".equals(action)
                    || containsMarker(weightDetail, "memeMaterial")
                    || containsMarker(weightDetail, "clipMomentum");
            case "SOCIAL_COLLAB" -> "NPC_INTERACT".equals(action)
                    || containsMarker(weightDetail, "collabEvidence")
                    || (result != null && (result.contains("联动") || result.contains("合作") || result.contains("同台")));
            case "BLACK_RED_MAIN_STAGE" -> hasBlackRedEvidence(log)
                    || containsMarker(weightDetail, "trafficSpike")
                    || containsMarker(weightDetail, "archivePressure");
            case "ELECTRONIC_PICKLE" -> "FAN_GROUP_MAINTAIN".equals(action)
                    || "TRAIN_TALK".equals(action)
                    || "REST".equals(action)
                    || containsMarker(weightDetail, "stableCommunity")
                    || containsMarker(weightDetail, "riskCooldown");
            default -> false;
        };
    }

    private String unknownFailureReason(String rule) {
        return switch (rule) {
            case "UNKNOWN_PASSIVE_IDLE_ARCHIVE" -> "整局太安静，行动记录像空白排班，观众没有抓手。";
            case "UNKNOWN_LOW_FANS_AND_UNFORMED_ROUTE" -> "粉丝基本盘不足，路线证据也没有成型，观众既没坐稳也没记住标签。";
            case "UNKNOWN_MINIMUM_FANS" -> "路线有方向，但总粉丝没有跨过基础门槛，像写好了标题却没人点进来。";
            case "UNKNOWN_ROUTE_UNFORMED" -> "粉丝够了，但30天证据太分散，观众记得你很努力，却说不出你是哪种V。";
            default -> "";
        };
    }

    private boolean hasGraduationEvidence(BusinessLog log) {
        if (log == null) {
            return false;
        }
        String action = log.getAction();
        return "FAN_GROUP_MAINTAIN".equals(action)
                || "TRAIN_TALK".equals(action)
                || "REST".equals(action)
                || containsMarker(log.getRouteScoreChange(), "ELECTRONIC_PICKLE");
    }

    private boolean hasBlackRedEvidence(BusinessLog log) {
        if (log == null) {
            return false;
        }
        String action = log.getAction();
        return "PUBLISH_CLIP".equals(action)
                || containsMarker(log.getRouteScoreChange(), "BLACK_RED_MAIN_STAGE")
                || containsMarker(log.getWeightDetail(), "BLACK_RED_MAIN_STAGE")
                || containsMarker(log.getResult(), "HARD_MOUTH")
                || containsMarker(log.getResult(), "ABSTRACT_MEME");
    }

    private boolean hasFanServiceEvidence(BusinessLog log) {
        return containsFanServiceMarker(log.getWeightDetail()) || containsFanServiceMarker(log.getResult());
    }

    private boolean containsMarker(String text, String marker) {
        return text != null && text.contains(marker);
    }

    private boolean containsFanServiceMarker(String text) {
        return text != null
                && (text.contains("FAN_SERVICE")
                || text.contains("醒目留言")
                || text.contains("高亮互动")
                || text.contains("谢SC")
                || text.contains("陪伴营业")
                || text.contains("榜一排班表"));
    }

    public String titleFor(String endingType) {
        return switch (endingType) {
            case "UNKNOWN" -> "查无此V";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "DD_BUS_STOP" -> "DD公交站";
            case "SINGING_IDOL" -> "歌势遗珠";
            case "SLICE_SAINT" -> "切片圣体";
            case "BLACK_RED_MAIN_STAGE" -> "黑红顶流";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            default -> "未知结局";
        };
    }

    public String subtitleFor(String endingType) {
        return switch (endingType) {
            case "UNKNOWN" -> "不是塌房，是没有留下能被转述的一句话。";
            case "GLORIOUS_GRADUATION" -> "这次没有被抬走，是自己走下舞台。";
            case "MAIN_STAGE_KING" -> "楼友记住了你，但也只是记住了你。";
            case "CYBER_GIRLFRIEND" -> "你不是虚拟的，但关系是。";
            case "DD_BUS_STOP" -> "你不是在联动，就是在去联动的路上。";
            case "SINGING_IDOL" -> "嗓子是好嗓子，可惜没吃到推荐。";
            case "SLICE_SAINT" -> "你不是每天都在直播，但每天都在被剪。";
            case "BLACK_RED_MAIN_STAGE" -> "热度很响，楼友也很忙。";
            case "ELECTRONIC_PICKLE" -> "不炸场，不开庭，稳定陪饭。";
            default -> "";
        };
    }

    public record EndingResult(
            String endingType,
            String rule,
            List<String> matchedRules,
            Map<String, Object> conditions
    ) {}

    private boolean isProactiveRouteEvidence(BusinessLog log) {
        if (log == null || log.getAction() == null) {
            return false;
        }
        return switch (log.getAction()) {
            case "TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK", "STREAM_PLAN", "PUBLISH_VIDEO",
                    "PUBLISH_CLIP", "FAN_GROUP_MAINTAIN", "NPC_INTERACT" -> true;
            default -> false;
        };
    }
}
