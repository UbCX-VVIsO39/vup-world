package com.example.vupworld.dto;

import java.util.Map;

/**
 * 统一结算输出。所有行动、标题、事件、米线工具的结算都通过此类输出。
 * Gate 4 要求：RewardDelta 包含粉丝结构、资源、舆论、路线分、债务、证据、
 * multiplierDetail、capDetail、clampDetail、rngDetail、weightDetail。
 */
public record RewardDelta(
        // 粉丝结构
        int trueFanChange,
        int funFanChange,
        int unicornFanChange,
        int ddFanChange,
        int fanChange,

        // 资源
        int popularityChange,
        int watchHeatChange,
        int reputationChange,
        int memeChange,
        int commercialChange,
        int coinChange,
        int inspirationChange,
        int staminaChange,

        // 路线
        int routeScoreChange,
        String routeType,

        // 证据
        Map<String, Object> evidenceRef,

        // 详情 JSON
        String multiplierDetail,
        String capDetail,
        String clampDetail,
        String rngDetail,
        String weightDetail
) {
    /**
     * 创建空的 RewardDelta（无变化）。
     */
    public static RewardDelta empty() {
        return new RewardDelta(0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, null,
                Map.of(),
                "{}", "{}", "{}", "{}", "{}");
    }

    /**
     * 应用阶段粉丝上限。
     */
    public RewardDelta applyFanCap(int stageCap) {
        int capped = Math.min(fanChange, Math.max(0, stageCap));
        if (capped == fanChange) return this;
        int delta = capped - fanChange;
        return new RewardDelta(
                trueFanChange, funFanChange, unicornFanChange, ddFanChange, capped,
                popularityChange, watchHeatChange, reputationChange, memeChange,
                commercialChange, coinChange, inspirationChange, staminaChange,
                routeScoreChange, routeType,
                evidenceRef,
                multiplierDetail,
                "{\"stageCap\":" + stageCap + ",\"hit\":" + (fanChange > stageCap) + "}",
                clampDetail, rngDetail, weightDetail
        );
    }

    /**
     * 应用新秀保护（降低高风险收益）。
     */
    public RewardDelta applyNewPlayerProtection(int percent) {
        int p = percent;
        return new RewardDelta(
                trueFanChange * p / 100, funFanChange * p / 100,
                unicornFanChange, ddFanChange * p / 100,
                0, // fanChange will be recomputed
                popularityChange * p / 100, watchHeatChange, reputationChange,
                memeChange, commercialChange, coinChange, inspirationChange, staminaChange,
                routeScoreChange, routeType,
                evidenceRef,
                multiplierDetail, capDetail,
                "{\"reputation\":" + reputationChange + "}",
                rngDetail, weightDetail
        ).recomputeFanChange();
    }

    private RewardDelta recomputeFanChange() {
        int total = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        return new RewardDelta(
                trueFanChange, funFanChange, unicornFanChange, ddFanChange, total,
                popularityChange, watchHeatChange, reputationChange, memeChange,
                commercialChange, coinChange, inspirationChange, staminaChange,
                routeScoreChange, routeType,
                evidenceRef,
                multiplierDetail, capDetail, clampDetail, rngDetail, weightDetail
        );
    }

    /**
     * 合并两个 RewardDelta。
     */
    public RewardDelta merge(RewardDelta other) {
        return new RewardDelta(
                trueFanChange + other.trueFanChange,
                funFanChange + other.funFanChange,
                unicornFanChange + other.unicornFanChange,
                ddFanChange + other.ddFanChange,
                fanChange + other.fanChange,
                popularityChange + other.popularityChange,
                watchHeatChange + other.watchHeatChange,
                reputationChange + other.reputationChange,
                memeChange + other.memeChange,
                commercialChange + other.commercialChange,
                coinChange + other.coinChange,
                inspirationChange + other.inspirationChange,
                staminaChange + other.staminaChange,
                routeScoreChange + other.routeScoreChange,
                routeType != null ? routeType : other.routeType,
                evidenceRef,
                multiplierDetail, capDetail, clampDetail, rngDetail, weightDetail
        );
    }
}
