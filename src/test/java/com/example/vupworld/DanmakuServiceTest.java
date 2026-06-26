package com.example.vupworld;

import com.example.vupworld.dto.DanmakuDtos.DanmakuListDTO;
import com.example.vupworld.mapper.ContentCatalogMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.ContentCatalogService;
import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.content.DanmakuService;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DanmakuServiceTest {

    private final DeterministicRngService deterministicRngService =
            new DeterministicRngService(new DeterministicRngService.DaySessionMapperHelper() {
                @Override
                public void updateRngCursor(Long sessionId, int cursor) {
                }
            });
    private final ContentCatalogService contentCatalogService =
            new ContentCatalogService(new ContentCatalogMapper() {
                @Override
                public java.util.List<com.example.vupworld.model.GameContent> selectAll() {
                    return Collections.emptyList();
                }
                @Override
                public void insertBatch(java.util.List<com.example.vupworld.model.GameContent> list) {
                }
            });
    private final DanmakuService danmakuService = new DanmakuService(deterministicRngService, contentCatalogService);

    private DaySession createTestSession() {
        DaySession session = new DaySession();
        session.setId(1L);
        session.setRandomSeed("test-seed");
        session.setRngCursor(0);
        return session;
    }

    @Test
    void singingRouteSurfacesSingerAudienceDanmaku() {
        Vup vup = new Vup();
        vup.setCurrentRoute("SINGING_IDOL");
        vup.setSongPower(36);
        vup.setPopularity(50);
        vup.setReputation(70);

        DanmakuListDTO result = danmakuService.generateDanmaku(vup, "REST", "低压休整一天", createTestSession());

        assertTrue(result.danmakus().stream().anyMatch(danmaku ->
                "歌势民".equals(danmaku.persona())
                        && danmaku.text().contains("高音")
                        && danmaku.text().contains("气口")));
    }

    @Test
    void cyberGirlfriendRouteSurfacesCompanionAudienceDanmaku() {
        Vup vup = new Vup();
        vup.setCurrentRoute("CYBER_GIRLFRIEND");
        vup.setPopularity(62);
        vup.setReputation(68);

        DanmakuListDTO result = danmakuService.generateDanmaku(vup, "REST", "低压陪伴回收工", createTestSession());

        assertTrue(result.danmakus().stream().anyMatch(danmaku ->
                "陪伴粉".equals(danmaku.persona())
                        && danmaku.text().contains("陪伴感")
                        && danmaku.text().contains("排班表")));
    }

    @Test
    void singingBoostWeekSurfacesPlatformTasteDanmaku() {
        Vup vup = new Vup();
        vup.setDayCount(8);
        vup.setPopularity(55);
        vup.setReputation(70);

        DanmakuListDTO result = danmakuService.generateDanmaku(vup, "TRAIN_SONG", "练歌扶持周", createTestSession());

        assertTrue(result.danmakus().stream().anyMatch(danmaku ->
                "平台算法".equals(danmaku.persona())
                        && danmaku.text().contains("歌回扶持周")
                        && danmaku.text().contains("高音")));
    }

    @Test
    void memeOutbreakWeekSurfacesClipAndBoundaryDanmaku() {
        Vup vup = new Vup();
        vup.setDayCount(15);
        vup.setPopularity(55);
        vup.setReputation(70);

        DanmakuListDTO result = danmakuService.generateDanmaku(vup, "PUBLISH_CLIP", "切片进场", createTestSession());

        assertTrue(result.danmakus().stream().anyMatch(danmaku ->
                "平台算法".equals(danmaku.persona())
                        && danmaku.text().contains("抽象出圈周")
                        && danmaku.text().contains("米线")));
    }
}
