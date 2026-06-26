package com.example.vupworld.model;

import java.time.LocalDateTime;

public class GameUnlock {
    private Long id;
    private Long userId;
    private String unlockType;
    private String unlockKey;
    private String sourceEndingType;
    private int sourceRunScore;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUnlockType() { return unlockType; }
    public void setUnlockType(String unlockType) { this.unlockType = unlockType; }
    public String getUnlockKey() { return unlockKey; }
    public void setUnlockKey(String unlockKey) { this.unlockKey = unlockKey; }
    public String getSourceEndingType() { return sourceEndingType; }
    public void setSourceEndingType(String sourceEndingType) { this.sourceEndingType = sourceEndingType; }
    public int getSourceRunScore() { return sourceRunScore; }
    public void setSourceRunScore(int sourceRunScore) { this.sourceRunScore = sourceRunScore; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
