package com.example.vupworld.service.risk;

public final class RiskDebtLabels {
    private RiskDebtLabels() {
    }

    public static String debtTypeLabel(String debtType) {
        return switch (debtType) {
            case "TITLE_BACKFIRE" -> "标题党反噬";
            case "BOOMERANG_CLIP" -> "回旋镖切片";
            case "UNICORN_EXPECTATION" -> "独角兽期待落差";
            case "COMMERCIAL_BACKLASH" -> "商业反噬";
            case "BLACK_HISTORY_STOCK" -> "黑历史库存";
            case "VOICE_ACCIDENT" -> "声线事故";
            case "COLLAB_SPILLOVER" -> "联动外溢";
            case "FAN_GROUP_DRAMA" -> "粉丝群小作文";
            default -> "未归档旧账";
        };
    }
}
