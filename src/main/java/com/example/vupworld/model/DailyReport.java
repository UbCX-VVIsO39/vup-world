package com.example.vupworld.model;

public class DailyReport {
    private Long id;
    private Long vupId;
    private int day;
    private String summary;
    private String selectedTitle;
    private String reportTone;
    private String platformTrendId;
    private int fanDelta;
    private int coinDelta;
    private int popularityDelta;
    private int watchHeatDelta;
    private int reputationDelta;
    private int memeDelta;
    private int commercialDelta;
    private Long highlightEventId;
    private String debtSummary;
    private String riskHint;
    private String visibleItemsJson;
    private String evidenceRefsJson;
    private String templateRefsJson;
    private String renderVersion;
    private String strategyPanelJson = "{}";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVupId() {
        return vupId;
    }

    public void setVupId(Long vupId) {
        this.vupId = vupId;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSelectedTitle() {
        return selectedTitle;
    }

    public void setSelectedTitle(String selectedTitle) {
        this.selectedTitle = selectedTitle;
    }

    public String getReportTone() {
        return reportTone;
    }

    public void setReportTone(String reportTone) {
        this.reportTone = reportTone;
    }

    public String getPlatformTrendId() {
        return platformTrendId;
    }

    public void setPlatformTrendId(String platformTrendId) {
        this.platformTrendId = platformTrendId;
    }

    public int getFanDelta() {
        return fanDelta;
    }

    public void setFanDelta(int fanDelta) {
        this.fanDelta = fanDelta;
    }

    public int getCoinDelta() {
        return coinDelta;
    }

    public void setCoinDelta(int coinDelta) {
        this.coinDelta = coinDelta;
    }

    public int getPopularityDelta() {
        return popularityDelta;
    }

    public void setPopularityDelta(int popularityDelta) {
        this.popularityDelta = popularityDelta;
    }

    public int getWatchHeatDelta() {
        return watchHeatDelta;
    }

    public void setWatchHeatDelta(int watchHeatDelta) {
        this.watchHeatDelta = watchHeatDelta;
    }

    public int getReputationDelta() {
        return reputationDelta;
    }

    public void setReputationDelta(int reputationDelta) {
        this.reputationDelta = reputationDelta;
    }

    public int getMemeDelta() {
        return memeDelta;
    }

    public void setMemeDelta(int memeDelta) {
        this.memeDelta = memeDelta;
    }

    public int getCommercialDelta() {
        return commercialDelta;
    }

    public void setCommercialDelta(int commercialDelta) {
        this.commercialDelta = commercialDelta;
    }

    public Long getHighlightEventId() {
        return highlightEventId;
    }

    public void setHighlightEventId(Long highlightEventId) {
        this.highlightEventId = highlightEventId;
    }

    public String getDebtSummary() {
        return debtSummary;
    }

    public void setDebtSummary(String debtSummary) {
        this.debtSummary = debtSummary;
    }

    public String getRiskHint() {
        return riskHint;
    }

    public void setRiskHint(String riskHint) {
        this.riskHint = riskHint;
    }

    public String getVisibleItemsJson() {
        return visibleItemsJson;
    }

    public void setVisibleItemsJson(String visibleItemsJson) {
        this.visibleItemsJson = visibleItemsJson;
    }

    public String getEvidenceRefsJson() {
        return evidenceRefsJson;
    }

    public void setEvidenceRefsJson(String evidenceRefsJson) {
        this.evidenceRefsJson = evidenceRefsJson;
    }

    public String getTemplateRefsJson() {
        return templateRefsJson;
    }

    public void setTemplateRefsJson(String templateRefsJson) {
        this.templateRefsJson = templateRefsJson;
    }

    public String getRenderVersion() {
        return renderVersion;
    }

    public void setRenderVersion(String renderVersion) {
        this.renderVersion = renderVersion;
    }

    public String getStrategyPanelJson() {
        return strategyPanelJson;
    }

    public void setStrategyPanelJson(String strategyPanelJson) {
        this.strategyPanelJson = strategyPanelJson;
    }
}
