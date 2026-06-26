package com.example.vupworld.service.risk;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.ActionType;
import com.example.vupworld.dto.RewardDelta;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 统一结算引擎。所有行动、标题、事件的结算都通过此类输出 RewardDelta。
 * Gate 4 要求：调用方统一落 business_log。
 */
@Component
public class RewardCalculator {

    private final BalanceConfig balanceConfig;
    private final JsonService jsonService;

    public RewardCalculator(BalanceConfig balanceConfig, JsonService jsonService) {
        this.balanceConfig = balanceConfig;
        this.jsonService = jsonService;
    }

    /**
     * 非直播行动的结算。
     */
    public RewardDelta resolveNonStreamAction(Vup vup, ActionType actionType, String npcTendency) {
        return resolveNonStreamAction(vup, actionType, npcTendency, 1);
    }

    /**
     * 非直播行动的结算（带天数，用于判断周增益）。
     */
    public RewardDelta resolveNonStreamAction(Vup vup, ActionType actionType, String npcTendency, int day) {
        return switch (actionType) {
            case TRAIN_SONG -> resolveTrainSong(vup, day);
            case TRAIN_DANCE -> resolveTrainDance(vup);
            case TRAIN_TALK -> resolveTrainTalk(vup, day);
            case PUBLISH_VIDEO -> resolvePublishVideo(vup, day);
            case PUBLISH_CLIP -> resolvePublishClip(vup);
            case NPC_INTERACT -> resolveNpcInteract(npcTendency);
            case FAN_GROUP_MAINTAIN -> resolveFanGroupMaintain();
            case REST -> resolveRest();
            default -> RewardDelta.empty();
        };
    }

    /**
     * 将 RewardDelta 应用到 Vup 状态。
     */
    public void applyToVup(Vup vup, RewardDelta delta) {
        int totalFanChange = delta.trueFanChange() + delta.funFanChange() + delta.unicornFanChange() + delta.ddFanChange();
        vup.setTrueFans(Math.max(0, vup.getTrueFans() + delta.trueFanChange()));
        vup.setFunFans(Math.max(0, vup.getFunFans() + delta.funFanChange()));
        vup.setUnicornFans(Math.max(0, vup.getUnicornFans() + delta.unicornFanChange()));
        vup.setDdFans(Math.max(0, vup.getDdFans() + delta.ddFanChange()));
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        vup.setPopularity(Math.max(0, vup.getPopularity() + delta.popularityChange()));
        vup.setWatchHeat(Math.max(0, vup.getWatchHeat() + delta.watchHeatChange()));
        vup.setReputation(Math.min(100, Math.max(0, vup.getReputation() + delta.reputationChange())));
        vup.setMemeLevel(Math.min(100, Math.max(0, vup.getMemeLevel() + delta.memeChange())));
        vup.setCommercialLevel(Math.min(100, Math.max(0, vup.getCommercialLevel() + delta.commercialChange())));
        vup.setCoin(vup.getCoin() + delta.coinChange());
        vup.setInspiration(Math.max(0, vup.getInspiration() + delta.inspirationChange()));
        vup.setStamina(Math.min(vup.getMaxStamina(), Math.max(0, vup.getStamina() + delta.staminaChange())));
        // Apply attribute changes based on action type stored in evidenceRef
        String actionType = delta.evidenceRef() != null ? String.valueOf(delta.evidenceRef().getOrDefault("type", "")) : "";
        applyAttributeChange(vup, actionType);
    }

    private void applyAttributeChange(Vup vup, String actionType) {
        switch (actionType) {
            case "train_song" -> vup.setSongPower(Math.min(100, vup.getSongPower() + 1));
            case "train_dance" -> vup.setDancePower(Math.min(100, vup.getDancePower() + 1));
            case "train_talk" -> vup.setTalkPower(Math.min(100, vup.getTalkPower() + 1));
        }
    }

