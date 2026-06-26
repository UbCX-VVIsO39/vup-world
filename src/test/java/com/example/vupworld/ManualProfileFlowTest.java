package com.example.vupworld;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("manual")
class ManualProfileFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void manualProfileExposesDemoToolsForPackagedJarSmokeChecks() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canPlayP0").value(true))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'devDemo' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'devDemo' && @.detail =~ /.*开发验证脚本可用.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'prodConfig')]", hasSize(0)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'galleryAssets' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'seedData' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.defenseEvidence", hasSize(4)))
                .andExpect(content().string(not(containsString("DEV_DEMO_ENABLED"))))
                .andExpect(content().string(not(containsString("CONTENT_SAFETY"))))
                // 未登录访问 config-check 时 devTools 被裁剪为 null；manual 下 dev 工具可用性由后续 run-script 测试验证。
                .andExpect(jsonPath("$.data.devTools").doesNotExist());
    }

    @Test
    void manualProfileCanResetAndRunDemoToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady"
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
                                  "stopOnError": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentDay").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.endingReviewId").exists())
                .andExpect(jsonPath("$.data.endingType").exists())
                .andExpect(jsonPath("$.data.endingReason").exists());
    }

    @Test
    void manualProfileFriendDemoQuickStartAliasesDemoResetAndSetsSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/friend/demo/quick-start")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.runSeed").value("demo-steady-001"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/friend/demo/quick-start")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "cyber_girlfriend",
                                  "runSeed": "friend-demo-cyber-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.runSeed").value("friend-demo-cyber-001"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(get("/api/dev/demo/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.runSeed").value("friend-demo-cyber-001"))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"));
    }
}
