package com.example.vupworld.service.content;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PlatformTrendService {
    public static final String DEFAULT_TREND_ID = "SAFE_WEEK";
    public static final String SINGING_BOOST_WEEK = "SINGING_BOOST_WEEK";
    public static final String MEME_OUTBREAK_WEEK = "MEME_OUTBREAK_WEEK";
    public static final String COMMERCIAL_REVIEW_WEEK = "COMMERCIAL_REVIEW_WEEK";

    public Map<String, Object> currentTrendForDay(int day) {
        return trendFor(trendIdForDay(day));
    }

    public String trendIdForDay(int day) {
        if (day >= 22) {
            return COMMERCIAL_REVIEW_WEEK;
        }
        if (day >= 15) {
            return MEME_OUTBREAK_WEEK;
        }
        if (day >= 8) {
            return SINGING_BOOST_WEEK;
        }
        return DEFAULT_TREND_ID;
    }

    public Map<String, Object> trendFor(String trendId) {
        if (DEFAULT_TREND_ID.equals(trendId)) {
            return Map.of(
                    "id", DEFAULT_TREND_ID,
                    "label", "清朗低压周",
                    "description", "平台口味收紧，低压、稳健和基本功内容更容易被推；标题太冲容易压流，乐子人先别急着开庭。"
            );
        }
        if (SINGING_BOOST_WEEK.equals(trendId)) {
            return Map.of(
                    "id", SINGING_BOOST_WEEK,
                    "label", "歌回扶持周",
                    "description", "平台最近爱推歌回和基本功切片，稳唱、投稿和低压歌杂更容易进推荐，预支高音仍然会被录播组记账。"
            );
        }
        if (MEME_OUTBREAK_WEEK.equals(trendId)) {
            return Map.of(
                    "id", MEME_OUTBREAK_WEEK,
                    "label", "抽象出圈周",
                    "description", "平台流量开始偏爱整活和切片传播，乐子人进场更快，但标题太冲会让米线压力一起上涨。"
            );
        }
        if (COMMERCIAL_REVIEW_WEEK.equals(trendId)) {
            return Map.of(
                    "id", COMMERCIAL_REVIEW_WEEK,
                    "label", "商业复审周",
                    "description", "平台更看重稳定排班和商业转化，高亮互动回应和低压陪伴更容易形成运营反馈，但老粉会盯着你别太像上班。"
            );
        }
        return Map.of(
                "id", trendId == null ? "UNKNOWN" : trendId,
                "label", "未知风向",
                "description", "平台口味暂未明示，日报组只能先按普通流量池观察。"
        );
    }
}