    /**
     * 用 RewardDelta 填充 BusinessLog 的结算字段。
     */
    public void populateLogFromDelta(BusinessLog log, RewardDelta delta, String phase, String actionType) {
        int totalFanChange = delta.trueFanChange() + delta.funFanChange() + delta.unicornFanChange() + delta.ddFanChange();
        log.setPhase(phase);
        log.setAction(actionType);
        log.setRawFanGain(totalFanChange);
        log.setFinalFanGain(totalFanChange);
        log.setFanChange(totalFanChange);
        log.setTrueFanChange(delta.trueFanChange());
        log.setFunFanChange(delta.funFanChange());
        log.setUnicornFanChange(delta.unicornFanChange());
        log.setDdFanChange(delta.ddFanChange());
        log.setPopularityChange(delta.popularityChange());
        log.setWatchHeatChange(delta.watchHeatChange());
        log.setReputationChange(delta.reputationChange());
        log.setMemeChange(delta.memeChange());
        log.setCommercialChange(delta.commercialChange());
        log.setCoinChange(delta.coinChange());
        log.setInspirationChange(delta.inspirationChange());
        log.setMultiplierDetail(delta.multiplierDetail());
        log.setCapDetail(delta.capDetail());
        log.setClampDetail(delta.clampDetail());
        log.setWeightDetail(delta.weightDetail());
        log.setExpectationChange("{}");
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
    }

    // --- 私有结算方法 ---

    private RewardDelta resolveTrainSong(Vup vup, int day) {
        int singingBoost = isSingingBoostWeek(day) ? 4 : 0;
        return new RewardDelta(
                balanceConfig.trainSongTrueFanChange() + singingBoost, 0, 0,
                balanceConfig.trainSongDdFanChange(), 0,
                balanceConfig.trainSongPopularity(),
                0, balanceConfig.trainSongReputation(), 0, 0, 0, 0,
                -balanceConfig.trainActionStaminaCost(),
                balanceConfig.standardRouteScoreChange(), "SINGING_IDOL",
                Map.of("type", "train_song"),
                multiplierDetail("TRAIN_SONG"), "{}",
                clampDetail(vup), "{}", "{}"
        );
    }

    private RewardDelta resolveTrainDance(Vup vup) {
        return new RewardDelta(
                balanceConfig.trainDanceTrueFanChange(),
                balanceConfig.trainDanceFunFanChange(), 0,
                balanceConfig.trainDanceDdFanChange(), 0,
                balanceConfig.trainDancePopularity(),
                balanceConfig.trainDanceWatchHeat(),
                0, balanceConfig.trainDanceMemeLevel(), 0, 0, 0,
                -balanceConfig.trainActionStaminaCost(),
                balanceConfig.danceStreamRouteScoreChange(), "DANCE_MEME",
                Map.of("type", "train_dance"),
                multiplierDetail("TRAIN_DANCE"), "{}",
                clampDetail(vup), "{}", "{}"
        );
    }

    private RewardDelta resolveTrainTalk(Vup vup, int day) {
        boolean safeWeek = isSafeWeek(day);
        int safeWeekBonus = safeWeek ? 2 : 0;
        return new RewardDelta(
                balanceConfig.trainTalkTrueFanChange() + safeWeekBonus, 0, 0, 0, 0,
                5,
                0, safeWeek ? balanceConfig.trainTalkSafeWeekReputation() : balanceConfig.trainTalkReputation(), 0, 0, 0, 1,
                -balanceConfig.trainActionStaminaCost(),
                0, "ELECTRONIC_PICKLE",
                Map.of("type", "train_talk"),
                multiplierDetail("TRAIN_TALK"), "{}",
                clampDetail(vup), "{}", "{}"
        );
    }

    private RewardDelta resolvePublishVideo(Vup vup, int day) {
        int singingBoost = isSingingBoostWeek(day) ? 2 : 0;
        return new RewardDelta(
                balanceConfig.publishVideoTrueFanChange() + singingBoost,
                balanceConfig.publishVideoFunFanChange(), 0,
                balanceConfig.publishVideoDdFanChange() + singingBoost * 3, 0,
                balanceConfig.publishVideoPopularity(),
                balanceConfig.publishVideoWatchHeat(), 0, balanceConfig.publishVideoMemeLevel(), 0, 0, 0,
                -balanceConfig.publishVideoStaminaCost(),
                balanceConfig.standardRouteScoreChange(), "SLICE_SAINT",
                Map.of("type", "publish_video"),
                multiplierDetail("PUBLISH_VIDEO"), "{}",
                clampDetail(vup), "{}", "{}"
        );
    }

