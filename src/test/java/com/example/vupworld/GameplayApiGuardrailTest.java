package com.example.vupworld;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "game.run.off-stream-required=true")
class GameplayApiGuardrailTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.example.vupworld.mapper.VupMapper vupMapper;

    @Test
    void gameplayApiExposesPlayabilityFieldsAndKeepsSingleDayNumbersBounded() throws Exception {
        String runKey = "guard_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.maxDay").value(30))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.currentRoute").isString())
                .andExpect(jsonPath("$.data.route.currentRoute").isString())
                .andExpect(jsonPath("$.data.route.routeScore").isMap())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.resources.stamina").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.resources.stamina").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.resources.inspiration").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.resources.inspiration").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.opinion.popularity").value(lessThanOrEqualTo(100)))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(lessThanOrEqualTo(100)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(lessThanOrEqualTo(100)));

        mockMvc.perform(get("/api/stage/briefing").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentDay").value(1))
                .andExpect(jsonPath("$.data.stageRhythm.phaseKey").value("NEWBIE"))
                .andExpect(jsonPath("$.data.stageRhythm.startDay").value(1))
                .andExpect(jsonPath("$.data.stageRhythm.endDay").value(7))
                .andExpect(jsonPath("$.data.nextMilestoneDay").value(7))
                .andExpect(jsonPath("$.data.daysUntilMilestone").value(6))
                .andExpect(jsonPath("$.data.currentTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.nextTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.routeSnapshot").isString())
                .andExpect(jsonPath("$.data.routeTendency.primaryRoute.routeType").isString())
                .andExpect(jsonPath("$.data.routeTendency.primaryRoute.score").isNumber())
                .andExpect(jsonPath("$.data.routeTendency.competingRoutes").isArray())
                .andExpect(jsonPath("$.data.riskSnapshot").isString())
                .andExpect(jsonPath("$.data.actionHint").isString())
                .andExpect(jsonPath("$.data.objectiveActionType").isString())
                .andExpect(jsonPath("$.data.objectiveProgress").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.objectiveProgress").value(lessThanOrEqualTo(100)))
                .andExpect(jsonPath("$.data.objectiveItems").isArray())
                .andExpect(jsonPath("$.data.image", matchesPattern("v4/.+\\.png")));

        MvcResult actionsResult = mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(9))))
                .andExpect(jsonPath("$.data[0].actionType").isString())
                .andExpect(jsonPath("$.data[0].routeBiasType").isString())
                .andExpect(jsonPath("$.data[0].routeBiasLabel").isString())
                .andExpect(jsonPath("$.data[0].riskLevel").isString())
                .andExpect(jsonPath("$.data[0].planArchetype").isString())
                .andExpect(jsonPath("$.data[0].tempoHint").isString())
                .andExpect(jsonPath("$.data[0].routeFocusHint").isString())
                .andReturn();
        assertObjectHasKeys(actionsResult, "$.data[0]", "comboKey", "comboLabel", "comboHint");

        MvcResult riskToolResult = mockMvc.perform(get("/api/risk-tool/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].toolType").isString())
                .andExpect(jsonPath("$.data[0].label").isString())
                .andExpect(jsonPath("$.data[0].costPreview").isString())
                .andExpect(jsonPath("$.data[0].effectPreview").isString())
                .andExpect(jsonPath("$.data[0].supportedDebtTypes").isArray())
                .andReturn();
        assertObjectHasKeys(riskToolResult, "$.data[0]", "enabled", "disabledReason", "targetDebtId",
                "targetSummary", "urgencyPreview", "targetDebt");

        MvcResult actionResult = mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "%s-action"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("OFF_STREAM_READY"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(lessThanOrEqualTo(80)))
                .andExpect(jsonPath("$.data.actionResult.staminaChange").value(greaterThanOrEqualTo(-10)))
                .andExpect(jsonPath("$.data.actionResult.staminaChange").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef").isMap())
                .andReturn();
        assertBoundedDelta(actionResult, "$.data.actionResult.trueFanChange", 0, 80);
        assertBoundedDelta(actionResult, "$.data.actionResult.funFanChange", 0, 80);
        assertBoundedDelta(actionResult, "$.data.actionResult.unicornFanChange", 0, 80);
        assertBoundedDelta(actionResult, "$.data.actionResult.ddFanChange", 0, 80);

        mockMvc.perform(get("/api/offstream/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(14))))
                .andExpect(jsonPath("$.data[0].type").isString())
                .andExpect(jsonPath("$.data[0].name").isString())
                .andExpect(jsonPath("$.data[0].effectPreview").isString())
                .andExpect(jsonPath("$.data[0].benefitPreview").isString())
                .andExpect(jsonPath("$.data[0].costPreview").isString())
                .andExpect(jsonPath("$.data[0].bestFor").isString())
                .andExpect(jsonPath("$.data[0].riskPreview").isString())
                .andExpect(jsonPath("$.data[0].recommendedReason").isString())
                .andExpect(jsonPath("$.data[0].routeBias").isString())
                .andExpect(jsonPath("$.data[0].recommended").isBoolean());

        mockMvc.perform(post("/api/offstream/skip").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.endingReady").value(false));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.dataDelta.fanDelta").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.dataDelta.fanDelta").value(lessThanOrEqualTo(80)))
                .andExpect(jsonPath("$.data.dataDelta.popularityDelta").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.dataDelta.popularityDelta").value(lessThanOrEqualTo(80)))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.highlights", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.highlights", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.visibleItems", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.evidenceRefs", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.evidenceRefs[0].type").value("business_log"))
                .andExpect(jsonPath("$.data.riskHint").isString())
                .andExpect(jsonPath("$.data.nextDayEnabled").value(true));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.riskHints").isArray())
                .andExpect(jsonPath("$.data.disabledActions").isArray());
    }

    @Test
    void gameplayApiShowsOperationalPressureLocksAndReplacementActions() throws Exception {
        String runKey = "pressure_guard_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);
        Long userId = (Long) session.getAttribute("USER_ID");

        var vup = vupMapper.findActiveByUserId(userId);
        Map<String, Object> operationalPressure = new LinkedHashMap<>();
        operationalPressure.put("score", 5);
        operationalPressure.put("lastUpdatedDay", 1);
        operationalPressure.put("sourceKey", "CONTENT_DRY");
        operationalPressure.put("lockedGroupKey", "CONTENT_DRY");
        operationalPressure.put("lockedUntilDay", 7);
        operationalPressure.put("woundedUntilDay", 9);
        operationalPressure.put("traceCounts", Map.of(
                "mainStageBody", 0,
                "boundaryCrack", 0,
                "emptyContent", 1
        ));
        vup.setTutorialFlagsJson(new com.example.vupworld.service.infra.JsonService(new com.fasterxml.jackson.databind.ObjectMapper())
                .write(Map.of("operatingPressure", operationalPressure)));
        vupMapper.updateState(vup);

        MvcResult actionsResult = mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(9))))
                .andReturn();

        String body = new String(actionsResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        List<Map<String, Object>> actions = JsonPath.read(body, "$.data");
        Map<String, Object> blockedAction = actions.stream()
                .filter(action -> "PUBLISH_VIDEO".equals(action.get("actionType")))
                .findFirst()
                .orElseThrow();
        assertEquals(false, blockedAction.get("enabled"));
        assertEquals("OPERATIONAL_PRESSURE_LOCKED", blockedAction.get("disabledReason"));
        assertEquals("过载", blockedAction.get("pressureState"));
        assertEquals("内容枯竭", blockedAction.get("pressureLockGroupLabel"));
        assertNotNull(blockedAction.get("pressureReplacementActionType"));
        assertNotNull(blockedAction.get("pressureReplacementActionLabel"));
        assertTrue(String.valueOf(blockedAction.get("pressureHint")).contains("内容"));
    }

    @Test
    void gameplayApiRejectsPressureLockedActionSubmissionWithReplacementHint() throws Exception {
        String runKey = "pressure_submit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);
        Long userId = (Long) session.getAttribute("USER_ID");

        seedOperationalPressure(userId, 5, "CONTENT_DRY", 7, 9);

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "%s-submit"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("OPERATIONAL_PRESSURE_LOCKED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void gameplayApiDecaysOperationalPressureOnNextDay() throws Exception {
        String runKey = "pressure_decay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);
        Long userId = (Long) session.getAttribute("USER_ID");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "%s-action"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/offstream/skip").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        seedOperationalPressure(userId, 5, "CONTENT_DRY", 7, 9);

        MvcResult dayOneActions = mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertEquals("过载", extractActionField(dayOneActions, "PUBLISH_VIDEO", "pressureState"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "%s-next"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(2));

        MvcResult dayTwoActions = mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertEquals("绷紧", extractActionField(dayTwoActions, "PUBLISH_VIDEO", "pressureState"));
    }

    @Test
    void gameplayApisRejectOutOfPhaseWritesWithClearPhaseErrors() throws Exception {
        String runKey = "phase_guard_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 11,
                                  "idempotencyKey": "%s-title"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PHASE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").isString());

        mockMvc.perform(post("/api/offstream/skip").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PHASE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").isString());

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "%s-next"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PHASE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void offStreamActionsStayLightweightAndKeepDailyFlowMoving() throws Exception {
        String runKey = "offstream_guard_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockHttpSession session = registerLoginAndCreateVup(runKey);

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "%s-action"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("OFF_STREAM_READY"));

        mockMvc.perform(get("/api/offstream/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.type == 'READ_LETTERS' && @.benefitPreview =~ /.*真爱粉.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.type == 'READ_LETTERS' && @.costPreview =~ /.*热点.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.type == 'READ_LETTERS' && @.riskPreview =~ /.*热度.*/)]", hasSize(1)));

        MvcResult result = mockMvc.perform(post("/api/offstream/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "offStreamType": "READ_LETTERS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Integer offStreamFanDelta = JsonPath.read(body, "$.data.actionResult.evidenceRef.offStreamAction.fanDelta");
        Integer offStreamRouteDelta = JsonPath.read(body, "$.data.actionResult.evidenceRef.offStreamAction.routeScoreDelta");
        String phase = JsonPath.read(body, "$.data.phase");
        assertTrue(offStreamFanDelta >= 0 && offStreamFanDelta <= 2, "off-stream fan delta must stay lightweight");
        assertTrue(offStreamRouteDelta >= 0 && offStreamRouteDelta <= 1, "off-stream route delta must stay lightweight");
        assertTrue(List.of("REPORT_READY", "ENDING_READY", "NEED_EVENT_CHOICE", "NEED_INTERACTION_CHOICE").contains(phase),
                "off-stream should either settle the day or surface a pending choice, not loop back to a second main action");
    }

    private MockHttpSession registerLoginAndCreateVup(String runKey) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "pass1234",
                                  "nickname": "guard"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "pass1234"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s_vup",
                                  "persona": "guard persona"
                                }
                                """.formatted(runKey)))
                .andExpect(status().isOk());

        return session;
    }

    private static void assertBoundedDelta(MvcResult result, String path, int min, int max) throws Exception {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Integer value = JsonPath.read(body, path);
        assertNotNull(value, path);
        assertTrue(value >= min && value <= max, path + " expected " + min + ".." + max + " but was " + value);
    }

    private static void assertObjectHasKeys(MvcResult result, String path, String... keys) throws Exception {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Map<String, Object> object = JsonPath.read(body, path);
        for (String key : keys) {
            assertTrue(object.containsKey(key), path + " should expose key " + key);
        }
    }

    private void seedOperationalPressure(Long userId, int score, String sourceKey, int lockedUntilDay, int woundedUntilDay) {
        var vup = vupMapper.findActiveByUserId(userId);
        Map<String, Object> operationalPressure = new LinkedHashMap<>();
        operationalPressure.put("score", score);
        operationalPressure.put("sourceKey", sourceKey);
        operationalPressure.put("lockedGroupKey", sourceKey);
        operationalPressure.put("lockedUntilDay", lockedUntilDay);
        operationalPressure.put("woundedUntilDay", woundedUntilDay);
        vup.setTutorialFlagsJson(new com.example.vupworld.service.infra.JsonService(
                new com.fasterxml.jackson.databind.ObjectMapper()
        ).write(Map.of("operatingPressure", operationalPressure)));
        vupMapper.updateState(vup);
    }

    private String extractActionField(MvcResult result, String actionType, String fieldName) throws Exception {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        List<Map<String, Object>> actions = JsonPath.read(body, "$.data");
        Map<String, Object> action = actions.stream()
                .filter(item -> actionType.equals(item.get("actionType")))
                .findFirst()
                .orElseThrow();
        return String.valueOf(action.get(fieldName));
    }
}
