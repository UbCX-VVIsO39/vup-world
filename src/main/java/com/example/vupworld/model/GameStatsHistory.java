package com.example.vupworld.model;

import java.time.LocalDateTime;

public class GameStatsHistory {
    private Long id;
    private Long userId;
    private Long vupId;
    private String endingType;
    private int finalScore;
    private String finalGrade;
    private int finalFans;
    private int finalReputation;
    private int runDays;
    private String routeKey;
    private int ngPlusLevel;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getVupId() { return vupId; }
    public void setVupId(Long vupId) { this.vupId = vupId; }
    public String getEndingType() { return endingType; }
    public void setEndingType(String endingType) { this.endingType = endingType; }
    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }
    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
    public int getFinalFans() { return finalFans; }
    public void setFinalFans(int finalFans) { this.finalFans = finalFans; }
    public int getFinalReputation() { return finalReputation; }
    public void setFinalReputation(int finalReputation) { this.finalReputation = finalReputation; }
    public int getRunDays() { return runDays; }
    public void setRunDays(int runDays) { this.runDays = runDays; }
    public String getRouteKey() { return routeKey; }
    public void setRouteKey(String routeKey) { this.routeKey = routeKey; }
    public int getNgPlusLevel() { return ngPlusLevel; }
    public void setNgPlusLevel(int ngPlusLevel) { this.ngPlusLevel = ngPlusLevel; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
