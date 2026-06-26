package com.example.vupworld.service.operating;

import com.example.vupworld.domain.ActionType;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.PlatformTrendService;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperatingPressureService {
    private static final String FLAG_KEY = "operatingPressure";

    private static final Map<String, PressureGroup> ACTION_GROUPS = Map.of(
            "CONTENT_DRY", new PressureGroup(
                    "CONTENT_DRY",
                    "内容枯竭",
                    List.of(ActionType.PUBLISH_VIDEO.name(), ActionType.PUBLISH_CLIP.name(), ActionType.STREAM_PLAN.name()),
                    "内容库存开始见底，先补素材再硬剪会更稳。",
                    "连续创作掏空了灵感，需要补给素材"
            ),
            "HEAT_OVERDRIVE", new PressureGroup(
                    "HEAT_OVERDRIVE",
                    "硬冲热度",
                    List.of(ActionType.STREAM_PLAN.name(), ActionType.PUBLISH_CLIP.name()),
                    "热度冲得太猛，先降温复盘比继续硬顶更值。",
                    "热度推得太猛，观众审美疲劳了"
            ),
            "RELATION_OVERDRAWN", new PressureGroup(
                    "RELATION_OVERDRAWN",
                    "关系透支",
                    List.of(ActionType.NPC_INTERACT.name(), ActionType.FAN_GROUP_MAINTAIN.name()),
                    "关系线已经透支，先整理边界再续营业。",
                    "社交关系透支了，需要保持距离"
            ),
            "OLD_LEDGER_BURN", new PressureGroup(
                    "OLD_LEDGER_BURN",
                    "旧账发酵",
                    List.of(ActionType.PUBLISH_CLIP.name(), ActionType.STREAM_PLAN.name(), ActionType.NPC_INTERACT.name()),
                    "旧账正在发热，先拆证据链再继续推进。",
                    "旧账发酵中，需要处理风险"
            )
    );

    private final JsonService jsonService;
    private final PlatformTrendService platformTrendService;

    public OperatingPressureService(JsonService jsonService, PlatformTrendService platformTrendService) {
        this.jsonService = jsonService;
        this.platformTrendService = platformTrendService;
    }

    public PressureState pressureState(Vup vup, int day) {
        PressureData data = load(vup);
        int score = clamp(data.score);
        // Phase 1: 4 status tiers — 松弛(0-2), 绷紧(3-4), 过载(5-7), 失控边缘(8+)
        String state = score >= 8 ? "失控边缘" : score >= 5 ? "过载" : score >= 3 ? "绷紧" : "松弛";
        PressureGroup group = data.lockedGroupKey == null ? null : ACTION_GROUPS.get(data.lockedGroupKey);
        if (group == null && score >= 5) {
            group = ACTION_GROUPS.get(data.sourceKey);
        }
        boolean wounded = data.woundedUntilDay >= day && data.woundedUntilDay > 0;
        int cooldownLeft = Math.max(0, data.lockedUntilDay - day + 1);
        int woundLeft = Math.max(0, data.woundedUntilDay - day + 1);
        List<PressureAlternative> alternatives = alternativesFor(group, day);
        PressureReplacement replacement = alternatives.isEmpty() ? PressureReplacement.none() : toReplacement(alternatives.get(0));
        return new PressureState(
                score,
                state,
                data.sourceKey,
                sourceLabel(data.sourceKey),
                group == null ? null : group.key,
                group == null ? null : group.label,
                group == null ? null : group.lockedActions,
                group == null ? null : group.hint,
                cooldownLeft,
                woundLeft,
                wounded,
                replacement,
                alternatives,
                data.trendAmplified,
                data.lastTrendId,
                group == null ? null : group.monologue,
                stateMonologueFor(state)
        );
    }

    public boolean isLocked(Vup vup, int day, ActionType actionType) {
        PressureState pressure = pressureState(vup, day);
        if (pressure.lockedGroupKey() == null || pressure.cooldownLeft() <= 0) {
            return false;
        }
        PressureGroup group = ACTION_GROUPS.get(pressure.lockedGroupKey());
        return group != null && group.lockedActions.contains(actionType.name());
    }

    public String disabledReason(Vup vup, int day, ActionType actionType) {
        if (!isLocked(vup, day, actionType)) {
            return null;
        }
        return "OPERATIONAL_PRESSURE_LOCKED";
    }

    public PressureReplacement replacementForAction(Vup vup, int day, ActionType actionType) {
        PressureState pressure = pressureState(vup, day);
        if (!isLocked(vup, day, actionType)) {
            return PressureReplacement.none();
        }
        return pressure.replacement();
    }

    /**
     * Record pressure from an action. Phase 1 adds platform trend amplification:
     * if the current trend matches the action category, +1 extra pressure.
     */
    public void recordActionPressure(Vup vup, int day, ActionType actionType) {
        PressureData data = load(vup);
        PressureAlternative recoveryAlternative = activeRecoveryAlternativeFor(data, day, actionType);
        // Phase 1: Platform trend amplification
        String trendId = platformTrendService != null ? platformTrendService.trendIdForDay(day) : null;
        int trendBonus = trendAmplification(trendId, actionType);
        int nextScore = clamp(data.score + pressureGainFor(actionType) + trendBonus);
        if (actionType == ActionType.REST) {
            nextScore = Math.max(0, nextScore - 2);
        }
        if (recoveryAlternative != null) {
            nextScore = clamp(nextScore + recoveryAlternative.pressureReduction());
        }
        String sourceKey = pressureSourceFor(actionType);
        if (nextScore >= 5) {
            if (recoveryAlternative == null || data.lockedGroupKey == null) {
                data.lockedGroupKey = sourceKey;
            }
            // Phase 2: lock for 6 days
            data.lockedUntilDay = Math.max(data.lockedUntilDay, day + 6);
            // Phase 4: wounded recovery for 1-2 days after lockout (day + 8 = lockout + 2)
            data.woundedUntilDay = Math.max(data.woundedUntilDay, day + 8);
        } else {
            data.lockedGroupKey = null;
            data.woundedUntilDay = 0;
        }
        data.score = nextScore;
        data.sourceKey = recoveryAlternative != null && nextScore >= 5 && data.lockedGroupKey != null
                ? data.lockedGroupKey
                : sourceKey;
        data.trendAmplified = trendBonus > 0;
        data.lastTrendId = trendBonus > 0 ? trendId : null;
        save(vup, data);
    }

    public void advanceDay(Vup vup, int day) {
        PressureData data = load(vup);
        // Phase 1: Natural decay (-1 per day)
        if (data.score > 0) {
            data.score = Math.max(0, data.score - 1);
        }
        if (data.lockedUntilDay > 0 && day > data.lockedUntilDay) {
            data.lockedGroupKey = null;
        }
        if (data.woundedUntilDay > 0 && day > data.woundedUntilDay) {
            data.woundedUntilDay = 0;
        }
        // Reset daily trend flag
        data.trendAmplified = false;
        data.lastTrendId = null;
        save(vup, data);
    }

    // ==================== Phase 5-6: Historical pressure queries ====================

    /**
     * Whether the player was ever locked out during this run.
     * lockedUntilDay is never reset to 0, so >0 means a lockout was triggered at some point.
     */
    public boolean wasEverLocked(Vup vup) {
        PressureData data = load(vup);
        return data.lockedUntilDay > 0;
    }

    /**
     * Get the pressure source key from the last recorded pressure action.
     * Returns null if no pressure has been recorded.
     */
    public String lastSourceKey(Vup vup) {
        PressureData data = load(vup);
        return data.sourceKey;
    }

    /**
     * Build a natural-language pressure narrative for the daily report.
     * Explains "昨天的行动 -> 今天的压力结果" in player-facing language.
     * Returns null if pressure is relaxed (score < 3).
     */
    public String pressureNarrative(Vup vup, int day) {
        PressureState state = pressureState(vup, day);
        if (state.score() < 3) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("运营脑压力：昨天的操作让状态进入「").append(state.stateLabel()).append("」");
        if (state.sourceLabel() != null) {
            sb.append("，来源是").append(state.sourceLabel()).append("方向的过度操作");
        }
        if (state.lockedGroupLabel() != null && state.cooldownLeft() > 0) {
            sb.append("。").append(state.lockedGroupLabel())
                    .append("方向的动作已被锁定（剩余").append(state.cooldownLeft()).append("天冷却）");
            if (state.hint() != null) {
                sb.append("——").append(state.hint());
            }
        }
        if (state.wounded()) {
            sb.append("。当前处于带伤恢复阶段，收益降低、风险上升");
        }
        sb.append("。");
        return sb.toString();
    }

    /**
     * Build a pressure mood effect line for MoodService integration.
     * Returns null if pressure is not overloaded (score < 5).
     */
    public String pressureMoodEffect(Vup vup, int day) {
        PressureState state = pressureState(vup, day);
        if (state.score() >= 8) {
            return "运营脑接近失控边缘，行动严重受限，失败率大幅上升";
        }
        if (state.score() >= 5) {
            return "运营脑过载，关键动作被锁，行动失败率上升";
        }
        return null;
    }

    // ==================== Phase 4: Cooldown and Shadow ====================

    /**
     * Whether the vup is currently in the "wounded recovery" state.
     * Active during the lockout period AND the 1-2 day recovery window after lockout ends.
     */
    public boolean isWounded(Vup vup, int day) {
        PressureData data = load(vup);
        return data.woundedUntilDay >= day && data.woundedUntilDay > 0;
    }

    /**
     * Days remaining in the wounded recovery state (0 if not wounded).
     */
    public int woundDaysLeft(Vup vup, int day) {
        PressureData data = load(vup);
        return Math.max(0, data.woundedUntilDay - day + 1);
    }

    /**
     * Gain multiplier during wounded state. Returns 0.7 (30% reduced gains) when wounded.
     */
    public double woundGainMultiplier(Vup vup, int day) {
        return isWounded(vup, day) ? 0.7 : 1.0;
    }

    /**
     * Risk multiplier during wounded state. Returns 1.3 (30% increased risk) when wounded.
     */
    public double woundRiskMultiplier(Vup vup, int day) {
        return isWounded(vup, day) ? 1.3 : 1.0;
    }

    /**
     * Clear wound state early through recovery actions.
     * Returns true if wound was cleared, false if not in wound state.
     */
    public boolean tryClearWound(Vup vup, int day) {
        PressureData data = load(vup);
        if (data.woundedUntilDay > 0 && day <= data.woundedUntilDay) {
            data.woundedUntilDay = 0;
            save(vup, data);
            return true;
        }
        return false;
    }

    // ==================== Phase 1: Trend amplification ====================

    /**
     * Returns +1 if the current platform trend amplifies the action's pressure category,
     * 0 otherwise. Trends act as secondary amplifiers, not primary pressure sources.
     */
    private int trendAmplification(String trendId, ActionType actionType) {
        if (trendId == null || platformTrendService == null) {
            return 0;
        }
        return switch (trendId) {
            case PlatformTrendService.SINGING_BOOST_WEEK ->
                    (actionType == ActionType.TRAIN_SONG || actionType == ActionType.TRAIN_DANCE) ? 1 : 0;
            case PlatformTrendService.MEME_OUTBREAK_WEEK ->
                    (actionType == ActionType.PUBLISH_CLIP || actionType == ActionType.STREAM_PLAN) ? 1 : 0;
            case PlatformTrendService.COMMERCIAL_REVIEW_WEEK ->
                    (actionType == ActionType.NPC_INTERACT || actionType == ActionType.FAN_GROUP_MAINTAIN) ? 1 : 0;
            default -> 0;
        };
    }

    // ==================== Phase 3: Alternative Buttons ====================

    /**
     * When an action group is locked, provide 4 replacement actions:
     * - "冷静复盘" (cooldown review) — costs 2 stamina, -2 pressure
     * - "边界重整" (boundary reorganization) — costs 1 inspiration, -1 pressure
     * - "素材补给" (material replenishment) — costs 1 stamina, -1 pressure
     * - "证据链回顾" (evidence chain review) — free, 0 pressure reduction (low yield)
     *
     * Each group uses different underlying actions to avoid mapping to locked actions.
     */
    private List<PressureAlternative> alternativesFor(PressureGroup group, int day) {
        if (group == null) {
            return List.of();
        }
        return switch (group.key) {
            case "CONTENT_DRY" -> List.of(
                    new PressureAlternative(ActionType.TRAIN_TALK.name(), "冷静复盘",
                            "先把素材库存盘点清楚再动手。", 2, 0, 0, -2,
                            "退一步想想，哪里出了问题"),
                    new PressureAlternative(ActionType.FAN_GROUP_MAINTAIN.name(), "边界重整",
                            "整理一下哪些内容是重复的，哪些还能用。", 0, 1, 0, -1,
                            "重新划清底线，不能再透支了"),
                    new PressureAlternative(ActionType.REST.name(), "素材补给",
                            "停下来补点新素材，别硬剪旧内容。", 1, 0, 0, -1,
                            "找点新灵感，充实素材库"),
                    new PressureAlternative(ActionType.TRAIN_SONG.name(), "证据链回顾",
                            "翻翻以前的素材，看看哪些还有二创价值。", 0, 0, 0, 0,
                            "回顾一下走过的路，理清思路")
            );
            case "HEAT_OVERDRIVE" -> List.of(
                    new PressureAlternative(ActionType.TRAIN_TALK.name(), "冷静复盘",
                            "先降降温，看看这波热度到底值不值。", 2, 0, 0, -2,
                            "退一步想想，哪里出了问题"),
                    new PressureAlternative(ActionType.FAN_GROUP_MAINTAIN.name(), "边界重整",
                            "重新划定热度的底线，别把口碑全押上去。", 0, 1, 0, -1,
                            "重新划清底线，不能再透支了"),
                    new PressureAlternative(ActionType.REST.name(), "素材补给",
                            "换个赛道补点低压素材，给脑子放个假。", 1, 0, 0, -1,
                            "找点新灵感，充实素材库"),
                    new PressureAlternative(ActionType.TRAIN_DANCE.name(), "证据链回顾",
                            "复盘之前的冲热度记录，看看哪些亏了。", 0, 0, 0, 0,
                            "回顾一下走过的路，理清思路")
            );
            case "RELATION_OVERDRAWN" -> List.of(
                    new PressureAlternative(ActionType.TRAIN_TALK.name(), "冷静复盘",
                            "先和核心粉聊清楚关系线到底在哪。", 2, 0, 0, -2,
                            "退一步想想，哪里出了问题"),
                    new PressureAlternative(ActionType.TRAIN_DANCE.name(), "边界重整",
                            "重新整理和谁的关系该推进、该缓一缓。", 0, 1, 0, -1,
                            "重新划清底线，不能再透支了"),
                    new PressureAlternative(ActionType.REST.name(), "素材补给",
                            "找点不需要营业的轻松素材。", 1, 0, 0, -1,
                            "找点新灵感，充实素材库"),
                    new PressureAlternative(ActionType.TRAIN_SONG.name(), "证据链回顾",
                            "翻翻联动和私信的历史记录。", 0, 0, 0, 0,
                            "回顾一下走过的路，理清思路")
            );
            case "OLD_LEDGER_BURN" -> List.of(
                    new PressureAlternative(ActionType.TRAIN_TALK.name(), "冷静复盘",
                            "先把旧账的来龙去脉理一遍。", 2, 0, 0, -2,
                            "退一步想想，哪里出了问题"),
                    new PressureAlternative(ActionType.FAN_GROUP_MAINTAIN.name(), "边界重整",
                            "划清哪些旧账该认、哪些该放。", 0, 1, 0, -1,
                            "重新划清底线，不能再透支了"),
                    new PressureAlternative(ActionType.REST.name(), "素材补给",
                            "准备一些正面素材对冲旧账。", 1, 0, 0, -1,
                            "找点新灵感，充实素材库"),
                    new PressureAlternative(ActionType.TRAIN_DANCE.name(), "证据链回顾",
                            "把旧账相关的证据链整理出来。", 0, 0, 0, 0,
                            "回顾一下走过的路，理清思路")
            );
            default -> List.of();
        };
    }

    private PressureReplacement toReplacement(PressureAlternative alt) {
        return new PressureReplacement(alt.actionType(), alt.label(), alt.hint(),
                alt.staminaCost(), alt.inspirationCost(), alt.reputationCost(), alt.pressureReduction());
    }

    // ==================== Private helpers ====================

    private int pressureGainFor(ActionType actionType) {
        return switch (actionType) {
            case STREAM_PLAN -> 2;
            case PUBLISH_VIDEO, PUBLISH_CLIP -> 2;
            case NPC_INTERACT -> 2;
            case FAN_GROUP_MAINTAIN, TRAIN_TALK -> 1;
            case TRAIN_SONG, TRAIN_DANCE -> 1;
            case REST -> -2;
        };
    }

    private String pressureSourceFor(ActionType actionType) {
        return switch (actionType) {
            case STREAM_PLAN -> "HEAT_OVERDRIVE";
            case PUBLISH_VIDEO, PUBLISH_CLIP -> "CONTENT_DRY";
            case NPC_INTERACT, FAN_GROUP_MAINTAIN -> "RELATION_OVERDRAWN";
            case TRAIN_TALK -> "OLD_LEDGER_BURN";
            case TRAIN_SONG, TRAIN_DANCE -> "CONTENT_DRY";
            case REST -> "CONTENT_DRY";
        };
    }

    private String sourceLabel(String sourceKey) {
        if (sourceKey == null) {
            return null;
        }
        PressureGroup group = ACTION_GROUPS.get(sourceKey);
        return group == null ? null : group.label;
    }

    private String stateMonologueFor(String stateLabel) {
        return switch (stateLabel) {
            case "绷紧" -> "节奏有点紧，但还撑得住";
            case "过载" -> "快到极限了，得注意休息";
            case "失控边缘" -> "再这样下去要出事了";
            default -> null;
        };
    }

    private PressureAlternative activeRecoveryAlternativeFor(PressureData data, int day, ActionType actionType) {
        if (data.lockedGroupKey == null || data.lockedUntilDay <= 0 || day > data.lockedUntilDay) {
            return null;
        }
        PressureGroup group = ACTION_GROUPS.get(data.lockedGroupKey);
        if (group == null) {
            return null;
        }
        return alternativesFor(group, day).stream()
                .filter(alt -> actionType.name().equals(alt.actionType()))
                .findFirst()
                .orElse(null);
    }

    private PressureData load(Vup vup) {
        if (vup.getTutorialFlagsJson() == null || vup.getTutorialFlagsJson().isBlank()) {
            return PressureData.empty();
        }
        try {
            Map<String, Object> flags = jsonService.readMap(vup.getTutorialFlagsJson());
            Object raw = flags.get(FLAG_KEY);
            if (!(raw instanceof Map<?, ?> rawMap)) {
                return PressureData.empty();
            }
            return new PressureData(
                    intValue(rawMap.get("score"), 0),
                    stringValue(rawMap.get("sourceKey")),
                    stringValue(rawMap.get("lockedGroupKey")),
                    intValue(rawMap.get("lockedUntilDay"), 0),
                    intValue(rawMap.get("woundedUntilDay"), 0),
                    boolValue(rawMap.get("trendAmplified")),
                    stringValue(rawMap.get("lastTrendId"))
            );
        } catch (Exception ignored) {
            return PressureData.empty();
        }
    }

    private void save(Vup vup, PressureData data) {
        Map<String, Object> flags;
        if (vup.getTutorialFlagsJson() == null || vup.getTutorialFlagsJson().isBlank()) {
            flags = new LinkedHashMap<>();
        } else {
            try {
                flags = new LinkedHashMap<>(jsonService.readMap(vup.getTutorialFlagsJson()));
            } catch (Exception ignored) {
                flags = new LinkedHashMap<>();
            }
        }
        Map<String, Object> pressure = new LinkedHashMap<>();
        pressure.put("score", clamp(data.score));
        if (data.sourceKey != null) pressure.put("sourceKey", data.sourceKey);
        if (data.lockedGroupKey != null) pressure.put("lockedGroupKey", data.lockedGroupKey);
        if (data.lockedUntilDay > 0) pressure.put("lockedUntilDay", data.lockedUntilDay);
        if (data.woundedUntilDay > 0) pressure.put("woundedUntilDay", data.woundedUntilDay);
        if (data.trendAmplified) pressure.put("trendAmplified", true);
        if (data.lastTrendId != null) pressure.put("lastTrendId", data.lastTrendId);
        flags.put(FLAG_KEY, pressure);
        vup.setTutorialFlagsJson(jsonService.write(flags));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(10, value));
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String stringValue(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static boolean boolValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    // ==================== Records ====================

    public record PressureState(
            int score,
            String stateLabel,
            String sourceKey,
            String sourceLabel,
            String lockedGroupKey,
            String lockedGroupLabel,
            List<String> lockedActionTypes,
            String hint,
            int cooldownLeft,
            int woundLeft,
            boolean wounded,
            PressureReplacement replacement,
            List<PressureAlternative> alternatives,
            boolean trendAmplified,
            String lastTrendId,
            String monologue,
            String stateMonologue
    ) {
    }

    public record PressureReplacement(
            String actionType,
            String label,
            String hint,
            int staminaCost,
            int inspirationCost,
            int reputationCost,
            int pressureReduction
    ) {
        public static PressureReplacement none() {
            return new PressureReplacement(null, null, null, 0, 0, 0, 0);
        }

        public boolean available() {
            return actionType != null && label != null;
        }
    }

    /**
     * Phase 3: Alternative button for locked action groups.
     * Each alternative has its own costs and pressure reduction effect.
     */
    public record PressureAlternative(
            String actionType,
            String label,
            String hint,
            int staminaCost,
            int inspirationCost,
            int reputationCost,
            int pressureReduction,
            String monologue
    ) {
    }

    private record PressureGroup(
            String key,
            String label,
            List<String> lockedActions,
            String hint,
            String monologue
    ) {
    }

    private static final class PressureData {
        private int score;
        private String sourceKey;
        private String lockedGroupKey;
        private int lockedUntilDay;
        private int woundedUntilDay;
        private boolean trendAmplified;
        private String lastTrendId;

        private PressureData(int score, String sourceKey, String lockedGroupKey,
                             int lockedUntilDay, int woundedUntilDay,
                             boolean trendAmplified, String lastTrendId) {
            this.score = score;
            this.sourceKey = sourceKey;
            this.lockedGroupKey = lockedGroupKey;
            this.lockedUntilDay = lockedUntilDay;
            this.woundedUntilDay = woundedUntilDay;
            this.trendAmplified = trendAmplified;
            this.lastTrendId = lastTrendId;
        }

        private static PressureData empty() {
            return new PressureData(0, null, null, 0, 0, false, null);
        }
    }
}
