package com.example.vupworld;

import com.example.vupworld.service.content.ContentCatalogService;
import com.example.vupworld.service.content.MemeQuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DefenseExperienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemeQuoteService memeQuoteService;

    @Autowired
    private ContentCatalogService contentCatalogService;

    @Test
    void desktopFrontendSurfacesDefenseCockpitAndExpandedStylePresets() throws Exception {
        String appJs = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String appCss = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function renderDefenseCockpit()"));
        assertTrue(appJs.contains("答辩驾驶舱"));
        assertTrue(appJs.contains("重置并跑满30天"));
        assertTrue(appJs.contains("逐日调用正式 Service"));
        assertTrue(appJs.contains("梗舞练功房"));
        assertTrue(appJs.contains("黑红法庭"));
        assertTrue(appJs.contains("花房歌会"));
        assertTrue(appCss.contains(".defense-cockpit"));
        assertTrue(appCss.contains(".create-style-grid"));
        assertTrue(appCss.contains("repeat(3, minmax(0, 1fr))"));
    }

    @Test
    void demoToolsStillExplainOfficialServiceFlowInConfigCheck() throws Exception {
        String response = mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(response.contains("开发验证脚本可用"));
        assertTrue(response.contains("逐日调用正式 Service"));
    }

    @Test
    void configCheckAndFrontendExposeSsmDefenseEvidence() throws Exception {
        String response = mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String appJs = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String appCss = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(response.contains("defenseEvidence"));
        assertTrue(response.contains("Controller -> Service -> Mapper"));
        assertTrue(response.contains("business_log"));
        assertTrue(response.contains("api_idempotency_record"));
        assertTrue(appJs.contains("function renderDefenseEvidencePanel()"));
        assertTrue(appJs.contains("SSM证据台"));
        assertTrue(appCss.contains(".defense-evidence-panel"));
        assertTrue(appCss.contains(".defense-evidence-strip"));
    }

    @Test
    void memeCatalogIncludesMorePublicSafeGuoVFlavor() {
        List<String> quotes = new ArrayList<>();
        quotes.addAll(contentCatalogService.getStringList("MEME_QUOTE", "GENERAL"));
        quotes.addAll(contentCatalogService.getStringList("MEME_QUOTE", "PUBLISH_CLIP"));
        quotes.addAll(contentCatalogService.getStringList("MEME_QUOTE", "TRAIN_TALK"));
        quotes.addAll(contentCatalogService.getStringList("MEME_QUOTE", "high_heat"));
        String joinedQuotes = String.join("\n", quotes);

        assertTrue(memeQuoteService.getQuoteCount() >= 72);
        assertTrue(joinedQuotes.contains("标题组把悬念写成欠条"));
        assertTrue(joinedQuotes.contains("切片组开始等饭点"));
        assertTrue(joinedQuotes.contains("楼友先不贷款"));
    }
}
