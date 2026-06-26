package com.example.vupworld.model;

public class EndingReview {
    private Long id;
    private Long vupId;
    private Long finalReportId;
    private String endingType;
    private String finalTitle;
    private String subtitle;
    private String endingTagsJson;
    private String endingReason;
    private String endingReasonJson;
    private String summary;
    private String fanProfileJson;
    private String keyEventsJson;
    private String debtRefsJson;
    private String routeReviewJson;
    private String restartHint;
    private String timelineJson = "[]";
    private String careerStatsJson = "{}";
    private String playerProfileJson = "{}";
    private String comparisonJson = "{}";

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

    public Long getFinalReportId() {
        return finalReportId;
    }

    public void setFinalReportId(Long finalReportId) {
        this.finalReportId = finalReportId;
    }

    public String getEndingType() {
        return endingType;
    }

    public void setEndingType(String endingType) {
        this.endingType = endingType;
    }

    public String getFinalTitle() {
        return finalTitle;
    }

    public void setFinalTitle(String finalTitle) {
        this.finalTitle = finalTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getEndingTagsJson() {
        return endingTagsJson;
    }

    public void setEndingTagsJson(String endingTagsJson) {
        this.endingTagsJson = endingTagsJson;
    }

    public String getEndingReason() {
        return endingReason;
    }

    public void setEndingReason(String endingReason) {
        this.endingReason = endingReason;
    }

    public String getEndingReasonJson() {
        return endingReasonJson;
    }

    public void setEndingReasonJson(String endingReasonJson) {
        this.endingReasonJson = endingReasonJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFanProfileJson() {
        return fanProfileJson;
    }

    public void setFanProfileJson(String fanProfileJson) {
        this.fanProfileJson = fanProfileJson;
    }

    public String getKeyEventsJson() {
        return keyEventsJson;
    }

    public void setKeyEventsJson(String keyEventsJson) {
        this.keyEventsJson = keyEventsJson;
    }

    public String getDebtRefsJson() {
        return debtRefsJson;
    }

    public void setDebtRefsJson(String debtRefsJson) {
        this.debtRefsJson = debtRefsJson;
    }

    public String getRouteReviewJson() {
        return routeReviewJson;
    }

    public void setRouteReviewJson(String routeReviewJson) {
        this.routeReviewJson = routeReviewJson;
    }

    public String getRestartHint() {
        return restartHint;
    }

    public void setRestartHint(String restartHint) {
        this.restartHint = restartHint;
    }

    public String getTimelineJson() {
        return timelineJson;
    }

    public void setTimelineJson(String timelineJson) {
        this.timelineJson = timelineJson;
    }

    public String getCareerStatsJson() {
        return careerStatsJson;
    }

    public void setCareerStatsJson(String careerStatsJson) {
        this.careerStatsJson = careerStatsJson;
    }

    public String getPlayerProfileJson() {
        return playerProfileJson;
    }

    public void setPlayerProfileJson(String playerProfileJson) {
        this.playerProfileJson = playerProfileJson;
    }

    public String getComparisonJson() {
        return comparisonJson;
    }

    public void setComparisonJson(String comparisonJson) {
        this.comparisonJson = comparisonJson;
    }
}
