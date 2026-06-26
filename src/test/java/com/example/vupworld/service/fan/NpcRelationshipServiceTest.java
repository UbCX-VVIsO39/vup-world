package com.example.vupworld.service.fan;

import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 平台功能：NPC互动系统的TDD测试。
 * 验证新增的弹幕互动类型（praise/flame/technical）和偷学行为。
 */
class NpcRelationshipServiceTest {

    private VupMapper vupMapper;
    private DaySessionMapper daySessionMapper;
    private JsonService jsonService;
    private NpcRelationshipService service;

    @BeforeEach
    void setUp() {
        vupMapper = mock(VupMapper.class);
        daySessionMapper = mock(DaySessionMapper.class);
        jsonService = new JsonService(new ObjectMapper());
        DeterministicRngService rngService = mock(DeterministicRngService.class);
        service = new NpcRelationshipService(vupMapper, daySessionMapper, jsonService, rngService);
    }

    private Vup createVup() {
        Vup vup = new Vup();
        vup.setId(1L);
        vup.setDayCount(1);
        vup.setRunSeed("test-seed");
        return vup;
    }

    private DaySession createSession() {
        DaySession session = new DaySession();
        session.setRandomSeed("test-seed");
        session.setRngCursor(0);
        return session;
    }

    /**
     * Tracer bullet：发送赞美弹幕应显著提升亲密度（≥5）。
     * 当前 default 分支只给 +1~+3，此测试应为 RED。
     */
    @Test
    void praiseDanmakuIncreasesAffinity() {
        Vup vup = createVup();
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession());

        var result = service.interact(vup, "SENN_LIN", "DANMAKU_PRAISE");

        assertTrue(result.affinityChange() >= 5,
                "赞美弹幕应至少提升5点亲密度，实际为 " + result.affinityChange());
    }

    /**
     * 发送引战弹幕应降低亲密度（≤-3）。
     * 当前 default 分支给正数，此测试应为 RED。
     */
    @Test
    void flameDanmakuDecreasesAffinity() {
        Vup vup = createVup();
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession());

        var result = service.interact(vup, "SENN_LIN", "DANMAKU_FLAME");

        assertTrue(result.affinityChange() <= -3,
                "引战弹幕应至少降低3点亲密度，实际为 " + result.affinityChange());
    }

    /**
     * 发送技术弹幕应解锁对该NPC的偷学能力。
     * 需要亲密度≥15（偷学消耗门槛），预设亲密度为20。
     */
    @Test
    void technicalDanmakuUnlocksStealLearn() {
        Vup vup = createVup();
        vup.setTutorialFlagsJson("{\"npcAffinity\":{\"SENN_LIN\":20}}");
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession());

        service.interact(vup, "SENN_LIN", "DANMAKU_TECHNICAL");

        assertTrue(service.canStealLearn(vup, "SENN_LIN"),
                "发送技术弹幕后应解锁偷学");
    }

    /**
     * 偷学应消耗至少15点亲密度。
     * 预设亲密度为30、已解锁偷学，偷学后亲密度应≤15。
     */
    @Test
    void stealLearnConsumesAtLeastFifteenAffinity() {
        Vup vup = createVup();
        vup.setTutorialFlagsJson("{\"npcAffinity\":{\"SENN_LIN\":30},\"npcStealUnlocked\":{\"SENN_LIN\":true}}");
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession());

        var result = service.stealLearn(vup, "SENN_LIN");

        assertTrue(result.affinityChange() <= -15,
                "偷学应至少消耗15点亲密度，实际为 " + result.affinityChange());
        assertTrue(result.newAffinity() <= 15,
                "偷学后亲密度应至多为15，实际为 " + result.newAffinity());
    }

    /**
     * 偷学后应获得下次直播的临时buff。
     */
    @Test
    void stealLearnGrantsBuffForNextStream() {
        Vup vup = createVup();
        vup.setTutorialFlagsJson("{\"npcAffinity\":{\"SENN_LIN\":30},\"npcStealUnlocked\":{\"SENN_LIN\":true}}");
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession());

        service.stealLearn(vup, "SENN_LIN");

        assertNotNull(service.getActiveStealBuff(vup),
                "偷学后应获得临时buff");
    }

    /**
     * 被发现时（seed-c触发）应额外-10亲密度、-5声望。
     */
    @Test
    void stealLearnDiscoveryAppliesPenaltyWhenDiscovered() {
        Vup vup = createVup();
        vup.setReputation(50);
        vup.setTutorialFlagsJson("{\"npcAffinity\":{\"SENN_LIN\":30},\"npcStealUnlocked\":{\"SENN_LIN\":true}}");
        DaySession session = createSession();
        session.setRandomSeed("seed-c"); // seed-c 触发被发现
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(session);

        var result = service.stealLearn(vup, "SENN_LIN");

        assertTrue(result.discovered(), "seed-c应触发被发现");
        assertEquals(-5, result.reputationChange(), "被发现时声望应-5");
        assertEquals(45, vup.getReputation(), "被发现时声望应从50降到45");
        assertTrue(result.affinityChange() <= -25,
                "被发现时亲密度应至少-25（15消耗+10惩罚），实际为 " + result.affinityChange());
    }

    /**
     * 未被发现时（test-seed）不应有声望惩罚。
     */
    @Test
    void stealLearnNoPenaltyWhenNotDiscovered() {
        Vup vup = createVup();
        vup.setReputation(50);
        vup.setTutorialFlagsJson("{\"npcAffinity\":{\"SENN_LIN\":30},\"npcStealUnlocked\":{\"SENN_LIN\":true}}");
        when(daySessionMapper.findByVupIdAndDay(anyLong(), anyInt()))
                .thenReturn(createSession()); // test-seed, 不被发现

        var result = service.stealLearn(vup, "SENN_LIN");

        assertFalse(result.discovered(), "test-seed不应触发被发现");
        assertEquals(0, result.reputationChange(), "未被发现时声望不变");
        assertEquals(50, vup.getReputation(), "未被发现时声望不变");
    }
}
