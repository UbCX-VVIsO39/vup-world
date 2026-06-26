package com.example.vupworld.domain;

public enum DayPhase {
    READY,
    NEED_TITLE,
    ACTION_RESOLVED,
    OFF_STREAM_READY,      // 等待选择下播行动
    OFF_STREAM_RESOLVED,   // 下播行动已结算
    NEED_INTERACTION_CHOICE,
    NEED_EVENT_CHOICE,
    REPORT_READY,
    ENDING_READY
}
