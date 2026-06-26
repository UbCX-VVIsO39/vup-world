package com.example.vupworld.service.risk;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.dto.ActionDtos.DebtCreatedDTO;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtService {
    private final RiskDebtMapper riskDebtMapper;
    private final BalanceConfig balanceConfig;

    public DebtService(RiskDebtMapper riskDebtMapper, BalanceConfig balanceConfig) {
        this.riskDebtMapper = riskDebtMapper;
        this.balanceConfig = balanceConfig;
    }

    public List<DebtCreatedDTO> createForStreamTitleIfNeeded(Vup vup, DaySession session, BusinessLog log, TitleOptionDTO title) {
        if ("SAFE".equals(title.style())) {
            return List.of();
        }

        if ("BUSINESS_SAFE".equals(title.style())) {
            RiskDebt debt = new RiskDebt();
            debt.setVupId(vup.getId());
            debt.setSourceLogId(log.getId());
            debt.setDebtType("COMMERCIAL_BACKLASH");
            debt.setStatus("OPEN");
            debt.setSeverity(1);
            debt.setCreateDay(session.getDay());
            debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + balanceConfig.commercialBacklashDebtDelay()));
            debt.setSourceAction(log.getAction());
            debt.setSourceTitle(title.titleText());
            debt.setSummary("收益：稳定排班和活动预算同时加分。代价：商业味被端上桌，老粉开始担心味儿变了，三天后可能回流成商业反噬旧账。");
            riskDebtMapper.insert(debt);
            return List.of(toDebtCreatedDto(debt, session));
        }

        if ("FAN_SERVICE".equals(title.style())) {
            RiskDebt debt = new RiskDebt();
            debt.setVupId(vup.getId());
            debt.setSourceLogId(log.getId());
            debt.setDebtType("UNICORN_EXPECTATION");
            debt.setStatus("OPEN");
            debt.setSeverity(1);
            debt.setCreateDay(session.getDay());
            debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + balanceConfig.unicornExpectationDebtDelay()));
            debt.setSourceAction(log.getAction());
            debt.setSourceTitle(title.titleText());
            debt.setSummary("收益：陪伴营业让关系感和互动热度更明确。代价：独角兽期待被抬高，榜一排班表和小作文素材同时入账，三天后可能回流成独角兽期待旧账。");
            riskDebtMapper.insert(debt);
            return List.of(toDebtCreatedDto(debt, session));
        }

        RiskDebt debt = new RiskDebt();
        boolean singingBoostHardMouth = isSingingBoostHardMouth(session, title);
        debt.setVupId(vup.getId());
        debt.setSourceLogId(log.getId());
        debt.setDebtType("TITLE_BACKFIRE");
        debt.setStatus("OPEN");
        int baseSeverity = singingBoostHardMouth ? 3 : "HARD_MOUTH".equals(title.style()) ? 2 : 1;
        debt.setSeverity(severityWithRepeatPressure(vup, "TITLE_BACKFIRE", baseSeverity, 0));
        debt.setCreateDay(session.getDay());
        debt.setDueDay(Math.min(
                balanceConfig.maxDay(),
                session.getDay() + (singingBoostHardMouth ? 2 : balanceConfig.titleBackfireDebtDelay())
        ));
        debt.setSourceAction(log.getAction());
        debt.setSourceTitle(title.titleText());
        debt.setSummary(singingBoostHardMouth
                ? "收益：歌回扶持周把标题推给更多路人。代价：高音贷款留下更重的标题党反噬旧账，录播组和考据组可能两天后上桌。"
                : "收益：高风险标题换到更高点击。代价：标题党反噬旧账入账，录播组和考据组可能三天后上桌。");
        riskDebtMapper.insert(debt);
        return List.of(toDebtCreatedDto(debt, session));
    }

    public ActionDebtCreation createForActionIfNeeded(Vup vup, DaySession session, BusinessLog log) {
        if (!isMemeOutbreakClip(session, log)) {
            return ActionDebtCreation.empty();
        }

        RiskDebt debt = new RiskDebt();
        debt.setVupId(vup.getId());
        debt.setSourceLogId(log.getId());
        debt.setDebtType("BOOMERANG_CLIP");
        debt.setStatus("OPEN");
        debt.setSeverity(severityWithRepeatPressure(vup, "BOOMERANG_CLIP", 1, 0));
        debt.setCreateDay(session.getDay());
        debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + balanceConfig.boomerangClipDebtDelay()));
        debt.setSourceAction(log.getAction());
        debt.setSourceTitle("抽象出圈周切片");
        debt.setSummary("收益：抽象出圈周让切片传播更快，乐子人更容易进场。代价：米线压力留下轻量回旋镖旧账，录播组可能两天后把笑点和上下文一起端上桌。");
        riskDebtMapper.insert(debt);
        return new ActionDebtCreation(List.of(toDebtCreatedDto(debt, session)), List.of(debt.getId()));
    }

    public record ActionDebtCreation(
            List<DebtCreatedDTO> created,
            List<Long> debtIds
    ) {
        static ActionDebtCreation empty() {
            return new ActionDebtCreation(List.of(), List.of());
        }
    }

    public List<Map<String, Object>> applyDefensiveAction(Vup vup, DaySession session, BusinessLog log) {
        if (!isDefensiveAction(log.getAction())) {
            return List.of();
        }

        RiskDebt target = riskDebtMapper.findOpenByVupId(vup.getId()).stream()
                .filter(debt -> debt.getCreateDay() < session.getDay())
                .filter(debt -> defensiveActionSupports(log.getAction(), debt))
                .min(defensiveTargetPriority(log.getAction(), session))
                .orElse(null);
        if (target == null) {
            return List.of();
        }

        int severityBefore = target.getSeverity();
        int dueDayBefore = target.getDueDay();
        String mode = defensiveMode(log.getAction(), target, session);
        if ("DELAY".equals(mode)) {
            target.setDueDay(Math.min(balanceConfig.maxDay(), target.getDueDay() + 1));
        } else {
            target.setSeverity(Math.max(1, target.getSeverity() - 1));
        }

        int severityDelta = target.getSeverity() - severityBefore;
        int dueDayDelta = target.getDueDay() - dueDayBefore;
        if (severityDelta == 0 && dueDayDelta == 0) {
            return List.of();
        }

        target.setSummary(target.getSummary() + defensiveSummary(log.getAction(), mode, target, session));
        riskDebtMapper.updateMitigation(target);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("debtId", target.getId());
        evidence.put("debtType", target.getDebtType());
        evidence.put("debtLabel", RiskDebtLabels.debtTypeLabel(target.getDebtType()));
        evidence.put("actionType", log.getAction());
        evidence.put("mode", mode);
        evidence.put("severityDelta", severityDelta);
        evidence.put("dueDayDelta", dueDayDelta);
        evidence.put("severityAfter", target.getSeverity());
        evidence.put("dueDayAfter", target.getDueDay());
        evidence.put("remainingDays", Math.max(0, target.getDueDay() - session.getDay()));
        evidence.put("sourceLine", RiskDebtPresenter.sourceLine(target));
        evidence.put("consequencePreview", RiskDebtPresenter.consequencePreview(target, session.getDay()));
        evidence.put("recommendedAction", RiskDebtPresenter.recommendedAction(target, session.getDay()));
        return List.of(evidence);
    }

    public RiskDebt createForEventChoiceIfNeeded(
            Vup vup,
            DaySession session,
            RiskDebt resolvedDebt,
            String choiceType,
            int resolvedSeverity,
            Long sourceLogId
    ) {
        if (!"traffic".equals(choiceType) && !"meme".equals(choiceType)) {
            return null;
        }

        int severityBump = "traffic".equals(choiceType)
                ? balanceConfig.trafficRolloverSeverityBump()
                : balanceConfig.memeRolloverSeverityBump();
        int delay = "traffic".equals(choiceType)
                ? balanceConfig.trafficRolloverDebtDelay()
                : balanceConfig.memeRolloverDebtDelay();

        RiskDebt debt = new RiskDebt();
        debt.setVupId(vup.getId());
        debt.setSourceLogId(sourceLogId);
        debt.setDebtType(resolvedDebt.getDebtType());
        debt.setStatus("OPEN");
        debt.setSeverity(severityWithRepeatPressure(
                vup,
                resolvedDebt.getDebtType(),
                Math.max(1, resolvedSeverity + severityBump),
                1
        ));
        debt.setCreateDay(session.getDay());
        debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + delay));
        debt.setSourceAction("EVENT_CHOICE:" + choiceType);
        debt.setSourceTitle(resolvedDebt.getSourceTitle());
        debt.setSummary(eventChoiceRolloverSummary(choiceType, resolvedDebt, debt.getSeverity(), delay, debt.getDueDay()));
        riskDebtMapper.insert(debt);
        return debt;
    }

    public RiskDebt createForOrdinaryEventChoiceIfNeeded(
            Vup vup,
            DaySession session,
            BusinessLog log,
            String eventKey,
            String eventTitle,
            String eventType,
            String choiceType
    ) {
        if (!"traffic".equals(choiceType) && !"meme".equals(choiceType)) {
            return null;
        }

        String debtType = ordinaryEventDebtType(eventKey, eventTitle, eventType, choiceType);
        int delay = "traffic".equals(choiceType)
                ? balanceConfig.trafficRolloverDebtDelay()
                : balanceConfig.memeRolloverDebtDelay();
        int baseSeverity = ordinaryEventBaseSeverity(eventType, choiceType);

        RiskDebt debt = new RiskDebt();
        debt.setVupId(vup.getId());
        debt.setSourceLogId(log.getId());
        debt.setDebtType(debtType);
        debt.setStatus("OPEN");
        debt.setSeverity(severityWithRepeatPressure(vup, debtType, baseSeverity, 0));
        debt.setCreateDay(session.getDay());
        debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + delay));
        debt.setSourceAction("ORDINARY_EVENT:" + choiceType);
        debt.setSourceTitle(eventTitle == null || eventTitle.isBlank() ? eventKey : eventTitle);
        debt.setSummary(ordinaryEventDebtSummary(choiceType, eventTitle, debtType, debt.getSeverity(), debt.getDueDay()));
        riskDebtMapper.insert(debt);
        return debt;
    }

    public RiskDebt createForInteractionChoiceIfNeeded(
            Vup vup,
            DaySession session,
            BusinessLog log,
            String eventKey,
            String choiceType
    ) {
        if (!"traffic".equals(choiceType) && !"meme".equals(choiceType)) {
            return null;
        }

        String debtType = interactionDebtType(eventKey, choiceType);
        int delay = "traffic".equals(choiceType)
                ? balanceConfig.trafficRolloverDebtDelay()
                : balanceConfig.memeRolloverDebtDelay();
        int baseSeverity = interactionBaseSeverity(eventKey, choiceType);

        RiskDebt debt = new RiskDebt();
        debt.setVupId(vup.getId());
        debt.setSourceLogId(log.getId());
        debt.setDebtType(debtType);
        debt.setStatus("OPEN");
        debt.setSeverity(severityWithRepeatPressure(vup, debtType, baseSeverity, 0));
        debt.setCreateDay(session.getDay());
        debt.setDueDay(Math.min(balanceConfig.maxDay(), session.getDay() + delay));
        debt.setSourceAction("INTERACTION_CHOICE:" + choiceType);
        debt.setSourceTitle(interactionSourceTitle(eventKey));
        debt.setSummary(interactionDebtSummary(eventKey, choiceType, debtType, debt.getSeverity(), debt.getDueDay()));
        riskDebtMapper.insert(debt);
        return debt;
    }

    private boolean isSingingBoostHardMouth(DaySession session, TitleOptionDTO title) {
        return session.getDay() >= 8
                && session.getDay() <= 14
                && Long.valueOf(2L).equals(session.getSelectedPlanId())
                && "HARD_MOUTH".equals(title.style());
    }

    private boolean isMemeOutbreakClip(DaySession session, BusinessLog log) {
        return session.getDay() >= 15
                && session.getDay() <= 21
                && "PUBLISH_CLIP".equals(log.getAction());
    }

    public List<RiskDebt> openDebts(Vup vup) {
        return riskDebtMapper.findOpenByVupId(vup.getId());
    }

    public TitleDebtPreview previewForTitle(Vup vup, DaySession session, TitleOptionDTO title) {
        if ("SAFE".equals(title.style())) {
            return new TitleDebtPreview(null, null, 0, 0, false, false,
                    "旧账：不新增标题旧账，适合保口碑");
        }
        if ("BUSINESS_SAFE".equals(title.style())) {
            int dueDay = Math.min(balanceConfig.maxDay(), session.getDay() + balanceConfig.commercialBacklashDebtDelay());
            return new TitleDebtPreview(
                    "COMMERCIAL_BACKLASH",
                    RiskDebtLabels.debtTypeLabel("COMMERCIAL_BACKLASH"),
                    1,
                    dueDay,
                    false,
                    true,
                    "旧账：商业反噬 1级，剩余" + Math.max(0, dueDay - session.getDay())
                            + "天，第" + dueDay + "天可能回流；来源为今日标题，后果是老粉信任和口碑承压"
            );
        }
        if ("FAN_SERVICE".equals(title.style())) {
            int dueDay = Math.min(balanceConfig.maxDay(), session.getDay() + balanceConfig.unicornExpectationDebtDelay());
            return new TitleDebtPreview(
                    "UNICORN_EXPECTATION",
                    RiskDebtLabels.debtTypeLabel("UNICORN_EXPECTATION"),
                    1,
                    dueDay,
                    false,
                    true,
                    "旧账：独角兽期待 1级，剩余" + Math.max(0, dueDay - session.getDay())
                            + "天，第" + dueDay + "天可能回流；来源为今日标题，后果是边界期待和小作文风险抬头"
            );
        }

        boolean singingBoostHardMouth = isSingingBoostHardMouth(session, title);
        int delay = singingBoostHardMouth ? 2 : balanceConfig.titleBackfireDebtDelay();
        int baseSeverity = singingBoostHardMouth ? 3 : "HARD_MOUTH".equals(title.style()) ? 2 : 1;
        int severity = severityWithRepeatPressure(vup, "TITLE_BACKFIRE", baseSeverity, 0);
        int dueDay = Math.min(balanceConfig.maxDay(), session.getDay() + delay);
        String riskLine = "旧账：标题党反噬 " + severity + "级，剩余"
                + Math.max(0, dueDay - session.getDay())
                + "天，第" + dueDay + "天可能回流；来源为今日标题，后果是录播组和考据楼翻旧账";
        if (singingBoostHardMouth) {
            riskLine += "；歌回扶持周高音贷款，录播组和回旋镖更快上桌";
        } else {
            riskLine += "；录播组和回旋镖可能上桌";
        }
        boolean repeatPressured = severity > baseSeverity;
        if (repeatPressured) {
            riskLine += "；同类旧账加压，实际严重度已上调";
        }
        return new TitleDebtPreview(
                "TITLE_BACKFIRE",
                RiskDebtLabels.debtTypeLabel("TITLE_BACKFIRE"),
                severity,
                dueDay,
                repeatPressured,
                true,
                riskLine
        );
    }

    public DebtActionPreview previewForAction(Vup vup, DaySession session, String actionType) {
        return previewForAction(riskDebtMapper.findOpenByVupId(vup.getId()), session, actionType);
    }

    public DebtActionPreview previewForAction(List<RiskDebt> openDebts, DaySession session, String actionType) {
        if (openDebts.isEmpty()) {
            return DebtActionPreview.empty();
        }

        RiskDebt dueSoon = openDebts.stream()
                .min(Comparator
                        .comparingInt((RiskDebt debt) -> Math.max(0, debt.getDueDay() - session.getDay()))
                        .thenComparing(Comparator.comparingInt(RiskDebt::getSeverity).reversed()))
                .orElse(null);
        RiskDebt defendable = isDefensiveAction(actionType)
                ? openDebts.stream()
                        .filter(debt -> debt.getCreateDay() < session.getDay())
                        .filter(debt -> defensiveActionSupports(actionType, debt))
                        .min(defensiveTargetPriority(actionType, session))
                        .orElse(null)
                : null;

        if (defendable != null) {
            String mode = defensiveMode(actionType, defendable, session);
            String label = RiskDebtLabels.debtTypeLabel(defendable.getDebtType());
            String effectLine = "旧账窗口：可处理「" + label + "」，"
                    + ("DELAY".equals(mode)
                    ? "预计把回流延后1天"
                    : "预计把严重度从" + defendable.getSeverity() + "压到" + Math.max(1, defendable.getSeverity() - 1))
                    + "；" + RiskDebtPresenter.sourceLine(defendable);
            String riskLine = dueSoon != null && !sameDebt(defendable, dueSoon)
                    ? "仍有「" + RiskDebtLabels.debtTypeLabel(dueSoon.getDebtType()) + "」"
                    + dueText(dueSoon, session.getDay()) + "，别让它抢事件位"
                    : "";
            return new DebtActionPreview(effectLine, riskLine, true);
        }

        if (dueSoon == null) {
            return DebtActionPreview.empty();
        }
        String riskLine = "旧账窗口：「" + RiskDebtLabels.debtTypeLabel(dueSoon.getDebtType()) + "」"
                + dueText(dueSoon, session.getDay())
                + "，严重度" + dueSoon.getSeverity()
                + "；" + RiskDebtPresenter.sourceLine(dueSoon)
                + "；本手不拆可能优先回流";
        return new DebtActionPreview("", riskLine, true);
    }

    public RiskDebt dueDebt(Vup vup, int day) {
        return riskDebtMapper.findDueOpenByVupIdAndDay(vup.getId(), day);
    }

    public RiskDebt escalatableOpenDebt(Vup vup, int day) {
        return riskDebtMapper.findEscalatableOpenByVupIdAndDay(vup.getId(), day);
    }

    private boolean isDefensiveAction(String actionType) {
        return "TRAIN_TALK".equals(actionType)
                || "FAN_GROUP_MAINTAIN".equals(actionType);
    }

    private boolean defensiveActionSupports(String actionType, RiskDebt debt) {
        return switch (actionType) {
            case "TRAIN_TALK" -> "TITLE_BACKFIRE".equals(debt.getDebtType())
                    || "BOOMERANG_CLIP".equals(debt.getDebtType())
                    || "BLACK_HISTORY_STOCK".equals(debt.getDebtType())
                    || "VOICE_ACCIDENT".equals(debt.getDebtType());
            case "FAN_GROUP_MAINTAIN" -> "UNICORN_EXPECTATION".equals(debt.getDebtType())
                    || "FAN_GROUP_DRAMA".equals(debt.getDebtType())
                    || "COMMERCIAL_BACKLASH".equals(debt.getDebtType())
                    || "COLLAB_SPILLOVER".equals(debt.getDebtType());
            default -> false;
        };
    }

    private Comparator<RiskDebt> defensiveTargetPriority(String actionType, DaySession session) {
        Comparator<RiskDebt> urgencyFirst = Comparator
                .comparingInt((RiskDebt debt) -> Math.max(0, debt.getDueDay() - session.getDay()))
                .thenComparing(Comparator.comparingInt(RiskDebt::getSeverity).reversed())
                .thenComparingLong(debt -> debt.getId() == null ? Long.MAX_VALUE : debt.getId());
        if ("REST".equals(actionType)) {
            return urgencyFirst;
        }
        return Comparator
                .comparingInt((RiskDebt debt) -> debt.getSeverity()).reversed()
                .thenComparingInt(debt -> Math.max(0, debt.getDueDay() - session.getDay()))
                .thenComparingLong(debt -> debt.getId() == null ? Long.MAX_VALUE : debt.getId());
    }

    private String defensiveMode(String actionType, RiskDebt debt, DaySession session) {
        if ("REST".equals(actionType)) {
            return debt.getDueDay() <= session.getDay() + 2 && debt.getDueDay() < balanceConfig.maxDay()
                    ? "DELAY"
                    : "COOL";
        }
        return debt.getSeverity() > 1 || debt.getDueDay() >= balanceConfig.maxDay() ? "COOL" : "DELAY";
    }

    private String defensiveSummary(String actionType, String mode, RiskDebt debt, DaySession session) {
        String actionLabel = switch (actionType) {
            case "TRAIN_TALK" -> "杂谈复盘";
            case "FAN_GROUP_MAINTAIN" -> "粉丝群维护";
            case "REST" -> "休息调整";
            default -> "防守行动";
        };
        if ("DELAY".equals(mode)) {
            return " " + actionLabel + "争取到一点窗口，" + RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                    + "延后到第" + debt.getDueDay() + "天回流。";
        }
        return " " + actionLabel + "补了上下文，" + RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                + "降温到" + debt.getSeverity() + "级，仍需后续留意。";
    }

    private boolean sameDebt(RiskDebt left, RiskDebt right) {
        if (left == null || right == null || left.getId() == null || right.getId() == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }

    private String dueText(RiskDebt debt, int day) {
        int remainingDays = Math.max(0, debt.getDueDay() - day);
        if (remainingDays <= 0) {
            return "今天回流";
        }
        if (remainingDays == 1) {
            return "明天回流";
        }
        return remainingDays + "天后回流";
    }

    private int severityWithRepeatPressure(Vup vup, String debtType, int baseSeverity, int excludedCurrentDebts) {
        if (!hasRepeatPressure(debtType)) {
            return clampSeverity(baseSeverity);
        }
        int priorSameType = Math.max(0, riskDebtMapper.countByVupIdAndDebtType(vup.getId(), debtType) - excludedCurrentDebts);
        return clampSeverity(baseSeverity + priorSameType * balanceConfig.debtRepeatSeverityStep());
    }

    private boolean hasRepeatPressure(String debtType) {
        return "TITLE_BACKFIRE".equals(debtType) || "BOOMERANG_CLIP".equals(debtType);
    }

    private String ordinaryEventDebtType(String eventKey, String eventTitle, String eventType, String choiceType) {
        String text = ((eventKey == null ? "" : eventKey) + " "
                + (eventTitle == null ? "" : eventTitle) + " "
                + (eventType == null ? "" : eventType)).toUpperCase();
        if (text.contains("COLLAB") || text.contains("联动")) {
            return "COLLAB_SPILLOVER";
        }
        if (text.contains("SPONSOR") || text.contains("BOSS") || text.contains("COMMERCIAL") || text.contains("商")) {
            return "COMMERCIAL_BACKLASH";
        }
        if (text.contains("FAN_GROUP") || text.contains("BOUNDARY") || text.contains("UNICORN") || text.contains("边界") || text.contains("粉丝群")) {
            return "FAN_GROUP_DRAMA";
        }
        if (text.contains("OLD") || text.contains("INTERNET_MEMORY") || text.contains("RESEARCH") || text.contains("黑历史")) {
            return "BLACK_HISTORY_STOCK";
        }
        if ("meme".equals(choiceType) || text.contains("CLIP") || text.contains("MEME") || text.contains("切片") || text.contains("梗")) {
            return "BOOMERANG_CLIP";
        }
        return "TITLE_BACKFIRE";
    }

    private int ordinaryEventBaseSeverity(String eventType, String choiceType) {
        int severity = "traffic".equals(choiceType) ? 2 : 1;
        if ("NEGATIVE".equals(eventType)) {
            severity += 1;
        }
        if ("MIXED".equals(eventType) && "traffic".equals(choiceType)) {
            severity += 1;
        }
        return clampSeverity(severity);
    }

    private String interactionDebtType(String eventKey, String choiceType) {
        return switch (eventKey == null ? "" : eventKey) {
            case "SC_BOSS_QUESTION" -> "traffic".equals(choiceType) ? "UNICORN_EXPECTATION" : "COMMERCIAL_BACKLASH";
            case "COLLAB_RHYTHM" -> "COLLAB_SPILLOVER";
            case "MARSHMALLOW_BOMB" -> "meme".equals(choiceType) ? "VOICE_ACCIDENT" : "BOOMERANG_CLIP";
            case "CHAT_BAIT" -> "meme".equals(choiceType) ? "BOOMERANG_CLIP" : "TITLE_BACKFIRE";
            default -> "meme".equals(choiceType) ? "BOOMERANG_CLIP" : "TITLE_BACKFIRE";
        };
    }

    private int interactionBaseSeverity(String eventKey, String choiceType) {
        int severity = "traffic".equals(choiceType) ? 2 : 1;
        if ("MARSHMALLOW_BOMB".equals(eventKey) && "meme".equals(choiceType)) {
            severity += 1;
        }
        if ("COLLAB_RHYTHM".equals(eventKey)) {
            severity += 1;
        }
        if ("SC_BOSS_QUESTION".equals(eventKey) && "traffic".equals(choiceType)) {
            severity += 1;
        }
        return clampSeverity(severity);
    }

    private String interactionSourceTitle(String eventKey) {
        return switch (eventKey == null ? "" : eventKey) {
            case "CHAT_BAIT" -> "弹幕锐评开庭";
            case "MARSHMALLOW_BOMB" -> "棉花糖爆弹";
            case "SC_BOSS_QUESTION" -> "榜一问题接球";
            case "COLLAB_RHYTHM" -> "联动节奏";
            default -> "直播现场互动";
        };
    }

    private String interactionDebtSummary(String eventKey, String choiceType, String debtType, int severity, int dueDay) {
        String source = interactionSourceTitle(eventKey);
        if ("traffic".equals(choiceType)) {
            return "你把「" + source + "」硬接成直播间主会场，当场换到热度和人气，但"
                    + RiskDebtLabels.debtTypeLabel(debtType) + "留下短期旧账，严重度"
                    + severity + "，第" + dueDay + "天可能回流。";
        }
        return "你把「" + source + "」顺势做成梗，切片组拿到了新素材，但"
                + RiskDebtLabels.debtTypeLabel(debtType) + "也被装进时间轴，严重度"
                + severity + "，第" + dueDay + "天可能回流。";
    }

    private String ordinaryEventDebtSummary(String choiceType, String eventTitle, String debtType, int severity, int dueDay) {
        String source = eventTitle == null || eventTitle.isBlank() ? "这次突发事件" : "「" + eventTitle + "」";
        if ("traffic".equals(choiceType)) {
            return "你把" + source + "硬接成临时主会场，热度当场兑现，但"
                    + RiskDebtLabels.debtTypeLabel(debtType) + "留下短期旧账，严重度"
                    + severity + "，第" + dueDay + "天可能回流。";
        }
        return "你把" + source + "顺势做成梗，切片组有了新料，但"
                + RiskDebtLabels.debtTypeLabel(debtType) + "也被打包进时间轴，严重度"
                + severity + "，第" + dueDay + "天可能回流。";
    }

    private int clampSeverity(int severity) {
        return Math.min(balanceConfig.debtSeverityMax(), Math.max(1, severity));
    }

    private String eventChoiceRolloverSummary(String choiceType, RiskDebt resolvedDebt, int severity, int delay, int dueDay) {
        String source = resolvedDebt.getSourceTitle() == null || resolvedDebt.getSourceTitle().isBlank()
                ? "这次正式事件"
                : "《" + resolvedDebt.getSourceTitle() + "》";
        if ("traffic".equals(choiceType)) {
            return "你把" + RiskDebtLabels.debtTypeLabel(resolvedDebt.getDebtType())
                    + "硬接成主会场流量，旧楼暂时散场，但" + source
                    + "留下了更短期的新旧账，严重度" + severity
                    + "，第" + dueDay + "天会再次回流；明天日报会优先提醒这笔账。";
        }
        return "你把" + RiskDebtLabels.debtTypeLabel(resolvedDebt.getDebtType())
                + "顺势做成梗，旧证据换了包装继续传播，" + source
                + "留下新一轮切片旧账，严重度" + severity
                + "，第" + dueDay + "天会再次回流；接下来" + delay + "天切片组会继续翻这份材料。";
    }

    private DebtCreatedDTO toDebtCreatedDto(RiskDebt debt, DaySession session) {
        return new DebtCreatedDTO(
                debt.getDebtType(),
                debt.getSeverity(),
                Math.max(0, debt.getDueDay() - session.getDay())
        );
    }

    public record DebtActionPreview(
            String effectLine,
            String riskLine,
            boolean active
    ) {
        static DebtActionPreview empty() {
            return new DebtActionPreview("", "", false);
        }
    }

    public record TitleDebtPreview(
            String debtType,
            String debtLabel,
            int severity,
            int dueDay,
            boolean repeatPressured,
            boolean active,
            String riskLine
    ) {
    }
}
