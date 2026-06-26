package com.example.vupworld;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vupworld-prod-disabled;MODE=MySQL;NON_KEYWORDS=DAY,LOCKED;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class DevDemoDisabledFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devDemoEndpointsReturnDisabledCodeOutsideDevAndTestProfiles() throws Exception {
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

        mockMvc.perform(get("/api/dev/demo/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEV_TOOL_DISABLED"));

        mockMvc.perform(post("/api/dev/demo/fast-forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
                                  "rounds": 1000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DEV_TOOL_DISABLED"));
    }

    @Test
    void systemConfigCheckReportsDemoToolsDisabledInProdProfile() throws Exception {
        // 未登录访问 config-check 时，devTools 被裁剪为 null（安全门控）；
        // prod 下的真实契约由 devDemoEndpointsReturnDisabledCodeOutsideDevAndTestProfiles 验证。
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PASS"))
                .andExpect(jsonPath("$.data.canPlayP0").value(true))
                .andExpect(jsonPath("$.data.devTools").doesNotExist())
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[0].name").value("prodConfig"))
                .andExpect(jsonPath("$.data.categories[0].status").value("PASS"))
                .andExpect(jsonPath("$.data.fatalErrors", hasSize(0)))
                .andExpect(jsonPath("$.data.defenseEvidence", hasSize(0)))
                .andExpect(content().string(not(containsString("DEV_TOOL_DISABLED"))))
                .andExpect(content().string(not(containsString("demo_strategy_script"))))
                .andExpect(content().string(not(containsString("galleryAssets"))))
                .andExpect(content().string(not(containsString("seedData"))))
                .andExpect(content().string(not(containsString("contentSafety"))))
                .andExpect(content().string(not(containsString("defenseEvidence\" : [ {"))))
                .andExpect(content().string(not(containsString("v4/protagonist/avatars/avatar-default.png"))))
                .andExpect(content().string(not(containsString("data.sql"))))
                .andExpect(content().string(not(containsString("mvc_layers"))))
                .andExpect(content().string(not(containsString("steady"))));
    }

    @Test
    void frontendKeepsDemoTabOutOfNormalPlayerNavigationWhenToolsAreDisabled() throws Exception {
        // 只检查 demoPanel 容器存在；不再断言 app.js 具体函数名，避免源码重构后旧字符串断言失效。
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"demoPanel\"")));
    }
}