    private RewardDelta resolvePublishClip(Vup vup) {
        return new RewardDelta(
                0, balanceConfig.publishClipFunFanChange(), 0,
                balanceConfig.publishClipDdFanChange(), 0,
                balanceConfig.publishClipPopularity(),
                balanceConfig.publishClipWatchHeat(), 0, balanceConfig.publishClipMemeLevel(), 0, 0, 0,
                -balanceConfig.publishClipStaminaCost(),
                balanceConfig.standardRouteScoreChange(), "SLICE_SAINT",
                Map.of("type", "publish_clip"),
                multiplierDetail("PUBLISH_CLIP"), "{}",
                clampDetail(vup), "{}", "{}"
        );
    }

    private RewardDelta resolveNpcInteract(String tendency) {
        return switch (tendency != null ? tendency : "AVOID") {
            case "RAID" -> new RewardDelta(
                    balanceConfig.npcInteractTrueFanChange(), 0, 0,
                    balanceConfig.npcInteractDdFanChange(), 0,
                    balanceConfig.npcInteractPopularity(),
                    8, balanceConfig.npcInteractReputation(), 0, 0, 0, 0,
                    -3, balanceConfig.standardRouteScoreChange(), "SOCIAL_COLLAB",
                    Map.of("type", "npc_raid"),
                    multiplierDetail("NPC_INTERACT"), "{}", "{}", "{}", "{}"
            );
            case "COLLAB" -> new RewardDelta(
                    balanceConfig.npcInteractTrueFanChange(), 0, 0, 18, 0,
                    10, 5, 2, 0, 0, 0, 0,
                    -3, balanceConfig.highRouteScoreChange(), "SOCIAL_COLLAB",
                    Map.of("type", "npc_collab"),
                    multiplierDetail("NPC_INTERACT"), "{}", "{}", "{}", "{}"
            );
            case "BORROW_HEAT" -> new RewardDelta(
                    balanceConfig.npcInteractTrueFanChange(), 8, 0, 16, 0,
                    15, 12, -2, 0, 0, 0, 0,
                    -3, 1, "BLACK_RED_MAIN_STAGE",
                    Map.of("type", "npc_borrow_heat"),
                    multiplierDetail("NPC_INTERACT"), "{}", "{}", "{}", "{}"
            );
            default -> new RewardDelta(
                    balanceConfig.npcInteractTrueFanChange(), 0, 0, 4, 0,
                    2, 0, 1, 0, 0, 0, 0,
                    -3, 0, "ELECTRONIC_PICKLE",
                    Map.of("type", "npc_avoid"),
                    multiplierDetail("NPC_INTERACT"), "{}", "{}", "{}", "{}"
            );
        };
    }

    private RewardDelta resolveFanGroupMaintain() {
        return new RewardDelta(
                balanceConfig.fanGroupMaintainTrueFanChange(), 0, 0, 0, 0,
                3, 0, balanceConfig.fanGroupMaintainReputation(), 0, 0, 0, 0,
                -2, balanceConfig.standardRouteScoreChange(), "ELECTRONIC_PICKLE",
                Map.of("type", "fan_group_maintain"),
                multiplierDetail("FAN_GROUP_MAINTAIN"), "{}", "{}", "{}", "{}"
        );
    }

    private RewardDelta resolveRest() {
        return new RewardDelta(
                balanceConfig.restTrueFanChange(), 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0,
                balanceConfig.restStaminaRecovery(), 0, null,
                Map.of("type", "rest"),
                multiplierDetail("REST"), "{}", "{}", "{}", "{}"
        );
    }

    private String multiplierDetail(String actionType) {
        return "{\"pipeline\":\"P0_SIMPLE\",\"action\":\"" + actionType + "\"}";
    }

    private String clampDetail(Vup vup) {
        return "{\"reputation\":" + vup.getReputation()
                + ",\"stamina\":" + vup.getStamina() + "}";
    }

    private boolean isSingingBoostWeek(int day) {
        return day >= 8 && day <= 14;
    }

    private boolean isSafeWeek(int day) {
        return day >= 1 && day <= 7;
    }
}
