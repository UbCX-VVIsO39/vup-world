package com.example.vupworld.service.risk;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.dto.RiskToolDtos.RiskToolOptionDTO;
import com.example.vupworld.dto.RiskToolDtos.RiskToolResultDTO;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import com.example.vupworld.dto.VupDtos.DebtDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class RiskToolService {
    private static final String RISK_TOOL_PATH = "/api/risk-tool/use";

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final BusinessLogMapper businessLogMapper;
    private final JsonService jsonService;
    private final IdempotencyRunner idempotencyRunner;
    private final RequestHashService requestHashService;
    private final DayFlowService dayFlowService;
    private final BalanceConfig balanceConfig;

    public RiskToolService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            RiskDebtMapper riskDebtMapper,
            BusinessLogMapper businessLogMapper,
            JsonService jsonService,
            IdempotencyRunner idempotencyRunner,
            RequestHashService requestHashService,
            DayFlowService dayFlowService,
            BalanceConfig balanceConfig
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.businessLogMapper = businessLogMapper;
        this.jsonService = jsonService;
        this.idempotencyRunner = idempotencyRunner;
        this.requestHashService = requestHashService;
        this.dayFlowService = dayFlowService;
        this.balanceConfig = balanceConfig;
    }

    public List<RiskToolOptionDTO> options(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        return List.of(
                optionFor(vup, session, openDebts, toolConfig("FULL_CLIP_CONTEXT")),
                optionFor(vup, session, openDebts, toolConfig("COOLING_NOTICE")),
                optionFor(vup, session, openDebts, toolConfig("TEMP_MOD_TEAM"))
        );
    }

    @Transactional
    public RiskToolResultDTO useTool(Long userId, UseRiskToolRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, RiskToolResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(RISK_TOOL_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, RISK_TOOL_PATH, idempotencyKey, requestHash,
                () -> doUseTool(vup, session, request, idempotencyKey),
                RiskToolResultDTO.class, buildRecord);
    }

    private RiskToolResultDTO doUseTool(Vup vup, DaySession session, UseRiskToolRequest request, String idempotencyKey) {
        if (!riskToolPhaseAllowed(session)) {
            throw new GameException("RISK_TOOL_NOT_AVAILABLE", "当前阶段不能补票使用米线工具。");
        }
        if (session.isRiskToolUsed()) {
            throw new GameException("RISK_TOOL_DAILY_LIMIT", "今天已经用过米线工具了，不能把法槌当连点器。");
        }

        ToolConfig tool = toolConfig(request.toolType());
        RiskDebt debt = chooseTargetDebt(vup, session, request.targetDebtId(), tool);
        AppliedCost appliedCost = applyCost(vup, tool);
        applyToolEffect(vup, debt, tool);
        vupMapper.updateState(vup);
        riskDebtMapper.updateMitigation(debt);

        BusinessLog log = toBusinessLog(vup, session, debt, tool, appliedCost, idempotencyKey);
        businessLogMapper.insert(log);

        session.setRiskToolUsed(true);
        daySessionMapper.updateAfterAction(session);

        return new RiskToolResultDTO(
                tool.toolType,
                session.getPhase(),
                -appliedCost.coinCost,
                -appliedCost.inspirationCost,
                -tool.severityReduce,
                tool.reputationChange,
                tool.popularityChange,
                vup.getCoin(),
                vup.getStamina(),
                vup.getInspiration(),
                toDebtDto(debt, session),
                summary(tool, debt)
        );
    }

    private ToolConfig toolConfig(String toolType) {
        return switch (toolType) {
            case "FULL_CLIP_CONTEXT" -> new ToolConfig("FULL_CLIP_CONTEXT", 0, 0,
                    balanceConfig.fullClipContextInspirationCost(), 0, 2, -80, -10, 3);
            case "COOLING_NOTICE" -> new ToolConfig("COOLING_NOTICE", balanceConfig.coolingNoticeCoinCost(),
                    0, 0, 1, 1, -60, -8, 2);
            case "TEMP_MOD_TEAM" -> new ToolConfig("TEMP_MOD_TEAM", balanceConfig.tempModTeamCoinCost(),
                    0, 0, 0, 1, -30, -15, 0);
            default -> throw new GameException("RISK_TOOL_NOT_AVAILABLE", "未知米线工具。");
        };
    }

    private RiskToolOptionDTO optionFor(Vup vup, DaySession session, List<RiskDebt> openDebts, ToolConfig tool) {
        RiskDebt target = targetDebtForOption(session, openDebts, tool);
        String disabledReason = disabledReason(vup, session, target, tool);
        return new RiskToolOptionDTO(
                tool.toolType,
                toolLabel(tool.toolType),
                costPreview(tool),
                effectPreview(tool),
                disabledReason == null,
                disabledReason,
                target == null ? null : target.getId(),
                target == null ? null : RiskDebtPresenter.toDebtDto(target, session.getDay()),
                targetSummary(session, target),
                urgencyPreview(session, target),
                supportedDebtTypes(tool)
        );
    }

    private String disabledReason(Vup vup, DaySession session, RiskDebt target, ToolConfig tool) {
        if (!riskToolPhaseAllowed(session)) {
            return "PHASE_NOT_ALLOWED";
        }
        if (session.isRiskToolUsed()) {
            return "RISK_TOOL_DAILY_LIMIT";
        }
        if (target == null) {
            return "RISK_TOOL_NO_TARGET";
        }
        if (!canPay(vup, tool)) {
            return insufficientCostReason(vup, tool);
        }
        return null;
    }

    private boolean canPay(Vup vup, ToolConfig tool) {
        if (vup.getInspiration() < tool.inspirationCost) {
            return false;
        }
        if (vup.getCoin() >= tool.coinCost && vup.getStamina() >= tool.staminaCost) {
            return true;
        }
        return tool.fallbackStaminaCost > 0
                && vup.getStamina() >= tool.staminaCost + tool.fallbackStaminaCost;
    }

    private String insufficientCostReason(Vup vup, ToolConfig tool) {
        if (vup.getInspiration() < tool.inspirationCost) {
            return "INSUFFICIENT_INSPIRATION";
        }
        if (tool.fallbackStaminaCost > 0 && vup.getCoin() < tool.coinCost) {
            return "INSUFFICIENT_STAMINA";
        }
        if (vup.getCoin() < tool.coinCost) {
            return "INSUFFICIENT_COIN";
        }
        return "INSUFFICIENT_STAMINA";
    }

    private String toolLabel(String toolType) {
        return switch (toolType) {
            case "FULL_CLIP_CONTEXT" -> "补全切片";
            case "COOLING_NOTICE" -> "降温公告";
            case "TEMP_MOD_TEAM" -> "临时房管";
            default -> toolType;
        };
    }

    private String costPreview(ToolConfig tool) {
        return switch (tool.toolType) {
            case "FULL_CLIP_CONTEXT" -> balanceConfig.fullClipContextInspirationCost() + "灵感";
            case "COOLING_NOTICE" -> balanceConfig.coolingNoticeCoinCost() + "运营预算或1体力";
            case "TEMP_MOD_TEAM" -> balanceConfig.tempModTeamCoinCost() + "活动预算";
            default -> "无";
        };
    }

    private String effectPreview(ToolConfig tool) {
        return switch (tool.toolType) {
            case "FULL_CLIP_CONTEXT" -> "旧账严重度-2，补来源上下文；人气-80，录播组暂时撤庭";
            case "COOLING_NOTICE" -> "旧账严重度-1，围观热度-8，口碑+2，人气-60";
            case "TEMP_MOD_TEAM" -> "旧账严重度-1，围观热度-15，商业化+1";
            default -> "降低旧账风险";
        };
    }

    private List<String> supportedDebtTypes(ToolConfig tool) {
        if ("FULL_CLIP_CONTEXT".equals(tool.toolType)) {
            return List.of("TITLE_BACKFIRE", "BOOMERANG_CLIP");
        }
        return List.of("TITLE_BACKFIRE", "BOOMERANG_CLIP", "UNICORN_EXPECTATION", "COMMERCIAL_BACKLASH", "BLACK_HISTORY_STOCK", "VOICE_ACCIDENT", "COLLAB_SPILLOVER", "FAN_GROUP_DRAMA");
    }

    private boolean riskToolPhaseAllowed(DaySession session) {
        return DayPhase.READY.name().equals(session.getPhase())
                || DayPhase.NEED_EVENT_CHOICE.name().equals(session.getPhase());
    }

    private RiskDebt targetDebtForOption(DaySession session, List<RiskDebt> openDebts, ToolConfig tool) {
        if (DayPhase.NEED_EVENT_CHOICE.name().equals(session.getPhase()) && session.getPendingFormalEventId() != null) {
            return openDebts.stream()
                    .filter(debt -> debt.getId().equals(session.getPendingFormalEventId()))
                    .filter(debt -> supports(tool, debt))
                    .findFirst()
                    .orElse(null);
        }
        return openDebts.stream()
                .filter(debt -> supports(tool, debt))
                .min(targetPriority(session))
                .orElse(null);
    }

    private RiskDebt chooseTargetDebt(Vup vup, DaySession session, Long targetDebtId, ToolConfig tool) {
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        if (targetDebtId == null) {
            RiskDebt target = targetDebtForOption(session, openDebts, tool);
            if (target != null) {
                return target;
            }
            throw new GameException("RISK_TOOL_NOT_AVAILABLE", "没有可处理的目标债务。");
        }
        return openDebts.stream()
                .filter(debt -> debt.getId().equals(targetDebtId))
                .filter(debt -> supports(tool, debt))
                .findFirst()
                .orElseThrow(() -> new GameException("RISK_TOOL_NOT_AVAILABLE", "没有可处理的目标债务。"));
    }

    private Comparator<RiskDebt> targetPriority(DaySession session) {
        return Comparator
                .comparingInt((RiskDebt debt) -> debt.getSeverity()).reversed()
                .thenComparingInt(debt -> Math.max(0, debt.getDueDay() - session.getDay()))
                .thenComparingInt(RiskDebt::getCreateDay)
                .thenComparingLong(debt -> debt.getId() == null ? Long.MAX_VALUE : debt.getId());
    }

    private String targetSummary(DaySession session, RiskDebt debt) {
        if (debt == null) {
            return null;
        }
        String label = RiskDebtLabels.debtTypeLabel(debt.getDebtType());
        if (isPendingFormalEventTarget(session, debt)) {
            return "目标：" + label + " · " + debt.getSeverity() + "级 · 正在爆发 · "
                    + RiskDebtPresenter.sourceLine(debt);
        }
        return "目标：" + label + " · " + debt.getSeverity() + "级 · "
                + RiskDebtPresenter.dueText(debt, session.getDay())
                + " · " + RiskDebtPresenter.sourceLine(debt);
    }

    private String urgencyPreview(DaySession session, RiskDebt debt) {
        if (debt == null) {
            return null;
        }
        String prefix = RiskDebtPresenter.crisisLevelLabel(debt, session.getDay())
                + " · 严重度" + debt.getSeverity()
                + " · 剩余" + RiskDebtPresenter.remainingDays(debt, session.getDay()) + "天 · ";
        if (isPendingFormalEventTarget(session, debt)) {
            return prefix + "正在爆发 · " + RiskDebtPresenter.recommendedAction(debt, session.getDay());
        }
        return prefix + RiskDebtPresenter.dueText(debt, session.getDay())
                + " · " + RiskDebtPresenter.recommendedAction(debt, session.getDay());
    }

    private boolean isPendingFormalEventTarget(DaySession session, RiskDebt debt) {
        return DayPhase.NEED_EVENT_CHOICE.name().equals(session.getPhase())
                && session.getPendingFormalEventId() != null
                && session.getPendingFormalEventId().equals(debt.getId());
    }

    private boolean supports(ToolConfig tool, RiskDebt debt) {
        if ("FULL_CLIP_CONTEXT".equals(tool.toolType)) {
            return "TITLE_BACKFIRE".equals(debt.getDebtType()) || "BOOMERANG_CLIP".equals(debt.getDebtType());
        }
        return true;
    }

    private AppliedCost applyCost(Vup vup, ToolConfig tool) {
        int coinCost = tool.coinCost;
        int staminaCost = tool.staminaCost;
        if (coinCost > 0 && vup.getCoin() < coinCost && tool.fallbackStaminaCost > 0) {
            coinCost = 0;
            staminaCost += tool.fallbackStaminaCost;
        }
        if (vup.getCoin() < coinCost) {
            throw new GameException("INSUFFICIENT_COIN", "预算不足，米线施工队今天排不开班。");
        }
        if (vup.getStamina() < staminaCost) {
            throw new GameException("INSUFFICIENT_STAMINA", "体力不足，房管喊不动。");
        }
        if (vup.getInspiration() < tool.inspirationCost) {
            throw new GameException("INSUFFICIENT_INSPIRATION", "灵感不足，切片反转剪不出来。");
        }
        vup.setCoin(vup.getCoin() - coinCost);
        vup.setStamina(vup.getStamina() - staminaCost);
        vup.setInspiration(vup.getInspiration() - tool.inspirationCost);
        return new AppliedCost(coinCost, staminaCost, tool.inspirationCost);
    }

    private void applyToolEffect(Vup vup, RiskDebt debt, ToolConfig tool) {
        debt.setSeverity(Math.max(1, debt.getSeverity() - tool.severityReduce));
        debt.setSummary(debt.getSummary() + " 已使用" + toolLabel(tool.toolType) + "降温，材料还在，但火势被压住了一点。");
        vup.setPopularity(Math.max(0, vup.getPopularity() + tool.popularityChange));
        vup.setWatchHeat(Math.max(0, vup.getWatchHeat() + tool.watchHeatChange));
        vup.setReputation(Math.min(100, Math.max(0, vup.getReputation() + tool.reputationChange)));
    }

    private BusinessLog toBusinessLog(Vup vup, DaySession session, RiskDebt debt, ToolConfig tool, AppliedCost appliedCost, String idempotencyKey) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(session.getPhase());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction("RISK_TOOL:" + tool.toolType);
        log.setRiskToolId(tool.toolType);
        log.setResult(summary(tool, debt));
        log.setRawFanGain(0);
        log.setFinalFanGain(0);
        log.setFanChange(0);
        log.setTrueFanChange(0);
        log.setFunFanChange(0);
        log.setUnicornFanChange(0);
        log.setDdFanChange(0);
        log.setPopularityChange(tool.popularityChange);
        log.setWatchHeatChange(tool.watchHeatChange);
        log.setReputationChange(tool.reputationChange);
        log.setMemeChange(0);
        log.setCommercialChange("TEMP_MOD_TEAM".equals(tool.toolType) ? 1 : 0);
        log.setCoinChange(-appliedCost.coinCost);
        log.setInspirationChange(-appliedCost.inspirationCost);
        log.setMultiplierDetail(jsonService.write(Map.of("pipeline", "P0_RISK_TOOL")));
        log.setCapDetail(jsonService.write(Map.of("directFanGain", 0)));
        log.setClampDetail(jsonService.write(Map.of("debtSeverity", debt.getSeverity(), "watchHeat", vup.getWatchHeat())));
        log.setWeightDetail(jsonService.write(Map.of("toolType", tool.toolType, "targetDebtId", debt.getId())));
        log.setRngDetail(jsonService.write(Map.of("seed", session.getRandomSeed(), "cursor", session.getRngCursor())));
        log.setExpectationChange("{}");
        log.setRouteScoreChange("{}");
        log.setDebtIds(jsonService.write(List.of(debt.getId())));
        log.setAccidentMaterialIds(jsonService.write(List.of(accidentMaterialIdFor(debt.getDebtType()))));
        log.setEndingRefFlag(true);
        return log;
    }

    private String accidentMaterialIdFor(String debtType) {
        return switch (debtType) {
            case "BOOMERANG_CLIP" -> "BOOMERANG_CLIP_CONTEXT";
            case "UNICORN_EXPECTATION" -> "UNICORN_EXPECTATION_SCREENSHOT";
            case "COMMERCIAL_BACKLASH" -> "COMMERCIAL_BACKLASH_RECEIPT";
            default -> "TITLE_BACKFIRE_CONTEXT";
        };
    }

    private String summary(ToolConfig tool, RiskDebt debt) {
        String debtLabel = RiskDebtLabels.debtTypeLabel(debt.getDebtType());
        return switch (tool.toolType) {
            case "FULL_CLIP_CONTEXT" -> "切片反转发布，补上来源上下文，考据组暂时撤了一半法槌，" + debtLabel + "严重度降到" + debt.getSeverity() + "级。";
            case "COOLING_NOTICE" -> "降温公告发出，先承认边界再压传播，" + debtLabel + "严重度降到" + debt.getSeverity() + "级。";
            case "TEMP_MOD_TEAM" -> "临时房管进场，弹幕速度降了点，" + debtLabel + "严重度降到" + debt.getSeverity() + "级。";
            default -> "米线工具处理完成。";
        };
    }

    private DebtDTO toDebtDto(RiskDebt debt, DaySession session) {
        return RiskDebtPresenter.toDebtDto(debt, session.getDay());
    }

    private record ToolConfig(
            String toolType,
            int coinCost,
            int staminaCost,
            int inspirationCost,
            int fallbackStaminaCost,
            int severityReduce,
            int popularityChange,
            int watchHeatChange,
            int reputationChange
    ) {
    }

    private record AppliedCost(
            int coinCost,
            int staminaCost,
            int inspirationCost
    ) {
    }
}
