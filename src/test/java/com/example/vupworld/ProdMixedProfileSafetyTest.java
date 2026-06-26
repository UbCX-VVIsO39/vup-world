package com.example.vupworld;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vupworld-prod-mixed;MODE=MySQL;NON_KEYWORDS=DAY,LOCKED;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@ActiveProfiles({"prod", "manual"})
class ProdMixedProfileSafetyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prodProfileWinsOverManualForDevToolsAndDiagnostics() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 未登录访问 config-check 时 devTools 被裁剪为 null；prod 优先于 manual 由 dev 接口返回 DEV_TOOL_DISABLED 验证。
                .andExpect(jsonPath("$.data.devTools").doesNotExist())
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[0].name").value("prodConfig"))
                .andExpect(content().string(not(containsString("demo_strategy_script"))))
                .andExpect(content().string(not(containsString("steady"))));
    }

    @Test
    void prodProfileWinsOverManualForDevEndpoints() throws Exception {
        mockMvc.perform(post("/api/dev/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEV_TOOL_DISABLED"));

        mockMvc.perform(post("/api/friend/demo/quick-start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEV_TOOL_DISABLED"));

        mockMvc.perform(post("/api/dev/stress/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "random",
                                  "rounds": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEV_TOOL_DISABLED"));
    }
}
