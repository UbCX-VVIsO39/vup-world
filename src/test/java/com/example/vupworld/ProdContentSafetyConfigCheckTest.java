package com.example.vupworld;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "vupworld.content-safety.mode=PUBLIC_SAFE",
        "vupworld.content-safety.real-name-mapping.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:vupworld-prod-content-safety;MODE=MySQL;NON_KEYWORDS=DAY,LOCKED;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdContentSafetyConfigCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prodConfigCheckPrunesDiagnosticsEvenWhenP0Fails() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAIL"))
                .andExpect(jsonPath("$.data.canPlayP0").value(false))
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[0].name").value("prodConfig"))
                .andExpect(jsonPath("$.data.categories[0].status").value("FAIL"))
                .andExpect(jsonPath("$.data.fatalErrors", hasSize(1)))
                .andExpect(jsonPath("$.data.defenseEvidence", hasSize(0)))
                // 未登录访问 config-check 时 devTools 被裁剪为 null；prod 下 dev 接口不可用由 DevDemoDisabledFlowTest 验证。
                .andExpect(jsonPath("$.data.devTools").doesNotExist())
                .andExpect(content().string(not(containsString("公开安全模式启用了真实名映射"))))
                .andExpect(content().string(not(containsString("真实名映射"))))
                .andExpect(content().string(not(containsString("PUBLIC_SAFE"))))
                .andExpect(content().string(not(containsString("vupworld.content-safety"))))
                .andExpect(content().string(not(containsString("contentSafety"))))
                .andExpect(content().string(not(containsString("galleryAssets"))))
                .andExpect(content().string(not(containsString("seedData"))))
                .andExpect(content().string(not(containsString("demo_strategy_script"))))
                .andExpect(content().string(not(containsString("v4/protagonist/avatars/avatar-default.png"))))
                .andExpect(content().string(not(containsString("data.sql"))))
                .andExpect(content().string(not(containsString("mvc_layers"))));
    }
}
