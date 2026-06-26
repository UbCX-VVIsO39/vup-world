package com.example.vupworld.model;

public class RiskDebt {
    private Long id;
    private Long vupId;
    private Long sourceLogId;
    private String debtType;
    private String status;
    private int severity;
    private int createDay;
    private int dueDay;
    private String sourceAction;
    private String sourceTitle;
    private String summary;

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

    public Long getSourceLogId() {
        return sourceLogId;
    }

    public void setSourceLogId(Long sourceLogId) {
        this.sourceLogId = sourceLogId;
    }

    public String getDebtType() {
        return debtType;
    }

    public void setDebtType(String debtType) {
        this.debtType = debtType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public int getCreateDay() {
        return createDay;
    }

    public void setCreateDay(int createDay) {
        this.createDay = createDay;
    }

    public int getDueDay() {
        return dueDay;
    }

    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
    }

    public String getSourceAction() {
        return sourceAction;
    }

    public void setSourceAction(String sourceAction) {
        this.sourceAction = sourceAction;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
