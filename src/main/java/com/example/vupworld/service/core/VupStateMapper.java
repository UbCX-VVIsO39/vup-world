package com.example.vupworld.service.core;

import com.example.vupworld.service.risk.RiskDebtLabels;
import com.example.vupworld.service.risk.RiskDebtPresenter;

import com.example.vupworld.service.progression.PermanentUnlockService;
import com.example.vupworld.service.progression.StageObjectiveService;
import com.example.vupworld.service.progression.StrategyAdviceService;

import com.example.vupworld.service.content.MoodService;
import com.example.vupworld.service.content.PlatformTrendService;

import com.example.vupworld.service.fan.PersonaTagService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.event.FormalEventPresenter;
import com.example.vupworld.service.event.PendingInteractionPresenter;

import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.DayDtos.DisabledActionDTO;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.dto.VupDtos.AttributeDTO;
import com.example.vupworld.dto.VupDtos.FanStructureDTO;
import com.example.vupworld.dto.VupDtos.OpinionDTO;
import com.example.vupworld.dto.VupDtos.ResourceDTO;
import com.example.vupworld.dto.VupDtos.TutorialHintDTO;
import com.example.vupworld.dto.VupDtos.VupStateDTO;
import com.example.vupworld.dto.MoodDtos.MoodResult;
import com.example.vupworld.dto.MoodDtos.StrategyAdviceDTO;
import com.example.vupworld.dto.MoodDtos.PermanentUnlockDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VupStateMapper {
    private final RiskDebtMapper riskDebtMapper;
    private final BusinessLogMapper businessLogMapper;
    private final PlatformTrendService platformTrendService;
    private final JsonService jsonService;
    private final BalanceConfig balanceConfig;
    private final FormalEventPresenter formalEventPresenter;
    private final PendingInteractionPresenter pendingInteractionPresenter;
    private final MoodService moodService;
    private final StrategyAdviceService strategyAdviceService;
    private final PermanentUnlockService permanentUnlockService;

    public VupStateMapper(
            RiskDebtMapper riskDebtMapper,
            BusinessLogMapper businessLogMapper,
            PlatformTrendService platformTrendService,
            JsonService jsonService,
            BalanceConfig balanceConfig,
            FormalEventPresenter formalEventPresenter,
            PendingInteractionPresenter pendingInteractionPresenter,
            MoodService moodService,
            StrategyAdviceService strategyAdviceService,
            PermanentUnlockService permanentUnlockService
    ) {
        this.riskDebtMapper = riskDebtMapper;
        this.businessLogMapper = businessLogMapper;
        this.platformTrendService = platformTrendService;
        this.jsonService = jsonService;
        this.balanceConfig = balanceConfig;
        this.formalEventPresenter = formalEventPresenter;
        this.pendingInteractionPresenter = pendingInteractionPresenter;
        this.moodService = moodService;
        this.strategyAdviceService = strategyAdviceService;
        this.permanentUnlockService = permanentUnlockService;
    }

    public VupStateDTO toStateDto(Vup vup, DaySession session) {
        return toStateDto(vup, session, List.of());
    }

    public VupStateDTO toStateDto(Vup vup, DaySession session, List<TutorialHintDTO> tutorialHints) {
        // 计算心情
        MoodResult moodResult = session != null ? moodService.getMood(vup, session) : null;

        // 计算策略建议
        List<StrategyAdviceDTO> adviceList = session != null ? strategyAdviceService.getAdvice(vup, session) : List.of();

        // 获取永久解锁
        List<PermanentUnlockDTO> unlockList = permanentUnlockService.getUnlocks(vup.getUserId());

        return new VupStateDTO(
                vup.getId(),
                vup.getId(),
                Math.max(1, vup.getSlotNumber()),
                vup.getName(),
                vup.getPersona(),
                vup.getDayCount(),
                balanceConfig.maxDay(),
                session == null ? null : session.getPhase(),
                vup.getCurrentRoute(),
                route(vup),
                jsonMap(vup.getExpectationJson()),
                new ResourceDTO(vup.getStamina(), vup.getMaxStamina(), vup.getCoin(), vup.getInspiration()),
                new AttributeDTO(
                        vup.getSongPower(),
                        vup.getDancePower(),
                        vup.getTalkPower(),
                        vup.getMemePower(),
                        vup.getPlanPower(),
                        vup.getStressPower()
                ),
                new FanStructureDTO(
                        vup.getFans(),
                        vup.getTrueFans(),
                        vup.getFunFans(),
                        vup.getUnicornFans(),
                        vup.getDdFans()
                ),
                new OpinionDTO(
                        vup.getPopularity(),
                        vup.getWatchHeat(),
                        vup.getReputation(),
                        vup.getMemeLevel(),
                        vup.getCommercialLevel()
                ),
                riskDebtMapper.findOpenByVupId(vup.getId()).stream()
                        .map(debt -> RiskDebtPresenter.toDebtDto(debt, currentDay(vup, session)))
                        .toList(),
                Map.of(
                        "watchHeat", vup.getWatchHeat(),
                        "funDensity", vup.getMemeLevel(),
                        "trialHeat", 0,
                        "materialStock", businessLogMapper.countMaterialStockByVupId(vup.getId()),
                        "scatterPressure", 0,
                        "funTags", List.of("新人保护", "标题组待机")
                ),
                platformTrendService.currentTrendForDay(vup.getDayCount()),
                tutorialHints,
                // 新增字段
                moodResult != null ? moodResult.mood().name() : null,
                moodResult != null ? moodResult.moodLine() : null,
                moodResult != null ? moodResult.effect() : null,
                adviceList.stream()
                        .map(this::adviceToMap)
                        .toList(),
                unlockList.stream()
                        .map(this::unlockToMap)
                        .toList()
        );
    }

    private Map<String, Object> route(Vup vup) {
        return Map.of(
                "currentRoute", vup.getCurrentRoute(),
                "routeScore", jsonMap(vup.getRouteScoreJson())
        );
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(json);
    }

    public DaySessionDTO toDaySessionDto(DaySession session) {
        return new DaySessionDTO(
                session.getDay(),
                session.getPhase(),
                session.getSelectedAction(),
                session.getSelectedPlanId(),
                session.getSelectedTitleTemplateId(),
                session.getTitleCandidatesJson(),
                titleCandidates(session),
                session.getTitleRerollCount(),
                session.isStreamPlanCancelled(),
                session.getPendingInteractionEventId(),
                pendingInteractionEvent(session),
                session.getPendingFormalEventId(),
                pendingFormalEvent(session),
                session.getReportId(),
                session.getEndingReviewId(),
                riskHints(session),
                disabledActions(session),
                availableHint(session)
        );
    }

    private List<DisabledActionDTO> disabledActions(DaySession session) {
        if (!session.isStreamPlanCancelled()) {
            return List.of();
        }
        return List.of(new DisabledActionDTO("STREAM_PLAN", "STREAM_PLAN_CANCELLED_TODAY"));
    }

    private String availableHint(DaySession session) {
        if (!session.isStreamPlanCancelled()) {
            return null;
        }
        return "今天不能再次选择直播企划，但可以改做非直播行动。";
    }

    private com.example.vupworld.dto.EventDtos.PendingEventDTO pendingFormalEvent(DaySession session) {
        if (session.getVupId() == null || !"NEED_EVENT_CHOICE".equals(session.getPhase())) {
            return null;
        }
        Vup vup = new Vup();
        vup.setId(session.getVupId());
        return formalEventPresenter.pending(vup, session);
    }

    private com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO pendingInteractionEvent(DaySession session) {
        if (!"NEED_INTERACTION_CHOICE".equals(session.getPhase())) {
            return null;
        }
        return pendingInteractionPresenter.pending(session);
    }

    private List<TitleOptionDTO> titleCandidates(DaySession session) {
        if (session.getTitleCandidatesJson() == null || session.getTitleCandidatesJson().isBlank()) {
            return List.of();
        }
        return jsonService.readTitleOptions(session.getTitleCandidatesJson());
    }

    private List<String> riskHints(DaySession session) {
        if (session.getVupId() == null) {
            return List.of();
        }
        return riskDebtMapper.findOpenByVupId(session.getVupId()).stream()
                .map(debt -> "有旧账在发酵：" + RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                        + "，" + RiskDebtPresenter.crisisLevelLabel(debt, session.getDay())
                        + "，" + RiskDebtPresenter.dueText(debt, session.getDay())
                        + "。建议：" + RiskDebtPresenter.recommendedAction(debt, session.getDay()))
                .toList();
    }

    private int currentDay(Vup vup, DaySession session) {
        return session == null ? vup.getDayCount() : session.getDay();
    }

    private Map<String, Object> adviceToMap(StrategyAdviceDTO a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("priority", a.priority());
        map.put("icon", a.icon());
        map.put("text", a.text());
        map.put("reason", a.reason());
        return map;
    }

    private Map<String, Object> unlockToMap(PermanentUnlockDTO u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", u.key());
        map.put("label", u.label());
        map.put("description", u.description());
        map.put("sourceEnding", u.sourceEnding());
        return map;
    }
}
