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
        "vupworld.content-safety.real-name-mapping.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentSafetyConfigCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicSafeModeFailsConfigCheckWhenRealNameMappingIsEnabled() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAIL"))
                .andExpect(jsonPath("$.data.canPlayP0").value(false))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety' && @.status == 'FAIL')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety' && @.detail =~ /.*公开安全模式.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.fatalErrors", hasSize(1)))
                .andExpect(jsonPath("$.data.fatalErrors[?(@ =~ /.*真实名映射.*/)]", hasSize(1)))
                .andExpect(content().string(not(containsString("CONTENT_SAFETY"))))
                .andExpect(content().string(not(containsString("PUBLIC_SAFE"))))
                .andExpect(content().string(not(containsString("vupworld.content-safety"))));
    }
}
