package com.example.vupworld.service.report;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.progression.StageObjectiveService;
import com.example.vupworld.service.progression.ComboDiscoveryService;

import com.example.vupworld.service.content.MemeQuoteService;

import com.example.vupworld.service.content.PlatformTrendService;

import com.example.vupworld.service.content.RouteIdentityService;

import com.example.vupworld.service.fan.AudiencePressureService;

import com.example.vupworld.service.operating.OperatingPressureService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.fan.FanLetterService;
import com.example.vupworld.service.risk.RiskDebtPresenter;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.DayPhaseStateMachine;
import com.example.vupworld.dto.ActionDtos.ActionResultDTO;
import com.example.vupworld.dto.ComboDtos.ComboItemDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.dto.ReportDtos.DailyReportDTO;
import com.example.vupworld.mapper.DailyReportMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DailyReport;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private final BusinessLogMapper businessLogMapper;
    private final DailyReportMapper dailyReportMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final JsonService jsonService;
    private final PlatformTrendService platformTrendService;
    private final ComboDiscoveryService comboDiscoveryService;
    private final MemeQuoteService memeQuoteService;
    private final FanLetterService fanLetterService;
    private final DayFlowService dayFlowService;
    private final StageObjectiveService stageObjectiveService;
    private final BalanceConfig balanceConfig;
    private final OperatingPressureService operatingPressureService;
    private final RouteIdentityService routeIdentityService;

    public ReportService(
            BusinessLogMapper businessLogMapper,
            DailyReportMapper dailyReportMapper,
            RiskDebtMapper riskDebtMapper,
            JsonService jsonService,
            PlatformTrendService platformTrendService,
            ComboDiscoveryService comboDiscoveryService,
            MemeQuoteService memeQuoteService,
            FanLetterService fanLetterService,
            DayFlowService dayFlowService,
            StageObjectiveService stageObjectiveService,
            BalanceConfig balanceConfig,
            OperatingPressureService operatingPressureService,
            RouteIdentityService routeIdentityService
    ) {
        this.businessLogMapper = businessLogMapper;
        this.dailyReportMapper = dailyReportMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.jsonService = jsonService;
        this.platformTrendService = platformTrendService;
        this.comboDiscoveryService = comboDiscoveryService;
        this.memeQuoteService = memeQuoteService;
        this.fanLetterService = fanLetterService;
        this.dayFlowService = dayFlowService;
        this.stageObjectiveService = stageObjectiveService;
        this.balanceConfig = balanceConfig;
        this.operatingPressureService = operatingPressureService;
        this.routeIdentityService = routeIdentityService;
    }

    public DailyReport createReport(Vup vup, DaySession session, ActionResultDTO actionResult, BusinessLog log) {
        return createReport(vup, session, actionResult, log, null);
    }

    public DailyReport createReport(
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            BusinessLog log,
            String selectedTitle
    ) {
        return createReport(vup, session, actionResult, log, selectedTitle, List.of());
    }

    public DailyReport createReport(
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            BusinessLog log,
            String selectedTitle,
            List<Map<String, Object>> extraEvidenceRefs
    ) {
        ReportFlavor flavor = reportFlavorFor(actionResult);
        ReportDelta reportDelta = reportDeltaFor(vup.getId(), session.getDay(), actionResult, log);
        boolean contentAnxiety = consecutiveDefensiveAction(vup, session, actionResult);
        String commercialText = reportDelta.commercialChange() == 0
                ? ""
                : "，商业化" + signed(reportDelta.commercialChange());
        String coinText = reportDelta.coinChange() == 0
                ? ""
                : "，运营预算" + signed(reportDelta.coinChange());
        String inspirationText = reportDelta.inspirationChange() == 0
                ? ""
                : "，灵感" + signed(reportDelta.inspirationChange());
        String baseRouteHint = contentAnxiety
                ? "路线提示：连续防守触发内容焦虑，标题组开始担心素材断供，老粉也在问什么时候正常营业。"
                : flavor.routeHint();
        String routeHint = baseRouteHint + watchHeatExplanation(actionResult, log);
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        String riskHint = contentAnxiety
                ? "内容焦虑正在升温，明天最好安排直播、视频或切片，别让装死变成查无此V前奏。"
                : openDebtRiskHint(vup, openDebts);
        String finalVisibleHint = stageReviewFor(vup, session)
                .map(stageReview -> stageReview + " " + flavor.nextHint())
                .orElse(flavor.nextHint());
        List<String> visibleItems = new java.util.ArrayList<>(List.of(
                "数据变化：粉丝+" + reportDelta.fanChange()
                        + "，体力" + signed(reportDelta.staminaChange())
                        + "，人气" + signed(reportDelta.popularityChange())
                        + "，围观热度" + signed(reportDelta.watchHeatChange())
                        + "，口碑" + signed(reportDelta.reputationChange())
                        + "，梗浓度" + signed(reportDelta.memeChange())
                        + coinText
                        + commercialText
                        + inspirationText + "。",
                "主行动：" + nonRepeatingMainAction(actionResult.summary(), session.getDay()),
                selectedTitle == null ? "今日标题：无标题行动，标题组暂时待机。" : "今日标题：" + selectedTitle,
                routeHint,
                finalVisibleHint
        ));
        // 路线三件套每日化：每天日报都注入当前路线的身份卡片（想要/代价/下一手）。
        // Day 7/15/23 的阶段爆点保留 stageMilestoneFor 生成的专属文案，日常日子靠这张卡片补足路线身份感。
        String routeIdentityCard = routeIdentityService.dailyIdentityCard(vup.getCurrentRoute());
        if (!routeIdentityCard.isEmpty()) {
            int routeHintIndex = visibleItems.indexOf(routeHint);
            if (routeHintIndex >= 0) {
                visibleItems.add(routeHintIndex + 1, routeIdentityCard);
            } else {
                visibleItems.add(routeIdentityCard);
            }
        }
        String materialReceipt = materialStockReceiptLine(vup, actionResult);
        if (materialReceipt != null) {
            visibleItems.add(2, materialReceipt);
        }
        String eventChoiceReceipt = eventChoiceReceiptLine(log);
        if (eventChoiceReceipt != null) {
            visibleItems.add(2, eventChoiceReceipt);
        }
        List<ComboItemDTO> comboHits = comboDiscoveryService.reportHits(vup, log);
        comboHits.stream()
                .map(combo -> "组合技提示：发现【" + combo.label() + "】！" + combo.evidence() + " " + combo.hint())
                .forEach(visibleItems::add);

        // Phase 5: 添加运营脑压力旁白
        String pressureNarrative = operatingPressureService.pressureNarrative(vup, session.getDay());
        if (pressureNarrative != null) {
            visibleItems.add(pressureNarrative);
        }

        // 添加梗语录
        String memeQuote = memeQuoteService.getQuoteForAction(actionResult.actionType(), session);
        visibleItems.add("场外声音：" + memeQuote);

        // 添加粉丝来信（按 4 种粉丝类型加权抽取）
        var fanLetter = fanLetterService.getRandomLetter(
                vup.getTrueFans(), vup.getFunFans(), vup.getUnicornFans(), vup.getDdFans(),
                vup.getReputation(), session);
        visibleItems.add("粉丝来信【" + fanLetter.fanType() + "】" + fanLetter.fanName() + "：" + fanLetter.content());
        visibleItems.add("收益复盘：" + payoffReason(actionResult, log) + "。");
        visibleItems.add("代价复盘：" + costReason(reportDelta, actionResult, log, openDebts) + "。");
        visibleItems.add("路线证据：" + routeEvidenceLine(vup, actionResult, log) + "。");
        visibleItems.add("旧账前台：" + debtFrontDeskLine(openDebts, session.getDay()) + "。");
        visibleItems.add(tacticalReviewLine(vup, actionResult, log, openDebts));

        List<Map<String, Object>> evidenceRefs = new java.util.ArrayList<>();
        evidenceRefs.add(Map.of("type", "business_log", "id", log.getId(), "day", session.getDay(), "action", actionResult.actionType()));
        if (log.getPlanId() != null) {
            evidenceRefs.add(Map.of(
                    "type", "stream_plan",
                    "id", log.getPlanId()
            ));
        }
        if (log.getTitleTemplateId() != null) {
            Map<String, Object> titleRef = new java.util.LinkedHashMap<>();
            titleRef.put("type", "title_template");
            titleRef.put("id", log.getTitleTemplateId());
            if (selectedTitle != null) {
                titleRef.put("title", selectedTitle);
            }
            evidenceRefs.add(titleRef);
        }
        accidentMaterialRefs(log).forEach(evidenceRefs::add);
        extraEvidenceRefs.stream()
                .filter(ref -> ref != null && !ref.isEmpty())
                .forEach(ref -> evidenceRefs.add(new java.util.LinkedHashMap<>(ref)));
        comboHits.forEach(combo -> evidenceRefs.add(Map.of(
                "type", "combo",
                "comboKey", combo.comboKey(),
                "label", combo.label(),
                "tone", combo.tone(),
                "evidence", combo.evidence()
        )));
        openDebts.forEach(debt -> evidenceRefs.add(Map.of(
                "type", "risk_debt",
                "id", debt.getId(),
                "debtType", debt.getDebtType(),
                "status", debt.getStatus(),
                "severity", debt.getSeverity(),
                "dueDay", debt.getDueDay()
        )));
        stageMilestoneFor(vup, session, actionResult, log, openDebts).ifPresent(milestone -> {
            evidenceRefs.add(milestone);
            visibleItems.add("阶段爆点：" + milestone.get("title") + "｜" + milestone.get("spokenLine"));
        });
        String debtSummary = debtSummaryFor(vup, log, openDebts);
        List<String> highlights = reportHighlights(actionResult, log, reportDelta, visibleItems, debtSummary, riskHint);

        DailyReport report = new DailyReport();
        report.setVupId(vup.getId());
        report.setDay(session.getDay());
        report.setSummary(nonRepeatingSummary(flavor.summary(), session.getDay()));
        report.setSelectedTitle(selectedTitle);
        report.setReportTone(flavor.tone());
        report.setPlatformTrendId(platformTrendService.trendIdForDay(session.getDay()));
        report.setFanDelta(reportDelta.fanChange());
        report.setCoinDelta(reportDelta.coinChange());
        report.setPopularityDelta(reportDelta.popularityChange());
        report.setWatchHeatDelta(reportDelta.watchHeatChange());
        report.setReputationDelta(reportDelta.reputationChange());
        report.setMemeDelta(reportDelta.memeChange());
        report.setCommercialDelta(reportDelta.commercialChange());
        report.setDebtSummary(debtSummary);
        report.setRiskHint(riskHint);
        report.setVisibleItemsJson(jsonService.write(visibleItems));
        report.setEvidenceRefsJson(jsonService.write(evidenceRefs));
        report.setStrategyPanelJson(jsonService.write(Map.of("highlights", highlights)));
        boolean hasStageMilestone = evidenceRefs.stream()
                .anyMatch(ref -> "stage_milestone".equals(ref.get("type")));
        report.setTemplateRefsJson(jsonService.write(hasStageMilestone
                ? List.of("P0_SIMPLE_ACTION_REPORT", "STAGE_MILESTONE_V1")
                : List.of("P0_SIMPLE_ACTION_REPORT")));
        report.setRenderVersion("p0-simple-v1");
        dailyReportMapper.insert(report);
        return report;
    }

    private ReportDelta reportDeltaFor(Long vupId, int day, ActionResultDTO currentResult, BusinessLog currentLog) {
        List<BusinessLog> dayLogs = shouldAggregateDayDelta(currentResult)
                ? businessLogMapper.findByVupIdAndDay(vupId, day)
                : List.of(currentLog);
        if (dayLogs.isEmpty()) {
            dayLogs = List.of(currentLog);
        }
        return new ReportDelta(
                dayLogs.stream().mapToInt(BusinessLog::getFanChange).sum(),
                dayLogs.stream().mapToInt(log -> netStaminaChange(log, currentResult, currentLog)).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getPopularityChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getWatchHeatChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getReputationChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getMemeChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getCommercialChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getCoinChange).sum(),
                dayLogs.stream().mapToInt(BusinessLog::getInspirationChange).sum()
        );
    }

    private boolean shouldAggregateDayDelta(ActionResultDTO currentResult) {
        if (!"EVENT_CHOICE".equals(currentResult.actionType()) || currentResult.evidenceRef() == null) {
            return false;
        }
        return "ordinary_event".equals(String.valueOf(currentResult.evidenceRef().get("type")));
    }

    private int netStaminaChange(BusinessLog log, ActionResultDTO currentResult, BusinessLog currentLog) {
        if (log.getId() != null
                && currentLog.getId() != null
                && log.getId().equals(currentLog.getId())) {
            return currentResult.staminaChange();
        }
        return inferredStaminaChange(log.getAction());
    }

    private int inferredStaminaChange(String actionType) {
        return switch (actionType == null ? "" : actionType) {
            case "TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK", "FAN_GROUP_MAINTAIN" -> -2;
            case "PUBLISH_VIDEO" -> -4;
            case "STREAM_PLAN", "NPC_INTERACT" -> -3;
            case "PUBLISH_CLIP" -> -1;
            case "REST" -> balanceConfig.restStaminaRecovery();
            default -> 0;
        };
    }

    private List<Map<String, Object>> accidentMaterialRefs(BusinessLog log) {
        if (log.getAccidentMaterialIds() == null
                || log.getAccidentMaterialIds().isBlank()
                || "[]".equals(log.getAccidentMaterialIds())) {
            return List.of();
        }
        return jsonService.readStringList(log.getAccidentMaterialIds()).stream()
                .filter(materialId -> materialId != null && !materialId.isBlank())
                .distinct()
                .map(materialId -> {
                    Map<String, Object> ref = new java.util.LinkedHashMap<>();
                    ref.put("type", "accident_material");
                    ref.put("materialId", materialId);
                    ref.put("label", accidentMaterialLabel(materialId));
                    ref.put("sourceType", accidentMaterialSourceType(log));
                    ref.put("sourceAction", log.getAction());
                    ref.put("day", log.getDay());
                    return ref;
                })
                .toList();
    }

    private String materialStockReceiptLine(Vup vup, ActionResultDTO actionResult) {
        if (actionResult.actionType() == null || actionResult.actionType().isBlank()) {
            return null;
        }
        int materialStockAfter = businessLogMapper.countMaterialStockByVupId(vup.getId());
        return switch (actionResult.actionType()) {
            case "PUBLISH_VIDEO" -> "素材小票：切片组弹药库 +2，现有" + materialStockAfter + "份；评论区开始给录播组递刀叉。";
            case "FAN_GROUP_MAINTAIN" -> "素材小票：群友递来投稿素材 +1，现有" + materialStockAfter + "份；标题组从空白文档里活过来。";
            case "PUBLISH_CLIP" -> "素材小票：切片组出餐 -1，现有" + materialStockAfter + "份；同梗复读过多时，查重味会上来。";
            default -> null;
        };
    }

    private String eventChoiceReceiptLine(BusinessLog log) {
        if (!"EVENT_CHOICE".equals(log.getAction())
                || log.getEventId() == null
                || log.getRouteScoreChange() == null
                || log.getRouteScoreChange().isBlank()) {
            return null;
        }
        Map<String, Object> routeData = jsonService.readMap(log.getRouteScoreChange());
        String choiceType = String.valueOf(routeData.getOrDefault("choiceType", "safe"));
        String debtType = String.valueOf(routeData.getOrDefault("debtType", routeData.getOrDefault("eventKey", "")));
        String debtLabel = debtTypeLabel(debtType);
        String rolloverPreview = eventChoiceRolloverPreview(log);
        return switch (choiceType) {
            case "traffic" -> "事件分支：traffic 硬接流量，" + debtLabel
                    + "旧账已处理，但收益换来了" + rolloverPreview + "，日报按人气、围观和口碑真实入账。";
            case "meme" -> "事件分支：meme 顺势玩梗，" + debtLabel
                    + "旧账已处理，但切片传播留下" + rolloverPreview + "，日报按梗浓度和围观热度真实入账。";
            default -> "事件分支：safe 降温处理，" + debtLabel
                    + "旧账已清，收益较低但风险没有免费滚到明天。";
        };
    }

    private String eventChoiceRolloverPreview(BusinessLog log) {
        if (log.getDebtIds() == null || log.getDebtIds().isBlank() || "[]".equals(log.getDebtIds())) {
            return "新短期旧账";
        }
        return jsonService.readLongList(log.getDebtIds()).stream()
                .map(riskDebtMapper::findById)
                .filter(java.util.Objects::nonNull)
                .filter(debt -> "OPEN".equals(debt.getStatus()))
                .findFirst()
                .map(debt -> "新短期旧账（严重度" + debt.getSeverity() + "，第" + debt.getDueDay() + "天回流）")
                .orElse("新短期旧账");
    }

    private String accidentMaterialSourceType(BusinessLog log) {
        if (log.getEventId() != null) {
            return "risk_debt";
        }
        if (log.getInteractionEventId() != null) {
            return "live_interaction";
        }
        if (log.getRiskToolId() != null) {
            return "risk_tool";
        }
        return "business_log";
    }

    private String accidentMaterialLabel(String materialId) {
        return switch (materialId) {
            case "BOOMERANG_CLIP_CONTEXT" -> "回旋镖切片上下文";
            case "UNICORN_EXPECTATION_SCREENSHOT" -> "独角兽期待截图";
            case "COMMERCIAL_BACKLASH_RECEIPT" -> "商业反噬留档";
            case "TITLE_BACKFIRE_CONTEXT" -> "标题党反噬上下文";
            default -> "未署名事故素材";
        };
    }

    private String nonRepeatingSummary(String summary, int day) {
        return switch ((day - 1) % 3) {
            case 1 -> summary + " 日报组换了个角度：今天重点看观众是否真的留下，而不是只看热闹有没有来。";
            case 2 -> summary + " 日报组继续换角度：今天重点看路线证据有没有累积，避免三天都像复制粘贴。";
            default -> summary;
        };
    }

    private String nonRepeatingMainAction(String summary, int day) {
        return switch ((day - 1) % 3) {
            case 1 -> summary + " 复盘补充：今天重点看观众是否真的留下。";
            case 2 -> summary + " 复盘补充：路线证据继续累积，避免练习日志像复制粘贴。";
            default -> summary;
        };
    }

    private String openDebtRiskHint(Vup vup, List<RiskDebt> openDebts) {
        if (!openDebts.isEmpty()) {
            RiskDebt nextDebt = openDebts.stream()
                    .min(java.util.Comparator
                            .comparingInt((RiskDebt debt) -> Math.max(0, debt.getDueDay() - vup.getDayCount()))
                            .thenComparing(java.util.Comparator.comparingInt(RiskDebt::getSeverity).reversed()))
                    .orElse(openDebts.get(0));
            return "旧账压力：" + debtTypeLabel(nextDebt.getDebtType())
                    + "严重度" + nextDebt.getSeverity()
                    + "，剩余" + RiskDebtPresenter.remainingDays(nextDebt, vup.getDayCount()) + "天"
                    + "，第" + nextDebt.getDueDay()
                    + "天回流。" + RiskDebtPresenter.sourceLine(nextDebt)
                    + "。后果预览：" + RiskDebtPresenter.consequencePreview(nextDebt, vup.getDayCount())
                    + "。推荐：" + RiskDebtPresenter.recommendedAction(nextDebt, vup.getDayCount()) + "。";
        }
        if (hasRouteInstability(vup)) {
            return "路线摇摆预警：最近频繁切换路线，老粉和DD都在问你到底想做哪类V，期待落差可能导致粉丝流失。";
        }
        return "明天可以稳一点，先别让高音贷款变成回旋镖。";
    }

    private String tacticalReviewLine(Vup vup, ActionResultDTO actionResult, BusinessLog log, List<RiskDebt> openDebts) {
        return "结算看板：收益：" + payoffReason(actionResult, log)
                + "；代价/旧账：" + riskReason(openDebts, actionResult, log)
                + "；路线：" + routeEvidenceLine(vup, actionResult, log)
                + "；明天：" + tomorrowActionHint(vup, openDebts, actionResult, log) + "。";
    }

    private List<String> reportHighlights(
            ActionResultDTO actionResult,
            BusinessLog log,
            ReportDelta reportDelta,
            List<String> visibleItems,
            String debtSummary,
            String riskHint
    ) {
        return List.of(
                "今天：" + actionResult.summary(),
                "数值：" + actionDeltaLine(actionResult, log, reportDelta),
                gainDebtHighlight(visibleItems, debtSummary),
                "明天风险：" + riskHint
        );
    }

    private String actionDeltaLine(ActionResultDTO actionResult, BusinessLog log, ReportDelta reportDelta) {
        List<String> pieces = new java.util.ArrayList<>();
        pieces.add("粉丝" + signed(reportDelta.fanChange()));
        pieces.add("体力" + signed(reportDelta.staminaChange()));
        pieces.add("人气" + signed(reportDelta.popularityChange()));
        pieces.add("围观" + signed(reportDelta.watchHeatChange()));
        pieces.add("口碑" + signed(reportDelta.reputationChange()));
        pieces.add("梗" + signed(reportDelta.memeChange()));
        if (reportDelta.coinChange() != 0) {
            pieces.add("运营预算" + signed(reportDelta.coinChange()));
        }
        if (reportDelta.commercialChange() != 0) {
            pieces.add("商业化" + signed(reportDelta.commercialChange()));
        }
        if (reportDelta.inspirationChange() != 0) {
            pieces.add("灵感" + signed(reportDelta.inspirationChange()));
        }
        return String.join("，", pieces);
    }

    private String costReason(ReportDelta reportDelta, ActionResultDTO actionResult, BusinessLog log, List<RiskDebt> openDebts) {
        List<String> pieces = new java.util.ArrayList<>();
        if (reportDelta.staminaChange() < 0) {
            pieces.add("体力" + signed(reportDelta.staminaChange()));
        }
        if (reportDelta.reputationChange() < 0) {
            pieces.add("口碑" + signed(reportDelta.reputationChange()));
        }
        if (reportDelta.watchHeatChange() > 5 && actionResult.fanChange() * 3 < Math.max(1, log.getPopularityChange())) {
            pieces.add("围观多但转粉薄");
        }
        if (reportDelta.coinChange() < 0) {
            pieces.add("运营预算" + signed(reportDelta.coinChange()));
        }
        if (reportDelta.inspirationChange() < 0) {
            pieces.add("灵感" + signed(reportDelta.inspirationChange()));
        }
        if (!openDebts.isEmpty()) {
            RiskDebt debt = highestPriorityDebt(openDebts, log.getDay());
            pieces.add(debtTypeLabel(debt.getDebtType()) + "旧账仍在计时");
        }
        if (pieces.isEmpty()) {
            return "今天没有新增明显代价，但低风险不等于免费收益";
        }
        return String.join("，", pieces);
    }

    private String routeEvidenceLine(Vup vup, ActionResultDTO actionResult, BusinessLog log) {
        if (actionResult.routeScoreChange() > 0) {
            return dayFlowService.routeLabel(vup.getCurrentRoute()) + "证据+" + actionResult.routeScoreChange()
                    + "，来源是" + dayFlowService.actionLabel(actionResult.actionType());
        }
        if (log.getRouteScoreChange() != null && !log.getRouteScoreChange().isBlank() && !"{}".equals(log.getRouteScoreChange())) {
            return "路线证据已写入日志，明天看结局预演是否转绿";
        }
        return "本手偏防守或资源调整，不直接加厚路线，明天最好接一个路线行动";
    }

    private String debtFrontDeskLine(List<RiskDebt> openDebts, int currentDay) {
        if (openDebts.isEmpty()) {
            return "暂无未结清旧账，明天可以更主动补路线证据";
        }
        RiskDebt debt = highestPriorityDebt(openDebts, currentDay);
        return debtTypeLabel(debt.getDebtType())
                + "，严重度" + debt.getSeverity()
                + "，剩余" + RiskDebtPresenter.remainingDays(debt, currentDay) + "天"
                + "，" + RiskDebtPresenter.sourceLine(debt)
                + "，推荐：" + RiskDebtPresenter.recommendedAction(debt, currentDay);
    }

    private RiskDebt highestPriorityDebt(List<RiskDebt> openDebts, int currentDay) {
        return openDebts.stream()
                .min(java.util.Comparator
                        .comparingInt((RiskDebt debt) -> Math.max(0, debt.getDueDay() - currentDay))
                        .thenComparing(java.util.Comparator.comparingInt(RiskDebt::getSeverity).reversed()))
                .orElse(openDebts.get(0));
    }

    private String gainDebtHighlight(List<String> visibleItems, String debtSummary) {
        String gain = firstLineAfterPrefix(visibleItems, "素材小票：");
        if (gain == null) {
            gain = firstLineAfterPrefix(visibleItems, "组合技提示：");
        }
        if (gain == null) {
            gain = firstLineAfterPrefix(visibleItems, "事件分支：");
        }
        if (gain == null) {
            gain = "没有新增道具，主要收获写进路线证据";
        }
        return "获得/欠账：" + gain + "；" + compactDebtSummary(debtSummary);
    }

    private String firstLineAfterPrefix(List<String> visibleItems, String prefix) {
        return visibleItems.stream()
                .filter(item -> item != null && item.startsWith(prefix))
                .map(item -> item.substring(prefix.length()))
                .findFirst()
                .orElse(null);
    }

    private String compactDebtSummary(String debtSummary) {
        if (debtSummary == null || debtSummary.isBlank()) {
            return "欠账暂无";
        }
        if (debtSummary.startsWith("暂无未结清债务")) {
            return "欠账暂无";
        }
        if (debtSummary.startsWith("未结清旧账：")) {
            return "欠账：" + firstSummaryPart(debtSummary.substring("未结清旧账：".length()));
        }
        if (debtSummary.startsWith("今日已处理旧账：")) {
            return "清账：" + firstSummaryPart(debtSummary.substring("今日已处理旧账：".length()));
        }
        return debtSummary;
    }

    private String firstSummaryPart(String summary) {
        int separator = summary.indexOf('；');
        if (separator < 0) {
            return summary;
        }
        return summary.substring(0, separator);
    }

    private String payoffReason(ActionResultDTO actionResult, BusinessLog log) {
        if (actionResult.routeScoreChange() > 0) {
            return "路线证据+" + actionResult.routeScoreChange() + "，" + dayFlowService.actionLabel(actionResult.actionType()) + "已经写进结局材料";
        }
        if (actionResult.fanChange() > 0) {
            return "粉丝+" + actionResult.fanChange() + "，低压内容把观众留住了一部分";
        }
        if (log.getReputationChange() > 0 || actionResult.staminaChange() > 0) {
            return "口碑或体力回稳，明天有余地继续营业";
        }
        return "至少完成了一次排班，日报有真实行动可复盘";
    }

    private String riskReason(List<RiskDebt> openDebts, ActionResultDTO actionResult, BusinessLog log) {
        if (!openDebts.isEmpty()) {
            RiskDebt debt = highestPriorityDebt(openDebts, log.getDay());
            return debtTypeLabel(debt.getDebtType()) + "还没结清，严重度" + debt.getSeverity()
                    + "，剩余" + RiskDebtPresenter.remainingDays(debt, log.getDay())
                    + "天，第" + debt.getDueDay() + "天会回流";
        }
        if (log.getReputationChange() < 0) {
            return "口碑" + signed(log.getReputationChange()) + "，高热度正在消耗老粉耐心";
        }
        if (actionResult.staminaChange() < -2) {
            return "体力" + signed(actionResult.staminaChange()) + "，连续硬冲会压低后续收益";
        }
        if (log.getWatchHeatChange() > 5 && actionResult.fanChange() * 3 < Math.max(1, log.getPopularityChange())) {
            return "围观来得多，真正留下的人还不够";
        }
        return "没有新增大旧账，但别把低风险当作免费收益";
    }

    private String tomorrowActionHint(Vup vup, List<RiskDebt> openDebts, ActionResultDTO actionResult, BusinessLog log) {
        if (!openDebts.isEmpty()) {
            return "优先杂谈复盘、粉丝群维护或米线工具，把最高风险压住再冲";
        }
        if (actionResult.routeScoreChange() > 0) {
            return "继续沿" + dayFlowService.routeLabel(vup.getCurrentRoute()) + "补一手证据，不要临时换线";
        }
        if (actionResult.staminaChange() > 0) {
            return "体力回来了，接一张路线推进卡，把防守回合转成证据";
        }
        if (log.getWatchHeatChange() > 5) {
            return "热度在场，明天要么切片收割，要么先做复盘降温";
        }
        return "看结局预演缺口，选路线推进或稳妥营业";
    }

    private String watchHeatExplanation(ActionResultDTO actionResult, BusinessLog log) {
        if (log.getWatchHeatChange() < 10 || log.getPopularityChange() < 80) {
            return "";
        }
        if (actionResult.fanChange() * 3 >= log.getPopularityChange()) {
            return "";
        }
        return " 围观很多，但真正留下的不多：楼友主要是来坐主会场，转粉只留下了一部分乐子人和DD。";
    }

    private boolean hasRouteInstability(Vup vup) {
        List<BusinessLog> recentLogs = businessLogMapper.findEndingReferenceLogs(vup.getId());
        if (recentLogs.size() < 4) {
            return false;
        }
        // 查看最近4天是否频繁切换路线。
        List<BusinessLog> last4 = recentLogs.subList(Math.max(0, recentLogs.size() - 4), recentLogs.size());
        long routeChanges = 0;
        for (int i = 1; i < last4.size(); i++) {
            String prevRoute = routeFromLog(last4.get(i - 1));
            String currRoute = routeFromLog(last4.get(i));
            if (!prevRoute.equals(currRoute) && !"UNKNOWN".equals(prevRoute)) {
                routeChanges++;
            }
        }
        return routeChanges >= 2;
    }

    private String routeFromLog(BusinessLog log) {
        if (log.getRouteScoreChange() == null || log.getRouteScoreChange().isBlank()) {
            return "UNKNOWN";
        }
        try {
            Map<String, Object> routeData = jsonService.readMap(log.getRouteScoreChange());
            for (var entry : routeData.entrySet()) {
                if ("source".equals(entry.getKey())) continue;
                Object val = entry.getValue();
                if (val instanceof Number num && num.intValue() > 0) {
                    return entry.getKey();
                }
                // 兼容 JSON 解析后得到的字符串数字。
                if (val instanceof String str) {
                    try {
                        if (Integer.parseInt(str) > 0) return entry.getKey();
                    } catch (NumberFormatException ignored) {}
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String debtSummaryFor(Vup vup, BusinessLog log, List<RiskDebt> openDebts) {
        if (!openDebts.isEmpty()) {
            return "未结清旧账：" + String.join("；", openDebts.stream()
                    .map(this::openDebtSummary)
                    .toList());
        }

        if (log.getDebtIds() != null && !"[]".equals(log.getDebtIds())) {
            List<String> handledDebts = jsonService.readLongList(log.getDebtIds()).stream()
                    .map(riskDebtMapper::findById)
                    .filter(java.util.Objects::nonNull)
                    .map(debt -> debtTypeLabel(debt.getDebtType()) + "（" + debtStatusLabel(debt.getStatus()) + "）")
                    .toList();
            if (!handledDebts.isEmpty()) {
                return "今日已处理旧账：" + String.join("；", handledDebts);
            }
        }

        return "暂无未结清债务，录播组今天没拿到大材料。";
    }

    private String openDebtSummary(RiskDebt debt) {
        String source = debt.getSourceTitle() == null || debt.getSourceTitle().isBlank()
                ? "未署名素材"
                : "《" + debt.getSourceTitle() + "》";
        return debtTypeLabel(debt.getDebtType())
                + "（严重度" + debt.getSeverity()
                + "，第" + debt.getDueDay() + "天回流，来源" + source
                + "，" + debtCopy(debt.getSummary())
                + "）";
    }

    private String debtTypeLabel(String debtType) {
        return switch (debtType) {
            case "TITLE_BACKFIRE" -> "标题党反噬";
            case "BOOMERANG_CLIP" -> "回旋镖切片";
            case "UNICORN_EXPECTATION" -> "独角兽期待";
            case "COMMERCIAL_BACKLASH" -> "商业反噬";
            default -> "未知旧账";
        };
    }

    private String debtStatusLabel(String status) {
        return switch (status) {
            case "OPEN" -> "发酵中";
            case "CLEARED" -> "已降温";
            default -> "待观察";
        };
    }

    private String debtCopy(String summary) {
        if (summary == null || summary.isBlank()) {
            return "录播组还在补上下文";
        }
        return summary
                .replace("TITLE_BACKFIRE", "标题党反噬")
                .replace("BOOMERANG_CLIP", "回旋镖切片")
                .replace("UNICORN_EXPECTATION", "独角兽期待")
                .replace("COMMERCIAL_BACKLASH", "商业反噬")
                .replace("FAN_SERVICE", "陪伴营业")
                .replace("BUSINESS_SAFE", "稳健处理模拟礼物")
                .replace("PUBLISH_CLIP", "发布切片");
    }

    private boolean consecutiveDefensiveAction(Vup vup, DaySession session, ActionResultDTO actionResult) {
        if (session.getDay() <= 1 || !isDefensiveAction(actionResult.actionType())) {
            return false;
        }
        BusinessLog previous = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), session.getDay() - 1);
        return previous != null && isDefensiveAction(previous.getAction());
    }

    private boolean isDefensiveAction(String actionType) {
        return "REST".equals(actionType)
                || "TRAIN_TALK".equals(actionType)
                || "FAN_GROUP_MAINTAIN".equals(actionType);
    }

    public DailyReportDTO todayReport(Vup vup) {
        DailyReport report = dailyReportMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (report == null) {
            throw new GameException("REPORT_NOT_FOUND", "今日日报还没生成，主播不能虚空复盘。");
        }
        return toDto(report);
    }

    public List<DailyReportDTO> historyReports(Vup vup) {
        return dailyReportMapper.findAllByVupId(vup.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    private DailyReportDTO toDto(DailyReport report) {
        List<String> visibleItems = jsonService.readStringList(report.getVisibleItemsJson());
        List<Map<String, Object>> evidenceRefs = jsonService.readEvidenceRefs(report.getEvidenceRefsJson());
        return new DailyReportDTO(
                report.getId(),
                report.getDay(),
                report.getSummary(),
                report.getSelectedTitle(),
                Map.of(
                        "fanDelta", report.getFanDelta(),
                        "coinDelta", report.getCoinDelta(),
                        "popularityDelta", report.getPopularityDelta(),
                        "watchHeatDelta", report.getWatchHeatDelta(),
                        "reputationDelta", report.getReputationDelta(),
                        "memeDelta", report.getMemeDelta(),
                        "commercialDelta", report.getCommercialDelta()
                ),
                platformTrendService.trendFor(report.getPlatformTrendId()),
                dtoHighlights(report, visibleItems),
                visibleItems,
                evidenceRefs,
                report.getDebtSummary(),
                report.getRiskHint(),
                DayPhaseStateMachine.canStartNextDay(DayPhase.REPORT_READY, report.getDay(), balanceConfig.maxDay())
        );
    }

    private List<String> dtoHighlights(DailyReport report, List<String> visibleItems) {
        List<String> storedHighlights = storedHighlights(report.getStrategyPanelJson());
        if (storedHighlights.size() >= 3) {
            return storedHighlights.stream().limit(5).toList();
        }
        return List.of(
                "今天：" + report.getSummary(),
                "数值：" + reportDeltaLine(report),
                gainDebtHighlight(visibleItems, report.getDebtSummary()),
                "明天风险：" + report.getRiskHint()
        );
    }

    private List<String> storedHighlights(String strategyPanelJson) {
        if (strategyPanelJson == null || strategyPanelJson.isBlank() || "{}".equals(strategyPanelJson)) {
            return List.of();
        }
        Object raw;
        try {
            raw = jsonService.readMap(strategyPanelJson).get("highlights");
        } catch (IllegalStateException ignored) {
            return List.of();
        }
        if (!(raw instanceof List<?> rawHighlights)) {
            return List.of();
        }
        return rawHighlights.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private String reportDeltaLine(DailyReport report) {
        return "粉丝" + signed(report.getFanDelta())
                + "，运营预算" + signed(report.getCoinDelta())
                + "，人气" + signed(report.getPopularityDelta())
                + "，围观" + signed(report.getWatchHeatDelta())
                + "，口碑" + signed(report.getReputationDelta())
                + "，梗" + signed(report.getMemeDelta())
                + "，商业化" + signed(report.getCommercialDelta());
    }

    private ReportFlavor reportFlavorFor(ActionResultDTO actionResult) {
        String actionType = actionResult.actionType();
        String summary = actionResult.summary();
        if ("PUBLISH_CLIP".equals(actionType)) {
            // T12：日报中区分梗生命周期。
            if (summary.contains("回旋期")) {
                return new ReportFlavor(
                        "warning",
                        "路线提示：这梗已经进入回旋期，切片组建议换素材，再复读下去只剩围观没有转化，录播组开始写复盘而不是剪新片。",
                        "明日建议：必须换新梗或新企划，回旋期的素材只会堆围观热度和债务，不会涨粉。",
                        "今天发了切片，但这梗已经进入回旋期，切片组开始写复盘而不是剪新素材，围观热度还在但转化几乎为零。"
                );
            }
            if (summary.contains("复读")) {
                return new ReportFlavor(
                        "funny",
                        "路线提示：这梗已经被复读过了，乐子人进场速度变慢，切片组建议换新素材避免查重。",
                        "明日建议：可以再发一次但收益会继续递减，建议穿插其他内容或换新梗。",
                        "今天发了切片，但这梗已经复读过了，乐子人觉得没有新意，切片组调了一下时间轴就收工了。"
                );
            }
            return new ReportFlavor(
                    "funny",
                    "路线提示：切片路线开始成型，切片组比排班表更准时，乐子人正在给素材编号。",
                    "明日建议：可以继续供货，但别让同一个梗复读到被楼里查重。",
                    "今天发了切片，切片组开工，乐子人进场，主播本人负责提供原材料。"
            );
        }
        if ("PUBLISH_VIDEO".equals(actionType)) {
            return new ReportFlavor(
                    "funny",
                    "路线提示：视频引流开始起效，平台口味给了一点风，切片组先把素材存进弹药库。",
                    "明日建议：可以接直播企划吃视频引流，也可以继续发切片，但别把标题党写成诈骗简历。",
                    "今天发布视频蹭到平台口味，评论区开始给直播间导流，切片路线有了新证据。"
            );
        }
        if ("STREAM_PLAN".equals(actionType)
                && (summary.contains("FAN_SERVICE") || summary.contains("醒目留言") || summary.contains("高亮互动") || summary.contains("谢SC") || summary.contains("商业"))) {
            return new ReportFlavor(
                    "business",
                    "路线提示：商业化和陪伴期待开始升温，榜一排班表有点太像真排班表了。",
                    "明日建议：可以继续醒目留言回应，但最好穿插低压内容降气压，别把老粉熬成沉默观众。",
                    "今天开了醒目留言陪伴回，商业味上桌，独角兽和榜一同时开始认真记日程。"
            );
        }
        if ("STREAM_PLAN".equals(actionType)
                && (summary.contains("HARD_MOUTH") || summary.contains("ABSTRACT_MEME"))) {
            return new ReportFlavor(
                    "trial",
                    "路线提示：黑红主会场开始升温，标题组把高风险标题写进备忘录，楼友已经在等回旋镖。",
                    "明日建议：可以用杂谈复盘或粉丝群维护降温，不然标题债务会继续发酵。",
                    "今天开了高风险直播，标题组和楼友同时进场，主会场热度上来了，但口碑开始承压。"
            );
        }
        if ("NPC_INTERACT".equals(actionType)) {
            return new ReportFlavor(
                    "funny",
                    "路线提示：社交联动路线开始冒头，DD坐了一站，独角兽也开始看互动秒数。",
                    "明日建议：可以继续轻联动，但记得端水，别把公交站开成修罗场。",
                    "今天完成了同台互动，同行动态轻轻飘过来，DD顺手坐了一站。"
            );
        }
        if ("REST".equals(actionType) || "FAN_GROUP_MAINTAIN".equals(actionType) || "TRAIN_TALK".equals(actionType)) {
            // T14：解释防守行动为什么降低风险。
            String riskExplanation = "REST".equals(actionType)
                    ? "路线提示：稳健低压路线在保温，休息恢复了体力缓冲，降低了明天翻车的概率，老粉暂时不用写小作文。"
                    : "FAN_GROUP_MAINTAIN".equals(actionType)
                    ? "路线提示：稳健低压路线在保温，粉丝群维护降低了小作文爆发风险，群内气压暂时稳定。"
                    : "路线提示：稳健低压路线在保温，杂谈复盘清理了标题债务隐患，录播组暂时没有开庭素材。";
            return new ReportFlavor(
                    "safe",
                    riskExplanation,
                    "明日建议：可以继续防守，但连续装死也会让标题组没活可交。",
                    "今天选择了低压处理，没大爆，也没翻车，某种意义上赢很大。"
            );
        }
        return new ReportFlavor(
                "safe",
                "路线提示：歌势路线开始冒头，标题组暂时还没找到开庭材料。",
                "明日建议：可以继续练基本功，也可以尝试低压歌回，别一上来就贷款高音。",
                "今天完成了" + dayFlowService.actionLabel(actionType) + "，老粉看到基本功进度，DD顺手坐了一站。"
        );
    }

    private java.util.Optional<Map<String, Object>> stageMilestoneFor(
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            BusinessLog log,
            List<RiskDebt> openDebts
    ) {
        int day = session.getDay();
        if (day != 7 && day != 15 && day != 23) {
            return java.util.Optional.empty();
        }
        String routeType = effectiveMilestoneRoute(vup);
        String stage = switch (day) {
            case 7 -> "FIRST_SIGNAL";
            case 15 -> "MID_GAME_BACKLASH";
            default -> "ENDING_LOCK_WARNING";
        };
        StageMilestoneCopy copy = stageMilestoneCopy(day, routeType);
        Map<String, Object> ref = new java.util.LinkedHashMap<>();
        ref.put("type", "stage_milestone");
        ref.put("copyVersion", "STAGE_MILESTONE_V1");
        ref.put("day", day);
        ref.put("stage", stage);
        ref.put("stageLabel", copy.stageLabel());
        ref.put("routeType", routeType);
        ref.put("routeLabel", routeLabelForMilestone(routeType));
        ref.put("group", routeMilestoneGroup(routeType));
        ref.put("title", copy.title());
        ref.put("spokenLine", copy.spokenLine());
        ref.put("heckleLine", copy.heckleLine());
        ref.put("routeLine", copy.routeLine());
        ref.put("costLine", copy.costLine());
        ref.put("tomorrowLine", copy.tomorrowLine());
        ref.put("screenshotLabel", "第" + day + "天阶段爆点");
        ref.put("statLine", milestoneStatLine(vup, actionResult, log, openDebts));
        ref.put("actionType", actionResult.actionType());
        ref.put("openDebtCount", openDebts.size());
        ref.put("fans", vup.getFans());
        ref.put("watchHeat", vup.getWatchHeat());
        ref.put("reputation", vup.getReputation());
        ref.put("memeLevel", vup.getMemeLevel());
        ref.put("commercialLevel", vup.getCommercialLevel());
        return java.util.Optional.of(ref);
    }

    private String effectiveMilestoneRoute(Vup vup) {
        String currentRoute = safeRoute(vup.getCurrentRoute());
        if (!"UNKNOWN".equals(currentRoute)) {
            return currentRoute;
        }
        try {
            return jsonService.readMap(vup.getRouteScoreJson()).entrySet().stream()
                    .filter(entry -> !"UNKNOWN".equals(entry.getKey()))
                    .map(entry -> Map.entry(entry.getKey(), numberValue(entry.getValue())))
                    .filter(entry -> entry.getValue() > 0)
                    .max(java.util.Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse("UNKNOWN");
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }

    private int numberValue(Object value) {
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

    private String safeRoute(String routeType) {
        if (routeType == null || routeType.isBlank()) {
            return "UNKNOWN";
        }
        return switch (routeType) {
            case "ELECTRONIC_PICKLE", "SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE",
                 "CYBER_GIRLFRIEND", "DD_BUS_STOP", "MAIN_STAGE_KING", "GLORIOUS_GRADUATION",
                 "SOCIAL_COLLAB", "DANCE_MEME", "UNKNOWN" -> routeType;
            default -> "UNKNOWN";
        };
    }

    private String routeLabelForMilestone(String routeType) {
        return switch (routeType) {
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "SINGING_IDOL" -> "唱歌偶像";
            case "SLICE_SAINT" -> "切片圣体";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "DD_BUS_STOP" -> "DD公交站";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "DANCE_MEME" -> "梗舞整活";
            default -> "待定路线";
        };
    }

    private String routeMilestoneGroup(String routeType) {
        return switch (routeType) {
            case "SLICE_SAINT", "DANCE_MEME" -> "burst";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "heat";
            case "CYBER_GIRLFRIEND" -> "relationship";
            case "SOCIAL_COLLAB", "DD_BUS_STOP" -> "social";
            case "SINGING_IDOL" -> "singing";
            case "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION" -> "steady";
            default -> "unknown";
        };
    }

    private String milestoneStatLine(Vup vup, ActionResultDTO actionResult, BusinessLog log, List<RiskDebt> openDebts) {
        String debtText = openDebts.isEmpty() ? "旧账0" : "旧账" + openDebts.size();
        return "账面：粉丝" + signed(actionResult.fanChange())
                + "，围观" + signed(log.getWatchHeatChange())
                + "，口碑" + signed(log.getReputationChange())
                + "；当前总粉" + vup.getFans()
                + "，围观" + vup.getWatchHeat()
                + "，口碑" + vup.getReputation()
                + "，" + debtText;
    }

    private StageMilestoneCopy stageMilestoneCopy(int day, String routeType) {
        String group = routeMilestoneGroup(routeType);
        if (day == 7) {
            return day7MilestoneCopy(group);
        }
        if (day == 15) {
            return day15MilestoneCopy(group);
        }
        return day23MilestoneCopy(group);
    }

    private StageMilestoneCopy day7MilestoneCopy(String group) {
        return switch (group) {
            case "burst" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "切片组开工：第一条可拷打素材入库",
                    "我现在不是人在直播，是人在给剪辑台交素材。",
                    "主播别解释了，这段已经进素材库。",
                    "路线：乐子人和素材开始互相喂饭，爆点比稳定更重要。",
                    "代价：梗浓度会涨，老粉也会开始担心你被素材反向定义。",
                    "明天：继续补可剪片段，但别让同一个梗被复读到查重。"
            );
            case "heat" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "主会场闻到味：标题组开始排队",
                    "我这不是翻车，我这是提前给主会场占座。",
                    "别急着澄清，先让我把标题想完。",
                    "路线：围观正在变厚，黑红能量开始有了入口。",
                    "代价：口碑会被热度磨，旧账如果放着不管会自己长腿。",
                    "明天：热度够了就留证据，不够就先别硬贷款开庭。"
            );
            case "relationship" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "陪伴感初成：有人开始记你的排班",
                    "我以为我在营业，结果观众已经开始记考勤。",
                    "主播今天不说晚安，我这个关注算白点了吗？",
                    "路线：陪伴、商业化和独角兽期待正在一起升温。",
                    "代价：边界压力会越来越具体，不处理就会从糖变成债。",
                    "明天：继续营业可以，但要顺手补一次低压边界维护。"
            );
            case "social" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "DD上车提醒：这一站有人愿意多坐一会",
                    "我这局不是社牛，是把站牌先立起来。",
                    "主播你到底是哪家的？我先投币坐一站。",
                    "路线：联动和DD开始形成入口，但老粉还在看你有没有本体。",
                    "代价：接车太多会稀释标签，观众记住的是站点不是你。",
                    "明天：再接一站可以，记得用粉丝群把人留下。"
            );
            case "singing" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "副歌第一次被点名：有人开始等你唱完",
                    "今天不求全场破防，先让副歌被一个人记住。",
                    "这首先别毕业，主播你下次还唱不唱？",
                    "路线：歌力和真爱粉开始互相确认，作品感比热闹更值钱。",
                    "代价：成长慢，断练会让路线证据变薄。",
                    "明天：继续练歌或低压歌回，把一个固定记忆点唱稳。"
            );
            case "steady" -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "饭点录播组认证：这台适合挂后台",
                    "我这局不是没活，是把观众养成了准时吃饭。",
                    "主播你别爆火了，我怕你变忙。",
                    "路线：低压陪伴开始成型，真爱粉和口碑是主资产。",
                    "代价：爆点少，出圈慢，不能指望热搜替你涨粉。",
                    "明天：继续稳粉丝群，别突然硬冲黑红标题。"
            );
            default -> new StageMilestoneCopy(
                    "第7天路线苗头",
                    "路线分岔口：观众还没决定该怎么叫你",
                    "我现在最大的问题不是没活，是每个活都像别人的。",
                    "主播你到底想当哪种V，给个准话。",
                    "路线：证据还散，结局系统暂时只能看到摇摆。",
                    "代价：什么都沾一点会降低路线锁定，Day 30 容易查无此V。",
                    "明天：选一条最想要的路线，连续两天补同类证据。"
            );
        };
    }

    private StageMilestoneCopy day15MilestoneCopy(String group) {
        return switch (group) {
            case "burst" -> new StageMilestoneCopy(
                    "第15天中期开庭",
                    "切片二审：名场面开始被查重",
                    "坏消息是大家都看过了，好消息是说明真的传出去了。",
                    "这梗昨天笑过，今天要收二创税吗？",
                    "路线：切片圣体已经有传播面，下一步要证明不是只会复读。",
                    "代价：素材回旋会降低新鲜感，乐子人来得快也散得快。",
                    "明天：换新素材或补正片，让切片组有第二把刀。"
            );
            case "heat" -> new StageMilestoneCopy(
                    "第15天中期开庭",
                    "主会场开庭：标题组把你排进今日必看",
                    "坏消息是又被开了，好消息是全站都知道了。",
                    "你别急着道歉，我还没截图。",
                    "路线：围观够厚，主会场已经愿意给你留座。",
                    "代价：旧账和低口碑会一起追杀，热度不是免死金牌。",
                    "明天：热度够了，先拆旧账，否则 Day 23 会反噬。"
            );
            case "relationship" -> new StageMilestoneCopy(
                    "第15天中期边界",
                    "边界第一次被点名：陪伴感开始要说明书",
                    "我卖的是陪伴感，不是把自己交给排班表。",
                    "主播今天少播半小时，是不是感情淡了？",
                    "路线：赛博女友感已经立住，但观众开始索要确定性。",
                    "代价：商业化和独角兽期待会互相抬价，口碑承压。",
                    "明天：继续醒目留言回应可以，但要补一次边界和低压维护。"
            );
            case "social" -> new StageMilestoneCopy(
                    "第15天中期换乘",
                    "DD换乘潮：同接像公交线路图",
                    "我这直播间不是端水，是城市交通枢纽。",
                    "主播下一站去哪家？我先看站牌。",
                    "路线：DD和联动证据开始成网，过站流量变得可控。",
                    "代价：人脉感太强会冲淡主体，老粉会问你本台节目是什么。",
                    "明天：接车后做一次自家粉丝群维护，把路人留下。"
            );
            case "singing" -> new StageMilestoneCopy(
                    "第15天中期验收",
                    "歌回验收：副歌开始被催二创",
                    "今天唱准不是终点，是让别人想剪副歌。",
                    "主播别说练习版，这版已经有人存了。",
                    "路线：作品记忆点开始稳定，真爱粉有了可安利句。",
                    "代价：歌力证据不能断，热闹路线会一直抢注意力。",
                    "明天：继续补歌力，同时发一个能被转述的短视频或切片。"
            );
            case "steady" -> new StageMilestoneCopy(
                    "第15天中期复购",
                    "饭点稳定播放：老粉把你写进日程",
                    "今天不是峰值，是复购。",
                    "主播别整大活，饭还没吃完。",
                    "路线：电子榨菜已经从尝鲜变成习惯，口碑开始兜底。",
                    "代价：增长曲线会平，不主动补记忆点就不容易破圈。",
                    "明天：保持低压，但补一个能被复述的小标题或小段落。"
            );
            default -> new StageMilestoneCopy(
                    "第15天中期审稿",
                    "中期审稿会：什么都做过，但没人能复述你是谁",
                    "我现在像全平台试吃员，吃完自己也忘了菜单。",
                    "主播你这局是不是在开随机模式？",
                    "路线：内容样本很多，但缺少连续证据。",
                    "代价：中期还不收束，后期就只能靠运气赌结局。",
                    "明天：停掉一半摇摆行动，把最高路线分连续补到 Day 18。"
            );
        };
    }

    private StageMilestoneCopy day23MilestoneCopy(String group) {
        return switch (group) {
            case "burst" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "素材审判日：要做切片圣体还是被梗定义",
                    "现在不是缺热度，是缺一个能把热度变成结局的证据。",
                    "主播已经被做成素材了，本人什么时候上线？",
                    "路线：爆点足够，结局开始看你能不能驾驭素材。",
                    "代价：高梗浓度会吞掉本人，旧账没拆会变成最终标题。",
                    "明天：换新梗、补正片或拆旧账，别把最后一周交给复读。"
            );
            case "heat" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "热搜锁门前夜：主会场要么登顶要么反噬",
                    "我不是来洗白的，我是来确认这场到底谁说了算。",
                    "主播别下播，楼还没盖完。",
                    "路线：主会场已经成型，最后看热度、串味和旧账能不能闭环。",
                    "代价：口碑太低会把主会场之王打回普通黑红事故。",
                    "明天：只补能进结局证据的高光，顺手拆最高风险旧账。"
            );
            case "relationship" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "榜一排班表流出：陪伴感开始被排班",
                    "我以为我在营业，结果观众开始给我排班。",
                    "主播今天不播，是不是不爱我了？",
                    "路线：赛博女友结局已经接近门口，商业化和独角兽期待都在抬价。",
                    "代价：边界不立，陪伴感会变成追责表。",
                    "明天：要么立边界，要么冲赛博女友结局，别两边都含糊。"
            );
            case "social" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "末班车响铃：DD公交站要留站牌",
                    "我这局的结局不是去哪家，是大家为什么愿意在这站下车。",
                    "主播你别只报站名，给乘客一个回来的理由。",
                    "路线：DD公交站已经有车流，最后看能不能沉淀成你的站牌。",
                    "代价：只换乘不沉淀，结局会像热闹路过。",
                    "明天：联动后补粉丝群和自家节目标签，把DD变成留存。"
            );
            case "singing" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "终演候场：最后一首歌要有名字",
                    "我现在要的不是唱完，是让这首歌能代表这一轮。",
                    "主播别说随便唱，这首已经被安排压轴了。",
                    "路线：唱歌偶像已经有记忆点，最后看作品证据够不够厚。",
                    "代价：只练不展示会被系统当作后台努力，观众记不住。",
                    "明天：开歌回或发作品切片，把压轴曲写进结局材料。"
            );
            case "steady" -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "毕业预演：体面下播已经有证据",
                    "这局最难的不是爆火，是没有把自己播坏。",
                    "主播你要是体面毕业，我就真的不拷打了。",
                    "路线：稳健和光荣毕业都靠口碑、老粉和低风险证据收束。",
                    "代价：最后一周乱冲热度，会把二十多天的体面打穿。",
                    "明天：守住口碑、清掉旧账，继续补粉丝群维护或低压复盘。"
            );
            default -> new StageMilestoneCopy(
                    "第23天收官预警",
                    "最后转向窗口：再摇摆就会变成查无此V",
                    "这不是开放世界，这是结局系统在等我交卷。",
                    "主播别再试菜了，厨房要打烊了。",
                    "路线：最后一周必须锁一个答案，否则证据会互相抵消。",
                    "代价：摇摆会让所有路线都像半成品。",
                    "明天：选最高路线分打三连，其他行动只做风险兜底。"
            );
        };
    }

    private java.util.Optional<String> stageReviewFor(Vup vup, DaySession session) {
        int day = session.getDay();
        if (day != 7 && day != 14 && day != 21 && day != 28 && day != 30) {
            return java.util.Optional.empty();
        }
        String routeLabel = switch (vup.getCurrentRoute()) {
            case "SLICE_SAINT" -> "切片路线苗头";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场苗头";
            case "SOCIAL_COLLAB" -> "社交联动苗头";
            case "ELECTRONIC_PICKLE" -> "电子榨菜苗头";
            case "SINGING_IDOL" -> "歌势路线苗头";
            case "DANCE_MEME" -> "梗舞整活苗头";
            default -> "路线仍在摇摆";
        };
        String stageLabel = switch (day) {
            case 7 -> "第7天阶段复盘";
            case 14 -> "第14天路线成型";
            case 21 -> "第21天冲刺预警";
            case 28 -> "第28天毕业预演";
            default -> "第30天最终收束";
        };
        String memeLifecycleWarning = day == 21 ? repeatedClipMemeWarning(vup, day) : "";
        return java.util.Optional.of(stageLabel + "：" + routeLabel + "，日报组按真实行动记录下判断，不虚空贷款结局。"
                + " " + stageObjectiveService.reviewLineFor(vup)
                + " 粉丝结构：真爱粉" + vup.getTrueFans()
                + "、乐子人" + vup.getFunFans()
                + "、独角兽" + vup.getUnicornFans()
                + "、DD" + vup.getDdFans()
                + "，总粉丝" + vup.getFans()
                + "，口碑" + vup.getReputation() + "。"
                + memeLifecycleWarning
                + " " + nextTrendPreviewFor(day));
    }

    private String repeatedClipMemeWarning(Vup vup, int day) {
        long recentClipRepeats = businessLogMapper.findRecentClipLogsBySubtype(vup.getId(), "CLIP_SPREAD").stream()
                .filter(log -> day - log.getDay() <= 7)
                .count();
        if (recentClipRepeats < 3) {
            return "";
        }
        return " 近7天同梗切片" + recentClipRepeats + "次，梗开始被查重，切片组需要换素材或换企划。";
    }

    private String nextTrendPreviewFor(int day) {
        if (day >= 30) {
            return "最终提示：今天不再预告下一周口味，结局复盘只看本轮30天证据。";
        }
        String nextTrendId = platformTrendService.trendIdForDay(day + 1);
        String nextTrendLabel = "下周口味：" + String.valueOf(platformTrendService.trendFor(nextTrendId).get("label"));
        if (PlatformTrendService.SINGING_BOOST_WEEK.equals(nextTrendId)) {
            return nextTrendLabel + "，练歌和投稿更容易吃推荐，别把歌回素材全留到开播前临时抱佛脚。";
        }
        if (PlatformTrendService.MEME_OUTBREAK_WEEK.equals(nextTrendId)) {
            return nextTrendLabel + "，切片和整活传播会更香，素材库别空着，乐子人不吃空气。";
        }
        if (PlatformTrendService.COMMERCIAL_REVIEW_WEEK.equals(nextTrendId)) {
            return nextTrendLabel + "，高亮互动回应和陪伴商业化更好看，但别把直播间做成老板打卡机。";
        }
        return nextTrendLabel + "，低压稳健内容更容易兜底，标题组先别贷款开庭。";
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private record ReportFlavor(String tone, String routeHint, String nextHint, String summary) {
    }

    private record ReportDelta(
            int fanChange,
            int staminaChange,
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange,
            int commercialChange,
            int coinChange,
            int inspirationChange
    ) {
    }

    private record StageMilestoneCopy(
            String stageLabel,
            String title,
            String spokenLine,
            String heckleLine,
            String routeLine,
            String costLine,
            String tomorrowLine
    ) {
    }
}
