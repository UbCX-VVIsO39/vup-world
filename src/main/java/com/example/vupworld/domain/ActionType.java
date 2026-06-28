package com.example.vupworld.domain;

public enum ActionType {
    TRAIN_SONG,
    TRAIN_DANCE,
    TRAIN_TALK,
    STREAM_PLAN,
    PUBLISH_VIDEO,
    PUBLISH_CLIP,
    FAN_GROUP_MAINTAIN,
    NPC_INTERACT,
    REST;

    /**
     * 主行动消耗的行动点。统一 2 点；场外行动在别处单独处理。
     */
    public int actionPointCost() {
        return 2;
    }
}
