package com.example.vupworld.service.infra;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * N19/S19: Unified balance configuration entry point.
 * 所有游戏数值配置都应从这里读取，不要散落硬编码到各个服务里。
 */
@Component
public class BalanceConfig {

    /** 运行难度：影响 AP 上限、旧账延迟、对手强度等。 */
    public enum Difficulty {
        EASY,
        STANDARD,
        HARD
    }

    private final int maxDay;
    private final boolean offStreamRequired;
    private final Difficulty difficulty;

    public BalanceConfig(
            @Value("${game.run.max-day:30}") int maxDay,
            @Value("${game.run.off-stream-required:true}") boolean offStreamRequired,
            @Value("${game.run.difficulty:STANDARD}") String difficulty
    ) {
        if (maxDay < 1) {
            throw new IllegalArgumentException("game.run.max-day must be positive");
        }
        this.maxDay = maxDay;
        this.offStreamRequired = offStreamRequired;
        this.difficulty = parseDifficulty(difficulty);
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public static Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return Difficulty.STANDARD;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Difficulty.STANDARD;
        }
    }

    /** 旧账延迟的难度修正：EASY +2 天缓冲，HARD -2 天紧迫。 */
    private int debtDelayBonus(Difficulty d) {
        return switch (d) {
            case EASY -> 2;
            case STANDARD -> 0;
            case HARD -> -2;
        };
    }

    // === 粉丝变化 ===
    public int safeTitleTrueFanChange() { return 28; }
    public int safeTitleSingingBoostTrueFanChange() { return 36; }
    public int fanServiceTrueFanChange() { return 8; }
    public int defaultTitleTrueFanChange() { return 10; }
    public int blackRedFunFanChange() { return 20; }
    public int danceMemeFunFanChange() { return 18; }
    public int fanServiceUnicornFanChange() { return 30; }
    public int safeBusinessUnicornFanChange() { return 4; }
    public int fanServiceDdFanChange() { return 1; }
    public int defaultDdFanChange() { return 4; }

    // === 非直播行动粉丝变化 ===
    public int trainSongTrueFanChange() { return 10; }
    public int trainSongSingingBoostTrueFanChange() { return 14; }
    public int trainSongDdFanChange() { return 3; }
    public int trainDanceTrueFanChange() { return 6; }
    public int trainDanceFunFanChange() { return 5; }
    public int trainDanceDdFanChange() { return 2; }
    public int npcInteractTrueFanChange() { return 2; }
    public int npcInteractDdFanChange() { return 12; }
    public int publishClipFunFanChange() { return 20; }
    public int publishClipMemeOutbreakFunFanChange() { return 26; }
    public int publishClipDdFanChange() { return 3; }
    public int publishClipMemeOutbreakDdFanChange() { return 4; }
    public int publishVideoTrueFanChange() { return 8; }
    public int publishVideoSingingBoostTrueFanChange() { return 10; }
    public int publishVideoFunFanChange() { return 9; }
    public int publishVideoDdFanChange() { return 3; }
    public int publishVideoSingingBoostDdFanChange() { return 6; }
    public int trainTalkTrueFanChange() { return 4; }
    public int trainTalkSafeWeekTrueFanChange() { return 6; }
    public int fanGroupMaintainTrueFanChange() { return 4; }
    public int restTrueFanChange() { return 2; }

    // === 人气变化 ===
    public int safeTitlePopularity() { return 80; }
    public int safeTitleSingingBoostPopularity() { return 95; }
    public int fanServicePopularity() { return 100; }
    public int blackRedPopularity() { return 140; }
    public int trainSongPopularity() { return 15; }
    public int trainSongSingingBoostPopularity() { return 20; }
    public int trainDancePopularity() { return 18; }
    public int npcInteractPopularity() { return 18; }
    public int publishClipPopularity() { return 28; }
    public int publishClipMemeOutbreakPopularity() { return 36; }
    public int publishVideoPopularity() { return 35; }
    public int publishVideoSingingBoostPopularity() { return 42; }

    // === 口碑变化 ===
    public int safeTitleReputation() { return 3; }
    public int safeTitleSingingBoostReputation() { return 4; }
    public int blackRedReputation() { return -3; }
    public int fanServiceReputation() { return 1; }
    public int trainSongReputation() { return 1; }
    public int trainSongSingingBoostReputation() { return 2; }
    public int trainTalkSafeWeekReputation() { return 3; }
    public int trainTalkReputation() { return 2; }
    public int publishClipReputation() { return -1; }
    public int publishClipMemeOutbreakReputation() { return -2; }
    public int npcInteractReputation() { return 1; }
    public int fanGroupMaintainReputation() { return 2; }
    public int fanGroupMaintainCommercialReputation() { return 3; }

    // === 围观变化 ===
    public int safeTitleWatchHeat() { return 5; }
    public int fanServiceWatchHeat() { return 4; }
    public int blackRedWatchHeat() { return 20; }
    public int trainDanceWatchHeat() { return 2; }
    public int publishClipWatchHeat() { return 3; }
    public int publishClipMemeOutbreakWatchHeat() { return 6; }
    public int publishVideoWatchHeat() { return 4; }
    public int defensiveWatchHeat() { return -1; }

    // === 梗力变化 ===
    public int blackRedMemeLevel() { return 4; }
    public int danceMemeMemeLevel() { return 5; }
    public int trainDanceMemeLevel() { return 2; }
    public int publishClipMemeLevel() { return 3; }
    public int publishClipMemeOutbreakMemeLevel() { return 5; }
    public int publishVideoMemeLevel() { return 1; }

    // === 路线分变化 ===
    public int standardRouteScoreChange() { return 2; }
    public int highRouteScoreChange() { return 3; }
    public int streamRouteScoreChange() { return 3; }
    public int danceStreamRouteScoreChange() { return 4; }

    // === 体力消耗 ===
    // [数值调整] AP制重构：体力变长期状态，每日自然回复改为1，REST回复改为3
    public int dailyStaminaRecovery() { return 1; }
    public int streamPlanStaminaCost() { return 3; }
    public int publishVideoStaminaCost() { return 4; }
    public int publishClipStaminaCost() { return 1; }
    public int trainActionStaminaCost() { return 2; }
    public int restStaminaRecovery() { return 3; }
    /** 运营压力锁定首次触发时扣除的体力惩罚量。 */
    public int pressureLockoutStaminaPenalty() { return 1; }

    // === 硬币变化 ===
    public int fanServiceScThanksCoin() { return 600; }
    public int fanServiceScThanksCommercialReviewCoin() { return 800; }
    public int businessSafeCoin() { return 500; }
    public int businessSafeCommercialReviewCoin() { return 700; }
    public int fanGroupMaintainCommercialCoin() { return 200; }
    public int endgameVideoProductionCoinCost() { return 300; }
    public int endgameClipBoostCoinCost() { return 150; }
    public int endgameCollabBookingCoinCost() { return 250; }

    // === 商业化变化 ===
    public int fanServiceCommercial() { return 4; }
    public int fanServiceCommercialReviewCommercial() { return 6; }
    public int businessSafeCommercial() { return 3; }
    public int businessSafeCommercialReviewCommercial() { return 4; }
    public int fanGroupCommercial() { return 2; }

    // === 新人保护 ===
    public int newPlayerProtectionDays() { return 3; }
    public int newPlayerProtectionPercent() { return 60; }

    // === 梗疲劳 ===
    public int memeRepeatPercent() { return 70; }
    public int memeBoomerangDays() { return 7; }

    // === 阶段上限 ===
    public int newbieStageFanCap() { return 200; }
    public int startingStageFanCap() { return 600; }
    public int growthStageFanCap() { return 1200; }
    public int breakoutStageFanCap() { return 2000; }
    public int topStageFanCap() { return 3000; }

    // === 债务时机（含难度分支：EASY +2 / HARD -2）===
    public int titleBackfireDebtDelay() { return Math.max(1, 3 + debtDelayBonus(difficulty)); }
    public int unicornExpectationDebtDelay() { return Math.max(1, 3 + debtDelayBonus(difficulty)); }
    public int commercialBacklashDebtDelay() { return Math.max(1, 3 + debtDelayBonus(difficulty)); }
    public int boomerangClipDebtDelay() { return Math.max(1, 2 + debtDelayBonus(difficulty)); }
    public int debtRepeatSeverityStep() { return 1; }
    // [数值调整] 1→2: 债务雪崩风险过高，延长回流窗口给玩家更多缓冲时间
    public int trafficRolloverDebtDelay() { return Math.max(1, 2 + debtDelayBonus(difficulty)); }
    public int memeRolloverDebtDelay() { return Math.max(1, 2 + debtDelayBonus(difficulty)); }
    public int trafficRolloverSeverityBump() { return 1; }
    public int memeRolloverSeverityBump() { return 0; }

    // === 路线切换检测 ===
    public int routeSwitchWarningThreshold() { return 2; }
    public int routeSwitchLookbackDays() { return 4; }

    // === 行动疲劳 ===
    public int actionFatigueConsecutiveThreshold() { return 4; }
    // [数值调整] 疲劳曲线软化：5天75→85, 7天55→70, 10天35→50
    public int actionFatiguePercentAfter5() { return 85; }
    public int actionFatiguePercentAfter7() { return 70; }
    public int actionFatiguePercentAfter10Plus() { return 50; }

    // === Ending rule gates ===
    public int graduationReputationRequired() { return 65; }
    public int graduationEvidenceActionsRequired() { return 4; }
    // [数值调整] 35→50: 放宽上限使毕业路线更灵活
    public int graduationWatchHeatMax() { return 50; }
    public int blackRedEvidenceRequired() { return 4; }
    public int blackRedWatchHeatRequired() { return 65; }
    public int blackRedReputationMax() { return 70; }
    public int blackRedCrisisWatchHeatRequired() { return 70; }
    public int blackRedCrisisReputationMax() { return 35; }
    public int cyberFanServiceEvidenceRequired() { return 4; }
    public int cyberCommercialRequired() { return 50; }
    public int cyberUnicornRatioRequired() { return 30; }
    public int cyberUnicornFansRequired() { return 150; }
    public int ddBusStopDdRatioRequired() { return 33; }

    // === 内容焦虑 ===
    public int contentAnxietyConsecutiveDays() { return 2; }

    // === 粉丝衰减 ===
    public int fanDecayDivisor() { return 5000; }

    // === 数值范围 ===
    public int attributeMin() { return 0; }
    // [数值调整] 100→50: 属性天花板过高，降低上限使成长曲线更紧凑
    public int attributeMax() { return 50; }
    public int inspirationMax() { return 10; }
    public int debtSeverityMax() { return 5; }

    // === 标题重抽 ===
    public int titleRerollLimit() { return 2; }
    public int titleRerollInspirationCost() { return 1; }

    // === 风险工具消耗 ===
    public int fullClipContextInspirationCost() { return 1; }
    public int coolingNoticeCoinCost() { return 500; }
    // [数值调整] 800→500: 降价提升风险工具使用率
    public int tempModTeamCoinCost() { return 500; }

    // === 最大天数 ===
    public int maxDay() { return maxDay; }
    public boolean offStreamRequired() { return offStreamRequired; }

    // === 初始数值 ===
    public int startingStamina() { return 10; }
    public int startingCoin() { return 2000; }
    public int startingInspiration() { return 3; }
    public int startingTrueFans() { return 32; }
    public int startingFunFans() { return 8; }
    public int startingUnicornFans() { return 5; }
    public int startingDdFans() { return 7; }
    public int startingReputation() { return 60; }

    // === 礼物热度加成 ===
    public int giftHeatBonusCap() { return 20; }
    public int giftHeatBonusPerCoin() { return 500; }

    // === 弹幕热度加成 ===
    public int danmakuHeatBonusCap() { return 15; }
    public int danmakuNegativeReputationPenalty() { return 2; }

    // === 每日行动点（含难度分支：EASY=12 / STANDARD=10 / HARD=8）===
    public int dailyActionPoints() { return dailyActionPoints(difficulty); }

    public int dailyActionPoints(Difficulty d) {
        return switch (d) {
            case EASY -> 12;
            case STANDARD -> 10;
            case HARD -> 8;
        };
    }

    /** 体力透支档（1-2）时每日AP上限的缩减量。 */
    public int exhaustedApPenalty() { return 2; }

    // === 对手威胁阈值：对手粉丝超过玩家×该系数视为被超越 ===
    public int rivalOvertakeThresholdPercent() { return 120; }
}
