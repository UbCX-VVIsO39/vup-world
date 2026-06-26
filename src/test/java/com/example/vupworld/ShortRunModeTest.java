package com.example.vupworld;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "game.run.max-day=7")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShortRunModeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.example.vupworld.mapper.VupMapper vupMapper;

    @Test
    void configuredSevenDayRunGeneratesEndingAndBlocksDayEight() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady",
                                  "runSeed": "demo-short-run-007"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vup.maxDay").value(7))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "steady",
                                  "runSeed": "demo-short-run-007"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(7))
                .andExpect(jsonPath("$.data.currentDay").value(7))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(7))
                .andExpect(jsonPath("$.data.reportIds", hasSize(7)))
                .andExpect(jsonPath("$.data.logCount").value(greaterThanOrEqualTo(7)))
                .andExpect(jsonPath("$.data.endingReviewId").isNumber())
                .andExpect(jsonPath("$.data.endingType").isNotEmpty());

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(7))
                .andExpect(jsonPath("$.data.maxDay").value(7))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(7))
                .andExpect(jsonPath("$.data.nextDayEnabled").value(false));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "short-run-no-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DAY_LIMIT_REACHED"));
    }

    @Test
    void demoRejectsTargetsAfterConfiguredFinalDay() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady",
                                  "runSeed": "demo-short-run-target-guard"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "steady",
                                  "targetDay": 30,
                                  "runSeed": "demo-short-run-target-guard"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFIG_FIELD_INVALID"));
    }

    @Test
    void demoStrategiesKeepRunningWhenOperationalPressureLocksTheObviousActions() throws Exception {
        assertDemoStrategyCompletesThroughShortRun("clip", "CONTENT_DRY", "short-clip-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        assertDemoStrategyCompletesThroughShortRun("social", "RELATION_OVERDRAWN", "short-social-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        assertDemoStrategyCompletesThroughShortRun("glorious_graduation", "RELATION_OVERDRAWN", "short-grad-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        assertDemoStrategyCompletesThroughShortRun("random", "CONTENT_DRY", "demo-random-n12-b");
    }

    private void assertDemoStrategyCompletesThroughShortRun(String strategy, String pressureSourceKey, String runSeed) throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "%s",
                                  "runSeed": "%s"
                                }
                                """.formatted(strategy, runSeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Long userId = (Long) session.getAttribute("USER_ID");
        var vup = vupMapper.findActiveByUserId(userId);
        vup.setTutorialFlagsJson("""
                {
                  "operatingPressure": {
                    "score": 5,
                    "sourceKey": "%s",
                    "lockedGroupKey": "%s",
                    "lockedUntilDay": 7,
                    "woundedUntilDay": 9
                  }
                }
                """.formatted(pressureSourceKey, pressureSourceKey));
        vupMapper.updateState(vup);

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "%s",
                                  "targetDay": 7,
                                  "runSeed": "%s"
                                }
                                """.formatted(strategy, runSeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(7))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));
    }
}
