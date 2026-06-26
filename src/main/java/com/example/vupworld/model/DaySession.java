package com.example.vupworld.model;

public class DaySession {
    private Long id;
    private Long vupId;
    private int day;
    private String phase;
    private String selectedAction;
    private Long selectedPlanId;
    private Long selectedTitleTemplateId;
    private String titleCandidatesJson;
    private int titleRerollCount;
    private boolean streamPlanCancelled;
    private boolean riskToolUsed;
    private String cancelledPlanSnapshotJson;
    private Long pendingInteractionEventId;
    private Long pendingFormalEventId;
    private String formalEventSlotStatus;
    private String formalEventSource;
    private int formalEventPriority;
    private String formalEventRollDetailJson;
    private String pendingActionResultJson;
    private String pendingEventResultJson;
    private Long reportId;
    private Long endingReviewId;
    private String randomSeed;
    private int rngCursor;
    private boolean locked;
    private String offStreamAction;

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

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(String selectedAction) {
        this.selectedAction = selectedAction;
    }

    public Long getSelectedPlanId() {
        return selectedPlanId;
    }

    public void setSelectedPlanId(Long selectedPlanId) {
        this.selectedPlanId = selectedPlanId;
    }

    public Long getSelectedTitleTemplateId() {
        return selectedTitleTemplateId;
    }

    public void setSelectedTitleTemplateId(Long selectedTitleTemplateId) {
        this.selectedTitleTemplateId = selectedTitleTemplateId;
    }

    public String getTitleCandidatesJson() {
        return titleCandidatesJson;
    }

    public void setTitleCandidatesJson(String titleCandidatesJson) {
        this.titleCandidatesJson = titleCandidatesJson;
    }

    public int getTitleRerollCount() {
        return titleRerollCount;
    }

    public void setTitleRerollCount(int titleRerollCount) {
        this.titleRerollCount = titleRerollCount;
    }

    public boolean isStreamPlanCancelled() {
        return streamPlanCancelled;
    }

    public void setStreamPlanCancelled(boolean streamPlanCancelled) {
        this.streamPlanCancelled = streamPlanCancelled;
    }

    public boolean isRiskToolUsed() {
        return riskToolUsed;
    }

    public void setRiskToolUsed(boolean riskToolUsed) {
        this.riskToolUsed = riskToolUsed;
    }

    public String getCancelledPlanSnapshotJson() {
        return cancelledPlanSnapshotJson;
    }

    public void setCancelledPlanSnapshotJson(String cancelledPlanSnapshotJson) {
        this.cancelledPlanSnapshotJson = cancelledPlanSnapshotJson;
    }

    public Long getPendingInteractionEventId() {
        return pendingInteractionEventId;
    }

    public void setPendingInteractionEventId(Long pendingInteractionEventId) {
        this.pendingInteractionEventId = pendingInteractionEventId;
    }

    public Long getPendingFormalEventId() {
        return pendingFormalEventId;
    }

    public void setPendingFormalEventId(Long pendingFormalEventId) {
        this.pendingFormalEventId = pendingFormalEventId;
    }

    public String getFormalEventSlotStatus() {
        return formalEventSlotStatus;
    }

    public void setFormalEventSlotStatus(String formalEventSlotStatus) {
        this.formalEventSlotStatus = formalEventSlotStatus;
    }

    public String getFormalEventSource() {
        return formalEventSource;
    }

    public void setFormalEventSource(String formalEventSource) {
        this.formalEventSource = formalEventSource;
    }

    public int getFormalEventPriority() {
        return formalEventPriority;
    }

    public void setFormalEventPriority(int formalEventPriority) {
        this.formalEventPriority = formalEventPriority;
    }

    public String getFormalEventRollDetailJson() {
        return formalEventRollDetailJson;
    }

    public void setFormalEventRollDetailJson(String formalEventRollDetailJson) {
        this.formalEventRollDetailJson = formalEventRollDetailJson;
    }

    public String getPendingActionResultJson() {
        return pendingActionResultJson;
    }

    public void setPendingActionResultJson(String pendingActionResultJson) {
        this.pendingActionResultJson = pendingActionResultJson;
    }

    public String getPendingEventResultJson() {
        return pendingEventResultJson;
    }

    public void setPendingEventResultJson(String pendingEventResultJson) {
        this.pendingEventResultJson = pendingEventResultJson;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getEndingReviewId() {
        return endingReviewId;
    }

    public void setEndingReviewId(Long endingReviewId) {
        this.endingReviewId = endingReviewId;
    }

    public String getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(String randomSeed) {
        this.randomSeed = randomSeed;
    }

    public int getRngCursor() {
        return rngCursor;
    }

    public void setRngCursor(int rngCursor) {
        this.rngCursor = rngCursor;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getOffStreamAction() {
        return offStreamAction;
    }

    public void setOffStreamAction(String offStreamAction) {
        this.offStreamAction = offStreamAction;
    }
}
