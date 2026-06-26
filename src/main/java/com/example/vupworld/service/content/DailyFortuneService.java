package com.example.vupworld.service.content;

import com.example.vupworld.dto.DailyFortuneDtos.DailyFortuneDTO;
import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DailyFortuneService {

    public DailyFortuneService() {
    }

    private static final List<DailyFortuneDTO> FORTUNES = List.of(
            new DailyFortuneDTO(
                    "大吉",
                    "今天运势极佳，适合尝试高风险高收益的行动。",
                    "直播企划",
                    "🌟",
                    "好运"
            ),
            new DailyFortuneDTO(
                    "中吉",
                    "运势不错，适合稳步推进。",
                    "练歌或练舞",
                    "⭐",
                    "顺利"
            ),
            new DailyFortuneDTO(
                    "小吉",
                    "运势平稳，适合日常运营。",
                    "粉丝群维护",
                    "✨",
                    "平稳"
            ),
            new DailyFortuneDTO(
                    "吉",
                    "运势正常，按计划行动即可。",
                    "杂谈复盘",
                    "💫",
                    "正常"
            ),
            new DailyFortuneDTO(
                    "末吉",
                    "运势一般，建议低调行事。",
                    "休息",
                    "🌙",
                    "低调"
            ),
            new DailyFortuneDTO(
                    "凶",
                    "运势不佳，避免高风险行动。",
                    "休息或低压行动",
                    "⚡",
                    "谨慎"
            ),
            new DailyFortuneDTO(
                    "大凶",
                    "运势很差，建议今天休息。",
                    "休息",
                    "🌑",
                    "休息"
            )
    );

    private static final List<String> LUCKY_ACTIONS = List.of(
            "直播企划",
            "练歌",
            "练舞",
            "杂谈复盘",
            "发布视频",
            "发布切片",
            "粉丝群维护",
            "同台互动",
            "休息"
    );

    public DailyFortuneDTO getFortune(int day, int fans, int reputation, DaySession session) {
        int rand = stableDailyRoll(day, fans, reputation, session);

        // 声望高时，好运势概率增加
        if (reputation >= 70) {
            if (rand < 30) return FORTUNES.get(0); // 大吉
            if (rand < 60) return FORTUNES.get(1); // 中吉
            if (rand < 80) return FORTUNES.get(2); // 小吉
            return FORTUNES.get(3); // 吉
        }

        // 声望低时，差运势概率增加
        if (reputation < 40) {
            if (rand < 10) return FORTUNES.get(0); // 大吉
            if (rand < 25) return FORTUNES.get(1); // 中吉
            if (rand < 45) return FORTUNES.get(2); // 小吉
            if (rand < 65) return FORTUNES.get(3); // 吉
            if (rand < 80) return FORTUNES.get(4); // 末吉
            if (rand < 95) return FORTUNES.get(5); // 凶
            return FORTUNES.get(6); // 大凶
        }

        // 正常情况
        if (rand < 15) return FORTUNES.get(0); // 大吉
        if (rand < 35) return FORTUNES.get(1); // 中吉
        if (rand < 55) return FORTUNES.get(2); // 小吉
        if (rand < 75) return FORTUNES.get(3); // 吉
        if (rand < 88) return FORTUNES.get(4); // 末吉
        if (rand < 96) return FORTUNES.get(5); // 凶
        return FORTUNES.get(6); // 大凶
    }

    private int stableDailyRoll(int day, int fans, int reputation, DaySession session) {
        String seed = (session == null || session.getRandomSeed() == null ? "daily-fortune" : session.getRandomSeed())
                + ":" + day + ":" + Math.max(0, fans / 50) + ":" + Math.max(0, reputation / 10);
        return Math.floorMod(seed.hashCode(), 100);
    }

    public int getFortuneCount() {
        return FORTUNES.size();
    }
}
