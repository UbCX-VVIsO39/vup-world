package com.example.vupworld.service.progression;

import com.example.vupworld.service.ending.EndingAtlasService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.AchievementDtos.AchievementDTO;
import com.example.vupworld.dto.AchievementDtos.AchievementTitleDTO;
import com.example.vupworld.dto.AchievementDtos.EndingCollectionDTO;
import com.example.vupworld.dto.AchievementDtos.AchievementProgressDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementService {
    private final BusinessLogMapper businessLogMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final JsonService jsonService;
    private final EndingAtlasService endingAtlasService;

    public AchievementService(
            BusinessLogMapper businessLogMapper,
            RiskDebtMapper riskDebtMapper,
            JsonService jsonService,
            EndingAtlasService endingAtlasService
    ) {
        this.businessLogMapper = businessLogMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.jsonService = jsonService;
        this.endingAtlasService = endingAtlasService;
    }

    public AchievementProgressDTO getAchievementProgress(Long userId, Vup vup) {
        Long vupId = vup.getId();
        List<BusinessLog> logs = businessLogMapper.findRecentByVupId(vupId, 1000);
        List<AchievementDTO> achievements = new ArrayList<>();
        EndingCollectionDTO endingCollection = endingAtlasService.endingCollection(userId);

        // 基础成就
        achievements.add(createAchievement("FIRST_STREAM", "初入直播间", "完成第一次直播", "🎬", 1,
                (int) logs.stream().filter(l -> "STREAM_PLAN".equals(l.getAction())).count()));
        achievements.add(createAchievement("FIRST_VIDEO", "视频创作者", "发布第一个视频", "📹", 1,
                (int) logs.stream().filter(l -> "PUBLISH_VIDEO".equals(l.getAction())).count()));
        achievements.add(createAchievement("FIRST_CLIP", "切片大师", "发布第一个切片", "✂️", 1,
                (int) logs.stream().filter(l -> "PUBLISH_CLIP".equals(l.getAction())).count()));
        achievements.add(createAchievement("FAN_GROUP_LEADER", "粉丝群领袖", "维护粉丝群5次", "👥", 5,
                (int) logs.stream().filter(l -> "FAN_GROUP_MAINTAIN".equals(l.getAction())).count()));

        // 粉丝成就
        achievements.add(createAchievement("FAN_100", "百粉达成", "粉丝数达到100", "🎉", 100, vup.getFans()));
        achievements.add(createAchievement("FAN_500", "小有名气", "粉丝数达到500", "⭐", 500, vup.getFans()));
        achievements.add(createAchievement("FAN_1000", "千粉UP主", "粉丝数达到1000", "🌟", 1000, vup.getFans()));
        achievements.add(createAchievement("TRUE_FAN_100", "真爱粉100", "真爱粉达到100", "❤️", 100, vup.getTrueFans()));
        achievements.add(createAchievement("FUN_FAN_100", "乐子人100", "乐子人达到100", "😂", 100, vup.getFunFans()));

        // 路线成就
        achievements.add(createAchievement("SINGING_IDOL", "歌势偶像", "走歌势路线", "🎤", 1,
                "SINGING_IDOL".equals(vup.getCurrentRoute()) ? 1 : 0));
        achievements.add(createAchievement("SLICE_SAINT", "切片圣体", "走切片路线", "✂️", 1,
                "SLICE_SAINT".equals(vup.getCurrentRoute()) ? 1 : 0));
        achievements.add(createAchievement("BLACK_RED_MAIN_STAGE_ROUTE", "黑红主会场", "走黑红主会场基础路线", "🔥", 1,
                "BLACK_RED_MAIN_STAGE".equals(vup.getCurrentRoute()) ? 1 : 0));
        achievements.add(createAchievement("ELECTRONIC_PICKLE", "电子榨菜", "走电子榨菜路线", "🥒", 1,
                "ELECTRONIC_PICKLE".equals(vup.getCurrentRoute()) ? 1 : 0));
        achievements.add(createAchievement("SOCIAL_COLLAB_ROUTE", "社交联动", "走社交联动基础路线，目标可派生到DD公交站", "联", 1,
                "SOCIAL_COLLAB".equals(vup.getCurrentRoute()) ? 1 : 0));
        achievements.add(createAchievement("DANCE_MEME_ROUTE", "梗舞整活", "走梗舞基础路线，目标可派生到切片圣体", "舞", 1,
                "DANCE_MEME".equals(vup.getCurrentRoute()) ? 1 : 0));

        // 风险成就
        achievements.add(createAchievement("DEBT_FIRST", "第一次欠债", "第一次产生债务", "💸", 1,
                riskDebtMapper.findByVupId(vupId).size()));
        achievements.add(createAchievement("DEBT_MASTER", "债务大师", "同时拥有3个未结清债务", "📚", 3,
                riskDebtMapper.findOpenByVupId(vupId).size()));
        achievements.add(createAchievement("RISK_TOOL_USER", "米线工具人", "使用米线工具3次", "🔧", 3,
                (int) logs.stream().filter(l -> l.getRiskToolId() != null).count()));

        Set<String> comboImpactKeys = comboImpactKeys(businessLogMapper.findRecentByUserId(userId, 1000));
        achievements.add(createAchievement("COMBO_IMPACT_ARCHITECT", "连锁反应研究员", "触发3种不同连锁反馈，鼓励多打法复玩", "链", 3,
                comboImpactKeys.size()));
        achievements.add(createAchievement("ROUTE_FOCUS_PROOF", "路线主轴", "任一路线分数达到24，证明一轮打法足够专注", "靶", 24,
                routeLeaderScore(vup)));
        achievements.add(createAchievement("RISK_CONTROL_DESK", "旧账管理台", "使用风控工具或处理旧账3次", "账", 3,
                riskControlCount(logs)));
        achievements.add(createAchievement("ENDING_SCORECARD_A", "结局A档复盘", "结局scorecard总评达到80，给下一轮明确优化目标", "评", 80,
                bestEndingOverallScore(userId)));
        achievements.add(createAchievement("STAGE_OBJECTIVE_STREAK", "任务板三连", "连续3天命中阶段委托，形成短线运营节奏", "委", 3,
                maxStageObjectiveStreak(logs)));

        // 梗重复成就
        Map<String, Long> memeCounts = logs.stream()
                .filter(l -> l.getMemeSubtype() != null)
                .collect(Collectors.groupingBy(BusinessLog::getMemeSubtype, Collectors.counting()));
        int maxMemeRepeat = memeCounts.values().stream().mapToInt(Long::intValue).max().orElse(0);
        achievements.add(createAchievement("MEME_REPEAT", "复读机", "同一个梗复读3次", "🔄", 3, maxMemeRepeat));

        // 商业化成就
        achievements.add(createAchievement("BUDGET_1000", "预算稳住", "运营预算达到1000", "账", 1000, vup.getCoin()));
        achievements.add(createAchievement("COMMERCIAL_HIGH", "商业化达人", "商业化等级达到50", "📊", 50, vup.getCommercialLevel()));
        achievements.add(createAchievement("REPUTATION_HIGH", "口碑担当", "口碑达到80", "🏆", 80, vup.getReputation()));

        // 特殊成就（带 rewardType 的会在重开时作为开局 buff 继承到新存档）
        achievements.add(createAchievement("DAY_30_SURVIVOR", "30天出道", "完成30天挑战", "🎓", 30, vup.getDayCount(),
                "INSPIRATION", 5));
        achievements.add(createAchievement("ENDING_ATLAS_START", "结局图鉴开张", "完成任意1个当前结局", "册", 1,
                (int) endingCollection.unlockedCount(), "FANS", 10));
        achievements.add(createAchievement("ENDING_ATLAS_HALF", "结局收藏家", "解锁9个当前结局中的5个", "集", 5,
                (int) endingCollection.unlockedCount(), "FANS", 25));
        achievements.add(createAchievement("ENDING_ATLAS_FULL", "九结局全收录", "解锁全部9个当前结局", "全", 9,
                (int) endingCollection.unlockedCount(), "INSPIRATION", 10));
        achievements.add(createAchievement("NPC_SOCIAL", "社交达人", "同台互动5次", "🤝", 5,
                (int) logs.stream().filter(l -> "NPC_INTERACT".equals(l.getAction())).count()));
        achievements.add(createAchievement("REST_MASTER", "休息大师", "休息10次", "😴", 10,
                (int) logs.stream().filter(l -> "REST".equals(l.getAction())).count()));

        // 连续直播成就
        List<BusinessLog> streamLogs = logs.stream()
                .filter(l -> "STREAM_PLAN".equals(l.getAction()))
                .sorted(Comparator.comparing(BusinessLog::getDay))
                .toList();
        int maxConsecutive = calculateMaxConsecutive(streamLogs);
        achievements.add(createAchievement("STREAM_MARATHON", "直播马拉松", "连续3天直播", "🏃", 3, maxConsecutive));

        long unlockedCount = achievements.stream().filter(AchievementDTO::unlocked).count();
        int totalPoints = (int) unlockedCount * 10;
        List<AchievementTitleDTO> titleTrack = titleTrack(achievements);
        List<AchievementTitleDTO> earnedTitles = titleTrack.stream()
                .filter(AchievementTitleDTO::unlocked)
                .toList();
        AchievementTitleDTO featuredTitle = earnedTitles.isEmpty()
                ? starterTitle()
                : earnedTitles.get(earnedTitles.size() - 1);
        AchievementTitleDTO nextTitle = titleTrack.stream()
                .filter(title -> !title.unlocked())
                .findFirst()
                .orElse(null);

        return new AchievementProgressDTO(
                achievements,
                unlockedCount,
                achievements.size(),
                totalPoints,
                getAchievementTitle(unlockedCount),
                featuredTitle,
                earnedTitles,
                nextTitle,
                endingCollection
        );
    }

    private AchievementDTO createAchievement(String id, String name, String description, String icon, int target, int progress) {
        return createAchievement(id, name, description, icon, target, progress, null, 0);
    }

    private AchievementDTO createAchievement(String id, String name, String description, String icon, int target, int progress,
                                             String rewardType, int rewardValue) {
        boolean unlocked = progress >= target;
        return new AchievementDTO(
                id, name, description, icon, target,
                Math.min(progress, target),
                unlocked,
                unlocked ? "已解锁" : progress + "/" + target,
                rewardType,
                rewardValue
        );
    }

    /**
     * 成就解锁：按 rewardType 分发奖励到 vup。
     * INSPIRATION → vup.inspiration += rewardValue
     * COIN → vup.coin += rewardValue
     * FANS → vup.fans += rewardValue
     */
    public void unlock(Vup vup, AchievementDTO achievement) {
        if (!achievement.unlocked()) {
            return;
        }
        String rewardType = achievement.rewardType();
        if (rewardType == null || rewardType.isBlank()) {
            return;
        }
        int value = achievement.rewardValue();
        if (value <= 0) {
            return;
        }
        switch (rewardType) {
            case "INSPIRATION" -> vup.setInspiration(vup.getInspiration() + value);
            case "COIN" -> vup.setCoin(vup.getCoin() + value);
            case "FANS" -> vup.setFans(vup.getFans() + value);
            default -> { /* 未知奖励类型不做处理 */ }
        }
    }

    private List<AchievementTitleDTO> titleTrack(List<AchievementDTO> achievements) {
        Map<String, AchievementDTO> byId = achievements.stream()
                .collect(Collectors.toMap(AchievementDTO::id, achievement -> achievement));
        return List.of(
                titleFromAchievement(byId, "DAY_30_SURVIVOR", "DEBUT_SURVIVOR", "30天出道人", "基础",
                        "完成一次30天出道局，证明账号已经跑完整轮节奏。"),
                titleFromAchievement(byId, "STAGE_OBJECTIVE_STREAK", "TASK_BOARD_RUNNER", "任务板三连者", "节奏",
                        "连续命中阶段委托，擅长按短线目标推进。"),
                titleFromAchievement(byId, "COMBO_IMPACT_ARCHITECT", "COMBO_RESEARCHER", "连锁反应研究员", "打法",
                        "触发多种组合技，账号开始有可复现打法库。"),
                titleFromAchievement(byId, "RISK_CONTROL_DESK", "RISK_ACCOUNTANT", "旧账管理台", "风控",
                        "能处理旧账和米线工具，不只会硬冲热度。"),
                titleFromAchievement(byId, "ROUTE_FOCUS_PROOF", "ROUTE_ARCHITECT", "路线主轴建筑师", "路线",
                        "把路线分堆到足够厚，擅长把一轮打成清晰主轴。"),
                titleFromAchievement(byId, "ENDING_SCORECARD_A", "A_REVIEWER", "A档复盘人", "评分",
                        "结局评分达到A档附近，能把复盘转化成下一轮目标。"),
                titleFromAchievement(byId, "ENDING_ATLAS_HALF", "ATLAS_COLLECTOR", "结局收藏家", "图鉴",
                        "解锁多种派生结局，账号有稳定复玩广度。"),
                titleFromAchievement(byId, "ENDING_ATLAS_FULL", "NINE_ENDING_ARCHIVIST", "九结局全收录者", "传说",
                        "收齐全部当前结局图鉴，进入冲高分和净收官阶段。")
        );
    }

    private AchievementTitleDTO titleFromAchievement(
            Map<String, AchievementDTO> achievements,
            String achievementId,
            String titleKey,
            String label,
            String tier,
            String description
    ) {
        AchievementDTO achievement = achievements.get(achievementId);
        if (achievement == null) {
            return new AchievementTitleDTO(titleKey, label, tier, description, false, "0/1");
        }
        return new AchievementTitleDTO(
                titleKey,
                label,
                tier,
                description,
                achievement.unlocked(),
                achievement.progressText()
        );
    }

    private AchievementTitleDTO starterTitle() {
        return new AchievementTitleDTO(
                "ROOKIE_OPERATOR",
                "出道观察员",
                "起步",
                "账号还在积累第一批长期身份，先完成30天或解锁一个核心打法。",
                true,
                "默认"
        );
    }

    private Set<String> comboImpactKeys(List<BusinessLog> logs) {
        Set<String> keys = new LinkedHashSet<>();
        for (BusinessLog log : logs) {
            Map<String, Object> detail = safeReadMap(log.getMultiplierDetail());
            Object comboRaw = detail.get("comboImpact");
            if (comboRaw instanceof Map<?, ?> combo) {
                Object key = combo.get("comboKey");
                if (key instanceof String comboKey && !comboKey.isBlank()) {
                    keys.add(comboKey);
                    continue;
                }
            }
            String summaryKey = comboImpactKeyFromSummary(log.getResult());
            if (!summaryKey.isBlank()) {
                keys.add(summaryKey);
            }
        }
        return keys;
    }

    private String comboImpactKeyFromSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        if (summary.contains("HARD_PRACTICE") || summary.contains("刻苦练习") || summary.contains("一周内多次练功")) {
            return "HARD_PRACTICE";
        }
        if (summary.contains("PRACTICE_TO_STREAM") || summary.contains("练习后直播")) {
            return "PRACTICE_TO_STREAM";
        }
        if (summary.contains("VIDEO_TO_STREAM") || summary.contains("视频引流直播")
                || (summary.contains("投稿后") && summary.contains("直播接住回流"))) {
            return "VIDEO_TO_STREAM";
        }
        if (summary.contains("VIDEO_TO_CLIP") || summary.contains("一鱼两剪") || summary.contains("长视频素材被二剪")) {
            return "VIDEO_TO_CLIP";
        }
        if (summary.contains("CHARGE_COMPLETE") || summary.contains("充电完毕") || summary.contains("连续低压收工后再营业")) {
            return "CHARGE_COMPLETE";
        }
        if (summary.contains("FORGOTTEN") || summary.contains("被遗忘") || summary.contains("连续低压太久")) {
            return "FORGOTTEN";
        }
        if (summary.contains("OVERWORK") || summary.contains("过劳边缘") || summary.contains("最近直播太密")) {
            return "OVERWORK";
        }
        if (summary.contains("SOCIAL_WARMUP") || summary.contains("查房预热") || summary.contains("刚在同行场子露过脸")) {
            return "SOCIAL_WARMUP";
        }
        return "";
    }

    private int routeLeaderScore(Vup vup) {
        return safeReadMap(vup.getRouteScoreJson()).entrySet().stream()
                .filter(entry -> !"UNKNOWN".equals(entry.getKey()))
                .mapToInt(entry -> intValue(entry.getValue()))
                .max()
                .orElse(0);
    }

    private int riskControlCount(List<BusinessLog> logs) {
        return (int) logs.stream()
                .filter(log -> nonBlank(log.getRiskToolId()) || hasDebtRefs(log.getDebtIds()))
                .count();
    }

    private int bestEndingOverallScore(Long userId) {
        return endingAtlasService.bestOverallScore(userId);
    }

    private int maxStageObjectiveStreak(List<BusinessLog> logs) {
        int max = 0;
        int current = 0;
        int previousDay = -1;
        for (BusinessLog log : logs.stream()
                .sorted(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId))
                .toList()) {
            if (!hasStageObjectiveBonus(log)) {
                continue;
            }
            if (log.getDay() == previousDay) {
                continue;
            }
            current = previousDay >= 0 && log.getDay() == previousDay + 1 ? current + 1 : 1;
            previousDay = log.getDay();
            max = Math.max(max, current);
        }
        return max;
    }

    private boolean hasStageObjectiveBonus(BusinessLog log) {
        return safeReadMap(log.getMultiplierDetail()).containsKey("stageObjectiveBonus");
    }

    private boolean hasDebtRefs(String debtIds) {
        if (!nonBlank(debtIds)) {
            return false;
        }
        String normalized = debtIds.trim();
        return !"[]".equals(normalized) && !"{}".equals(normalized) && !"null".equalsIgnoreCase(normalized);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> safeReadMap(String json) {
        if (!nonBlank(json)) {
            return Map.of();
        }
        try {
            return jsonService.readMap(json);
        } catch (IllegalStateException ignored) {
            return Map.of();
        }
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int calculateMaxConsecutive(List<BusinessLog> streamLogs) {
        if (streamLogs.isEmpty()) return 0;
        int maxConsecutive = 1;
        int currentConsecutive = 1;
        for (int i = 1; i < streamLogs.size(); i++) {
            if (streamLogs.get(i).getDay() - streamLogs.get(i - 1).getDay() == 1) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 1;
            }
        }
        return maxConsecutive;
    }

    private String getAchievementTitle(long unlockedCount) {
        if (unlockedCount >= 20) return "成就大师";
        if (unlockedCount >= 15) return "成就收集者";
        if (unlockedCount >= 10) return "成就爱好者";
        if (unlockedCount >= 5) return "成就新手";
        return "成就入门";
    }

}
