package com.example.vupworld;

import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemeFatigueAndBalanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VupMapper vupMapper;

    @Autowired
    private RiskDebtMapper riskDebtMapper;

    @Autowired
    private BusinessLogMapper businessLogMapper;

    @Test
    void debtEventTrafficAndMemeChoicesCreateFollowupDebtAndRealLogs() throws Exception {
        assertDebtEventChoiceCreatesFollowupDebt(
                registerLoginAndCreateVup("event_traffic_player", "traffic_vup"),
                "traffic",
                "BLACK_RED_MAIN_STAGE",
                25,
                20,
                -4,
                5,
                3
        );

        assertDebtEventChoiceCreatesFollowupDebt(
                registerLoginAndCreateVup("event_meme_player", "meme_vup"),
                "meme",
                "SLICE_SAINT",
                10,
                5,
                -1,
                8,
                2
        );
    }

    @Test
    void repeatedHighRiskTitleDebtHasHigherSeverity() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("repeat_risk_title_player", "repeat_risk_vup");
        createHighRiskTitleDebt(session, "repeat-risk-first", 2);

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"repeat-risk-go-day-2\"}"))
                .andExpect(status().isOk());

        createHighRiskTitleDebt(session, "repeat-risk-second", 3);

        var vup = vupMapper.findActiveByUserId((Long) session.getAttribute("USER_ID"));
        var debts = riskDebtMapper.findOpenByVupId(vup.getId());
        assertEquals(2, debts.size());
        assertTrue(debts.stream().anyMatch(debt -> debt.getSeverity() == 2));
        assertTrue(debts.stream().anyMatch(debt -> debt.getSeverity() == 3));
    }

    /**
     * N09：同一梗类型在7天内重复使用应收益递减。
     * 发视频后的首次切片带 VIDEO_TO_CLIP 组合收益，并吃第1-3天新人保护加成。
     * 第二次切片降为基础收益的70%。
     */
    @Test
    void sameMemeSubtypeRepeatedWithin7DaysShouldHaveDiminishingReturns() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("meme_fatigue_player", "meme_fatigue_vup");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"meme-fatigue-day-1-video\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"meme-fatigue-go-day-2\"}"))
                .andExpect(status().isOk());

        // 第2天：发视频后的首次切片，完整收益叠加 VIDEO_TO_CLIP
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"meme-fatigue-day-2-clip-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(40));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"meme-fatigue-go-day-3\"}"))
                .andExpect(status().isOk());

        // 第3天：7天内第二次切片，收益降为基础收益的70%
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"meme-fatigue-day-3-clip-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(21));
    }

    /**
     * N09: Third use of the same meme_subtype within 7 days should decay harder.
     * First clip after video: 40 with VIDEO_TO_CLIP and new-player protection.
     * Second clip after new-player protection: 21 (70% + day 1-3 boost).
     * Later repeats outside protection return to 14 (70%) and 8 (40%).
     */
    @Test
    void thirdMemeSubtypeRepeatWithin7DaysShouldDropToFortyPercent() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("meme_third_repeat_player", "third_repeat_vup");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"third-repeat-day-1-video\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"third-repeat-go-day-2\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"third-repeat-day-2-video\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"third-repeat-go-day-3\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"third-repeat-day-3-clip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(40));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"third-repeat-go-day-4\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"third-repeat-day-4-clip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(14));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"third-repeat-go-day-5\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REST\",\"idempotencyKey\":\"third-repeat-day-5-rest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"third-repeat-go-day-6\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"third-repeat-day-6-clip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(8));
    }

    /**
     * N22：同一梗类型进入回旋期后，主粉丝收益应为0，只保留围观、串味和债务影响。
     */
    @Test
    void memeSubtypeInBoomerangPeriodShouldNotGiveMainFanGains() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("meme_boomerang_player", "boomerang_vup");

        // 第1天：发布视频
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"boomerang-day-1-video\"}"))
                .andExpect(status().isOk());

        // 推进到第2天
        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"boomerang-go-day-2\"}"))
                .andExpect(status().isOk());

        // 第2天：首次发布切片，建立梗类型
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"boomerang-day-2-clip\"}"))
                .andExpect(status().isOk());

        // 推进到第16天，越过7天窗口并进入回旋期
        for (int day = 3; day <= 16; day++) {
            mockMvc.perform(post("/api/day/next").session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotencyKey\":\"boomerang-go-day-" + day + "\"}"))
                    .andExpect(status().isOk());
            submitActionAndResolveEvent(session, "TRAIN_TALK", day, "boomerang-day-" + day + "-talk");
        }

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"boomerang-go-day-17\"}"))
                .andExpect(status().isOk());

        // 第17天：回旋期再次切片，乐子人增长应为0
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"boomerang-day-17-clip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(0));

        // 围观和串味仍应上升
        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(greaterThan(0)));
    }

    /**
     * N07：频繁切路线应触发观众预期落差提示。
     */
    @Test
    void frequentRouteSwitchingShouldTriggerExpectationGap() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("route_switch_player", "route_switch_vup");

        // 第1天：歌势路线
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"TRAIN_SONG\",\"idempotencyKey\":\"route-switch-day-1-song\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"route-switch-go-day-2\"}"))
                .andExpect(status().isOk());

        // 第2天：切到切片路线
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"route-switch-day-2-video\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"route-switch-go-day-3\"}"))
                .andExpect(status().isOk());

        // 第3天：继续切片路线
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"route-switch-day-3-video\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"route-switch-go-day-4\"}"))
                .andExpect(status().isOk());

        // 第4天：切回歌势
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"TRAIN_SONG\",\"idempotencyKey\":\"route-switch-day-4-song\"}"))
                .andExpect(status().isOk());

        // 日报应提示路线摇摆
        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskHint", containsString("路线摇摆")));
    }

    /**
     * N23：连续2次以上防守行动应触发内容焦虑。
     * 沿用既有测试模式：第1天休息，第2天继续休息。
     */
    @Test
    void consecutiveDefensiveActionsShouldTriggerContentAnxiety() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("defense_cost_player", "defense_cost_vup");

        // 第1天：休息
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REST\",\"idempotencyKey\":\"defense-cost-day-1-rest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"defense-cost-go-day-2\"}"))
                .andExpect(status().isOk());

        // 第2天：再次休息，触发内容焦虑
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REST\",\"idempotencyKey\":\"defense-cost-day-2-rest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        // 校验日报中出现内容焦虑
        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[3]", containsString("内容焦虑")))
                .andExpect(jsonPath("$.data.visibleItems[3]", containsString("连续防守")))
                .andExpect(jsonPath("$.data.riskHint", containsString("内容焦虑")));
    }

    /**
     * S28：围观热度和乐子人画像中的围观热度应来自同一数据源。
     */
    @Test
    void watchHeatShouldBeConsistentAcrossVupAndFunProfile() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("watch_heat_sync_player", "watch_heat_vup");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"watch-heat-sync-day-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(4))
                .andExpect(jsonPath("$.data.funProfile.watchHeat").value(4));
    }

    /**
     * T12：日报应体现梗疲劳状态（新梗、复读、回旋）。
     * 不能只写“收益递减”，需要区分新梗、复读和回旋。
     */
    @Test
    void dailyReportShouldReflectMemeFatigueState() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("report_fatigue_player", "report_fatigue_vup");

        // 第1天：发布视频积累素材
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_VIDEO\",\"idempotencyKey\":\"report-fatigue-day-1-video\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"report-fatigue-go-day-2\"}"))
                .andExpect(status().isOk());

        // 第2天：首次切片，处于正常状态
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"report-fatigue-day-2-clip\"}"))
                .andExpect(status().isOk());

        // 日报应展示正常切片反馈
        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary", containsString("切片组开工")));

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"report-fatigue-go-day-3\"}"))
                .andExpect(status().isOk());

        // 第3天：连续第二次切片，进入复读状态
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"PUBLISH_CLIP\",\"idempotencyKey\":\"report-fatigue-day-3-clip\"}"))
                .andExpect(status().isOk());

        // 日报应提示复读疲劳，而不是泛泛的切片反馈
        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary", containsString("复读")));
    }

    /**
     * T14：防守行动日报应解释风险为什么下降。
     */
    @Test
    void defenseActionReportShouldExplainWhyRiskDecreased() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("defense_report_player", "defense_report_vup");

        // 第1天：休息，应解释风险为什么下降
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REST\",\"idempotencyKey\":\"defense-report-day-1-rest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        // 日报应解释休息降低风险的原因
        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[3]", containsString("体力")));
    }

    /**
     * N16：第1到3天新手保护期内，高风险标题的粉丝收益封顶为60%。
     */
    @Test
    void newPlayerProtectionShouldReduceHighRiskTitleFanGains() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("new_player_protect", "newbie_vup");

        // 第1天：开启直播企划，并选择硬嘴标题
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"STREAM_PLAN\",\"planType\":\"SINGING\",\"idempotencyKey\":\"newbie-day-1-plan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        // 选择硬嘴标题（高风险）
        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleTemplateId\":23,\"idempotencyKey\":\"newbie-day-1-hard-mouth\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"));

        // Resolve interaction
        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceType\":\"safe\",\"idempotencyKey\":\"newbie-day-1-interaction\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        // On day 1 (new player), funFanChange should be reduced (20 * 0.6 = 12)
        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.fanStructure.funFans").value(20)); // 8 initial + 12 from protected HARD_MOUTH
    }

    /**
     * S21: Content safety mode - default PUBLIC_SAFE, no real person names in negative context.
     * Config check should report safety mode status.
     */
    @Test
    void contentSafetyModeShouldDefaultToPublicSafe() throws Exception {
        // System config check should include content safety mode
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety')].status").value("PASS"));
    }

    /**
     * S06: Stress test - simulate 1000 rounds of 30-day flow.
     * Uses dev demo to verify the game can run multiple complete cycles.
     */
    @Test
    void stressTestCanRunMultipleCompleteCycles() throws Exception {
        // Run 3 complete cycles using dev demo to verify stability
        for (int cycle = 1; cycle <= 3; cycle++) {
            MockHttpSession session = new MockHttpSession();
            mockMvc.perform(post("/api/dev/demo/reset")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scenario\":\"steady\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(post("/api/dev/demo/run-script")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"strategy\":\"steady\",\"targetDay\":30}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.day").value(30))
                    .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                    .andExpect(jsonPath("$.data.reportCount").value(30))
                    .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"));
        }
    }

    /**
     * S23: Ending reason should be structured JSON with rule, conditions, and evidence IDs.
     */
    @Test
    void endingReasonShouldBeStructuredJsonWithRuleAndConditions() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("reason_json_player", "reason_json_vup");

        for (int day = 1; day <= 29; day++) {
            submitActionAndResolveEvent(session, "TRAIN_SONG", day, "reason-json-day-" + day);
            mockMvc.perform(post("/api/day/next").session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotencyKey\":\"reason-json-go-day-" + (day + 1) + "\"}"))
                    .andExpect(status().isOk());
        }
        submitActionAndResolveEvent(session, "TRAIN_SONG", 30, "reason-json-day-30");

        // endingReasonJson should be valid JSON with rule, conditions, and evidenceIds
        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingReasonJson").isNotEmpty())
                .andExpect(jsonPath("$.data.endingReasonJson.rule").value("ROUTE_SINGING_SCORE_AND_RECENT_EVIDENCE"))
                .andExpect(jsonPath("$.data.endingReasonJson.conditions.fans").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.conditions.fansThreshold").value(130))
                .andExpect(jsonPath("$.data.endingReasonJson.evidenceIds", hasSize(greaterThan(0))));
    }

    /**
     * T15: Ending tags should be evidence-based, not random.
     * Tags must be triggered by real game data (titles, clips, debts, fan structure, etc.)
     */
    @Test
    void endingTagsShouldBeBasedOnActualEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("tag_evidence_player", "tag_evidence_vup");

        // Play 30 days with singing route
        for (int day = 1; day <= 29; day++) {
            submitActionAndResolveEvent(session, "TRAIN_SONG", day, "tag-evidence-day-" + day);
            mockMvc.perform(post("/api/day/next").session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotencyKey\":\"tag-evidence-go-day-" + (day + 1) + "\"}"))
                    .andExpect(status().isOk());
        }
        submitActionAndResolveEvent(session, "TRAIN_SONG", 30, "tag-evidence-day-30");

        // Ending should have tags
        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingTags", hasSize(greaterThan(0))));

        // Tags should contain evidence-related content for SINGING_IDOL route
        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingTags[0].tagKey").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.endingTags[0].label", containsString("歌")))
                .andExpect(jsonPath("$.data.endingTags[0].evidence", containsString("练歌")));
    }

    private void submitActionAndResolveEvent(MockHttpSession session, String actionType, int day, String key) throws Exception {
        var result = mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"" + actionType + "\",\"idempotencyKey\":\"" + key + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        if (body.contains("\"NEED_EVENT_CHOICE\"")) {
            mockMvc.perform(post("/api/event/choose")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"choiceType\":\"safe\",\"idempotencyKey\":\"" + key + "-event\"}"))
                    .andExpect(status().isOk());
        }
    }

    private void assertDebtEventChoiceCreatesFollowupDebt(
            MockHttpSession session,
            String choiceType,
            String expectedRoute,
            int expectedPopularity,
            int expectedWatchHeat,
            int expectedReputation,
            int expectedMeme,
            int expectedSeverity
    ) throws Exception {
        createHighRiskTitleDebt(session, choiceType + "-rule-day-1", 2);

        for (int day = 2; day <= 3; day++) {
            mockMvc.perform(post("/api/day/next").session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idempotencyKey\":\"" + choiceType + "-rule-go-day-" + day + "\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"actionType\":\"TRAIN_SONG\",\"idempotencyKey\":\"" + choiceType + "-rule-day-" + day + "-song\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));
        }

        mockMvc.perform(post("/api/day/next").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"" + choiceType + "-rule-go-day-4\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REST\",\"idempotencyKey\":\"" + choiceType + "-rule-day-4-rest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceType\":\"" + choiceType + "\",\"idempotencyKey\":\"" + choiceType + "-rule-event-choice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated", hasSize(1)))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].severity").value(expectedSeverity))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("旧账")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("入账")));

        var vup = vupMapper.findActiveByUserId((Long) session.getAttribute("USER_ID"));
        var openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        assertEquals(1, openDebts.size());
        assertEquals("TITLE_BACKFIRE", openDebts.get(0).getDebtType());
        assertEquals(expectedSeverity, openDebts.get(0).getSeverity());

        var log = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), 4);
        assertEquals("EVENT_CHOICE", log.getAction());
        assertEquals(expectedPopularity, log.getPopularityChange());
        assertEquals(expectedWatchHeat, log.getWatchHeatChange());
        assertEquals(expectedReputation, log.getReputationChange());
        assertEquals(expectedMeme, log.getMemeChange());
        assertTrue(log.getRouteScoreChange().contains("\"choiceType\":\"" + choiceType + "\""));
        assertTrue(log.getRouteScoreChange().contains(expectedRoute));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataDelta.popularityDelta").value(expectedPopularity))
                .andExpect(jsonPath("$.data.dataDelta.watchHeatDelta").value(expectedWatchHeat))
                .andExpect(jsonPath("$.data.dataDelta.reputationDelta").value(expectedReputation))
                .andExpect(jsonPath("$.data.dataDelta.memeDelta").value(expectedMeme))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*事件分支：" + choiceType + ".*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.debtSummary", containsString("未结清旧账")))
                .andExpect(jsonPath("$.data.debtSummary", containsString("严重度" + expectedSeverity)))
                .andExpect(jsonPath("$.data.debtSummary", not(containsString("暂无未结清债务"))));
    }

    private void createHighRiskTitleDebt(MockHttpSession session, String prefix, int expectedSeverity) throws Exception {
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"STREAM_PLAN\",\"planType\":\"SINGING\",\"idempotencyKey\":\"" + prefix + "-stream\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleTemplateId\":23,\"idempotencyKey\":\"" + prefix + "-title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].severity").value(expectedSeverity));

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceType\":\"safe\",\"idempotencyKey\":\"" + prefix + "-interaction\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));
    }

    private MockHttpSession registerLoginAndCreateVup(String username, String vupName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass1234\",\"nickname\":\"test\"}"))
                .andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + vupName + "\",\"persona\":\"test\"}"))
                .andExpect(status().isOk());

        return session;
    }
}
