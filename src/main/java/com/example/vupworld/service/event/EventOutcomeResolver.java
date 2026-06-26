package com.example.vupworld.service.event;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.ActionDtos.ActionResultDTO;
import com.example.vupworld.dto.RewardDelta;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 统一事件结果解析器。safe / traffic / meme 三分支输出统一结果。
 * Gate 5 要求：每个分支都能写入日志、日报和后续风险；高风险分支不能纯赚。
 */
@Component
public class EventOutcomeResolver {

    private final BalanceConfig balanceConfig;

    public EventOutcomeResolver(BalanceConfig balanceConfig) {
        this.balanceConfig = balanceConfig;
    }

    /**
     * 解析正式事件（债务事件或普通事件）的选择结果。
     */
    public EventOutcome resolveFormalEvent(Vup vup, String choiceType, String eventKey, boolean isDebtEvent) {
        return switch (choiceType) {
            case "safe" -> resolveFormalSafe(vup, isDebtEvent);
            case "traffic" -> resolveFormalTraffic(vup, eventKey, isDebtEvent);
            case "meme" -> resolveFormalMeme(vup, eventKey, isDebtEvent);
            default -> resolveFormalSafe(vup, isDebtEvent);
        };
    }

    /**
     * 解析直播现场事件的选择结果。
     */
    public EventOutcome resolveInteraction(Vup vup, String choiceType, String eventKey) {
        return switch (choiceType) {
            case "safe" -> resolveInteractionSafe(vup);
            case "traffic" -> resolveInteractionTraffic(vup, eventKey);
            case "meme" -> resolveInteractionMeme(vup, eventKey);
            default -> resolveInteractionSafe(vup);
        };
    }

    // --- 正式事件 safe 分支 ---

    private EventOutcome resolveFormalSafe(Vup vup, boolean isDebtEvent) {
        RewardDelta delta;
        if (isDebtEvent) {
            delta = new RewardDelta(
                    0, 0, 0, 0, 0,
                    0, -5, 2, 0, 0, 0, 0,
                    0, 1, "ELECTRONIC_PICKLE",
                    Map.of("type", "formal_event_safe", "debtCleared", true),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}", "{}", "{}", "{}"
            );
        } else {
            delta = new RewardDelta(
                    3, 0, 0, 0, 0,
                    0, -3, 3, 0, 0, 0, 0,
                    0, 1, "ELECTRONIC_PICKLE",
                    Map.of("type", "formal_event_safe"),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}", "{}", "{}", "{}"
            );
        }
        return new EventOutcome(delta, "选择降温处理，风险解除。", true, false);
    }

    // --- 正式事件 traffic 分支 ---

    private EventOutcome resolveFormalTraffic(Vup vup, String eventKey, boolean isDebtEvent) {
        RewardDelta delta;
        if (isDebtEvent) {
            // 高风险：大量围观热度和粉丝，但口碑下降，可能产生新债务
            delta = new RewardDelta(
                    0, 15, 0, 20, 0,
                    25, 20, -4, 5, 0, 0, 0,
                    0, 2, "BLACK_RED_MAIN_STAGE",
                    Map.of("type", "formal_event_traffic", "debtCleared", true, "risk", "high"),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}",
                    "{\"reputation\":" + vup.getReputation() + "}", "{}", "{}"
            );
        } else {
            delta = new RewardDelta(
                    0, 8, 0, 10, 0,
                    15, 12, -2, 3, 0, 0, 0,
                    0, 1, "BLACK_RED_MAIN_STAGE",
                    Map.of("type", "formal_event_traffic", "risk", "medium"),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}",
                    "{\"reputation\":" + vup.getReputation() + "}", "{}", "{}"
            );
        }
        return new EventOutcome(delta,
                "硬接流量，围观热度飙升，但口碑承压。",
                false, true);
    }

    // --- 正式事件 meme 分支 ---

    private EventOutcome resolveFormalMeme(Vup vup, String eventKey, boolean isDebtEvent) {
        RewardDelta delta;
        if (isDebtEvent) {
            // 中风险：切片路线粉丝，梗等级上升，但可能产生事故素材
            delta = new RewardDelta(
                    0, 12, 0, 5, 0,
                    10, 5, -1, 8, 0, 0, 0,
                    0, 2, "SLICE_SAINT",
                    Map.of("type", "formal_event_meme", "debtCleared", true, "accidentRisk", true),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}",
                    "{\"reputation\":" + vup.getReputation() + "}", "{}", "{}"
            );
        } else {
            delta = new RewardDelta(
                    0, 6, 0, 3, 0,
                    8, 3, 0, 5, 0, 0, 0,
                    0, 1, "SLICE_SAINT",
                    Map.of("type", "formal_event_meme", "accidentRisk", true),
                    "{\"pipeline\":\"P0_SIMPLE\"}", "{}", "{}", "{}", "{}"
            );
        }
        return new EventOutcome(delta,
                "顺势玩梗，梗等级飙升，但可能被切片组放大。",
                false, true);
    }

    // --- 直播现场 safe 分支 ---

    private EventOutcome resolveInteractionSafe(Vup vup) {
        RewardDelta delta = new RewardDelta(
                0, 0, 0, 0, 0,
                0, -3, 1, 0, 0, 0, 0,
                0, 1, "ELECTRONIC_PICKLE",
                Map.of("type", "interaction_safe"),
                "{\"pipeline\":\"P0_SIMPLE\"}", "{}", "{}", "{}", "{}"
        );
        return new EventOutcome(delta, "选择稳住现场，风险解除。", true, false);
    }

    // --- 直播现场 traffic 分支 ---

    private EventOutcome resolveInteractionTraffic(Vup vup, String eventKey) {
        RewardDelta delta = new RewardDelta(
                0, 10, 0, 15, 0,
                20, 15, -3, 3, 0, 0, 0,
                0, 2, "BLACK_RED_MAIN_STAGE",
                Map.of("type", "interaction_traffic", "risk", "high"),
                "{\"pipeline\":\"P0_SIMPLE\"}", "{}",
                "{\"reputation\":" + vup.getReputation() + "}", "{}", "{}"
        );
        return new EventOutcome(delta,
                "理性聊两句，围观热度上升，但口碑承压。",
                false, true);
    }

    // --- 直播现场 meme 分支 ---

    private EventOutcome resolveInteractionMeme(Vup vup, String eventKey) {
        RewardDelta delta = new RewardDelta(
                0, 8, 0, 5, 0,
                12, 5, 0, 6, 0, 0, 0,
                0, 1, "SLICE_SAINT",
                Map.of("type", "interaction_meme", "accidentRisk", true),
                "{\"pipeline\":\"P0_SIMPLE\"}", "{}", "{}", "{}", "{}"
        );
        return new EventOutcome(delta,
                "直接开香槟，梗等级上升，但可能被切片组放大。",
                false, true);
    }

    /**
     * 事件结果。
     */
    public record EventOutcome(
            RewardDelta delta,
            String summary,
            boolean safe,       // 是否安全（无风险/债务）
            boolean hasRisk     // 是否有风险（高风险分支）
    ) {}
}
