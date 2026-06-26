package com.example.vupworld;

import com.example.vupworld.dto.DailyHighlightDtos.DailyHighlightDTO;
import com.example.vupworld.dto.DailyHighlightDtos.HighlightListDTO;
import com.example.vupworld.service.progression.DailyHighlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DailyHighlightTest {

    @Autowired
    private DailyHighlightService dailyHighlightService;

    @Test
    void shouldGenerateHighlightForFirstStream() {
        // Given: 第一次直播
        Long vupId = 1L;
        int day = 1;
        String actionType = "STREAM_PLAN";
        String summary = "你完成了第一次直播，弹幕说新人加油。";

        // When: 生成精彩时刻
        DailyHighlightDTO highlight = dailyHighlightService.generateHighlight(vupId, day, actionType, summary);

        // Then: 应该生成一个精彩时刻
        assertNotNull(highlight);
        assertEquals(day, highlight.day());
        assertNotNull(highlight.title());
        assertNotNull(highlight.description());
        assertNotNull(highlight.icon());
        // 由于summary不包含"首播"，应该生成默认的"今日记录"
        assertTrue(highlight.title().contains("今日记录") || highlight.title().contains("首播"));
    }

    @Test
    void shouldGenerateHighlightForHighFanGain() {
        // Given: 大量涨粉
        Long vupId = 1L;
        int day = 5;
        String actionType = "PUBLISH_VIDEO";
        String summary = "视频爆火，粉丝暴涨100人。";

        // When: 生成精彩时刻
        DailyHighlightDTO highlight = dailyHighlightService.generateHighlight(vupId, day, actionType, summary);

        // Then: 应该生成涨粉相关的精彩时刻
        assertNotNull(highlight);
        assertTrue(highlight.title().contains("爆火") || highlight.title().contains("涨粉"));
    }

    @Test
    void shouldGenerateHighlightForMemeRepeat() {
        // Given: 梗被复读
        Long vupId = 1L;
        int day = 10;
        String actionType = "PUBLISH_CLIP";
        String summary = "切片被复读，梗开始被查重。";

        // When: 生成精彩时刻
        DailyHighlightDTO highlight = dailyHighlightService.generateHighlight(vupId, day, actionType, summary);

        // Then: 应该生成梗相关的精彩时刻
        assertNotNull(highlight);
        assertTrue(highlight.title().contains("复读") || highlight.title().contains("梗"));
    }

    @Test
    void shouldGetHighlightHistory() {
        // Given: 有多天的精彩时刻
        Long vupId = 1L;

        // When: 查询历史精彩时刻
        HighlightListDTO highlights = dailyHighlightService.getHighlights(vupId);

        // Then: 应该返回列表
        assertNotNull(highlights);
        assertNotNull(highlights.highlights());
    }

    @Test
    void shouldGenerateHighlightForDebtCreation() {
        // Given: 产生债务
        Long vupId = 1L;
        int day = 15;
        String actionType = "STREAM_PLAN";
        String summary = "高风险标题产生债务，楼友开始等回旋镖。";

        // When: 生成精彩时刻
        DailyHighlightDTO highlight = dailyHighlightService.generateHighlight(vupId, day, actionType, summary);

        // Then: 应该生成债务相关的精彩时刻
        assertNotNull(highlight);
        assertTrue(highlight.title().contains("债务") || highlight.title().contains("回旋镖"));
    }
}
