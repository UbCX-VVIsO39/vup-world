package com.example.vupworld.service.risk;

import com.example.vupworld.dto.VupDtos.DebtDTO;
import com.example.vupworld.model.RiskDebt;

public final class RiskDebtPresenter {
    private RiskDebtPresenter() {
    }

    public static DebtDTO toDebtDto(RiskDebt debt, int currentDay) {
        return new DebtDTO(
                debt.getId(),
                debt.getDebtType(),
                debt.getStatus(),
                debt.getSeverity(),
                debt.getCreateDay(),
                debt.getDueDay(),
                sourceSummary(debt),
                RiskDebtLabels.debtTypeLabel(debt.getDebtType()),
                remainingDays(debt, currentDay),
                crisisLevelLabel(debt, currentDay),
                consequencePreview(debt, currentDay),
                recommendedAction(debt, currentDay),
                urgent(debt, currentDay)
        );
    }

    public static int remainingDays(RiskDebt debt, int currentDay) {
        return Math.max(0, debt.getDueDay() - currentDay);
    }

    public static String crisisLevelLabel(RiskDebt debt, int currentDay) {
        if (remainingDays(debt, currentDay) <= 0) {
            return "爆发中";
        }
        if (remainingDays(debt, currentDay) <= 1 || debt.getSeverity() >= 5) {
            return "高危";
        }
        if (remainingDays(debt, currentDay) <= 2 || debt.getSeverity() >= 3) {
            return "中危";
        }
        return "低危";
    }

    public static boolean urgent(RiskDebt debt, int currentDay) {
        return remainingDays(debt, currentDay) <= 2 || debt.getSeverity() >= 4;
    }

    public static String consequencePreview(RiskDebt debt, int currentDay) {
        int days = remainingDays(debt, currentDay);
        String countdown = days <= 0 ? "今天回流" : "剩余" + days + "天";
        return "严重度" + debt.getSeverity() + "，" + countdown
                + "，预计" + dueText(debt, currentDay) + "：" + consequenceByType(debt.getDebtType());
    }

    public static String recommendedAction(RiskDebt debt, int currentDay) {
        String action = switch (debt.getDebtType()) {
            case "TITLE_BACKFIRE", "BOOMERANG_CLIP", "BLACK_HISTORY_STOCK", "VOICE_ACCIDENT" ->
                    "用补全切片上下文或杂谈复盘补证据";
            case "UNICORN_EXPECTATION", "FAN_GROUP_DRAMA" ->
                    "用降温公告或粉丝群维护重申边界";
            case "COMMERCIAL_BACKLASH", "COLLAB_SPILLOVER" ->
                    "用临时房管、降温公告或粉丝群维护控住扩散";
            default -> "用米线工具、杂谈复盘或粉丝群维护先降温";
        };
        if (remainingDays(debt, currentDay) <= 0) {
            return "今天优先处理：" + action;
        }
        if (urgent(debt, currentDay)) {
            return "本日优先：" + action + "，避免它抢掉事件位";
        }
        return action + "，处理后再回到路线推进";
    }

    public static String dueText(RiskDebt debt, int currentDay) {
        int days = remainingDays(debt, currentDay);
        if (days <= 0) {
            return "今天爆雷";
        }
        if (days == 1) {
            return "明天爆雷";
        }
        return days + "天后爆雷";
    }

    private static String consequenceByType(String debtType) {
        return switch (debtType) {
            case "TITLE_BACKFIRE" -> "录播组和考据楼可能把标题贷款翻成主会场，口碑承压。";
            case "BOOMERANG_CLIP" -> "切片笑点会带着上下文回流，围观热度上涨，误读成本变高。";
            case "UNICORN_EXPECTATION" -> "陪伴期待可能变成排班表小作文，独角兽粉情绪波动。";
            case "COMMERCIAL_BACKLASH" -> "商单味会被集中讨论，口碑与老粉信任承压。";
            case "BLACK_HISTORY_STOCK" -> "旧素材被重新翻出，主会场开始补课考古。";
            case "VOICE_ACCIDENT" -> "声线事故被反复剪辑，专业感和口碑承压。";
            case "COLLAB_SPILLOVER" -> "联动节奏外溢到双方粉圈，弹幕管理压力上升。";
            case "FAN_GROUP_DRAMA" -> "粉丝群截图扩散成小作文，老粉和DD一起进场。";
            default -> "旧账可能回流成正式事件，围观和口碑同时承压。";
        };
    }

    public static String sourceLine(RiskDebt debt) {
        String action = sourceActionLabel(debt.getSourceAction());
        String title = debt.getSourceTitle() == null || debt.getSourceTitle().isBlank()
                ? "未署名素材"
                : "「" + debt.getSourceTitle() + "」";
        return "来源：第" + debt.getCreateDay() + "天" + action + " " + title;
    }

    private static String sourceSummary(RiskDebt debt) {
        String summary = debt.getSummary();
        if (summary == null || summary.isBlank()) {
            return sourceLine(debt) + "。录播组还在补上下文。";
        }
        if (summary.startsWith("来源：")) {
            return summary;
        }
        return sourceLine(debt) + "。" + summary;
    }

    private static String sourceActionLabel(String sourceAction) {
        return switch (sourceAction == null ? "" : sourceAction) {
            case "TRAIN_SONG" -> "练歌";
            case "TRAIN_DANCE" -> "练舞";
            case "TRAIN_TALK" -> "杂谈复盘";
            case "STREAM_PLAN" -> "直播企划";
            case "PUBLISH_VIDEO" -> "发布视频";
            case "PUBLISH_CLIP" -> "发布切片";
            case "FAN_GROUP_MAINTAIN" -> "粉丝群维护";
            case "NPC_INTERACT" -> "同台互动";
            case "REST" -> "休息";
            default -> {
                if (sourceAction != null && sourceAction.startsWith("EVENT_CHOICE:")) {
                    yield "事件选择";
                }
                if (sourceAction != null && sourceAction.startsWith("ORDINARY_EVENT:")) {
                    yield "突发事件";
                }
                if (sourceAction != null && sourceAction.startsWith("INTERACTION_CHOICE:")) {
                    yield "直播互动";
                }
                yield "行动记录";
            }
        };
    }
}
