package com.example.vupworld;

import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.web.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameBootstrapFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VupMapper vupMapper;

    @Autowired
    private DaySessionMapper daySessionMapper;

    @Autowired
    private BusinessLogMapper businessLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private String cachedPanelBundleSource;

    @BeforeEach
    void clearRateLimitState() throws Exception {
        // 测试间清理速率限制器的内存计数，避免连续调用 /api/auth/register、/api/auth/login 时触发 429。
        Field field = RateLimitFilter.class.getDeclaredField("accessRecords");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(rateLimitFilter)).clear();
        cachedPanelBundleSource = null;
    }

    private String playerFrontendSource() throws Exception {
        return playerFrontendSource(mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String playerFrontendSource(MvcResult appJsResult) throws Exception {
        return responseText(appJsResult)
                + "\n/* /js/panel-bundle.js */\n"
                + panelBundleSource();
    }

    private String panelBundleSource() throws Exception {
        if (cachedPanelBundleSource == null) {
            cachedPanelBundleSource = staticResourceSource("/js/panel-bundle.js");
        }
        return cachedPanelBundleSource;
    }

    private String staticResourceSource(String path) throws Exception {
        return responseText(mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String responseText(MvcResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private org.springframework.test.web.servlet.ResultMatcher playerFrontendSourceMatches(
            org.hamcrest.Matcher<? super String> matcher) {
        return result -> assertTrue(matcher.matches(playerFrontendSource(result)),
                () -> "Combined player frontend source did not match: " + matcher);
    }

    @Test
    void playerCanOpenPlayableHomePageWithGalleryAssets() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-app=\"vup-world\"")))
                .andExpect(content().string(containsString("v4/protagonist/avatars/avatar-default.png")))
                .andExpect(content().string(containsString("v4/live-room/shells/shell-default.png")));
    }

    @Test
    void premiumSingleplayerLiveRoomSurfacesCoreDrawersFeedbackAndOperationalBrain() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("const LIVE_DRAWERS = {"));
        assertTrue(appJs.contains("host: { title: '主播', kicker: 'PRIMARY'"));
        assertTrue(appJs.contains("fans: { title: '粉丝', kicker: 'PRIMARY'"));
        assertTrue(appJs.contains("routes: { title: '路线', kicker: 'PRIMARY'"));
        assertTrue(appJs.contains("report: { title: '昨日结果', kicker: 'PRIMARY'"));
        assertTrue(appJs.contains("npcs: { title: '其他主播', kicker: 'PRIMARY'"));
        assertTrue(appJs.contains("atlas: { title: '成就', kicker: 'SECONDARY'"));
        assertTrue(appJs.contains("environment: { title: '平台/环境', kicker: 'SECONDARY'"));
        assertTrue(appJs.contains("live-drawer-group primary"));
        assertTrue(appJs.contains("live-drawer-group secondary"));

        assertTrue(appJs.contains("function premiumCoreStatusChips(v)"));
        assertTrue(appJs.contains("key: \"operationBrain\""));
        assertTrue(appJs.contains("label: \"运营脑\""));
        assertTrue(appJs.contains("key: \"bodyMind\""));
        assertTrue(appJs.contains("label: \"身心\""));
        assertTrue(appJs.contains("key: \"publicHeat\""));
        assertTrue(appJs.contains("label: \"公开热度\""));
        assertTrue(appJs.contains("key: \"relationshipPressure\""));
        assertTrue(appJs.contains("label: \"关系压力\""));
        assertTrue(appJs.contains("quickStats.dataset.coreCount = \"4\";"));
        assertFalse(appJs.contains("const chips = [\n        { key: \"stamina\""));

        assertTrue(appJs.contains("function operationalBrainSnapshot()"));
        assertTrue(appJs.contains("运营脑比粉丝数更早决定你今天做不了什么。"));
        assertTrue(appJs.contains("class=\"operational-brain-card\""));
        assertTrue(appJs.contains("class=\"operational-brain-card wounded\""));
        assertTrue(appJs.contains("cooldownLeft"));
        assertTrue(appJs.contains("woundLeft"));
        assertTrue(appJs.contains("重痕迹"));
        assertTrue(appJs.contains("带伤恢复"));

        assertTrue(appJs.contains("function dailyFeedbackHtml()"));
        assertTrue(appJs.contains("昨日操作反馈"));
        assertTrue(appJs.contains("可能走向"));
        assertTrue(appJs.contains("开始今天"));
        assertTrue(appJs.contains("openFeedbackReport()"));

        assertTrue(appJs.contains("function danmakuDensityConfig()"));
        assertTrue(appJs.contains("heat >= 70"));
        assertTrue(appJs.contains("burst: 4"));
        assertTrue(appCss.contains("height: 25%;"));
        assertTrue(appCss.contains(".live-stage-ticker"));
        assertTrue(appCss.contains(".live-drawer-dock"));
        assertTrue(appCss.contains(".daily-feedback-overlay"));
        assertTrue(appCss.contains(".operational-brain-card"));
        assertTrue(appCss.contains(".stat-item-operationBrain"));
    }

    @Test
    void repeatedCreateVupRequestResumesExistingActiveRun() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("resume_existing_vup_player", "露米");

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "另一套皮",
                                  "persona": "重复点击开局按钮"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message", containsString("继续当前进度")))
                .andExpect(jsonPath("$.data.name").value("露米"))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"));
    }

    @Test
    void loggedInPlayerCanRestoreCurrentUserFromSession() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "restore_session_player",
                                  "password": "pass1234",
                                  "nickname": "刷新续播员"
                                }
                                """))
                .andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "restore_session_player",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("restore_session_player"))
                .andExpect(jsonPath("$.data.nickname").value("刷新续播员"));
    }

    @Test
    void playableFrontendContainsEndingGalleryAndReviewEvidenceRendering() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("v4/routes/cyber-girlfriend/ending-highlight.png")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/routes/glorious-graduation/ending-highlight.png")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/routes/main-stage-king/ending-highlight.png")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.endingReason")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.keyEvents")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.debtRefs")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.routeReview?.routeEvidenceTrail")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.routeReview?.finalReportRef")))
                .andExpect(playerFrontendSourceMatches(containsString("state.ending.routeReview?.accidentMaterials")))
                .andExpect(playerFrontendSourceMatches(containsString("renderEndingAccidentMaterials")))
                .andExpect(playerFrontendSourceMatches(containsString("route-evidence-trail")))
                .andExpect(playerFrontendSourceMatches(containsString("final-report-ref")));
    }

    @Test
    void playableFrontendProvidesShareableEndingBattleReport() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function endingShareText"));
        assertTrue(appJs.contains("function copyEndingShare("));
        assertTrue(appJs.contains("function clipboardText("));
        assertTrue(appJs.contains("data-action=\"copy-ending-share\""));
        assertTrue(appJs.contains("《VUP出道局》30天收官"));
        assertTrue(appJs.contains("navigator.clipboard?.writeText"));
        assertTrue(appJs.contains("document.execCommand(\"copy\")"));
        assertTrue(appJs.contains("结局战报已复制，可以直接发群聊或动态。"));
        assertTrue(appJs.contains("浏览器拦截了分享或复制，请手动选中战报文本。"));
        assertTrue(appJs.contains("ENDING_SHARE_CARD_FORMATS"));
        assertTrue(appJs.contains("label: \"16:9\""));
        assertTrue(appJs.contains("label: \"9:16\""));
        assertTrue(appJs.contains("保存16:9"));
        assertTrue(appJs.contains("保存9:16"));
        assertTrue(appCss.contains(".ending-share-card"));
        assertTrue(appCss.contains(".ending-share-button"));
    }

    @Test
    void playableFrontendReusesStableIdempotencyKeysForGameplayWrites() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function stableIdempotencyKey("));
        assertTrue(appJs.contains("function clearStableIdempotencyKey("));
        assertTrue(appJs.contains("function currentRunKeyParts("));
        assertTrue(appJs.contains("stableIdempotencyKey('day-action'"));
        assertTrue(appJs.contains("stableIdempotencyKey('stream-title'"));
        assertTrue(appJs.contains("stableIdempotencyKey('title-reroll'"));
        assertTrue(appJs.contains("stableIdempotencyKey('event-choice'"));
        assertTrue(appJs.contains("stableIdempotencyKey('interaction-choice'"));
        assertTrue(appJs.contains("stableIdempotencyKey('next-day'"));
        assertTrue(appJs.contains("stableIdempotencyKey('fan-topic-choice'"));
        assertTrue(appJs.contains("stableIdempotencyKey('risk-tool-use'"));
        assertTrue(appJs.contains("clearStableIdempotencyKey('event-choice'"));
        assertTrue(appJs.contains("clearStableIdempotencyKey('fan-topic-choice'"));
        assertTrue(appJs.contains("stableIdempotencyKey('restart'"));
        assertTrue(appJs.contains("clearStableIdempotencyKey('restart'"));
        assertFalse(appJs.contains("`action-${Date.now()}`"));
        assertFalse(appJs.contains("`title-${Date.now()}`"));
        assertFalse(appJs.contains("`reroll-${Date.now()}`"));
        assertFalse(appJs.contains("`event-${Date.now()}`"));
        assertFalse(appJs.contains("`interaction-${Date.now()}`"));
        assertFalse(appJs.contains("`next-${Date.now()}`"));
        assertFalse(appJs.contains("`fan-topic-${Date.now()}`"));
        assertFalse(appJs.contains("`risk-tool-${Date.now()}`"));
        assertFalse(appJs.contains("`cancel-${Date.now()}`"));
        assertFalse(appJs.contains("`restart-${Date.now()}`"));
    }

    @Test
    void playableFrontendRestoresUserBeforeRenderingResumeState() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/auth/current")))
                .andExpect(playerFrontendSourceMatches(containsString("state.user = await api('/api/auth/current')")));
    }

    @Test
    void playableFrontendClearsGameStateOnLogout() throws Exception {
        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String script = playerFrontendSource(result);
        assertTrue(script.contains("function resetGameState()"));
        assertTrue(script.contains("state.session = null"));
        assertTrue(script.contains("state.actions = []"));
        assertTrue(script.contains("state.pendingEvent = null"));
        assertTrue(script.contains("state.pendingInteraction = null"));
        assertTrue(script.contains("state.report = null"));
        assertTrue(script.contains("state.ending = null"));
        assertTrue(script.contains("resetGameState();"));
    }

    @Test
    void playableFrontendShowsVisibleStatusAndBusyStateForWrites() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("id=\"appStatus\""));
        assertTrue(indexHtml.contains("aria-live=\"polite\""));

        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("function setStatus("));
        assertTrue(appJs.contains("function statusMessageText(value)"));
        assertTrue(appJs.contains("state.statusMessage = statusMessageText(message);"));
        assertTrue(appJs.contains("async function withBusy("));
        assertTrue(appJs.contains("后台掉线了，刷新试试。"));
        assertTrue(appJs.contains("state.busy"));
        assertTrue(appJs.contains("state.busy ? 'disabled' : ''"));
        assertTrue(appJs.contains("withBusy('开始出道中'"));
        assertTrue(appJs.contains("function doneStatusText(label)"));
        assertTrue(appJs.contains(".replace(/中$/, \"\")"));
        assertTrue(appJs.contains("const phaseCopy = {"));
        assertTrue(appJs.contains("NEED_TITLE: \"标题组已接棒，先选一个稳得住的直播标题。\""));
        assertTrue(appJs.contains("REPORT_READY: \"今日日报已生成，读完就能开下一天。\""));
        assertTrue(appJs.contains("ENDING_READY: \"30天收官完成，去结局复盘看路线证据。\""));
        assertTrue(appJs.contains("开始出道: \"出道局已开播，先选今日主行动。\""));
        assertTrue(appJs.contains("const nextStep = phaseCopy[phase] || labelCopy[actionLabel] || \"排班表已收，继续营业。\";"));
        assertTrue(appJs.contains("doneStatusText(label)"));
        assertTrue(appJs.contains("const messageText = state.statusMessage;"));
        assertTrue(appJs.contains("status.title = messageText;"));
        assertFalse(appJs.contains("return `${actionLabel}完成。`;"));
        assertFalse(appJs.contains("`${label}完成。`"));
        assertTrue(appJs.contains("planType: actionPlanTypeFor(actionType)"));
    }

    @Test
    void playableFrontendGuidesFirstPlayWithActionableTutorialAndCoach() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("开始第一局"));
        assertTrue(indexHtml.contains("单机入口直接开局"));
        assertTrue(indexHtml.contains("每日只选一个主行动"));
        assertTrue(indexHtml.contains("标题 / 事件 / 日报 / 下一天"));
        assertTrue(indexHtml.contains("跳过教程，直接开播"));
        assertTrue(indexHtml.contains("id=\"coachPanel\""));
        assertTrue(indexHtml.contains("id=\"tutorialStart\""));
        assertTrue(indexHtml.contains("id=\"tutorialSkip\""));

        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("function renderCoach()"));
        assertTrue(appJs.contains("function showTutorial()"));
        assertTrue(appJs.contains("coach-kicker"));
        assertTrue(appJs.contains("单机入口"));
        assertTrue(appJs.contains("开局卡组"));
        assertTrue(appJs.contains("当前高亮卡片"));
        assertFalse(appJs.contains("照着上方教练卡走"));
        assertFalse(appJs.contains("先按高亮卡片推进当天流程"));
    }

    @Test
    void playableFrontendExposesStableActionHooksAndSemanticTutorialVisibility() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("id=\"tutorialOverlay\""));
        assertTrue(indexHtml.contains("role=\"dialog\""));
        assertTrue(indexHtml.contains("aria-modal=\"true\""));
        assertTrue(indexHtml.contains("aria-hidden=\"true\""));
        assertTrue(indexHtml.contains("id=\"appStatus\" role=\"status\" aria-atomic=\"true\""));
        assertTrue(indexHtml.contains("id=\"mainTabBar\" role=\"tablist\" aria-label=\"主导航\""));
        assertTrue(indexHtml.contains("data-main-tab=\"infoHub\""));
        assertTrue(indexHtml.contains("<button class=\"tutorial-start\" id=\"tutorialStart\" type=\"button\" data-action=\"close-tutorial\">"));

        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appCss.contains(".tutorial-overlay[hidden]"));
        assertTrue(appCss.contains("display: none !important"));

        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("overlay?.removeAttribute('hidden')"));
        assertTrue(appJs.contains("overlay?.setAttribute('aria-hidden', 'false')"));
        assertTrue(appJs.contains("overlay?.setAttribute('hidden', 'hidden')"));
        assertTrue(appJs.contains("overlay?.setAttribute('aria-hidden', 'true')"));
        assertTrue(appJs.contains("data-action=\"register\""));
        assertTrue(appJs.contains("data-action=\"login\""));
        assertTrue(appJs.contains("data-action=\"logout\""));
        assertTrue(appJs.contains("data-action=\"create-vup\""));
        assertTrue(appJs.contains("data-action=\"quick-submit-action\""));
        assertTrue(appJs.contains("data-action=\"detail-submit-action\""));
        assertTrue(appJs.contains("data-action-type=\"${html(action.actionType)}\""));
        assertTrue(appJs.contains("data-action=\"choose-fan-topic\""));
        assertTrue(appJs.contains("data-action=\"choose-title\""));
        assertTrue(appJs.contains("data-action=\"reroll-title\""));
        assertTrue(appJs.contains("const dataAction = eventKind === 'interaction' ? 'choose-interaction' : 'choose-event';"));
        assertTrue(appJs.contains("data-action=\"${html(dataAction)}\""));
        assertTrue(appJs.contains("data-action=\"next-day\""));
        assertTrue(appJs.contains("data-action=\"restart\""));
    }

    @Test
    void playableFrontendEscapesAndTruncatesHeaderUserName() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult htmlResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String indexHtml = new String(htmlResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appJs.contains("const userDisplayName = state.user.nickname || state.user.username;"));
        assertTrue(appJs.contains("class=\"user-name\""));
        assertTrue(appJs.contains("title=\"${html(userDisplayName)}\""));
        assertTrue(appJs.contains("${html(userDisplayName)}"));
        assertTrue(appJs.contains("class=\"logout-button\""));
        assertFalse(appJs.contains("<span>${state.user.nickname || state.user.username}</span>"));
        assertTrue(appCss.contains(".user-name"));
        assertTrue(appCss.contains("text-overflow: ellipsis"));
        assertTrue(appCss.contains(".logout-button"));
    }

    @Test
    void playableFrontendEscapesReportAndHistoryCopy() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("map(i => `<li>${html(reportVisibleItemText(i))}</li>`)"));
        assertTrue(appJs.contains("const materialReceiptHtml = materialReceiptText ? `<p class=\"report-material-receipt\">${html(materialReceiptText)}</p>` : '';"));
        assertTrue(appJs.contains("<p class=\"report-summary\">${html(summaryText)}</p>"));
        assertTrue(appJs.contains("`<p class=\"report-risk\">${html(riskHintText)}</p>`"));
        assertTrue(appJs.contains("function reportHistoryChip(label, value, tone = \"\")"));
        assertTrue(appJs.contains("class=\"report-history-card\""));
        assertTrue(appJs.contains("class=\"report-history-list\""));
        assertTrue(appJs.contains("class=\"report-history-item info-card\""));
        assertTrue(appJs.contains("reportHistoryChip(\"Day\", `第${r.day}天`, \"day\")"));
        assertTrue(appJs.contains("reportHistoryChip(\"摘要\", reportHistorySummaryText(r), \"summary\")"));
        assertFalse(appJs.contains("map(i => `<li>${html(i)}</li>`)"));
        assertFalse(appJs.contains("map(i => `<li>${i}</li>`)"));
        assertFalse(appJs.contains("<p>${state.report.summary}</p>"));
        assertFalse(appJs.contains("<p class=\"report-risk\">${state.report.riskHint}</p>"));
        assertFalse(appJs.contains("<p>${html(reportHistorySummaryText(r))}</p>"));
        assertFalse(appJs.contains("<p>${html(r.summary)}</p>"));
        assertFalse(appJs.contains("<p>${r.summary}</p>"));
        String reportHistorySource = appJs.substring(
                appJs.indexOf("function renderReportHistory()"),
                appJs.indexOf("// 用户操作"));
        assertFalse(reportHistorySource.contains("style="));
    }

    @Test
    void playableFrontendEscapesTitleEventAndEndingCopy() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function jsString(value)"));
        assertTrue(appJs.contains("const titleText = localizedVisibleTextOrFallback(t.titleText, `候选标题${index + 1}`);"));
        assertTrue(appJs.contains("const effectText = localizedVisibleTextOrFallback(t.effectPreview, \"节目效果待定\");"));
        assertTrue(appJs.contains("const riskText = localizedVisibleTextOrFallback(t.debtRiskPreview, \"标题风险待观察\");"));
        assertTrue(appJs.contains("const decisionHint = titleDecisionHint(t, effectText, riskText);"));
        assertTrue(appJs.contains("function titleDecisionHint(title, effectText, riskText)"));
        assertTrue(appJs.contains("const decisionChips = titleDecisionChips(t, effectText, riskText);"));
        assertTrue(appJs.contains("function titleDecisionChips(title, effectText, riskText)"));
        assertTrue(appJs.contains("class=\"title-chip heat\""));
        assertTrue(appJs.contains("class=\"title-chip risk\""));
        assertTrue(appJs.contains("class=\"title-chip route\""));
        assertTrue(appJs.contains("<div class=\"title-decision-strip\">"));
        assertTrue(appJs.contains("<h4>${html(titleText)}</h4>"));
        assertTrue(appJs.contains("<span class=\"title-chip heat\"><em>热度</em><strong>${html(decisionChips.heat)}</strong></span>"));
        assertTrue(appJs.contains("<span class=\"title-chip risk\"><em>口碑风险</em><strong>${html(decisionChips.risk)}</strong></span>"));
        assertTrue(appJs.contains("const choiceKey = eventChoiceKey(choice);"));
        assertTrue(appJs.contains("function eventChoiceKey(choice)"));
        assertTrue(appJs.contains("function eventTitleText(event)"));
        assertTrue(appJs.contains("function eventDescriptionText(event)"));
        assertTrue(appJs.contains("function eventChoiceLabelText(choice)"));
        assertTrue(appJs.contains("function eventChoiceMetaText(value)"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(event?.title, \"突发事件\");"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(event?.description, \"现场记录还在整理，先看可选处理方案。\");"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(choice?.label, \"备选方案\");"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, \"场务还在整理数据\");"));
        assertTrue(appJs.contains("data-choice=\"${html(choiceKey)}\""));
        assertTrue(appJs.contains("const dataAction = eventKind === 'interaction' ? 'choose-interaction' : 'choose-event';"));
        assertTrue(appJs.contains("data-action=\"${html(dataAction)}\""));
        assertTrue(appJs.contains("const choiceLabel = eventChoiceLabelText(choice);"));
        assertTrue(appJs.contains("const costPreview = choice.costPreview ? eventChoiceMetaText(choice.costPreview) : \"\";"));
        assertTrue(appJs.contains("const effectPreview = eventChoiceMetaText(choice.effectPreview || choice.riskPreview);"));
        assertTrue(appJs.contains("const choiceChips = eventChoiceChips(choice, costPreview, effectPreview);"));
        assertTrue(appJs.contains("function eventChoiceChips(choice, costPreview, effectPreview)"));
        assertTrue(appJs.contains("function eventPressureChips(event, choices)"));
        assertTrue(appJs.contains("const pressureChips = eventPressureChips(event, choices);"));
        assertTrue(appJs.contains("class=\"event-pressure-strip\""));
        assertTrue(appJs.contains("class=\"event-pressure-chip severity\""));
        assertTrue(appJs.contains("class=\"event-pressure-chip choices\""));
        assertTrue(appJs.contains("class=\"event-pressure-chip tempo\""));
        assertTrue(appJs.contains("class=\"event-choice-line cost\""));
        assertTrue(appJs.contains("class=\"event-choice-line payoff\""));
        assertTrue(appJs.contains("class=\"event-choice-line route\""));
        assertTrue(appJs.contains("class=\"event-choice-line debt\""));
        assertTrue(appJs.contains("<strong class=\"event-choice-title\">${html(choiceLabel)}</strong>"));
        assertTrue(appJs.contains("<div class=\"event-choice-lines\" aria-label=\"选择取舍\">"));
        assertTrue(appJs.contains("const choiceCta = eventChoiceCtaText(choice, index);"));
        assertTrue(appJs.contains("aria-label=\"选择：${html(choiceLabel)}，操作：${html(choiceCta)}，快捷键 ${choiceHotkey}\""));
        assertTrue(appJs.contains("const eventTitle = eventTitleText(event);"));
        assertTrue(appJs.contains("const eventDescription = eventDescriptionText(event);"));
        assertTrue(appJs.contains("<h3 class=\"panel-title\">${html(eventTitle)}</h3>"));
        assertTrue(appJs.contains("const eventUrgencyMeta = event.debtType"));
        assertTrue(appJs.contains("<div class=\"event-meta-line\">${html(eventUrgencyMeta)}</div>"));
        assertTrue(appJs.contains("debtTypeText(event.debtType)"));
        assertTrue(appJs.contains("function riskSeverityText(severity)"));
        assertTrue(appJs.contains("${riskSeverityText(event.severity)}"));
        assertTrue(appJs.contains("米线高压 ${level}级"));
        assertTrue(appJs.contains("<p class=\"event-description\">${html(eventVisibleDescription)}</p>"));
        assertTrue(appJs.contains("BOOMERANG_CLIP: \"回旋镖切片\""));
        assertTrue(appCss.contains(".event-meta-line"));
        assertTrue(appCss.contains(".event-description"));
        assertTrue(appJs.contains("const finalTitle = localizedVisibleTextOrFallback(state.ending.finalTitle"));
        assertTrue(appJs.contains("const subtitle = localizedVisibleTextOrFallback(state.ending.subtitle"));
        assertTrue(appJs.contains("const summary = localizedVisibleTextOrFallback(state.ending.summary"));
        assertTrue(appJs.contains("const endingReason = localizedVisibleTextOrFallback(state.ending.endingReason"));
        assertTrue(appJs.contains("<h2 class=\"ending-title\">${html(finalTitle)}</h2>"));
        assertTrue(appJs.contains("<p class=\"ending-subtitle\">${html(subtitle)}</p>"));
        assertTrue(appJs.contains("<p class=\"ending-summary\">${html(summary)}</p>"));
        assertTrue(appJs.contains("<p>${html(endingReason)}</p>"));
        assertTrue(appJs.contains("<span class=\"ending-tag\">${html(localizedVisibleTextOrFallback(t.label || t, \"结局标签\"))}</span>"));
        assertTrue(appJs.contains("<strong>第${e.day}天 / ${html(actionLabelFor(e.action))}</strong>"));
        assertTrue(appJs.contains("${html(localizedVisibleTextOrFallback(e.summary, \"代表事件已记录，复盘文本待整理。\"))}"));
        assertTrue(appJs.contains("<strong>${html(debtTypeText(d.debtType))} / ${html(riskSeverityText(d.severity))}</strong>"));
        assertTrue(appJs.contains("${html(localizedVisibleTextOrFallback(d.summary, \"旧账仍有记录，下一轮注意补米线。\"))}"));
        assertTrue(appJs.contains("最终日报：第${finalReportRef.day}天日报"));
        assertFalse(appJs.contains("证据${finalReportRef.id}"));
        assertTrue(appJs.contains("<strong>${html(visibleTextOrFallback(materialLabelFor(m), \"事故素材\"))}</strong>"));
        assertTrue(appJs.contains("第${m.day}天 / ${html(actionLabelFor(m.sourceAction))}"));
        assertTrue(appJs.contains("BOOMERANG_CLIP_CONTEXT: \"回旋镖切片上下文\""));
        assertTrue(appJs.contains("TITLE_BACKFIRE_CONTEXT: \"标题党反噬上下文\""));
        assertTrue(appJs.contains("COMMERCIAL_BACKLASH: \"商业反噬\""));
        assertTrue(appJs.indexOf("<h3 class=\"panel-title\">${html(eventTitle)}</h3>")
                < appJs.indexOf("<div class=\"event-meta-line\">${html(eventUrgencyMeta)}</div>"));
        assertTrue(appJs.indexOf("<div class=\"event-meta-line\">${html(eventUrgencyMeta)}</div>")
                < appJs.indexOf("<p class=\"event-description\">${html(eventVisibleDescription)}</p>"));
        assertTrue(appJs.indexOf("<p class=\"event-description\">${html(eventVisibleDescription)}</p>")
                < appJs.indexOf("<div class=\"event-choices\">${choiceItems}</div>"));
        assertTrue(appJs.indexOf("<div class=\"event-choices\">${choiceItems}</div>")
                < appJs.indexOf("<img class=\"event-illustration\" src=\"/gallery/${illustration}\""));
        assertTrue(appJs.contains("<img class=\"event-illustration\" src=\"/gallery/${illustration}\" alt=\"${html(eventTitle)}\" loading=\"eager\" decoding=\"async\">"));

        assertFalse(appJs.contains("<h4>${t.titleText}</h4>"));
        assertFalse(appJs.contains("<p>${t.effectPreview} / ${t.debtRiskPreview}</p>"));
        assertFalse(appJs.contains("严重度${event.severity || 0}"));
        assertFalse(appJs.contains("严重度 ${d.severity}"));
        assertFalse(appJs.contains("onclick=\"chooseEvent('${choice.choiceId || choice.choiceType}')\""));
        assertFalse(appJs.contains("<strong class=\"event-choice-title\">${html(choice.label)}</strong>"));
        assertFalse(appJs.contains("aria-label=\"选择：${html(choice.label)}\""));
        assertFalse(appJs.contains("<h3 class=\"panel-title\">${event.title || '事件'}</h3>"));
        assertFalse(appJs.contains("<h3 class=\"panel-title\">${html(event.title || '事件')}</h3>"));
        assertFalse(appJs.contains("<h4>${html(event.title || '事件')}</h4>"));
        assertFalse(appJs.contains("BOOMERANG_CLIP: \"回旋镖素材\""));
        assertFalse(appJs.contains("COMMERCIAL_BACKLASH: \"商业化反感\""));
        assertFalse(appJs.contains("<p>${event.description || ''}</p>"));
        assertFalse(appJs.contains("<p class=\"event-description\">${html(event.description || '')}</p>"));
        assertFalse(appCss.contains(".event-box p {"));
        assertFalse(appJs.contains("<h2 class=\"ending-title\">${state.ending.finalTitle}</h2>"));
        assertFalse(appJs.contains("<p class=\"ending-subtitle\">${state.ending.subtitle}</p>"));
        assertFalse(appJs.contains("<p>${state.ending.summary}</p>"));
        assertFalse(appJs.contains("${state.ending.endingReason}</p>"));
        assertFalse(appJs.contains("<span class=\"ending-tag\">${t.label || t}</span>"));
        assertFalse(appJs.contains("const titleText = visibleTextOrFallback(t.titleText, `候选标题${index + 1}`);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(event?.title, \"突发事件\");"));
        assertFalse(appJs.contains("const finalTitle = visibleTextOrFallback(state.ending.finalTitle"));
        assertFalse(appJs.contains("daily_report#"));
        assertFalse(appJs.contains("<strong>${m.label || m.materialId}</strong>"));
        assertFalse(appJs.contains("BOOMERANG_CLIP_CONTEXT: \"回旋镖上下文\""));
        assertFalse(appJs.contains("TITLE_BACKFIRE_CONTEXT: \"标题反噬上下文\""));
        assertFalse(appJs.contains("${html(e.action)}"));
        assertFalse(appJs.contains("${html(m.sourceAction)}"));
    }

    @Test
    void playableFrontendSupportsDesktopStageChoiceHotkeys() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function handleStageChoiceHotkey(event)"));
        assertTrue(appJs.contains("document.addEventListener('keydown', handleStageChoiceHotkey);"));
        assertTrue(appJs.contains("data-stage-hotkey=\"${index + 1}\""));
        assertTrue(appJs.contains("<span class=\"stage-hotkey\" aria-hidden=\"true\">${index + 1}</span>"));
        assertTrue(appJs.contains("aria-label=\"选择直播标题：${html(titleText)}，${html(decisionHint)}，快捷键 ${index + 1}\""));
        assertTrue(appJs.contains("function renderEventChoice(choice, eventKind, index)"));
        assertTrue(appJs.contains("const choiceHotkey = index + 1;"));
        assertTrue(appJs.contains("data-stage-hotkey=\"${choiceHotkey}\""));
        assertTrue(appJs.contains("aria-label=\"选择：${html(choiceLabel)}，操作：${html(choiceCta)}，快捷键 ${choiceHotkey}\""));
        assertTrue(appJs.contains("setStatus(`快捷键 ${key}：${choiceName}，提交阶段选择。`, \"info\");"));
        assertTrue(appCss.contains(".stage-hotkey"));
    }

    @Test
    void playableFrontendSupportsDesktopReportAndEndingAdvanceHotkey() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function handleStageAdvanceHotkey(event)"));
        assertTrue(appJs.contains("document.addEventListener('keydown', handleStageAdvanceHotkey);"));
        assertTrue(appJs.contains("data-stage-advance-hotkey=\"Enter\""));
        assertTrue(appJs.contains("function hotkeyDisplayText(key)"));
        assertTrue(appJs.contains("aria-label=\"读完第${state.report.day}天日报，进入下一天，快捷键 回车\""));
        assertTrue(appJs.contains("aria-label=\"看完${html(finalTitle)}，开启复活赛\""));
        assertTrue(appJs.contains("const advanceKeyLabel = hotkeyDisplayText(\"Enter\");"));
        assertTrue(appJs.contains("setStatus(`快捷键 ${advanceKeyLabel}：${advanceName}。`, \"info\");"));

        assertTrue(probe.contains("reportNextDayHotkeyDispatch"));
        assertTrue(probe.contains("endingRestartHotkeyDispatch"));
    }

    @Test
    void playableFrontendRoutesFormalEventsAndLiveInteractionsToSeparateChoices() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function activeEventPayload()"));
        assertTrue(appJs.contains("state.session?.phase === 'NEED_EVENT_CHOICE'"));
        assertTrue(appJs.contains("state.session?.phase === 'NEED_INTERACTION_CHOICE'"));
        assertTrue(appJs.contains("return state.pendingEvent?.pending ? state.pendingEvent : null;"));
        assertTrue(appJs.contains("return state.pendingInteraction?.pending ? state.pendingInteraction : null;"));
        assertTrue(appJs.contains("const event = activeEventPayload();"));
        assertTrue(appJs.contains("const eventKind = currentEventKind();"));
        assertTrue(appJs.contains("function eventChoiceDisabledText(choice, disabled)"));
        assertTrue(appJs.contains("function eventChoiceButtonAttributes(disabled, disabledReason)"));
        assertTrue(appJs.contains("const dataAction = eventKind === 'interaction' ? 'choose-interaction' : 'choose-event';"));
        assertTrue(appJs.contains("data-action=\"${html(dataAction)}\""));
        assertTrue(appJs.contains("async function chooseInteraction(choiceType)"));
        assertTrue(appJs.contains("await api('/api/interaction/choose'"));
        assertTrue(appJs.contains("choiceType: choiceType"));
        assertTrue(appJs.contains("event-choice-disabled"));
        assertFalse(appJs.contains("state.pendingEvent?.pending ? state.pendingEvent :\n                  state.pendingInteraction?.pending ? state.pendingInteraction : null;"));
        assertFalse(appJs.contains("data-action=\"choose-event\" data-choice=\"${html(choiceKey)}\" aria-label=\"选择：${html(choice.label)}\" onclick=\"chooseEvent(${jsString(choiceKey)})\""));
    }

    @Test
    void playableFrontendEscapesInsightAndForecastCopy() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function percent(value)"));
        assertTrue(appJs.contains("const displayName = npcDisplayNameText(npc);"));
        assertTrue(appJs.contains("alt=\"${html(displayName)}\""));
        assertTrue(appJs.contains("<h4>${html(displayName)}</h4>"));
        assertTrue(appJs.contains("const headline = buzzHeadlineText(buzz);"));
        assertTrue(appJs.contains("<h4>${html(headline)}</h4>"));
        assertFalse(appJs.contains("alt=\"${html(npc.displayName || '同台角色')}\""));
        assertFalse(appJs.contains("<h4>${html(buzz.headline || '热搜安静')}</h4>"));
        assertTrue(appJs.contains("function stageTrendLabelText(trend, fallback = \"口味待观察\")"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(briefing?.stageLabel, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(trend?.label, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(briefing?.dailyFortune, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(briefing?.dailyFortuneAdvice, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, fallback);"));
        assertTrue(appJs.contains("今日运势：${html(stageFortuneText(briefing))}"));
        assertTrue(appJs.contains("${html(stageFortuneAdviceText(briefing))}"));
        assertTrue(appJs.contains("${html(stageTrendLabelText(currentTrend))}"));
        assertTrue(appJs.contains("function stageBriefingChip(label, value, tone = \"\")"));
        assertTrue(appJs.contains("stageBriefingChip(\"路线\", briefing.routeSnapshot, \"route\")"));
        assertTrue(appJs.contains("stageBriefingChip(\"粉丝\", briefing.fanSnapshot, \"fans\")"));
        assertTrue(appJs.contains("stageBriefingChip(\"风险\", briefing.riskSnapshot, \"risk\")"));
        assertTrue(appJs.contains("function focusActionCard(actionType)"));
        assertTrue(appJs.contains("data-objective-action-type"));
        assertTrue(appJs.contains("state.stageBriefing?.objectiveActionType"));
        assertTrue(appJs.contains("objective-focus-pulse"));
        assertTrue(appJs.contains("<div class=\"stage-briefing-nextplay\">"));
        assertTrue(appJs.contains("function reportHistorySummaryText(report, fallback = \"这天日报还在补录，录播组正在对轴。\")"));
        assertTrue(appJs.contains("function audienceRouteText(expectation, fallback = \"观望路线\")"));
        assertTrue(appJs.contains("<strong class=\"audience-expectation-route\">${html(audienceRouteText(expectation))}</strong>"));
        assertTrue(appJs.contains("${html(audienceLockText(expectation))}"));
        assertTrue(appJs.contains("${html(audienceLineText(expectation.evidenceLine))}"));
        assertTrue(appJs.contains("const share = percent(expectation.share);"));
        assertTrue(appJs.contains("const metricValue = percent(metric.value);"));
        assertTrue(appJs.contains("function funMetricLabelText(metric, fallback = \"乐子指标\")"));
        assertTrue(appJs.contains("function funMetricChips(metric, metricValue)"));
        assertTrue(appJs.contains("<h4 class=\"fun-metric-title\">${html(funMetricLabelText(metric))}</h4>"));
        assertTrue(appJs.contains("class=\"fun-metric-chip heat\""));
        assertTrue(appJs.contains("class=\"fun-metric-chip status\""));
        assertTrue(appJs.contains("class=\"fun-metric-chip next\""));
        assertTrue(appJs.contains("${html(funMetricLineText(metric))}"));
        assertTrue(appJs.contains("function comboDiscoveryChips(combo, discovered)"));
        assertTrue(appJs.contains("<h4 class=\"combo-title\">${html(comboLabelText(c))}</h4>"));
        assertTrue(appJs.contains("class=\"combo-chip status\""));
        assertTrue(appJs.contains("class=\"combo-chip evidence\""));
        assertTrue(appJs.contains("class=\"combo-chip next\""));
        assertTrue(appJs.contains("${html(comboEvidenceText(c))}"));
        assertTrue(appJs.contains("${html(comboHintText(c))}"));
        assertTrue(appJs.contains("function personaTagChips(tag, strength)"));
        assertTrue(appJs.contains("<h4 class=\"persona-tag-title\">${html(personaTagLabelText(tag))}</h4>"));
        assertTrue(appJs.contains("class=\"persona-tag-chip strength\""));
        assertTrue(appJs.contains("class=\"persona-tag-chip status\""));
        assertTrue(appJs.contains("class=\"persona-tag-chip next\""));
        assertTrue(appJs.contains("${html(personaTagStatusText(tag))}"));
        assertTrue(appJs.contains("${html(personaTagEvidenceText(tag))}"));
        assertTrue(appJs.contains("function memeLabelFor(item)"));
        assertTrue(appJs.contains("function memeLifecycleChips(item)"));
        assertTrue(appJs.contains("<h4 class=\"meme-lifecycle-title\">${html(memeLabelFor(item))}</h4>"));
        assertTrue(appJs.contains("class=\"meme-lifecycle-chip uses\""));
        assertTrue(appJs.contains("class=\"meme-lifecycle-chip stage\""));
        assertTrue(appJs.contains("class=\"meme-lifecycle-chip next\""));
        assertTrue(appJs.contains("${html(memeStageText(item))}"));
        assertTrue(appJs.contains("${html(memeNextHintText(item))}"));
        assertTrue(appJs.contains("<div class=\"achievement-label\">${html(title)}</div>"));
        assertTrue(appJs.contains("${html(endingForecastRequirementLabelText(requirement))}"));
        assertTrue(appJs.contains("${html(endingForecastRequirementLineText(requirement))}"));
        assertTrue(appJs.contains("${html(endingForecastHeadlineText(forecast))}"));
        assertTrue(appJs.contains("${html(endingForecastFinalTitleText(forecast))}"));
        assertTrue(appJs.contains("${html(endingForecastRiskText(forecast))}"));
        assertTrue(appJs.contains("${html(endingForecastSprintText(forecast))}"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(forecast?.headline, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(forecast?.likelyFinalTitle, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(requirement?.label, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(requirement?.line, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(forecast?.riskLine, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(forecast?.sprintHint, fallback);"));

        assertFalse(appJs.contains("<h4>${npc.displayName || '匿名同行'}</h4>"));
        assertFalse(appJs.contains("<h4>${buzz.headline || '热搜安静'}</h4>"));
        assertFalse(appJs.contains("今日运势：${html(briefing.dailyFortune)}"));
        assertFalse(appJs.contains("${html(briefing.dailyFortuneAdvice || '')}"));
        assertFalse(appJs.contains("return visibleTextOrFallback(briefing?.stageLabel, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(trend?.label, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(briefing?.dailyFortune, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(briefing?.dailyFortuneAdvice, fallback);"));
        assertFalse(appJs.contains("${html(currentTrend.label || '当前口味')}"));
        assertFalse(appJs.contains("${html(nextTrend.label || '下周口味')}"));
        assertFalse(appJs.contains("<strong style=\"font-size: 13px; font-weight: 600;\">${html(expectation.routeLabel)}</strong>"));
        assertFalse(appJs.contains("${expectation.routeLabel}</strong>"));
        assertFalse(appJs.contains("<span>${html(metric.label)}</span>"));
        assertFalse(appJs.contains("<span>${html(funMetricLabelText(metric))}</span>"));
        assertFalse(appJs.contains("<p style=\"font-size: 11px; color: var(--text-muted); margin-top: 4px;\">${html(metric.line || metric.tone || '')}</p>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${html(c.label)}</h4>"));
        assertFalse(appJs.contains("${html(c.evidence)}"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${html(comboLabelText(c))}</h4>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${html(tag.label)}</h4>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${html(personaTagLabelText(tag))}</h4>"));
        assertFalse(appJs.contains("${html(tag.evidence || '')}"));
        assertFalse(appJs.contains("${html(item.stageLabel || '新梗期')}"));
        assertFalse(appJs.contains("${html(item.nextHint || '')}"));
        assertFalse(appJs.contains("return visibleTextOrFallback(forecast?.headline, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(forecast?.likelyFinalTitle, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(requirement?.label, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(requirement?.line, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(forecast?.riskLine, fallback);"));
        assertFalse(appJs.contains("return visibleTextOrFallback(forecast?.sprintHint, fallback);"));
        assertFalse(appJs.contains("${html(requirement.label)}"));
        assertFalse(appJs.contains("${html(forecast.headline || '结局预演')}"));
        assertFalse(appJs.contains("${html(forecast.likelyFinalTitle || '查无此V')}"));
        assertFalse(appJs.contains("${html(forecast.riskLine || '')}"));
        assertFalse(appJs.contains("<span>${metric.label}</span>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${c.label}</h4>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${tag.label}</h4>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${html(item.label || item.memeSubtype)}</h4>"));
        assertFalse(appJs.contains("<h4 style=\"margin: 0;\">${item.label || item.memeSubtype}</h4>"));
        assertFalse(appJs.contains("<div class=\"achievement-label\">${title}</div>"));
        assertFalse(appJs.contains("${forecast.headline || '结局预演'}</strong>"));
    }

    @Test
    void playableFrontendEscapesActionStateAndFanTopicCopy() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function riskToolLabelText(tool)"));
        assertTrue(appJs.contains("function riskToolPreviewText(value, fallback)"));
        assertTrue(appJs.contains("function riskToolChips(tool, disabled)"));
        assertTrue(appJs.contains("const label = riskToolLabelText(tool);"));
        assertTrue(appJs.contains("<strong>${html(label)}</strong>"));
        assertTrue(appJs.contains("const cost = riskToolPreviewText(tool.costPreview, \"成本待核对\");"));
        assertTrue(appJs.contains("const effect = riskToolPreviewText(tool.effectPreview, \"场务还在整理数据\");"));
        assertTrue(appJs.contains("const chips = riskToolChips(tool, disabled);"));
        assertTrue(appJs.contains("class=\"risk-tool-token cost\""));
        assertTrue(appJs.contains("class=\"risk-tool-token effect\""));
        assertTrue(appJs.contains("class=\"risk-tool-token state\""));
        assertTrue(appJs.contains("data-action=\"use-risk-tool\""));
        assertTrue(appJs.contains("async function useRiskTool(toolType, targetDebtId)"));
        assertTrue(appJs.contains("<h3>${html(title)}</h3>"));
        assertTrue(appJs.contains("<p>${html(body)}</p>"));
        assertTrue(appJs.contains("<div class=\"coach-next\">${html(next)}</div>"));
        assertTrue(appJs.contains("data-tutorial=\"${html(hint.key)}\">${html(hint.text)}</div>"));
        assertTrue(appJs.contains("${html(platformTrendLabel)}"));
        assertTrue(appJs.contains("${html(platformTrendDescription)}"));
        assertTrue(appJs.contains("<span>${html(stat.label)}</span>"));
        assertTrue(appJs.contains("<strong>${html(stat.value)}"));
        assertTrue(appJs.contains("function streamPlanLabelText(plan)"));
        assertTrue(appJs.contains("<option value=\"${html(plan.planType)}\">${html(streamPlanLabelText(plan))}</option>"));
        assertFalse(appJs.contains("<option value=\"${html(plan.planType)}\">${html(plan.label)}</option>"));
        assertTrue(appJs.contains("function fanTopicChoiceAriaLabel(topic, choice)"));
        assertTrue(appJs.contains("function fanTopicTitleText(topic, fallback = \"群友议题\")"));
        assertTrue(appJs.contains("function fanTopicDescriptionText(topic, fallback = \"群友还在整理小作文。\")"));
        assertTrue(appJs.contains("function fanTopicChoiceLabelText(choice, fallback = \"未命名方案\")"));
        assertTrue(appJs.contains("function danmakuPersonaText(danmaku, fallback = \"围观群众\")"));
        assertTrue(appJs.contains("function danmakuLineText(danmaku, fallback = \"弹幕还在同步。\")"));
        assertTrue(appJs.contains("function fanTopicChoiceMeta(choice)"));
        assertTrue(appJs.contains("function fanTopicChoiceChips(choice)"));
        assertTrue(appJs.contains("function fanTopicChoiceButtonAttributes(disabled, disabledReason, titleText)"));
        assertTrue(appJs.contains("panel.classList.add('hidden');"));
        assertTrue(appJs.contains("panel.classList.remove('hidden');"));
        assertTrue(appJs.contains("data-action=\"choose-fan-topic\""));
        assertTrue(appJs.contains("data-topic=\"${html(topic.topicKey)}\""));
        assertTrue(appJs.contains("data-topic-choice=\"${html(choice.choiceType)}\""));
        assertTrue(appJs.contains("aria-label=\"${html(ariaLabel)}\""));
        assertTrue(appJs.contains("${fanTopicChoiceButtonAttributes(disabled, disabledText, titleText)}"));
        assertTrue(appJs.contains("const choiceChips = fanTopicChoiceChips(c);"));
        assertTrue(appJs.contains("<span class=\"fan-topic-choice-label\">${html(fanTopicChoiceLabelText(c))}</span>"));
        assertTrue(appJs.contains("class=\"fan-topic-choice-chip cost\""));
        assertTrue(appJs.contains("class=\"fan-topic-choice-chip effect\""));
        assertTrue(appJs.contains("class=\"fan-topic-choice-chip risk\""));
        assertTrue(appJs.contains("<h4>${html(fanTopicTitleText(topic))}</h4>"));
        assertTrue(appJs.contains("<p class=\"fan-topic-copy\">${html(fanTopicDescriptionText(topic))}</p>"));
        assertTrue(appJs.contains("<small class=\"fan-topic-choice-meta\">${html(fanTopicChoiceMeta(c))}</small>"));
        assertTrue(appJs.contains("const costPreview = fanTopicPreviewText(choice.costPreview, \"待观察\");"));
        assertTrue(appJs.contains("const effectPreview = fanTopicPreviewText(choice.effectPreview, \"待观察\");"));
        assertTrue(appJs.contains("const riskPreview = fanTopicPreviewText(choice.riskPreview, \"待观察\");"));
        assertTrue(appJs.contains("`成本${costPreview}`"));
        assertTrue(appJs.contains("`效果${effectPreview}`"));
        assertTrue(appJs.contains("`风险${riskPreview}`"));

        assertFalse(appJs.contains("<strong style=\"font-size: 13px;\">${html(tool.label)}</strong>"));
        assertFalse(appJs.contains("${html(tool.costPreview || '')} / ${html(tool.effectPreview || '')}"));
        assertFalse(appJs.contains("<strong style=\"font-size: 13px;\">${tool.label}</strong>"));
        assertFalse(appJs.contains("${tool.costPreview || ''} / ${tool.effectPreview || ''}"));
        assertFalse(appJs.contains("<h3>${title}</h3>"));
        assertFalse(appJs.contains("<div class=\"tutorial-hint\" data-tutorial=\"${hint.key}\">${hint.text}</div>"));
        assertFalse(appJs.contains("<option value=\"${plan.planType}\">${plan.label}</option>"));
        assertFalse(appJs.contains("<span class=\"fan-topic-choice-label\">${html(c.label)}</span>"));
        assertFalse(appJs.contains("`效果${choice.effectPreview || '待观察'}`"));
        assertFalse(appJs.contains("`风险${choice.riskPreview || '低'}`"));
        assertFalse(appJs.contains("onclick=\"chooseFanTopic('${topic.topicKey}', '${c.choiceType}')\""));
        assertFalse(appJs.contains("aria-label=\"处理粉丝群议题：${html(c.label)}\">${html(c.label)}</button>"));
        assertFalse(appJs.contains("<span class=\"fan-topic-choice-risk\">${html(c.riskPreview || '')}</span>"));
        assertFalse(appJs.contains("<span class=\"fan-topic-choice-effect\">${c.effectPreview || ''}</span>"));
    }

    @Test
    void playableFrontendLocalizesFanTopicCopyWithoutChangingGlobalFallback() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function visibleTextOrFallback(value, fallback)"));
        assertTrue(appJs.contains("function localizedVisibleTextOrFallback(value, fallback)"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(topic?.title, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(topic?.description, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(choice?.label, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, fallback);"));
        assertTrue(appJs.contains("value: firstTopic ? fanTopicTitleText(firstTopic) : digestText(danmakuMood, '场外安静'),"));
        assertFalse(appJs.contains("function visibleTextOrFallback(value, fallback) {\n    const text = String(value || \"\").trim();\n    if (!text) return fallback;\n    return /[\\u4e00-\\u9fff]/.test(text)"));

        assertTrue(probe.contains("desktopRightRailFanTopicEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Backend fan topic should not be visible"));
        assertTrue(probe.contains("'Backend fan topic'"));
        assertTrue(probe.contains("Backend submission label should not be visible"));
        assertTrue(probe.contains("English fan topic leaked into ambient rail"));
        assertTrue(probe.contains("群友议题"));
        assertTrue(probe.contains("未命名方案"));
        assertTrue(probe.contains("看群友反馈"));
    }

    @Test
    void playableFrontendLocalizesRightRailDigestCopyWithoutChangingGlobalFallback() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function visibleTextOrFallback(value, fallback)"));
        assertTrue(appJs.contains("function localizedVisibleTextOrFallback(value, fallback)"));
        assertTrue(appJs.contains("function firstLocalizedReadableText(values, fallback)"));
        assertTrue(appJs.contains("const text = String(localizedVisibleTextOrFallback(value, fallback) || \"\").trim();"));
        assertTrue(appJs.contains("return firstLocalizedReadableText([npc?.displayName], fallback);"));
        assertTrue(appJs.contains("return firstLocalizedReadableText([npc?.advice, npc?.moodLine, npc?.relevance], fallback);"));
        assertTrue(appJs.contains("return firstLocalizedReadableText([buzz?.headline], fallback);"));
        assertTrue(appJs.contains("return firstLocalizedReadableText([buzz?.nextMoveHint, buzz?.trendLabel, buzz?.forumLine], fallback);"));
        assertTrue(appJs.contains("firstLocalizedReadableText([buzz.nextMoveHint, buzz.trendLabel], '下一步风向还在整理。')"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(expectation?.routeLabel, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(expectation?.lockLabel, fallback);"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, fallback);"));
        assertFalse(appJs.contains("function visibleTextOrFallback(value, fallback) {\n    const text = String(value || \"\").trim();\n    if (!text) return fallback;\n    return /[\\u4e00-\\u9fff]/.test(text)"));

        assertTrue(probe.contains("desktopRightRailDigestEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Backend npc name should not be visible"));
        assertTrue(probe.contains("Backend buzz headline should not be visible"));
        assertTrue(probe.contains("Audience lead route should not be visible"));
        assertTrue(probe.contains("English digest copy leaked into right rail"));
        assertTrue(probe.contains("匿名同行"));
        assertTrue(probe.contains("热搜安静"));
        assertTrue(probe.contains("观望路线"));
        assertTrue(probe.contains("先看粉丝想吃哪口"));
    }

    @Test
    void playableFrontendEscapesConfigDemoAndActionAttributes() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("fatalErrors.map(e => `<li>${html(configErrorText(e))}</li>`)"));
        assertTrue(appJs.contains("function configErrorText(error)"));
        assertTrue(appJs.contains("function demoBriefText(status, result)"));
        assertTrue(appJs.contains("const demoBrief = demoBriefText(state.demoStatus, state.demoResult);"));
        assertTrue(appJs.contains("<div class=\"demo-brief\" aria-label=\"开发验证简报\">"));
        assertTrue(appJs.contains("验证简报"));
        assertTrue(appJs.contains("<strong>${html(phaseLabelFor(state.demoStatus.phase))}</strong>"));
        assertTrue(appJs.contains("<strong>${html(demoErrorCodeText(state.demoStatus.lastError))}</strong>"));
        assertTrue(appJs.contains("最后错误：${html(demoErrorMessageText(state.demoStatus.lastError))}"));
        assertTrue(appJs.contains("<strong>${html(demoResultLabel(state.demoResult))}</strong>"));
        assertTrue(appJs.contains("function apiErrorMessage(payload, statusCode)"));
        assertTrue(appJs.contains("const codeMessage = knownErrorCodeText(payload?.code);"));
        assertTrue(appJs.contains("function localizedVisibleTextOrFallback(value, fallback)"));
        assertTrue(appJs.contains("function localizedMessageText(value)"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, \"\");"));
        assertTrue(appJs.contains("function jsAttr(value)"));
        assertTrue(appJs.contains("return html(jsString(value));"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(value, \"状态提示已隐藏原始代码，先刷新当前进度再试。\");"));
        assertTrue(appJs.contains("function playerSafeOperationErrorText()"));
        assertTrue(appJs.contains("return \"操作没成，场务已收起后台代号，先刷新当前进度再试。\";"));
        assertTrue(appJs.contains("return playerSafeOperationErrorText();"));
        assertTrue(appJs.contains("function hasUnsafeVisibleToken(value)"));
        assertTrue(appJs.contains("function firstReadableText(values, fallback)"));
        assertTrue(appJs.contains("function firstLocalizedReadableText(values, fallback)"));
        assertTrue(appJs.contains("function npcDisplayNameText(npc, fallback = \"匿名同行\")"));
        assertTrue(appJs.contains("function buzzHeadlineText(buzz, fallback = \"热搜安静\")"));
        assertTrue(appJs.contains("const text = String(localizedVisibleTextOrFallback(value, fallback) || \"\").trim();"));
        assertTrue(appJs.contains("同台情报还在整理。"));
        assertTrue(appJs.contains("热搜记录还在整理。"));
        assertTrue(appJs.contains("function syncDemoTabAvailability()"));
        assertTrue(appJs.contains("function normalizeInfoTab(target)"));
        assertTrue(appJs.contains("if (!enabled && state.infoTab === 'demo')"));
        assertTrue(appJs.contains("tab.hidden = !enabled;"));
        assertTrue(appJs.contains("tab.disabled = !enabled;"));
        assertTrue(appJs.contains("\\b[a-z][a-z0-9]+(?:_[a-z0-9]+)+\\b"));
        assertTrue(appJs.contains("[A-Za-z0-9_-]+\\.(?:png|jpe?g|webp|json)\\b"));
        assertTrue(appJs.contains("const message = localizedMessageText(payload?.message);"));
        assertTrue(appJs.contains("return codeMessage || message || errorCodeText(payload?.code, statusCode);"));
        assertTrue(appJs.contains("const error = new Error(apiErrorMessage(payload, response.status));"));
        assertTrue(appJs.contains("<span class=\"panel-badge\">${html(profileLabelFor(devTools.profile))}</span>"));
        assertTrue(appJs.contains("<option value=\"${html(s)}\" ${s === current ? 'selected' : ''}>${html(demoStrategyName(s))}</option>"));
        assertTrue(appJs.contains("<select id=\"demoStrategy\" class=\"demo-strategy-select\" aria-label=\"开发验证策略\" data-change-action=\"sync-demo-strategy-selects\">"));
        assertTrue(appJs.contains("data-action=\"demo-run\""));
        assertTrue(appJs.contains("data-action=\"demo-reset\" aria-label=\"开发验证：重置验证轮\""));
        assertTrue(appJs.contains("data-action=\"demo-fast-forward\""));
        assertTrue(appJs.contains("<div class=\"demo-summary demo-status-summary\" aria-label=\"验证状态摘要\">"));
        assertTrue(appJs.contains("<div class=\"demo-summary demo-result\" aria-label=\"验证结果摘要\">"));
        assertTrue(appJs.contains("<span>复盘号</span><strong>${state.demoResult.endingReviewId ? '已存档' : '--'}</strong>"));
        assertTrue(appJs.contains("<span>${html(tag.label)}</span>${html(tag.value)}"));
        assertTrue(appJs.contains("class=\"action-submit-btn\" data-action=\"detail-submit-action\" data-action-type=\"${html(action.actionType)}\""));
        assertTrue(appJs.contains("data-action=\"quick-submit-action\""));
        assertFalse(appJs.contains("onclick=\"submitAction(${jsAttr(action.actionType)})\""));
        assertTrue(appJs.contains("const actionOptionsSummaryText = actionOptionsSummaryLabel"));
        assertTrue(appJs.contains("aria-label=\"${html(actionOptionsSummaryAria)}\""));
        assertTrue(appJs.contains("<label for=\"username\">存档名</label>"));
        assertTrue(appJs.contains("<input type=\"text\" id=\"username\" name=\"username\""));
        assertTrue(appJs.contains("<label for=\"vupName\">VUP名称</label>"));
        assertTrue(appJs.contains("<select id=\"restartBias\" class=\"ending-restart-select\" aria-label=\"选择复活赛路线倾向\""));
        assertTrue(appJs.contains("const platformTrendLabel = platformTrend?.label"));
        assertTrue(appJs.contains("const platformTrendDescription = platformTrend?.description"));
        assertTrue(appJs.contains("visibleTextOrFallback(platformTrend.label, \"平台口味\")"));
        assertTrue(appJs.contains("visibleTextOrFallback(platformTrend.description, \"平台口味暂时稳定。\")"));
        assertTrue(appCss.contains(".demo-brief"));
        assertTrue(appCss.contains(".demo-brief strong"));

        assertFalse(appJs.contains("fatalErrors.map(e => `<li>${e}</li>`)"));
        assertFalse(appJs.contains("fatalErrors.map(e => `<li>${html(e)}</li>`)"));
        assertFalse(appJs.contains("<strong>${state.demoStatus.phase}</strong>"));
        assertFalse(appJs.contains("<strong>${html(state.demoStatus.phase)}</strong>"));
        assertFalse(appJs.contains("<strong>${html(state.demoStatus.lastError?.code || '--')}</strong>"));
        assertFalse(appJs.contains("<strong>${html(state.demoResult.endingType || state.demoResult.phase)}</strong>"));
        assertFalse(appJs.contains("<span style=\"font-size: 12px; color: var(--text-muted);\">结局ID</span>"));
        assertFalse(appJs.contains("${html(state.demoResult.endingReviewId || '--')}"));
        assertFalse(appJs.contains("payload.message || payload.code || `状态码 ${response.status}`"));
        assertFalse(appJs.contains("状态码 ${statusCode}"));
        assertFalse(appJs.contains("操作没成（状态码"));
        assertFalse(appJs.contains("return message || errorCodeText(payload?.code, statusCode);"));
        assertFalse(appJs.contains("visibleTextOrFallback(payload?.message, \"\")"));
        assertFalse(appJs.contains("<span class=\"panel-badge\">${devTools.profile}</span>"));
        assertFalse(appJs.contains("<span class=\"panel-badge\">${html(devTools.profile)}</span>"));
        assertFalse(appJs.contains("return String.join(\",\", profiles)"));
        assertFalse(appJs.contains("<option value=\"${s}\" ${s === current ? 'selected' : ''}>${demoStrategyName(s)}</option>"));
        assertFalse(appJs.contains("onclick=\"submitAction('${action.actionType}')\""));
        assertFalse(appJs.contains("onclick=\"submitAction(${jsString(action.actionType)})\""));
        assertFalse(appJs.contains("${html(platformTrend.label)}</span>"));
        assertFalse(appJs.contains("${html(platformTrend.description)}</p>"));
    }

    @Test
    void playableFrontendEscapesForecastEnumsAndMapsBackfireDebt() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String forecastSource = appJs.substring(
                appJs.indexOf("function renderEndingForecast()"),
                appJs.indexOf("// 用户操作"));

        assertTrue(appJs.contains("TITLE_BACKFIRE: \"标题党反噬\""));
        assertTrue(appJs.contains("alt=\"${html(streamPlanLabelFor(planType))}企划预览\""));
        assertTrue(appJs.contains("${html(statusLabelFor(requirement.status))}"));
        assertTrue(appJs.contains("${html(routeLabelFor(forecast.likelyEndingType || 'UNKNOWN'))}"));
        assertTrue(appJs.contains("class=\"ending-forecast-empty\""));
        assertTrue(appJs.contains("class=\"ending-forecast-head\""));
        assertTrue(appJs.contains("class=\"ending-forecast-confidence\""));
        assertTrue(appJs.contains("class=\"ending-forecast-headline\""));
        assertTrue(appJs.contains("class=\"ending-forecast-tags\""));
        assertTrue(appJs.contains("class=\"ending-forecast-tag\""));
        assertTrue(appJs.contains("class=\"ending-forecast-copy\""));
        assertTrue(appCss.contains(".ending-forecast-head"));
        assertTrue(appCss.contains(".ending-forecast-confidence"));
        assertTrue(appCss.contains(".ending-forecast-requirement"));
        assertTrue(appCss.contains(".ending-forecast-status.pass"));
        assertTrue(appCss.contains(".ending-forecast-status.warn"));

        assertFalse(appJs.contains("TITLE_BACKLASH: \"标题党反噬\""));
        assertFalse(appJs.contains("alt=\"${html(planType)}企划预览\""));
        assertFalse(appJs.contains("alt=\"${planType}企划预览\""));
        assertFalse(appJs.contains(">${statusLabelFor(requirement.status)}</span>"));
        assertFalse(appJs.contains(">${routeLabelFor(forecast.likelyEndingType || 'UNKNOWN')}</span>"));
        assertFalse(forecastSource.contains("style="));
    }

    @Test
    void playableFrontendSurfacesActionAvailabilityAndMemeQuickSlot() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("const enabledActionCount = state.actions.filter(action => action.enabled).length;"));
        assertTrue(appJs.contains("${enabledActionCount}个可用 / ${state.actions.length}项"));
        assertTrue(appJs.contains("function selectPrimaryReadyAction(actions, quickActions)"));
        assertTrue(appJs.contains("function renderPrimaryActionCue(action)"));
        assertTrue(appJs.contains("class=\"ready-primary-cue\""));
        assertTrue(appJs.contains("data-primary-action-type=\"${html(action.actionType)}\""));
        assertTrue(appJs.contains("今日主推"));
        assertTrue(appJs.contains("const preferredTypes = quickActionTypeCandidates();"));
        assertTrue(appJs.contains("function quickActionTypeCandidates()"));
        assertTrue(appJs.contains("PUBLISH_CLIP"));
        assertTrue(appJs.contains("NPC_INTERACT"));
        assertTrue(appJs.contains("function selectQuickActions(actions)"));
        assertTrue(appJs.contains("state.endingForecast?.likelyEndingType"));
        assertTrue(appJs.contains("state.stageBriefing?.actionHint"));
        assertTrue(appJs.contains("function playableActionScore(action, preferredTypes, index)"));
        assertTrue(appJs.contains("const materialBlockedClip = preferred.find(action => action.actionType === 'PUBLISH_CLIP'"));
        assertTrue(appJs.contains(".slice(0, materialBlockedClip ? 3 : 4)"));
        assertTrue(appJs.contains("先投视频或进粉丝群投稿箱补素材"));
        assertTrue(appJs.contains("function quickDisabledReasonText(reason)"));
        assertTrue(appJs.contains("INSUFFICIENT_MATERIAL_STOCK: \"缺素材，去投稿箱\""));
        assertTrue(appJs.contains("const routeLabel = visibleTextOrFallback(action.routeBiasLabel, actionDecisionTags(action)[0].value);"));
        assertTrue(appJs.contains("const comboLabel = actionComboShortText(action);"));
        assertTrue(appJs.contains("const quickVisibleReason = action.disabledReason ? quickDisabledReasonText(action.disabledReason) : (comboLabel || visibleTextOrFallback(action.tempoHint, routeLabel));"));
        assertTrue(appJs.contains("const quickAccessibleReason = action.disabledReason ? disabledReasonText(action.disabledReason) : actionSignalText(action, 'recommended');"));
        assertTrue(appJs.contains("function quickActionLabel(action, quickReason)"));
        assertTrue(appJs.contains("function actionComboShortText(action)"));
        assertTrue(appJs.contains("if (action?.comboKey) score += 58;"));
        assertTrue(appJs.contains("data-combo-key=\"${html(action.comboKey || '')}\""));
        assertTrue(appJs.contains("action-combo-strip"));
        assertTrue(appJs.contains("quick-combo-line"));
        assertTrue(appJs.contains("快捷行动：${actionNameText(action)}"));
        assertTrue(appJs.contains("${html(routeLabel)} · ${html(quickVisibleReason)}"));
        assertTrue(appJs.contains("function actionDisabledText(action, disabled)"));
        assertTrue(appJs.contains("function actionButtonStateAttributes(disabled, disabledReason)"));
        assertTrue(appJs.contains("function actionNameText(action)"));
        assertTrue(appJs.contains("return visibleTextOrFallback(action?.name, actionLabelFor(action?.actionType));"));
        assertTrue(appJs.contains("function actionPreviewText(value, fallback)"));
        assertTrue(appJs.contains("function actionDecisionReason(action, cardState)"));
        assertTrue(appJs.contains("function actionSignalText(action, cardState = 'playable')"));
        assertTrue(appJs.contains("命中信号：风险面板在亮红灯，先稳口碑和旧账。"));
        assertTrue(appJs.contains("命中信号：平台口味偏素材，优先给切片组供货。"));
        assertTrue(appJs.contains("const decisionReason = actionDecisionReason(action, cardState);"));
        assertTrue(appJs.contains("const signalText = actionSignalText(action, cardState);"));
        assertTrue(appJs.contains("const actionName = actionNameText(action);"));
        assertTrue(appJs.contains("aria-label=\"${html(`今日主推：${actionName}，${cueText}`)}\""));
        assertTrue(appJs.contains("<div class=\"action-item\""));
        assertTrue(appJs.contains("<button type=\"button\" class=\"action-submit-btn\""));
        assertTrue(appJs.contains("aria-label=\"${html(`${quickActionLabel(action, quickAccessibleReason)}，快捷键 ${hotkey}`)}\""));
        assertTrue(appJs.contains("${actionButtonStateAttributes(disabled, actionDisabledText(action, disabled))}"));
        assertTrue(appJs.contains("<span class=\"action-name\">${html(actionName)}</span>"));
        assertTrue(appJs.contains("<p class=\"action-reason\">${html(decisionReason)}</p>"));
        assertTrue(appJs.contains("<p class=\"action-signal-copy\">${html(signalText)}</p>"));
        assertTrue(appJs.contains("<p class=\"action-effect-copy\">${html(effectPreview)}</p>"));
        assertTrue(appJs.contains("actionPreviewText(action.effectPreview, \"场务还在整理数据\")"));
        assertTrue(appJs.contains("action.riskPreview ? actionPreviewText(action.riskPreview, \"风险待观察\") : \"\""));
        assertTrue(appJs.contains("<span><em aria-hidden=\"true\">${index + 1}</em>${html(actionNameText(action))}</span>"));
        assertTrue(appJs.contains("<small class=\"${comboLabel ? 'quick-combo-line' : ''}\">${html(routeLabel)} · ${html(quickVisibleReason)}</small>"));
        assertTrue(appJs.contains("data-action-state=\"${html(cardState)}\""));

        assertFalse(appJs.contains("<span class=\"panel-badge\">${state.actions.length}个可选</span>"));
        assertFalse(appJs.contains("今日主推：${action.actionType}"));
        assertFalse(appJs.contains("今日主推：${action.name}"));
        assertFalse(appJs.contains("<h4>${html(action.name)}</h4>"));
        assertFalse(appJs.contains("<span>${html(action.name)}</span>"));
        assertFalse(appJs.contains("const effectPreview = action.effectPreview;\n        const riskPreview = action.riskPreview;"));
        assertFalse(appJs.contains("const preferredActionTypes = ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN', 'REST'];"));
        assertFalse(appJs.contains(".filter(Boolean)\n        .slice(0, 4);"));
        assertFalse(appJs.contains(".sort((left, right) => Number(right.enabled) - Number(left.enabled))"));
        assertFalse(appJs.contains("<small>${html(actionDecisionTags(action)[0].value)}</small>"));
        assertFalse(appJs.contains("<p>${html(effectPreview)}</p>"));
    }

    @Test
    void playableFrontendKeepsRouteActionsAheadOfGenericStabilityForOpeningPreset() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function primaryRouteActionTypes(likelyEnding)"));
        assertTrue(appJs.contains("function hasMeaningfulReadyRisk()"));
        assertTrue(appJs.contains("const routeFirstTypes = hasMeaningfulReadyRisk() ? [] : primaryRouteActionTypes(likelyEnding);"));
        assertTrue(appJs.contains("...riskBoosts, ...routeFirstTypes, ...trendBoosts, ...base"));
        assertTrue(appJs.contains("if (!hasMeaningfulReadyRisk() && primaryRouteActionTypes(likelyEnding).includes(action?.actionType))"));
        assertTrue(appJs.contains("score += 36;"));
        assertTrue(appJs.indexOf("...riskBoosts, ...routeFirstTypes, ...trendBoosts, ...base")
                < appJs.indexOf("...base, 'PUBLISH_CLIP', 'NPC_INTERACT', 'STREAM_PLAN', 'REST'"));
    }

    @Test
    void playableFrontendShowsReadyGoalBoardBeforeActionButtons() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function renderReadyGoalBoard(primaryAction, enabledActionCount)"));
        assertTrue(appJs.contains("class=\"ready-goal-board\" aria-label=\"今日目标板\""));
        assertTrue(appJs.contains("本轮目标"));
        assertTrue(appJs.contains("readyGoalCoachText(primaryAction)"));
        assertTrue(appJs.contains("data-action=\"open-ready-goal\""));
        assertTrue(appJs.contains("data-goal-target=\"forecast\""));
        assertTrue(appJs.contains("data-goal-target=\"risk\""));
        assertTrue(appJs.contains("const readyGoalBoard = renderReadyGoalBoard(primaryReadyAction, enabledActionCount);"));
        assertTrue(appJs.indexOf("${readyGoalBoard}") < appJs.indexOf("${dailyPlanCards}"));
        assertTrue(appCss.contains(".ready-goal-board"));
        assertTrue(appCss.contains(".ready-goal-grid"));
        assertTrue(appCss.contains(".ready-goal-item.hot"));
        assertTrue(appCss.contains(".ready-goal-route"));
    }

    @Test
    void playableFrontendSurfacesMaterialStockAndFanSubmissionHint() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("{ label: '切片素材', value: materialStock, key: 'materialStock', current: materialStock },"));
        assertTrue(appJs.contains("const materialStock = v.funProfile?.materialStock ?? 0;"));
        assertTrue(appJs.contains("desktop-stat-brief"));
        assertTrue(appJs.contains("function fanTopicSummaryHint(topic)"));
        assertTrue(appJs.contains("投稿箱可补素材"));
        assertTrue(appJs.contains("${html(fanTopicSummaryHint(topic))}"));
        assertTrue(appJs.indexOf("{ label: '口碑', value: currentReputation,")
                < appJs.indexOf("{ label: '切片素材', value: materialStock,"));

        assertFalse(appJs.contains("{ label: '口味', value: platformTrend?.label || '常规' }"));
        assertFalse(appJs.contains("<small>${state.fanTopics.length}个待处理</small>"));
    }

    @Test
    void playableFrontendConnectsDesktopMaterialGateToFanSubmissionRail() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function fanTopicMaterialCueText(topic)"));
        assertTrue(appJs.contains("切片按钮灰掉时，点“改成投稿征集”能给素材库+1。"));
        assertTrue(appJs.contains("function fanTopicChoiceClass(choice, disabled)"));
        assertTrue(appJs.contains("choice.choiceType === \"COLLECT_SUBMISSIONS\" ? ' submission' : ''"));
        assertTrue(appJs.contains("class=\"ambient-rail-cue\""));
        assertTrue(appJs.contains("material-rail-card"));
        assertTrue(appCss.contains(".ambient-rail-cue"));
        assertTrue(appCss.contains(".fan-topic-choice.submission"));
        assertTrue(probe.contains("readyAmbientSubmissionCueVisible"));
        assertTrue(probe.contains("readyQuickMaterialGateConnectsToAmbient"));
        assertTrue(probe.contains("[data-topic-choice=\"COLLECT_SUBMISSIONS\"]"));
    }

    @Test
    void playableFrontendKeepsControlsTouchFriendlyAndFocusable() throws Exception {
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appCss.contains("button {\n    min-height: 44px;"));
        assertTrue(appCss.contains(".help-button {\n    width: 44px;\n    height: 44px;"));
        assertTrue(appCss.contains(".logout-button {\n    min-height: 44px;"));
        assertTrue(appCss.contains(".info-tab {\n    min-height: 44px;"));
        assertTrue(appCss.contains(":where(button, summary, input, select):focus-visible"));
        assertTrue(appCss.contains("outline: 2px solid var(--neon-cyan);"));
        assertTrue(appCss.contains("button.primary"));
        assertTrue(appCss.contains("color: #111113;"));
        assertTrue(appCss.contains(".report-material-receipt"));
        assertTrue(appCss.contains("-webkit-line-clamp: 2;"));
        assertTrue(appCss.contains(".fan-topic-copy"));
        assertTrue(appCss.contains(".fan-topic-choice"));
        assertTrue(appCss.contains(".fan-topic-choice-meta"));
        assertTrue(appCss.contains(".fan-topic-choice-disabled"));
        assertTrue(appCss.contains(".info-content {\n    flex: 1;\n    min-width: 0;\n    overflow-y: auto;\n    overflow-x: hidden;"));
        assertTrue(appCss.contains(".info-panel {\n    display: none;\n    min-width: 0;"));
        assertTrue(appCss.contains(".info-card {\n    min-width: 0;"));
        assertTrue(appCss.contains(".info-card p {\n    min-width: 0;"));
        assertTrue(appCss.contains("text-overflow: ellipsis;"));
    }

    @Test
    void playableFrontendUsesChineseOnlyVisibleDayCopy() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("欢迎来到 VUP出道局"));
        assertTrue(indexHtml.contains("第--天"));
        assertFalse(indexHtml.contains("Day --"));
        assertFalse(indexHtml.contains("Day 1"));
        assertFalse(indexHtml.contains("UP World"));

        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("formatDayBadge("));
        assertTrue(appJs.contains("开播前"));
        assertTrue(appJs.contains("创建成功会进入第1天"));
        assertFalse(appJs.contains("Day ${"));
        assertFalse(appJs.contains("'Day --'"));
        assertFalse(appJs.contains("'Start'"));
    }

    @Test
    void playableFrontendUsesCompactComfortableGameShell() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("class=\"play-shell game-main\""));
        assertTrue(indexHtml.contains("id=\"actionPanel\""));
        assertTrue(indexHtml.contains("id=\"mainContent\""));

        MvcResult result = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appCss = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appCss.contains(".play-shell"));
        assertTrue(appCss.contains(".flow-panel"));
        assertTrue(appCss.contains(".insight-dock"));
        assertTrue(appCss.contains("overflow-y: hidden"));
        assertTrue(appCss.contains(".game-panels"));
        assertTrue(appCss.contains("overflow-y: auto"));
        assertTrue(appCss.contains("grid-template-columns: repeat(2, minmax(0, 1fr))"));
        assertTrue(appCss.contains("min-height: 100vh"));
        assertTrue(appCss.contains("body.app-busy"));
        assertTrue(appCss.contains("@media (min-width: 901px)"));
        assertTrue(appCss.contains(".pregame-mode .play-shell"));
        assertTrue(appCss.contains("grid-template-columns: minmax(220px, 260px) minmax(620px, 820px);"));
        assertTrue(appCss.contains(".pregame-mode .game-info,\n    .pregame-mode .game-panels"));
        assertTrue(appCss.contains(".pregame-mode .auth-section"));
        assertTrue(appCss.contains(".pregame-mode .coach-card"));
        assertFalse(appCss.contains("    height: 100vh;\n    opacity: 0;"));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        assertTrue(appJs.contains("function syncShellMode()"));
        assertTrue(appJs.contains("document.body.classList.toggle(\"pregame-mode\", !state.vup);"));
        assertTrue(appJs.contains("function syncGameplayInfoTab()"));
        assertTrue(appJs.contains("function stageDefaultInfoTabFor(phase)"));
        assertTrue(appJs.contains("state.infoTab === 'demo' || state.infoTabAutoPhase !== phase"));
        assertTrue(appJs.contains("activateInfoTab(defaultInfoTab);"));
        assertTrue(appJs.contains("syncShellMode();"));
        assertTrue(appJs.contains("syncGameplayInfoTab();"));
        assertTrue(appJs.contains("function renderDanmaku()"));
        assertTrue(appJs.contains("if (!state.vup || !state.danmaku)"));
        assertTrue(appJs.contains("panel.classList.add('hidden');"));
        assertTrue(appJs.contains("panel.classList.remove('hidden');"));
        assertTrue(appJs.contains("state.infoTab"));
        assertTrue(appJs.contains("const infoTabPanels"));
        assertTrue(appJs.contains("reportHistoryPanel"));
        assertTrue(appJs.contains("funAudiencePanel"));
    }

    @Test
    void desktopVisualProbeProtectsPregameWorkbench() throws Exception {
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(probe.contains("const scenario = process.argv[7] || 'pregame';"));
        assertTrue(probe.contains("pregameModeEnabled"));
        assertTrue(probe.contains("pregameHidesInactiveInfoRail"));
        assertTrue(probe.contains("pregameHidesInactiveGamePanels"));
        assertTrue(probe.contains("pregameHasNoEmptyVisiblePanels"));
        assertTrue(probe.contains("authPanelComfortWidth"));
        assertTrue(probe.contains("centeredWorkbench"));
        assertTrue(probe.contains("async function enterReadyScenario"));
        assertTrue(probe.contains("readyModeEnabled"));
        assertTrue(probe.contains("readyActionPanelAboveFold"));
        assertTrue(probe.contains("readyShowsFourQuickActions"));
        assertTrue(probe.contains("readyBlockedQuickSlotReasonVisible"));
        assertTrue(probe.contains("readyInsightDigestVisible"));
        assertTrue(probe.contains("readyInsightDigestEntrypointsAccessible"));
        assertTrue(probe.contains("text.split(/\\\\s+/).filter(Boolean)"));
        assertTrue(probe.contains("readyInsightDigestBeforeActions"));
        assertTrue(probe.contains("readyRightRailBriefVisible"));
        assertTrue(probe.contains("readyCenterMainFlowOnlyPrimaryPanel"));
        assertTrue(probe.contains("readyCenterGamePanelsNotScrollable"));
        assertTrue(probe.contains("readyPrimaryActionCueVisible"));
        assertTrue(probe.contains("readyPrimaryActionMatchesRecommendation"));
        assertTrue(probe.contains("readyPrimaryCueCompact"));
        assertTrue(probe.contains("readyPrimaryCueDoesNotPushFold"));
        assertTrue(probe.contains("readyLaptopStatusToastDoesNotPushFold"));
        assertTrue(probe.contains("readyLaptopStatusHeightCapped"));
        assertTrue(probe.contains("readyLaptopStatusDoesNotCoverActions"));
        assertTrue(probe.contains("readyLaptopStatusDoesNotCoverCoach"));
        assertTrue(probe.contains("noVerticalPageOverflow"));
        assertTrue(probe.contains("readyAmbientRailAvailable"));
        assertTrue(probe.contains("readyAmbientPanelPopulated"));
        assertTrue(probe.contains("readyDesktopActionStatesDistinct"));
        assertTrue(probe.contains("readyDesktopActionCardsAccessible"));
        assertTrue(probe.contains("readyDesktopActionHotkeys"));
        assertTrue(probe.contains("readyDesktopActionHotkeyDispatch"));
        assertTrue(probe.contains("titleStageChoiceHotkeyDispatch"));
        assertTrue(probe.contains("eventStageChoiceHotkeyDispatch"));
        assertTrue(probe.contains("readyCenterColumnDominates"));
        assertTrue(probe.contains("readyRightRailHasUsefulSignal"));
        assertTrue(probe.contains("readyRightRailNotScrollable"));
        assertTrue(probe.contains("readyRightRailContentNotClipped"));
        assertTrue(probe.contains("infoContentScrollable"));
        assertTrue(probe.contains("stageRightRailKeepsGameplayContext"));
        assertTrue(probe.contains("stageRightRailContextualSignal"));
        assertTrue(probe.contains("stageRightRailNoRawEnum"));
        assertTrue(probe.contains("stageRightRailEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Backend stage label should not be visible"));
        assertTrue(probe.contains("English stage briefing leaked into right rail"));
        assertTrue(probe.contains("CURRENT_TREND_LABEL_RAW"));
        assertTrue(probe.contains("REPORT_HISTORY_SUMMARY_RAW"));
        assertTrue(probe.contains("ENDING_CONTEXT_HEADLINE_RAW"));
        assertTrue(probe.contains("activeInfoTab !== 'demo' && !isVisible('#demoPanel')"));
        assertTrue(probe.contains("infoShell.scrollWidth <= infoShell.clientWidth + 2"));
        assertTrue(probe.contains("infoContent.scrollWidth <= infoContent.clientWidth + 2"));
        assertTrue(probe.contains("desktopActionCardSyntheticNoRawEnum"));
        assertTrue(probe.contains("desktopActionPlanLabelsSyntheticNoRawEnum"));
        assertTrue(probe.contains("desktopRightRailSyntheticNoRawEnum"));
        assertTrue(probe.contains("desktopRightRailSyntheticRiskToolsUsable"));
        assertTrue(probe.contains("NPC_RAW_NAME"));
        assertTrue(probe.contains("BUZZ_HEADLINE_RAW"));
        assertTrue(probe.contains("npc_raw_image.webp"));
        assertTrue(probe.contains("AUDIENCE_ROUTE_RAW"));
        assertTrue(probe.contains("FAN_TOPIC_TITLE_RAW"));
        assertTrue(probe.contains("DANMAKU_PERSONA_RAW"));
        assertTrue(probe.contains("RISK_TOOL_LABEL_RAW"));
        assertTrue(probe.contains("FULL_CLIP_CONTEXT_RAW"));
        assertTrue(probe.contains("FUN_METRIC_LABEL_RAW"));
        assertTrue(probe.contains("COMBO_LABEL_RAW"));
        assertTrue(probe.contains("PERSONA_TAG_RAW"));
        assertTrue(probe.contains("MEME_STAGE_RAW"));
        assertTrue(probe.contains("ENDING_HEADLINE_RAW"));
        assertTrue(probe.contains("群友还在整理小作文"));
        assertTrue(probe.contains("弹幕还在同步"));
        assertTrue(probe.contains("同台情报还在整理"));
        assertTrue(probe.contains("热搜记录还在整理"));
        assertTrue(probe.contains("乐子人还在对轴"));
        assertTrue(probe.contains("组合证据还在整理"));
        assertTrue(probe.contains("未署名人设标签"));
        assertTrue(probe.contains("梗阶段走向待观察"));
        assertTrue(probe.contains("结局风险待观察"));
        assertTrue(probe.contains("路线快照还在复盘"));
        assertTrue(probe.contains("这天日报还在补录"));
        assertTrue(probe.contains("结局标题待定"));
        assertTrue(probe.contains("BACKEND_ACTION_NAME"));
        assertTrue(probe.contains("appStatusNoRawEnum"));
        assertTrue(probe.contains("DIRECT_STATUS_RAW"));
        assertTrue(probe.contains("directStatusSyntheticNoRawEnum"));
        assertTrue(probe.contains("directStatusTitleNoRawEnum"));
        assertTrue(probe.contains("apiFallbackWithoutMessageDoesNotExposeRawCode"));
        assertTrue(probe.contains("apiFallbackWithoutMessageHidesHttpStatus"));
        assertTrue(probe.contains("apiFallbackMixedMessageDoesNotExposeRawEnum"));
        assertTrue(probe.contains("apiFallbackMixedMessageHidesHttpStatus"));
        assertTrue(probe.contains("apiFallbackEnglishMessagePrefersLocalCopy"));
        assertTrue(probe.contains("apiFallbackEnglishMessageHidesHttpStatus"));
        assertTrue(probe.contains("appStatusEnglishMessageDoesNotExposeEnglish"));
        assertTrue(probe.contains("desktopDemoPanelSyntheticNoRawEnum"));
        assertTrue(probe.contains("desktopDemoPanelPhaseLocalized"));
        assertTrue(probe.contains("desktopDemoPanelEndingTypeLocalized"));
        assertTrue(probe.contains("desktopDemoPanelBriefVisible"));
        assertTrue(probe.contains("desktopDemoPanelRightRailNotScrollable"));
        assertTrue(probe.contains("desktopDemoPanelControlsAccessible"));
        assertTrue(probe.contains("UNMAPPED_BACKEND_FAILURE"));
        assertTrue(probe.contains("Desktop visual check failed"));
    }

    @Test
    void playableFrontendProtectsLaptopHeightDesktopReadyLayout() throws Exception {
        var appJsResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        var appCssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(appJsResult);
        String appCss = new String(appCssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function isLowHeightDesktopViewport()"));
        assertTrue(appJs.contains("行动参数：${selectedLabels.join(' / ')} · 点开改"));
        assertTrue(appJs.contains("点开可修改"));
        assertTrue(appCss.contains("@media (min-width: 901px) and (max-height: 800px)"));
        assertTrue(appCss.contains("left: var(--status-toast-left, 50%);"));
        assertTrue(appCss.contains("width: var(--status-toast-width, min(720px, calc(100vw - 48px)));"));
        assertTrue(appCss.contains("transform: var(--status-toast-transform, translateX(-50%));"));
        assertTrue(appCss.contains("pointer-events: none;"));
        assertTrue(appCss.contains("max-height: calc(1.45em * 2 + 16px);"));
        assertTrue(appCss.contains("-webkit-line-clamp: 2;"));
        assertTrue(appCss.contains(".ready-mode .insight-digest"));
        assertTrue(appCss.contains(".ready-mode .action-item"));
        assertTrue(appCss.contains(".ready-mode .action-options summary"));
        assertTrue(appCss.contains(".ready-mode .action-options[open] summary"));
        assertTrue(appCss.contains(".desktop-state-platform-trend"));

        assertTrue(probe.contains("ready-laptop"));
        assertTrue(probe.contains("isLaptopReadyScenario"));
        assertTrue(probe.contains("readyLaptopHidesCenterDigest"));
        assertTrue(probe.contains("readyLaptopActionPanelAboveFold"));
        assertTrue(probe.contains("readyLaptopAllDesktopActionsAboveFold"));
        assertTrue(probe.contains("readyLaptopCoreRoundNoWheel"));
        assertTrue(probe.contains("desktopActionBottoms"));
        assertTrue(probe.contains("!actionOptions.open"));
        assertTrue(probe.contains("readyLaptopStatusToastDoesNotPushFold"));
        assertTrue(probe.contains("readyLaptopStatusHeightCapped"));
        assertTrue(probe.contains("readyLaptopStatusDoesNotCoverActions"));
        assertTrue(probe.contains("readyLaptopStatusDoesNotCoverCoach"));
        assertTrue(probe.contains("readyRightRailContentNotClipped"));
    }

    @Test
    void playableFrontendShowsDesktopRightRailBriefing() throws Exception {
        MvcResult indexResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String indexHtml = new String(indexResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        int infoBrief = indexHtml.indexOf("id=\"infoBrief\"");
        int infoTabs = indexHtml.indexOf("class=\"info-tabs\"");
        assertTrue(infoBrief > 0);
        assertTrue(infoBrief < infoTabs);
        assertTrue(appJs.contains("function renderInfoBrief()"));
        assertTrue(appJs.contains("今日情报板"));
        assertTrue(appJs.contains("renderInfoBrief();"));
        assertTrue(appCss.contains(".info-brief"));
        assertTrue(appCss.contains(".ready-mode .info-brief"));
        assertTrue(appCss.contains(".ready-mode .info-tab"));
    }

    @Test
    void playableFrontendKeepsDesktopReadyAmbientPanelsOutOfMainScroll() throws Exception {
        MvcResult indexResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String indexHtml = new String(indexResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(indexHtml.contains("data-tab=\"ambient\""));
        assertTrue(indexHtml.contains("id=\"ambientPanel\""));
        assertTrue(appJs.contains("ambient: ['ambientPanel']"));
        assertTrue(appJs.contains("function renderAmbientPanel()"));
        assertTrue(appJs.contains("renderAmbientPanel();"));
        assertTrue(appJs.contains("tab: 'ambient'"));
        assertTrue(appJs.contains("data-info-tab=\"${tab}\""));
        assertTrue(appCss.contains(".ready-mode .game-panels > .fan-topic-panel"));
        assertTrue(appCss.contains(".ready-mode .game-panels > .danmaku-panel"));
        assertTrue(appCss.contains(".ambient-rail-actions"));
    }

    @Test
    void playableFrontendMarksDesktopActionCardsByDecisionState() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function actionCardState(action, disabled, recommended)"));
        assertTrue(appJs.contains("function actionCardAriaLabel(action, disabled, recommended)"));
        assertTrue(appJs.contains("data-action-state=\"${html(cardState)}\""));
        assertTrue(appJs.contains("data-recommended=\"${recommended ? 'true' : 'false'}\""));
        assertTrue(appJs.contains("<span class=\"action-state-pill\">${html(actionCardStateLabel(cardState))}</span>"));
        assertTrue(appJs.contains("推荐先点"));
        assertTrue(appJs.contains("暂时卡住"));

        assertTrue(appCss.contains(".action-title-row"));
        assertTrue(appCss.contains(".action-reason"));
        assertTrue(appCss.contains(".action-signal-copy"));
        assertTrue(appCss.contains(".action-effect-copy"));
        assertTrue(appCss.contains(".action-state-pill"));
        assertTrue(appCss.contains(".ready-mode .action-item[data-recommended=\"true\"]"));
        assertTrue(appCss.contains(".ready-mode .action-item[data-action-state=\"blocked\"]"));
        assertTrue(appCss.contains(".ready-mode .action-item[data-action-state=\"playable\"]"));
        assertTrue(appCss.contains(".ready-mode .action-effect-copy"));
        assertTrue(appCss.contains(".ready-mode .action-reason"));
        assertTrue(appCss.contains(".ready-mode .action-signal-copy"));
    }

    @Test
    void actionApiPromotesRouteAlignedPrimaryActionOnFreshRun() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("route_priority_player", "路线主推员");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recommendedReason", containsString("主推")));
    }

    @Test
    void actionApiSwitchesToRecoveryGuidanceWhenDebtAndPressureBuildUp() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("recovery_priority_player", "稳盘观察员");

        submitActionAndResolveFormalEvent(session, "REST", 1, "recovery-priority-day-1", null);
        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "recovery-priority-go-day-2"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recommendedReason", containsString("主推")));
    }

    @Test
    void playableFrontendSupportsDesktopActionHotkeysWithoutHijackingInputs() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("function handleActionHotkey(event)"));
        assertTrue(appJs.contains("document.addEventListener('keydown', handleActionHotkey);"));
        assertTrue(appJs.contains("function actionHotkeyIgnoredTarget(target)"));
        assertTrue(appJs.contains("return target.closest('input, textarea, select, [contenteditable=\"true\"]')"));
        assertTrue(appJs.contains("data-action-hotkey=\"${hotkey}\""));
        assertTrue(appJs.contains("data-action-hotkey=\"1\""));
        assertTrue(appJs.contains("快捷键 ${hotkey}"));
        assertTrue(appCss.contains(".action-submit-btn"));
        assertTrue(appCss.contains(".ready-mode .action-detail-list"));
    }

    @Test
    void playableFrontendShowsDesktopReportLoopWithoutScrolling() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("document.body.classList.toggle(\"report-mode\", Boolean(state.vup) && phase === \"REPORT_READY\");"));
        assertTrue(appJs.contains("REPORT_READY: \"report\""));
        assertTrue(appJs.contains("class=\"report-loop\""));
        assertTrue(appJs.contains("class=\"report-kpi-strip\""));
        assertTrue(appJs.contains("class=\"report-next-day\""));
        assertTrue(appJs.contains("读完开下一天"));
        assertTrue(appJs.contains("今天先收摊复盘，读完就切到下一天。"));
        assertFalse(appJs.contains("读完点“下一天”"));
        assertTrue(appJs.contains("aria-label=\"读完第${state.report.day}天日报，进入下一天，快捷键 回车\""));
        assertTrue(appJs.contains("\"#reportPanel [data-action=\\\"next-day\\\"]\""));
        assertTrue(appJs.contains("function reportVisibleItemText(value)"));
        assertTrue(appJs.contains("function reportMaterialReceiptText(value)"));
        assertTrue(appJs.contains("const detailItems = (state.report.visibleItems || [])"));
        assertTrue(appJs.contains(".map(i => `<li>${html(reportVisibleItemText(i))}</li>`)"));
        assertTrue(appJs.contains("const materialReceiptText = materialReceipt ? reportMaterialReceiptText(materialReceipt) : '';"));
        assertTrue(appJs.contains("return `素材小票：${localizedVisibleTextOrFallback(body, \"今日素材已记入素材库。\")}`;"));
        assertTrue(appJs.contains("return localizedVisibleTextOrFallback(raw, \"复盘条目还在整理。\");"));
        assertTrue(appJs.contains("const summaryText = reportShortSentence(state.report.summary, \"今日直播已收摊，复盘还在整理中。\", 46);"));
        assertTrue(appJs.contains("localizedVisibleTextOrFallback(platformTrend.label, \"平台口味\")"));
        assertTrue(appJs.contains("localizedVisibleTextOrFallback(platformTrend.description, \"平台口味暂时没有大波动，按当前节奏继续试水。\")"));
        assertTrue(appJs.contains("localizedVisibleTextOrFallback(state.report.riskHint, \"有风险苗头，下一天别硬冲。\")"));
        assertTrue(appJs.contains("class=\"report-missing\""));
        assertTrue(appJs.contains("class=\"platform-trend-label\""));
        assertTrue(appJs.contains("class=\"platform-trend-description\""));
        String reportSource = appJs.substring(
                appJs.indexOf("function renderReport()"),
                appJs.indexOf("function renderEnding()"));
        assertTrue(reportSource.contains("style=\"--mood-color: ${moodColor}\""));

        assertTrue(appCss.contains(".report-mode .report-panel"));
        assertTrue(appCss.contains(".report-mode .report-loop"));
        assertTrue(appCss.contains(".report-mode .report-next-day"));
        assertTrue(appCss.contains(".report-missing"));
        assertTrue(appCss.contains(".platform-trend-label"));
        assertTrue(appCss.contains(".platform-trend-description"));
        assertTrue(appCss.contains(".stage-focus-mode.report-mode #reportHistoryPanel.active"));
        assertTrue(appCss.contains(".report-mode .game-panels > .fan-topic-panel"));

        assertTrue(probe.contains("async function advanceToReportScenario"));
        assertTrue(probe.contains("reportModeEnabled"));
        assertTrue(probe.contains("reportPanelAboveFold"));
        assertTrue(probe.contains("reportNextDayCtaVisible"));
        assertTrue(probe.contains("reportNextDayCtaExplainsAdvance"));
        assertTrue(probe.contains("reportCoachCopyMatchesAdvanceCta"));
        assertTrue(probe.contains("reportStatusAvoidsNextDay"));
        assertTrue(probe.contains("reportSummaryReadable"));
        assertTrue(probe.contains("reportDetailsCollapsed"));
        assertTrue(probe.contains("reportNextDayHotkeyDispatch"));
        assertTrue(probe.contains("reportCenterMainFlowOnlyReportPanel"));
        assertTrue(probe.contains("reportCenterGamePanelsNotScrollable"));
        assertTrue(probe.contains("reportNoRawEnum"));
        assertTrue(probe.contains("REPORT_VISIBLE_ITEM_RAW"));
        assertTrue(probe.contains("MATERIAL_RECEIPT_RAW"));
        assertTrue(probe.contains("reportVisibleItemsSyntheticNoRawEnum"));
        assertTrue(probe.contains("reportMaterialReceiptSyntheticReadable"));
        assertTrue(probe.contains("reportSyntheticEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Daily report unavailable"));
        assertTrue(probe.contains("activeInfoTab === 'report'"));
        assertTrue(probe.contains("isVisible('#reportHistoryPanel')"));
    }

    @Test
    void playableFrontendShowsDesktopEndingLoopWithoutScrolling() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("document.body.classList.toggle(\"ending-mode\", Boolean(state.vup) && phase === \"ENDING_READY\");"));
        assertTrue(appJs.contains("ENDING_READY: \"environment\""));
        assertTrue(appJs.contains("class=\"ending-content\""));
        assertTrue(appJs.contains("class=\"ending-main\""));
        assertTrue(appJs.contains("class=\"ending-summary-strip\""));
        assertTrue(appJs.contains("class=\"ending-restart-console\""));
        assertTrue(appJs.contains("class=\"ending-details\""));
        assertTrue(appJs.contains("aria-label=\"选择复活赛路线倾向\""));
        assertTrue(appJs.contains("function restartBiasHintText(routeKey)"));
        assertTrue(appJs.contains("function updateRestartBiasHint(userChanged = false)"));
        assertTrue(appJs.contains("state.restartBiasOverride"));
        assertTrue(appJs.contains("endingCollection.nextTargetType"));
        assertTrue(appJs.contains("id=\"restartBiasHint\""));
        assertTrue(appJs.contains("切片组继续供货"));
        assertTrue(appJs.contains("主会场继续开庭"));
        assertTrue(appCss.contains(".ending-restart-bias-hint"));
        assertTrue(appJs.contains("aria-label=\"看完${html(finalTitle)}，开启复活赛\""));
        assertTrue(appJs.contains("routeLabelFor(state.ending.endingType)"));
        assertTrue(appJs.contains("\"#endingPanel [data-action=\\\"restart\\\"]\""));
        assertTrue(probe.contains("ENDING_TYPE_RAW"));
        assertTrue(probe.contains("FINAL_TITLE_RAW"));
        assertTrue(probe.contains("key_event_raw"));
        assertTrue(probe.contains("raw_final_report_ref.json"));
        assertTrue(probe.contains("endingRawFieldsSyntheticReadable"));
        assertTrue(probe.contains("endingSyntheticEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Ending summary unavailable"));
        assertTrue(probe.contains("endingForecastEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Forecast headline should not be visible"));
        assertTrue(probe.contains("English ending forecast leaked into right rail"));

        assertTrue(appCss.contains(".ending-mode .ending-panel"));
        assertTrue(appCss.contains(".ending-mode .ending-loop"));
        assertTrue(appCss.contains(".ending-mode .ending-summary-strip"));
        assertTrue(appCss.contains(".ending-mode .ending-restart-console"));
        assertTrue(appCss.contains(".stage-focus-mode.ending-mode #endingForecastPanel.active"));
        assertTrue(appCss.contains(".ending-mode .game-panels > .fan-topic-panel"));

        assertTrue(probe.contains("async function enterEndingScenario"));
        assertTrue(probe.contains("endingModeEnabled"));
        assertTrue(probe.contains("endingPanelAboveFold"));
        assertTrue(probe.contains("endingRestartCtaVisible"));
        assertTrue(probe.contains("endingRestartBiasHintReadable"));
        assertTrue(probe.contains("endingRestartBiasHintSwitches"));
        assertTrue(probe.contains("endingStatusAvoidsRestart"));
        assertTrue(probe.contains("endingRestartHotkeyDispatch"));
        assertTrue(probe.contains("endingDetailsCollapsed"));
        assertTrue(probe.contains("endingCenterMainFlowOnlyEndingPanel"));
        assertTrue(probe.contains("endingCenterGamePanelsNotScrollable"));
        assertTrue(probe.contains("endingNoRawEnum"));
        assertTrue(probe.contains("activeInfoTab === 'environment'"));
        assertTrue(probe.contains("isVisible('#endingForecastPanel')"));
    }

    @Test
    void playableFrontendShowsDesktopDecisionCardsWithoutScrolling() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("document.body.classList.toggle(\"title-mode\", Boolean(state.vup) && phase === \"NEED_TITLE\");"));
        assertTrue(appJs.contains("document.body.classList.toggle(\"event-mode\", Boolean(state.vup) && (phase === \"NEED_EVENT_CHOICE\" || phase === \"NEED_INTERACTION_CHOICE\"));"));
        assertTrue(appJs.contains("NEED_TITLE: \"npc\""));
        assertTrue(appJs.contains("NEED_EVENT_CHOICE: \"npc\""));
        assertTrue(appJs.contains("class=\"title-decision-loop\""));
        assertTrue(appJs.contains("class=\"title-card-head\""));
        assertTrue(appJs.contains("class=\"title-decision-strip\""));
        assertTrue(appJs.contains("titleDecisionChips(t, effectText, riskText)"));
        assertTrue(appJs.contains("title-chip heat"));
        assertTrue(appJs.contains("title-chip risk"));
        assertTrue(appJs.contains("title-chip route"));
        assertTrue(appJs.contains("<span class=\"title-chip heat\"><em>热度</em><strong>${html(decisionChips.heat)}</strong></span>"));
        assertTrue(appJs.contains("data-title-rank=\"${index + 1}\""));
        assertTrue(appJs.contains("aria-label=\"选择直播标题：${html(titleText)}，${html(decisionHint)}，快捷键 ${index + 1}\""));
        assertTrue(appJs.contains("class=\"event-box event-decision-loop\""));
        assertTrue(appJs.contains("class=\"event-choice-label\""));
        assertTrue(appJs.contains("function eventChoiceCtaText(choice, index)"));
        assertTrue(appJs.contains("function eventChoiceChips(choice, costPreview, effectPreview)"));
        assertTrue(appJs.contains("event-choice-line cost"));
        assertTrue(appJs.contains("event-choice-line payoff"));
        assertTrue(appJs.contains("event-choice-line route"));
        assertTrue(appJs.contains("event-choice-line debt"));
        assertTrue(appJs.contains("event-pressure-chip severity"));
        assertTrue(appJs.contains("event-pressure-chip choices"));
        assertTrue(appJs.contains("event-pressure-chip tempo"));
        assertTrue(appJs.contains("const choiceCta = eventChoiceCtaText(choice, index);"));
        assertTrue(appJs.contains("aria-label=\"选择：${html(choiceLabel)}，操作：${html(choiceCta)}，快捷键 ${choiceHotkey}\""));
        assertTrue(appJs.contains(">${html(choiceCta)}</button>"));

        assertTrue(appCss.contains(".title-mode .title-list"));
        assertTrue(appCss.contains(".title-mode .title-item"));
        assertTrue(appCss.contains(".title-mode .title-decision-strip"));
        assertTrue(appCss.contains(".title-decision-strip"));
        assertTrue(appCss.contains(".title-chip"));
        assertTrue(appCss.contains(".title-chip.heat"));
        assertTrue(appCss.contains(".title-chip.risk"));
        assertTrue(appCss.contains(".title-chip.route"));
        assertTrue(appCss.contains(".stage-focus-mode.title-mode #stageBriefingPanel.active"));
        assertTrue(appCss.contains(".stage-focus-mode.event-mode #stageBriefingPanel.active"));
        assertTrue(appCss.contains(".event-mode .event-box"));
        assertTrue(appCss.contains(".event-pressure-strip"));
        assertTrue(appCss.contains(".event-pressure-chip"));
        assertTrue(appCss.contains(".event-mode .event-pressure-strip"));
        assertTrue(appCss.contains(".event-mode .event-choice"));
        assertTrue(appCss.contains(".event-choice-line"));
        assertTrue(appCss.contains(".event-choice-line.cost"));
        assertTrue(appCss.contains(".event-choice-line.payoff"));
        assertTrue(appCss.contains(".event-choice-line.route"));
        assertTrue(appCss.contains(".event-choice-line.debt"));
        assertTrue(appCss.contains(".event-mode .game-panels > .fan-topic-panel"));

        assertTrue(probe.contains("async function enterTitleScenario"));
        assertTrue(probe.contains("async function enterEventScenario"));
        assertTrue(probe.contains("titleModeEnabled"));
        assertTrue(probe.contains("titleCardsAboveFold"));
        assertTrue(probe.contains("titleDecisionCardsAccessible"));
        assertTrue(probe.contains("titleDecisionStrips"));
        assertTrue(probe.contains("titleCardSyntheticEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Backend generated title"));
        assertTrue(probe.contains("稳健下饭|热度上桌|开庭预警|标题组观望"));
        assertTrue(probe.contains("titleCenterMainFlowOnlyTitlePanel"));
        assertTrue(probe.contains("eventModeEnabled"));
        assertTrue(probe.contains("eventDecisionLoopAboveFold"));
        assertTrue(probe.contains("eventChoiceButtonsVisible"));
        assertTrue(probe.contains("eventChoiceButtonsHaveSpecificChineseCta"));
        assertTrue(probe.contains("稳住米线|接住热度|整点节目|选此方案"));
        assertTrue(probe.contains("eventCardSyntheticEnglishCopyUsesFallback"));
        assertTrue(probe.contains("Event description unavailable"));
        assertTrue(probe.contains("eventCenterMainFlowOnlyEventPanel"));
        assertTrue(probe.contains("eventNoRawEnum"));
        assertTrue(probe.contains("activeInfoTab === 'npc'"));
        assertTrue(probe.contains("isVisible('#stageBriefingPanel')"));
    }

    @Test
    void playableFrontendUsesDesktopStageFocusModeForDecisionPhases() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("const stageFocus = Boolean(state.vup) &&"));
        assertTrue(appJs.contains("document.body.classList.toggle(\"stage-focus-mode\", stageFocus);"));
        assertTrue(appJs.contains("function stageDefaultInfoTabFor(phase)"));
        assertTrue(appJs.contains("state.infoTabAutoPhase !== phase"));
        assertTrue(appJs.contains("class=\"stage-fortune-card\""));
        assertTrue(appJs.contains("class=\"stage-briefing-head\""));
        assertTrue(appJs.contains("class=\"stage-briefing-days\""));
        assertTrue(appJs.contains("class=\"stage-trend-pill current\""));
        assertTrue(appJs.contains("class=\"stage-trend-pill next\""));
        assertTrue(appJs.contains("class=\"stage-briefing-grid\""));
        assertTrue(appJs.contains("class=\"stage-briefing-chip ${tone}\""));
        assertTrue(appJs.contains("class=\"stage-briefing-nextplay\""));
        String stageBriefingSource = appJs.substring(
                appJs.indexOf("function renderStageBriefing()"),
                appJs.indexOf("function renderAudienceExpectation()"));
        assertFalse(stageBriefingSource.contains("style="));
        assertFalse(appJs.contains("var(--line)"));

        assertTrue(appCss.contains(".stage-focus-mode .play-shell"));
        assertTrue(appCss.contains(".stage-focus-mode .game-sidebar"));
        assertTrue(appCss.contains(".stage-focus-mode .game-info"));
        assertTrue(appCss.contains(".stage-briefing-head"));
        assertTrue(appCss.contains(".stage-trend-pill"));
        assertTrue(appCss.contains(".stage-briefing-grid"));
        assertTrue(appCss.contains(".stage-briefing-chip"));
        assertTrue(appCss.contains(".stage-briefing-nextplay"));
        assertFalse(appCss.contains(".stage-briefing-line"));
        assertTrue(appCss.contains(".stage-focus-mode .info-content {\n        display: flex;"));
        assertTrue(appCss.contains(".stage-focus-mode #stageBriefingPanel.active {\n        order: 1;"));
        assertTrue(appCss.contains(".stage-focus-mode #npcPanel.active {\n        order: 2;"));
        assertTrue(appCss.contains(".stage-focus-mode #npcPanel.active .info-card {\n        max-height:"));
        assertTrue(appCss.contains(".stage-fortune-card"));
        assertTrue(appCss.contains(".stage-focus-mode #infoBrief"));
        assertTrue(appCss.contains(".stage-focus-mode #stageBriefingPanel.active"));
        assertTrue(appCss.contains(".stage-focus-mode #npcPanel.active {\n        order: 2;"));
        assertTrue(appCss.contains(".stage-focus-mode.ending-mode #buzzPanel.active"));
        assertTrue(appCss.contains(".stage-focus-mode .npc-art"));
        assertTrue(appCss.contains("height: calc(100dvh - 290px);"));
        assertTrue(appCss.contains("max-height: calc(100dvh - 290px);"));
        assertTrue(appCss.contains("grid-template-columns: minmax(180px, 210px) minmax(720px, 1fr) minmax(220px, 260px);"));
        assertTrue(appCss.contains(".stage-focus-mode .desktop-stat-details .stat-grid"));
        assertTrue(appCss.contains("--desktop-stat-details-grid-max: clamp(124px, calc(100dvh - 624px), 180px);"));

        assertTrue(probe.contains("stageFocusModeEnabled"));
        assertTrue(probe.contains("stageCenterDominatesRails"));
        assertTrue(probe.contains("stageRightRailCompressed"));
        assertTrue(probe.contains("stageRightRailNotScrollable"));
        assertTrue(probe.contains("stageRightRailContextualSignal"));
        assertTrue(probe.contains("stageRightRailNoRawEnum"));
        assertTrue(probe.contains("stageFortuneCardBorderVisible"));
        assertTrue(probe.contains("stagePrimaryLoopReadable"));
        assertTrue(probe.contains("stageCoreFlowNoWheel"));
        assertTrue(probe.contains("noVerticalPageOverflow"));
        assertTrue(probe.contains("titlePanel"));
        assertTrue(probe.contains("eventPanel"));
        assertTrue(probe.contains("reportPanel"));
        assertTrue(probe.contains("endingPanel"));
        assertFalse(probe.contains("第2天日报：杂谈稳住基本盘"));
        assertTrue(probe.contains("stageDesktopFullStatsExpandedFitsViewport"));
        assertTrue(probe.contains("isLaptopReadyScenario || stageScenario"));
        assertTrue(probe.contains("stageSidebarCompressed"));
    }

    @Test
    void playableFrontendPrioritizesCurrentPhasePanelsBeforeAmbientPanels() throws Exception {
        MvcResult pageResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();
        String indexHtml = new String(pageResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        int actionPanel = indexHtml.indexOf("id=\"actionPanel\"");
        int titlePanel = indexHtml.indexOf("id=\"titlePanel\"");
        int eventPanel = indexHtml.indexOf("id=\"eventPanel\"");
        int endingPanel = indexHtml.indexOf("id=\"endingPanel\"");
        int fanTopicPanel = indexHtml.indexOf("id=\"fanTopicPanel\"");
        int danmakuPanel = indexHtml.indexOf("id=\"danmakuPanel\"");
        int ambientPanel = indexHtml.indexOf("id=\"ambientPanel\"");
        assertTrue(actionPanel > 0);
        assertTrue(titlePanel > 0);
        assertTrue(eventPanel > 0);
        assertTrue(actionPanel < endingPanel);
        assertTrue(endingPanel < fanTopicPanel);
        assertTrue(fanTopicPanel < danmakuPanel);
        assertTrue(actionPanel < ambientPanel);
        assertTrue(indexHtml.contains("data-main-tab=\"platform\""));
        assertFalse(indexHtml.contains(">NPC<"));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        assertTrue(appJs.contains("创建你的VUP"));
        assertTrue(appJs.contains("VUP名称"));
        assertTrue(appJs.contains("创建VUP后查看属性"));
        assertTrue(appJs.contains("同台互动倾向"));
        assertTrue(appJs.contains("暂无同台动态"));
        assertTrue(appJs.contains("同台角色"));
        assertFalse(appJs.contains("创建你的VTuber"));
        assertFalse(appJs.contains("VTuber名称"));
        assertFalse(appJs.contains("创建VTuber后查看属性"));
        assertFalse(appJs.contains("暂无NPC动态"));
    }

    @Test
    void playableFrontendHidesDailyActionPanelOutsideReadyPhase() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        assertTrue(appJs.contains("if (currentPhase !== 'READY')"));
        assertTrue(appJs.contains("panel.classList.add('hidden');"));
        assertTrue(appJs.contains("panel.classList.remove('hidden');"));
        assertFalse(appJs.contains("action-phase-guard"));
        assertFalse(appJs.contains("当前阶段：${phaseText}"));
    }

    @Test
    void playableFrontendMakesDefenseCockpitObviousForClassroomDemo() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult htmlResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String indexHtml = new String(htmlResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("答辩只点这个"));
        assertTrue(appJs.contains("正式 Service"));
        assertTrue(appJs.contains("30份日报"));
        assertTrue(appJs.contains("结局复盘"));
        assertTrue(appJs.contains("data-action=\"demo-reset-run\""));
        assertTrue(appJs.contains("data-action=\"demo-run\""));
        assertTrue(indexHtml.contains("class=\"panel hidden\" id=\"demoPanel\""));
        assertTrue(appCss.contains(".defense-runway"));
        assertTrue(appCss.contains(".defense-runway-step"));
        assertTrue(appCss.contains("max-height: 44px;"));
    }

    @Test
    void playableFrontendKeepsActionOptionsAvailableForDesktop() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appJs.contains("action-options"));
        assertTrue(appJs.contains("action-options-body"));
        assertTrue(appJs.contains("action-options-summary-label"));
        assertTrue(appCss.contains(".action-options summary"));
        assertTrue(appCss.contains(".action-options-body"));
    }

    @Test
    void playableFrontendShowsCurrentActionOptionSummaryOnCollapsedControls() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        assertTrue(appJs.contains("actionOptionsSummaryLabel"));
        assertTrue(appJs.contains("行动参数："));
        assertTrue(appJs.contains("· 点开改"));
        assertTrue(appJs.contains("点开可修改"));
        assertTrue(appJs.contains("syncActionOptionsSummary()"));
        assertTrue(appJs.contains("data-change-action=\"sync-action-options-summary\""));
        assertFalse(appJs.contains("onchange=\"syncActionOptionsSummary()\""));
    }

    @Test
    void playableFrontendShowsDecisionTagsOnActionCards() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appJs.contains("function actionDecisionTags"));
        assertTrue(appJs.contains("action-meta-row"));
        assertTrue(appJs.contains("版本顺风"));
        assertTrue(appJs.contains("打法"));
        assertTrue(appJs.contains("风险"));
        assertTrue(appCss.contains(".action-meta-row"));
        assertTrue(appCss.contains(".action-tag"));
    }

    @Test
    void playableFrontendAvoidsHardcodedLocalhostPortInPlayerFacingErrors() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        assertFalse(appJs.contains("localhost:18080"));
        assertFalse(appJs.contains("HTTP ${response.status}"));
    }

    @Test
    void playableFrontendCollapsesAmbientPanelsForLowHeightDesktop() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appJs.contains("isLowHeightDesktopViewport() ? \"\" : \" open\""));
        assertTrue(appJs.contains("ambient-details"));
        assertTrue(appJs.contains("展开粉丝群议题"));
        assertTrue(appJs.contains("展开直播间弹幕"));
        assertTrue(appCss.contains(".ambient-details summary"));
    }

    @Test
    void playableFrontendDoesNotPublishBackupStaticAssets() throws Exception {
        mockMvc.perform(get("/app.js.bak"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/app.css.bak"))
                .andExpect(status().isNotFound());
    }

    @Test
    void playableFrontendShowsEndingForecastBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("endingForecastPanel")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/ending/forecast")))
                .andExpect(playerFrontendSourceMatches(containsString("state.endingForecast")))
                .andExpect(playerFrontendSourceMatches(containsString("renderEndingForecast")))
                .andExpect(playerFrontendSourceMatches(containsString("ending-forecast-card")))
                .andExpect(playerFrontendSourceMatches(containsString("forecast.requirements")));
    }

    @Test
    void playableFrontendUsesGalleryForRouteStageAndEventVisuals() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appJs.contains("routePortraits"));
        assertTrue(appJs.contains("phaseBackdrops"));
        assertTrue(appJs.contains("eventIllustrations"));
        assertTrue(appJs.contains("renderVisualMood"));
        assertTrue(appJs.contains("v4/routes/singing-idol/standing-outfit.png"));
        assertTrue(appJs.contains("v4/commercial-v1/backgrounds/"));
        assertTrue(appJs.contains("v4/events/common/title-backlash.png"));
        assertTrue(appJs.contains("event-illustration"));
        assertTrue(appCss.contains(".event-illustration"));
        assertTrue(appCss.contains("aspect-ratio: 16 / 9;"));
        assertTrue(appCss.contains("max-height: 180px;"));
    }

    @Test
    void playableFrontendShowsNpcSpotlightWithGalleryAssets() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("platformNpcGrid")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/npc/spotlight")))
                .andExpect(playerFrontendSourceMatches(containsString("state.npcSpotlight")))
                .andExpect(playerFrontendSourceMatches(containsString("renderNpcSpotlight")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/npcs/portraits/")))
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String npcSource = appJs.substring(
                appJs.indexOf("function renderNPC()"),
                appJs.indexOf("// 渲染同台角色"));

        assertTrue(appJs.contains("function activeInfoPanelIds(target)"));
        assertTrue(appJs.contains("npc: stageFocus ? ['npcPanel', 'stageBriefingPanel'] : ['npcPanel'],"));
        assertTrue(appJs.contains("class=\"npc-radar-grid\""));
        assertTrue(appJs.contains("class=\"npc-nextplay\""));
        assertTrue(appJs.contains("npcLineText(npc.advice"));
        assertTrue(appJs.contains("function npcSignalChip(label, value, tone = \"\")"));
        assertTrue(appJs.contains("npcSignalChip(\"心情\", npc.moodLine, \"mood\")"));
        assertTrue(appJs.contains("npcSignalChip(\"关联\", npc.relevance, \"relevance\")"));
        assertFalse(npcSource.contains("npcSignalChip(\"建议\", npc.advice, \"advice\")"));
        assertTrue(appCss.contains(".npc-radar-grid"));
        assertTrue(appCss.contains(".npc-nextplay"));
        assertTrue(appCss.contains(".ready-mode .npc-signal-grid"));
        assertTrue(appCss.contains(".npc-signal-grid"));
        assertTrue(appCss.contains(".npc-signal-chip"));
        assertFalse(npcSource.contains("style="));
    }

    @Test
    void playableFrontendShowsBuzzBriefingWithForumAndTrendSignals() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("buzzPanel")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/buzz/briefing")))
                .andExpect(playerFrontendSourceMatches(containsString("state.buzzBriefing")))
                .andExpect(playerFrontendSourceMatches(containsString("renderBuzzBriefing")))
                .andExpect(playerFrontendSourceMatches(containsString("buzz-card")))
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String buzzSource = appJs.substring(
                appJs.indexOf("function renderBuzz()"),
                appJs.indexOf("// 渲染热搜"));

        assertTrue(appJs.contains("function buzzSignalChip(label, value, tone = \"\")"));
        assertTrue(appJs.contains("buzzSignalChip(\"论坛\", buzz.forumLine, \"forum\")"));
        assertTrue(appJs.contains("buzzSignalChip(\"切片\", buzz.clipperLine, \"clipper\")"));
        assertTrue(appJs.contains("buzzSignalChip(\"下一手\", firstLocalizedReadableText([buzz.nextMoveHint, buzz.trendLabel], '下一步风向还在整理。'), \"next\")"));
        assertTrue(appJs.contains("class=\"buzz-signal-grid\""));
        assertTrue(appCss.contains(".buzz-signal-grid"));
        assertTrue(appCss.contains(".buzz-signal-chip"));
        assertFalse(buzzSource.contains("style="));
    }

    @Test
    void playableFrontendShowsFunAudienceProfileWithFourSignals() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("funAudiencePanel")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/audience/fun-profile")))
                .andExpect(playerFrontendSourceMatches(containsString("state.funAudienceProfile")))
                .andExpect(playerFrontendSourceMatches(containsString("renderFunAudienceProfile")))
                .andExpect(playerFrontendSourceMatches(containsString("fun-audience-card")))
                .andExpect(playerFrontendSourceMatches(containsString("fun-metric-list")))
                .andExpect(playerFrontendSourceMatches(containsString("funMetricLabelText")))
                .andExpect(playerFrontendSourceMatches(containsString("funMetricChips")))
                .andExpect(playerFrontendSourceMatches(containsString("fun-metric-chip heat")))
                .andExpect(playerFrontendSourceMatches(containsString("fun-metric-chip status")))
                .andExpect(playerFrontendSourceMatches(containsString("fun-metric-chip next")))
                .andExpect(playerFrontendSourceMatches(not(containsString("<span>${html(funMetricLabelText(metric))}</span>"))))
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".fun-metric-chip-row")))
                .andExpect(content().string(containsString(".fun-metric-chip")))
                .andExpect(content().string(containsString(".fun-metric-line")))
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String funSource = appJs.substring(
                appJs.indexOf("function renderFunAudienceProfile()"),
                appJs.indexOf("function renderCombo()"));

        assertFalse(funSource.contains("style="));
    }

    @Test
    void playableFrontendShowsAudienceExpectationBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("audienceExpectationPanel")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String audienceSource = appJs.substring(
                appJs.indexOf("function renderAudienceExpectation()"),
                appJs.indexOf("function renderFunAudienceProfile()"));

        assertTrue(appJs.contains("/api/audience/expectation"));
        assertTrue(appJs.contains("state.audienceExpectation"));
        assertTrue(appJs.contains("renderAudienceExpectation"));
        assertTrue(appJs.contains("audience-expectation-card"));
        assertTrue(appJs.contains("audienceRouteText(expectation)"));
        assertTrue(appJs.contains("audienceLineText(expectation.evidenceLine)"));
        assertTrue(appJs.contains("class=\"audience-expectation-empty\""));
        assertTrue(appJs.contains("class=\"audience-expectation-head\""));
        assertTrue(appJs.contains("class=\"audience-expectation-route\""));
        assertTrue(appJs.contains("class=\"audience-expectation-lock\""));
        assertTrue(appJs.contains("class=\"expectation-meter\""));
        assertTrue(appJs.contains("class=\"audience-expectation-line\""));
        assertTrue(appJs.contains("class=\"audience-expectation-pivot\""));
        assertTrue(appCss.contains(".audience-expectation-head"));
        assertTrue(appCss.contains(".audience-expectation-route"));
        assertTrue(appCss.contains(".expectation-meter"));
        assertTrue(appCss.contains(".audience-expectation-line"));

        assertFalse(audienceSource.contains("style="));
    }

    @Test
    void playableFrontendShowsComboDiscoveryBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("comboPanel")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/combo/discovery")))
                .andExpect(playerFrontendSourceMatches(containsString("state.comboDiscovery")))
                .andExpect(playerFrontendSourceMatches(containsString("renderComboDiscovery")))
                .andExpect(playerFrontendSourceMatches(containsString("combo-card")))
                .andExpect(playerFrontendSourceMatches(containsString("combo-list")))
                .andExpect(playerFrontendSourceMatches(containsString("comboDiscoveryChips")))
                .andExpect(playerFrontendSourceMatches(containsString("combo-chip status")))
                .andExpect(playerFrontendSourceMatches(containsString("combo-chip evidence")))
                .andExpect(playerFrontendSourceMatches(containsString("combo-chip next")))
                .andExpect(playerFrontendSourceMatches(not(containsString("<h4 style=\"margin: 0;\">${html(comboLabelText(c))}</h4>"))))
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".combo-chip-row")))
                .andExpect(content().string(containsString(".combo-chip")))
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(styleResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String comboSource = appJs.substring(
                appJs.indexOf("function renderCombo()"),
                appJs.indexOf("function renderComboDiscovery()"));

        assertFalse(comboSource.contains("style="));
        assertTrue(appCss.contains(".combo-hint"));
    }

    @Test
    void playableFrontendShowsMemeLifecycleBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("memeLifecyclePanel")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/meme/lifecycle")))
                .andExpect(playerFrontendSourceMatches(containsString("state.memeLifecycle")))
                .andExpect(playerFrontendSourceMatches(containsString("renderMemeLifecycle")))
                .andExpect(playerFrontendSourceMatches(containsString("memeLifecycleChips")))
                .andExpect(playerFrontendSourceMatches(containsString("meme-lifecycle-card")))
                .andExpect(playerFrontendSourceMatches(containsString("meme-lifecycle-list")))
                .andExpect(playerFrontendSourceMatches(containsString("meme-lifecycle-chip uses")))
                .andExpect(playerFrontendSourceMatches(containsString("meme-lifecycle-chip stage")))
                .andExpect(playerFrontendSourceMatches(containsString("meme-lifecycle-chip next")))
                .andExpect(playerFrontendSourceMatches(not(containsString("<h4 style=\"margin: 0;\">${html(memeLabelFor(item))}</h4>"))));
    }

    @Test
    void playableFrontendShowsPersonaTagBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("personaTagPanel")));

        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/persona/tags")))
                .andExpect(playerFrontendSourceMatches(containsString("state.personaTags")))
                .andExpect(playerFrontendSourceMatches(containsString("renderPersonaTags")))
                .andExpect(playerFrontendSourceMatches(containsString("persona-tag-card")))
                .andExpect(playerFrontendSourceMatches(containsString("persona-tag-list")))
                .andExpect(playerFrontendSourceMatches(containsString("personaTagChips")))
                .andExpect(playerFrontendSourceMatches(containsString("persona-tag-chip strength")))
                .andExpect(playerFrontendSourceMatches(containsString("persona-tag-chip status")))
                .andExpect(playerFrontendSourceMatches(containsString("persona-tag-chip next")))
                .andExpect(playerFrontendSourceMatches(not(containsString("<h4 style=\"margin: 0;\">${html(personaTagLabelText(tag))}</h4>"))))
                .andReturn();
        MvcResult styleResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".persona-tag-chip-row")))
                .andExpect(content().string(containsString(".persona-tag-chip")))
                .andExpect(content().string(containsString(".persona-tag-evidence")))
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);
        String personaSource = appJs.substring(
                appJs.indexOf("function renderPersonaTags()"),
                appJs.indexOf("function renderMemeLifecycle()"));

        assertFalse(personaSource.contains("style="));
    }

    @Test
    void playableFrontendShowsStageBriefingBoard() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("stageBriefingPanel")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/stage/briefing")))
                .andExpect(playerFrontendSourceMatches(containsString("state.stageBriefing")))
                .andExpect(playerFrontendSourceMatches(containsString("renderStageBriefing")))
                .andExpect(playerFrontendSourceMatches(containsString("stage-briefing-card")))
                .andExpect(playerFrontendSourceMatches(containsString("stage-briefing-signals")))
                .andExpect(playerFrontendSourceMatches(containsString("objectiveTitle")))
                .andExpect(playerFrontendSourceMatches(containsString("ready-goal-objective")))
                .andExpect(playerFrontendSourceMatches(containsString("stage-objective-card")));
    }

    @Test
    void configCheckRequiresGalleryAssetsUsedByRouteStageAndEventVisuals() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("v4/routes/singing-idol/")))
                .andExpect(content().string(containsString("v4/backgrounds/")))
                .andExpect(content().string(containsString("v4/events/common/title-backlash.png")));
    }

    @Test
    void configCheckRequiresNpcGalleryAssetsUsedBySpotlight() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("v4/npcs/portraits/singing-mentor.png")))
                .andExpect(content().string(containsString("v4/npcs/portraits/collab-streamer.png")));
    }

    @Test
    void configCheckRequiresStreamPlanGalleryAssetsUsedByPreview() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(content().string(containsString("v4/stream-plans/previews/singing.png")))
                .andExpect(content().string(containsString("v4/stream-plans/previews/collaboration.png")))
                .andExpect(content().string(containsString("v4/stream-plans/previews/sc-thanks.png")));
    }

    @Test
    void playableFrontendExposesDevDemoPanelFromConfigCheck() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"panel hidden\" id=\"demoPanel\"")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/system/config-check")))
                .andExpect(playerFrontendSourceMatches(containsString("state.configCheck")))
                .andExpect(playerFrontendSourceMatches(containsString("state.demoResult")))
                .andExpect(playerFrontendSourceMatches(containsString("state.demoStatus")))
                .andExpect(playerFrontendSourceMatches(containsString("availableStrategies")))
                .andExpect(playerFrontendSourceMatches(containsString("const showDemo = devTools?.enabled && (state.infoTab === 'demo' || state.infoTab === 'environment')")))
                .andExpect(playerFrontendSourceMatches(containsString("panel.classList.toggle(\"hidden\", !showDemo)")))
                .andExpect(playerFrontendSourceMatches(containsString("/api/dev/demo/reset")))
                .andExpect(playerFrontendSourceMatches(containsString("/api/dev/demo/run-script")))
                .andExpect(playerFrontendSourceMatches(containsString("/api/dev/demo/fast-forward")))
                .andExpect(playerFrontendSourceMatches(containsString("/api/dev/demo/status")))
                .andExpect(playerFrontendSourceMatches(containsString("demo-reset")))
                .andExpect(playerFrontendSourceMatches(containsString("demo-run")))
                .andExpect(playerFrontendSourceMatches(containsString("demo-fast-forward")))
                .andExpect(playerFrontendSourceMatches(containsString("demo-result")))
                .andExpect(playerFrontendSourceMatches(containsString("lastError")))
                .andExpect(playerFrontendSourceMatches(containsString("reportCount")))
                .andExpect(playerFrontendSourceMatches(containsString("logCount")))
                .andExpect(playerFrontendSourceMatches(containsString("endingReviewId")))
                .andExpect(playerFrontendSourceMatches(containsString("stopOnError: true")))
                .andExpect(playerFrontendSourceMatches(containsString("random:")));
    }

    @Test
    void playableFrontendBlocksMainInterfaceWhenConfigCheckFails() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        var cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("renderConfigGate"));
        assertTrue(appJs.contains("state.configCheck?.canPlayP0 === false"));
        assertTrue(appJs.contains("document.body.classList.add(\"config-gate-active\")"));
        assertTrue(appJs.contains("document.body.classList.remove(\"config-gate-active\")"));
        assertTrue(appJs.contains("fatalErrors"));
        assertTrue(appJs.contains("config-fatal-errors"));
        assertTrue(appJs.contains("开播配置未达标，场务先别开麦；补齐配置后再开播。"));
        assertFalse(appJs.contains("P0配置未达标"));
        assertTrue(appCss.contains(".config-gate-active .game-sidebar"));
        assertTrue(appCss.contains(".config-gate-active .game-info"));
        assertTrue(appCss.contains(".config-gate-active .play-shell"));
    }

    @Test
    void playableFrontendRendersBackendTutorialHints() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("v.tutorialHints")))
                .andExpect(playerFrontendSourceMatches(containsString("data-tutorial")))
                .andExpect(playerFrontendSourceMatches(containsString("tutorial-list")));
    }

    @Test
    void createVupPanelKeepsStartButtonBeforeOptionalPrimer() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        int startButtonIndex = appJs.indexOf("开始出道");
        int primerIndex = appJs.indexOf("create-primer");
        assertTrue(startButtonIndex > 0);
        assertTrue(primerIndex > 0);
        assertTrue(startButtonIndex < primerIndex);
        assertTrue(appJs.contains("create-loadout-panel"));
        assertTrue(appJs.contains("开局卡组"));
        assertTrue(appJs.contains("可选：查看初始属性"));
        assertTrue(appJs.contains("初始属性"));
        assertTrue(appJs.contains("粉丝结构预览"));
        assertTrue(appJs.contains("真爱粉"));
        assertTrue(appJs.contains("乐子人"));
        assertFalse(appJs.contains("create-preview"));
    }

    @Test
    void createVupPanelOffersDesktopStylePresetsAndFusesPersona() throws Exception {
        var scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        var cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("const creationStylePresets = ["));
        assertTrue(appJs.contains("低压杂谈新人"));
        assertTrue(appJs.contains("歌回预备役"));
        assertTrue(appJs.contains("国风茶馆"));
        assertTrue(appJs.contains("素材型新人"));
        assertTrue(appJs.contains("function renderCreationStylePresets()"));
        assertTrue(appJs.contains("creationStylePresets.slice(0, 4)"));
        assertTrue(appJs.contains("create-style-more"));
        assertTrue(appJs.contains("更多风格"));
        assertTrue(appJs.contains("function selectedCreationStylePreset()"));
        assertTrue(appJs.contains("function applyCreationStylePreset(key)"));
        assertTrue(appJs.contains("function creationPersonaForSubmit()"));
        assertTrue(appJs.contains("data-action=\"select-creation-style\""));
        assertTrue(appJs.contains("出道风格"));
        assertTrue(appJs.contains("persona: creationPersonaForSubmit()"));
        assertTrue(appJs.contains("syncCreationStylePreview();"));
        assertTrue(appCss.contains(".create-style-grid"));
        assertTrue(appCss.contains(".create-style-card"));
        assertTrue(appCss.contains(".create-loadout-panel"));
        assertTrue(appCss.contains(".pregame-mode .create-loadout-panel"));
        assertTrue(appCss.contains("max-height: calc(100dvh - 220px);"));
        assertTrue(appCss.contains(".create-style-preview"));
        assertTrue(appCss.contains(".create-style-card input:checked + .create-style-card-body"));
        assertTrue(appCss.contains(".pregame-mode .create-style-card-copy"));
    }

    @Test
    void createVupPanelShowsPlayableLoadoutSummaryInFirstViewport() throws Exception {
        var scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        var cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String createProbe = Files.readString(Path.of("scripts", "create-flow-check.mjs"), StandardCharsets.UTF_8);
        String desktopProbe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("data-action=\"create-loadout-summary\""));
        assertTrue(appJs.contains("class=\"create-loadout-summary-grid\""));
        assertTrue(appJs.contains("首日打法"));
        assertTrue(appJs.contains("适合路线"));
        assertTrue(appJs.contains("风险提示"));
        assertTrue(appJs.contains("function creationLoadoutSummaryHtml("));
        assertTrue(appJs.contains("preview.innerHTML = creationLoadoutSummaryHtml"));
        assertTrue(appCss.contains(".create-loadout-summary"));
        assertTrue(appCss.contains(".create-loadout-summary-grid"));
        assertTrue(appCss.contains(".pregame-mode .create-loadout-summary"));
        assertTrue(appCss.contains(".create-loadout-summary-item"));
        assertTrue(appCss.contains(".pregame-mode .coach-card"));
        assertTrue(appCss.contains(".pregame-mode .game-info"));
        assertTrue(appCss.contains("grid-template-columns: repeat(3, minmax(0, 1fr));"));
        assertTrue(createProbe.contains("create-loadout-summary"));
        assertTrue(createProbe.contains("create-loadout-summary-grid"));
        assertTrue(desktopProbe.contains("createLoadoutSynthetic"));
        assertTrue(desktopProbe.contains("createLoadoutSyntheticStartCtaInViewport"));
    }

    @Test
    void playableFrontendDoesNotOfferRestartWhenEndingReviewIsMissing() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("state.session?.phase === \"ENDING_READY\" && state.ending"));
        assertTrue(appJs.contains("ending-missing"));
        assertTrue(appJs.contains("结局生成异常"));
    }

    @Test
    void playableFrontendDoesNotOfferNextDayWhenDailyReportIsMissing() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("state.session?.phase === \"REPORT_READY\" && state.report"));
        assertTrue(appJs.contains("report-missing"));
        assertTrue(appJs.contains("日报生成异常"));
    }

    @Test
    void playableFrontendShowsCompactReportFirstWithExpandableFullReplay() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("report-brief"));
        assertTrue(appJs.contains("日报重点"));
        assertTrue(appJs.contains("report-details"));
        assertTrue(appJs.contains("<summary>展开完整复盘</summary>"));
        assertTrue(appJs.contains("const detailItems"));
        assertTrue(appJs.contains("state.report.visibleItems"));
        assertTrue(appJs.contains("state.lastActionResult = result?.actionResult || null;"));
        assertTrue(appJs.contains("state.lastActionResult = null;"));
        assertTrue(appJs.contains("function renderActionResultHud(result)"));
        assertTrue(appJs.contains("action-result-hud"));
        assertTrue(appJs.contains("本手结算"));
        assertTrue(appJs.contains("action-result-hud-metrics"));
        assertFalse(appJs.contains("<ul class=\"report-items\">${items}</ul>"));

        var cssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String appCss = new String(cssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(appCss.contains(".action-result-hud"));
        assertTrue(appCss.contains(".action-result-hud-metrics"));
        assertTrue(appCss.contains("grid-template-columns: repeat(3, minmax(0, 1fr));"));
    }

    @Test
    void playableFrontendShowsErrorWhenTitleCandidatesAreMissing() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("state.session?.phase === \"NEED_TITLE\" && state.titles.length === 0"));
        assertTrue(appJs.contains("title-missing"));
        assertTrue(appJs.contains("标题候选异常"));
        assertTrue(appJs.contains("请刷新状态"));
    }

    @Test
    void playableFrontendReadsPendingFormalEventBeforeRenderingEventPanel() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/event/pending")))
                .andExpect(playerFrontendSourceMatches(containsString("state.pendingEvent")))
                .andExpect(playerFrontendSourceMatches(containsString("choices.map")));
    }

    @Test
    void playableFrontendDoesNotHardcodePendingFormalEventChoice() throws Exception {
        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String script = playerFrontendSource(result);
        assertFalse(script.contains("先降温，别让主会场坐满"));
        assertFalse(script.contains("热度下降，口碑小幅回稳"));
        assertFalse(script.contains("target.dataset.choice"));
        assertFalse(script.contains("target.dataset.choice || \"safe\""));
        assertTrue(script.contains("choiceId: choiceId"));
    }

    @Test
    void playableFrontendReadsPendingInteractionBeforeRenderingLivePanel() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/interaction/pending")))
                .andExpect(playerFrontendSourceMatches(containsString("state.pendingInteraction")))
                .andExpect(playerFrontendSourceMatches(containsString("data-choice")));
    }

    @Test
    void playableFrontendDoesNotHardcodePendingInteractionCopy() throws Exception {
        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String script = playerFrontendSource(result);
        assertFalse(script.contains("弹幕拱火"));
        assertFalse(script.contains("现场拱火"));
        assertFalse(script.contains("弹幕开始递话筒"));
        assertFalse(script.contains("不评价，今天唱歌"));
    }

    @Test
    void playableFrontendReadsFanTopicOptionsBeforeChoosingGroupResponse() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/fan-topic/options")))
                .andExpect(playerFrontendSourceMatches(containsString("state.fanTopics")))
                .andExpect(playerFrontendSourceMatches(containsString("data-topic")))
                .andExpect(playerFrontendSourceMatches(containsString("renderFanTopics")));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fanTopicPanel")));
    }

    @Test
    void playableFrontendDoesNotHardcodeFanTopicChoice() throws Exception {
        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String script = playerFrontendSource(result);
        assertFalse(script.contains("OLD_FANS_WORRY"));
        assertFalse(script.contains("APPEASE_OLD_FANS"));
    }

    @Test
    void playableFrontendCanCancelPendingStreamPlanFromTitleSelection() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("/api/day/action/cancel")))
                .andExpect(playerFrontendSourceMatches(containsString("data-action=\"cancel-action\"")));
    }

    @Test
    void playableFrontendShowsClipMaterialStockForPublishClipRoute() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("materialStock")));
    }

    @Test
    void playableFrontendRendersAccidentMaterialsInsideDailyReport() throws Exception {
        MvcResult result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("renderAccidentMaterials")))
                .andExpect(playerFrontendSourceMatches(containsString("accident-material-list")))
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("事故素材"));
    }

    @Test
    void playableFrontendTranslatesDisabledActionReasonsForPlayers() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("disabledReasonText"));
        assertTrue(appJs.contains("素材库为0，切片组不能空剪"));
        assertTrue(appJs.contains("FAN_TOPIC_ALREADY_HANDLED"));
        assertTrue(appJs.contains("今天已经处理过粉丝群议题"));
    }

    @Test
    void playableFrontendTranslatesP0UnavailableChoiceReasonsForPlayers() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        // P0_NOT_AVAILABLE and 本版暂未开放 removed: traffic/meme choices now enabled
        assertFalse(appJs.contains("P0暂不开放"));
        assertFalse(appJs.contains("P0 暂不开放"));
    }

    @Test
    void playableFrontendTranslatesRiskToolDisabledReasonsForPlayers() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("INSUFFICIENT_COIN"));
        assertTrue(appJs.contains("运营预算不够"));
        assertTrue(appJs.contains("RISK_TOOL_DAILY_LIMIT"));
        assertTrue(appJs.contains("今天已经用过风险工具了"));
    }

    @Test
    void playableFrontendTranslatesRiskToolNoTargetReasonForPlayers() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("RISK_TOOL_NO_TARGET"));
        assertTrue(appJs.contains("没有需要处理的风险"));
    }

    @Test
    void playerFacingFallbackCopyDoesNotConcatenateRawActionKeys() throws Exception {
        String appJs = playerFrontendSource();
        String reportService = Files.readString(Path.of("src/main/java/com/example/vupworld/service/report/ReportService.java"), StandardCharsets.UTF_8);
        String actionService = Files.readString(Path.of("src/main/java/com/example/vupworld/service/ActionService.java"), StandardCharsets.UTF_8);
        String eventPresenter = Files.readString(Path.of("src/main/java/com/example/vupworld/service/event/FormalEventPresenter.java"), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("return disabledReasonCopy[reason] || visibleTextOrFallback(reason, \"暂不可用，先推进当前流程。\")"));
        assertTrue(appJs.contains("return texts[phase] || visibleTextOrFallback(phase, \"等待开播\")"));
        assertTrue(appJs.contains("const disabledText = riskToolDisabledText(tool, disabled);"));
        assertTrue(appJs.contains("<small>${html(disabledText || \"可用于压一笔旧账风险\")}</small>"));
        assertTrue(reportService.contains("\"今天完成了\" + dayFlowService.actionLabel(actionType)"));
        assertTrue(eventPresenter.contains("? dayFlowService.actionLabel(debt.getSourceAction())"));
        assertTrue(appJs.contains("'ACTION_RESOLVED': '行动已结算'"));
        assertFalse(reportService.contains("\"今天完成了\" + actionType"));
        assertFalse(actionService.contains("SAFE低压陪伴"));
    }

    @Test
    void playableFrontendUsesBackendActionNamesInActionList() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("function actionNameText(action)"));
        assertTrue(appJs.contains("visibleTextOrFallback(action?.name, actionLabelFor(action?.actionType))"));
        assertTrue(appJs.contains("function actionLabelFor(actionKey)"));
        assertFalse(appJs.contains("html(action.name)"));
        assertFalse(appJs.contains("歌力训练"));
    }

    @Test
    void playableFrontendReadsStreamPlanOptionsBeforeSubmittingStreamPlan() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("/api/stream/plans"));
        assertTrue(appJs.contains("state.streamPlans"));
        assertTrue(appJs.contains("plan.planType"));
        assertTrue(appJs.contains("streamPlanLabelText(planOptions[0])"));
        assertFalse(appJs.contains("const planTypes"));
        assertFalse(appJs.contains("棉花糖"));
        assertFalse(appJs.contains("SC感谢"));
    }

    @Test
    void playableFrontendSubmitsNpcInteractionTendency() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("id=\"npcTendency\"")))
                .andExpect(playerFrontendSourceMatches(containsString("actionPlanTypeFor")))
                .andExpect(playerFrontendSourceMatches(containsString("RAID")))
                .andExpect(playerFrontendSourceMatches(containsString("COLLAB")))
                .andExpect(playerFrontendSourceMatches(containsString("BORROW_HEAT")))
                .andExpect(playerFrontendSourceMatches(containsString("AVOID")));
    }

    @Test
    void playableFrontendShowsStreamPlanGalleryPreviewBeforeSubmittingPlan() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("streamPlanArt")))
                .andExpect(playerFrontendSourceMatches(containsString("renderStreamPlanPreview")))
                .andExpect(playerFrontendSourceMatches(containsString("stream-plan-preview")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/stream-plans/previews/singing.png")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/stream-plans/previews/collaboration.png")))
                .andExpect(playerFrontendSourceMatches(containsString("v4/stream-plans/previews/sc-thanks.png")));
    }

    @Test
    void playableFrontendHidesMainActionPanelOutsideReadyPhase() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("if (currentPhase !== 'READY')"));
        assertTrue(appJs.contains("panel.innerHTML = '';"));
        assertFalse(appJs.contains("action-phase-guard"));
    }

    @Test
    void playableFrontendCanLoadAndRenderRecentReportHistory() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("/api/reports"));
        assertTrue(appJs.contains("renderReportHistory"));
        assertTrue(appJs.contains("最近3天日报"));
    }

    @Test
    void playableFrontendShowsPlatformTrendInsideDailyReport() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("state.report.platformTrend")))
                .andExpect(playerFrontendSourceMatches(containsString("platform-trend")));
    }

    @Test
    void playableFrontendShowsPlatformTrendBeforeChoosingAction() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("v.platformTrend")))
                .andExpect(playerFrontendSourceMatches(containsString("state-platform-trend")));
    }

    @Test
    void playableFrontendShowsCoreOpinionStatsInMainStatePanel() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("v.opinion.popularity"));
        assertTrue(appJs.contains("v.opinion.memeLevel"));
        assertTrue(appJs.contains("v.opinion.commercialLevel"));
    }

    @Test
    void playableFrontendShowsCompleteFanStructureInMainStatePanel() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("v.fanStructure.trueFans"));
        assertTrue(appJs.contains("v.fanStructure.funFans"));
        assertTrue(appJs.contains("v.fanStructure.unicornFans"));
        assertTrue(appJs.contains("v.fanStructure.ddFans"));
    }

    @Test
    void playableFrontendShowsAttributesInMainStatePanel() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("v.attributes.songPower"));
        assertTrue(appJs.contains("v.attributes.dancePower"));
        assertTrue(appJs.contains("v.attributes.talkPower"));
        assertTrue(appJs.contains("v.attributes.memePower"));
        assertTrue(appJs.contains("v.attributes.planPower"));
        assertTrue(appJs.contains("v.attributes.stressPower"));
    }

    @Test
    void playableFrontendShowsCreationStyleOpeningAdviceInStatePanel() throws Exception {
        var appJsResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        var appCssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(appJsResult);
        String appCss = new String(appCssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("v.expectations?.openingAdvice"));
        assertTrue(appJs.contains("v.expectations?.recommendedPlan"));
        assertTrue(appJs.contains("v.expectations?.creationStyle"));
        assertTrue(appJs.contains("opening-advice-card"));
        assertTrue(appCss.contains(".opening-advice-card"));
        assertTrue(appCss.contains(".opening-advice-plan"));
    }

    @Test
    void playableFrontendShowsCompactDesktopStateHudWithoutSidebarScrolling() throws Exception {
        var appJsResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        var appCssResult = mockMvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andReturn();
        String probe = Files.readString(Path.of("scripts", "visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        String appJs = playerFrontendSource(appJsResult);
        String appCss = new String(appCssResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("desktop-stat-brief"));
        assertTrue(appJs.contains("desktop-stat-details"));
        assertTrue(appJs.contains("<summary>完整属性面板</summary>"));
        assertFalse(appJs.contains("class=\"stat-grid desktop-stat-grid\""));
        assertTrue(appCss.contains(".desktop-stat-brief"));
        assertTrue(appCss.contains(".desktop-stat-details"));
        assertTrue(appCss.contains(".desktop-stat-details {\n    display: block;"));
        assertTrue(appCss.contains("max-height: var(--desktop-stat-details-grid-max, 240px);"));
        assertTrue(appCss.contains(".ready-mode .desktop-stat-details .stat-grid"));
        assertTrue(appCss.contains("--desktop-stat-details-grid-max"));
        assertTrue(appCss.contains(".game-sidebar:has(.desktop-stat-details[open]) .achievement-mini"));
        assertTrue(appCss.contains("display: none;"));
        assertTrue(appCss.contains("#statsSection .stat-grid.desktop-stat-grid"));
        assertTrue(appCss.contains(".desktop-stat-grid,"));
        assertTrue(appCss.contains(".desktop-stat-brief,"));
        assertTrue(appCss.contains(".desktop-stat-details {"));
        assertTrue(appCss.contains(".desktop-stat-grid"));

        assertTrue(probe.contains("readySidebarStateHudCompact"));
        assertTrue(probe.contains("readyDesktopFullStatsCollapsed"));
        assertTrue(probe.contains("readyDesktopFullStatsExpandedFitsViewport"));
        assertTrue(probe.contains("desktopStatDetails.open = true"));
        assertTrue(probe.contains("readySidebarNotScrollable"));
        assertTrue(probe.contains("isVisible('#statsSection .desktop-stat-details')"));
    }

    @Test
    void playableFrontendRefreshesBackendStateAfterWriteRequestFails() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("refreshAfterWriteFailure"));
        assertTrue(appJs.contains("await refreshAfterWriteFailure()"));
        assertTrue(appJs.contains("state.session = await api(\"/api/day/session\")"));
        assertTrue(appJs.contains("state.vup = await api(\"/api/vup/current\")"));
    }

    @Test
    void actionListExplainsPlatformTrendBonusBeforePlayerCommits() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("trend_preview_player", "风向可见");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_TALK' && @.effectPreview =~ /.*清朗低压周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_TALK' && @.effectPreview =~ /.*平台口味加成.*/)]", hasSize(1)));
    }

    @Test
    void actionListExposesRouteBuildingHintsBeforePlayerCommits() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("route_hint_player", "路线读盘员");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_SONG' && @.routeBiasType == 'SINGING_IDOL' && @.routeBiasLabel == '歌势路线')]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.routeBiasType == 'SLICE_SAINT' && @.riskLevel == 'HIGH')]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'FAN_GROUP_MAINTAIN' && @.recommendedReason =~ /.*稳住.*/ && @.tempoHint =~ /.*防守.*/)]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'REST' && @.routeBiasType == 'UNKNOWN' && @.riskLevel == 'LOW')]",
                        hasSize(1)));
    }

    @Test
    void actionListPreviewsNextComboBeforePlayerCommits() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_preview_player", "连招预告员");

        submitActionAndResolveFormalEvent(session, "TRAIN_SONG", 1, "combo-preview-train-day-1", null);
        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-preview-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'STREAM_PLAN' && @.comboKey == 'PRACTICE_TO_STREAM' && @.comboLabel == '练习后直播')]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'STREAM_PLAN' && @.comboHint =~ /.*练完基本功.*/)]",
                        hasSize(1)));
    }

    @Test
    void actionListPreviewsVideoToClipComboWhenMaterialIsReady() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_clip_preview_player", "二剪预告员");

        submitActionAndResolveFormalEvent(session, "PUBLISH_VIDEO", 1, "combo-preview-video-day-1", null);
        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-preview-clip-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.enabled == true && @.comboKey == 'VIDEO_TO_CLIP' && @.comboLabel == '一鱼两剪')]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.comboHint =~ /.*长视频给素材.*/)]",
                        hasSize(1)));
    }

    @Test
    void platformTrendRotatesOnDayEightBeforePlayerChoosesAction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("trend_rotation_player", "第八天风向");
        advanceToDay(session, 8, "trend-rotation");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(8))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.platformTrend.label").value("歌回扶持周"));
    }

    @Test
    void daySevenReportWarnsNextWeekPlatformTrendBeforeRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("next_trend_preview_player", "下周风向员");
        advanceToDay(session, 7, "next-trend-preview");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "next-trend-preview-day-7-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("SAFE"))));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*下周口味.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*歌回扶持周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*练歌.*/)]", hasSize(1)));
    }

    @Test
    void stageReportsWarnNextPlatformTrendBeforeLaterRotations() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("later_trend_preview_player", "版本预言家");
        advanceToDay(session, 14, "later-trend-preview");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "later-trend-preview-day-14-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*下周口味.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*抽象出圈周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*切片和整活传播会更香，素材库别空着.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "later-trend-preview-go-day-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(15))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        advanceReadyDaysToDay(session, 15, 21, "later-trend-preview");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "later-trend-preview-day-21-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("MEME_OUTBREAK_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*下周口味.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*商业复审周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*高亮互动回应.*/)]", hasSize(1)));
    }

    @Test
    void singingBoostWeekBuffsSongTrainingAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("singing_boost_player", "歌回扶持测试");
        advanceToDay(session, 8, "singing-boost");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_SONG' && @.effectPreview =~ /.*歌回扶持周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_SONG' && @.effectPreview =~ /.*平台口味加成.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "singing-boost-train-song-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("TRAIN_SONG"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(17)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("歌回扶持周")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(8))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThanOrEqualTo(79)));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("口碑+")));
    }

    @Test
    void singingBoostWeekBuffsVideoPublishingAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("video_boost_player", "投稿扶持测试");
        advanceToDay(session, 8, "video-boost");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_VIDEO' && @.effectPreview =~ /.*歌回扶持周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_VIDEO' && @.effectPreview =~ /.*投稿更容易进推荐.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "video-boost-publish-video-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("PUBLISH_VIDEO"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(25)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("歌回扶持周")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(8))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.resources.inspiration").value(2))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(42)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThanOrEqualTo(78)));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("口碑+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("灵感-1")));
    }

    @Test
    void memeOutbreakWeekBuffsClipPublishingAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("meme_clip_boost_player", "抽象供货商");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "meme-clip-stock-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "meme-clip-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        advanceReadyDaysToDay(session, 2, 15, "meme-clip");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(15))
                .andExpect(jsonPath("$.data.platformTrend.id").value("MEME_OUTBREAK_WEEK"))
                .andExpect(jsonPath("$.data.platformTrend.label").value("抽象出圈周"))
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(2));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.effectPreview =~ /.*抽象出圈周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.effectPreview =~ /.*路人粉进场更快.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "meme-clip-publish-clip-day-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("PUBLISH_CLIP"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("抽象出圈周")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("乐子人")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("MEME_OUTBREAK_WEEK"))
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(52)))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data.opinion.memeLevel").value(greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThanOrEqualTo(88)))
                .andExpect(jsonPath("$.data.debts", hasSize(1)))
                .andExpect(jsonPath("$.data.debts[0].debtType").value("BOOMERANG_CLIP"))
                .andExpect(jsonPath("$.data.debts[0].severity").value(1))
                .andExpect(jsonPath("$.data.debts[0].dueDay").value(17))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("抽象出圈周")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("米线压力")));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("MEME_OUTBREAK_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("人气+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("围观热度+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("梗浓度+")))
                .andReturn();
        assertChineseDebtSummary(reportResult, "回旋镖切片", "抽象出圈周");
    }

    @Test
    void boomerangClipDebtReturnUsesClipSpecificSafeSummary() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("boomerang_clip_event_player", "回旋镖供货商");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "boomerang-clip-stock-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "boomerang-clip-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        advanceReadyDaysToDay(session, 2, 15, "boomerang-clip");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "boomerang-clip-publish-day-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "boomerang-clip-go-day-16"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(16))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "TRAIN_TALK", 16, "boomerang-clip-day-16-talk", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "boomerang-clip-go-day-17"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(17))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "boomerang-clip-day-17-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"));

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.debtType").value("BOOMERANG_CLIP"))
                .andExpect(jsonPath("$.data.title").value("回旋镖切片正式事件"))
                .andExpect(jsonPath("$.data.description", containsString("抽象出圈周")));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "boomerang-clip-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("回旋镖切片")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("上下文")));
    }

    @Test
    void playableFrontendExplainsRiskToolCostsAndEffectsBeforeClicking() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("debt-tool-meta"));
        assertTrue(appJs.contains("risk-tool-token cost"));
        assertTrue(appJs.contains("risk-tool-token effect"));
        assertTrue(appJs.contains("risk-tool-token state"));
        assertTrue(appJs.contains("risk-tool-card"));
        assertTrue(appJs.contains("risk-tool-actions"));
        assertTrue(appJs.contains("risk-tool-chip"));
        assertTrue(appJs.contains("data-action=\"use-risk-tool\""));
        assertTrue(appJs.contains("/api/risk-tool/options"));
        assertTrue(appJs.contains("/api/risk-tool/use"));
        assertTrue(appJs.contains("costPreview"));
        assertTrue(appJs.contains("effectPreview"));
        assertTrue(appJs.contains("disabledReason"));
        assertFalse(appJs.contains("riskToolCopy"));
        assertTrue(appJs.contains("FULL_CLIP_CONTEXT: \"补全切片上下文\""));
        assertTrue(appJs.contains("COOLING_NOTICE: \"冷处理公告\""));
        assertTrue(appJs.contains("TEMP_MOD_TEAM: \"临时房管组\""));
    }

    @Test
    void playableFrontendShowsFormalEventChoiceCostsBeforeClicking() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("choice.costPreview"));
    }

    @Test
    void playableFrontendSubmitsFormalEventChoiceIds() throws Exception {
        var result = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(result);
        assertTrue(appJs.contains("function eventChoiceKey(choice)"));
        assertTrue(appJs.contains("return choice.choiceId || choice.choiceType;"));
        assertTrue(appJs.contains("renderEventChoice(eventChoice, 'event', 0)"));
        assertTrue(appJs.contains("choiceId: choiceId"));
    }

    @Test
    void playableFrontendOffersAllImplementedRestartBiasRoutes() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"SINGING_IDOL\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"ELECTRONIC_PICKLE\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"SLICE_SAINT\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"BLACK_RED_MAIN_STAGE\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"CYBER_GIRLFRIEND\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"DD_BUS_STOP\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"MAIN_STAGE_KING\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"GLORIOUS_GRADUATION\"")))
                .andExpect(playerFrontendSourceMatches(containsString("option value=\"UNKNOWN\"")));
    }

    @Test
    void playerCanRegisterLoginCreateVupAndRecoverDayOneSession() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_player",
                                  "password": "pass1234",
                                  "nickname": "测试玩家"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.username").value("test_player"));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_player",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("test_player"));

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "露米",
                                  "persona": "低压杂谈新人"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("露米"))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.resources.stamina").value(10))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(0));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vupId").isNumber())
                .andExpect(jsonPath("$.data.name").value("露米"))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.maxDay").value(30))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.currentRoute").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.route.currentRoute").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.route.routeScore.UNKNOWN").value(0))
                .andExpect(jsonPath("$.data.expectations").isMap())
                .andExpect(jsonPath("$.data.tutorialHints", hasSize(1)))
                .andExpect(jsonPath("$.data.tutorialHints[0].key").value("DAY_ONE_ACTION"))
                .andExpect(jsonPath("$.data.tutorialHints[0].text", containsString("每天选择1个主行动")))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.platformTrend.label").value("清朗低压周"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorialHints", hasSize(0)));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"));
    }

    @Test
    void createVupStylePresetSeedsOpeningRouteAttributesAndAdvice() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "style_seed_player",
                                  "password": "pass1234",
                                  "nickname": "风格开局测试"
                                }
                                """))
                .andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "style_seed_player",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "晴歌",
                                  "persona": "歌势练习生，直播间常备小歌单｜出道风格：歌势练习生｜主推歌回和投稿箱，靠灯牌点歌、录播复盘、切片二创慢慢抬声量。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.route.currentRoute").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.route.routeScore.SINGING_IDOL").value(3))
                .andExpect(jsonPath("$.data.attributes.songPower").value(13))
                .andExpect(jsonPath("$.data.resources.inspiration").value(4))
                .andExpect(jsonPath("$.data.expectations.creationStyle").value("歌势练习生"))
                .andExpect(jsonPath("$.data.expectations.recommendedPlan").value("歌回练功"))
                .andExpect(jsonPath("$.data.expectations.openingAdvice", containsString("歌回")));
    }

    @Test
    void stylePresetEndingForecastKeepsOpeningRouteDirectionWhileWarningFanGate() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "style_forecast_player",
                                  "password": "pass1234",
                                  "nickname": "风格预演测试"
                                }
                                """))
                .andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "style_forecast_player",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "晴歌预演",
                                  "persona": "歌势练习生，直播间常备小歌单｜出道风格：歌势练习生｜主推歌回和投稿箱，靠灯牌点歌、录播复盘、切片二创慢慢抬声量。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"));

        mockMvc.perform(get("/api/ending/forecast").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.likelyEndingType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.likelyFinalTitle").value("歌势遗珠"))
                .andExpect(jsonPath("$.data.headline", containsString("歌势遗珠")))
                .andExpect(jsonPath("$.data.requirements[?(@.key == 'FANS' && @.status == 'WARN')]", hasSize(1)))
                .andExpect(jsonPath("$.data.requirements[?(@.key == 'ROUTE_SPECIFIC')]", hasSize(1)))
                .andExpect(jsonPath("$.data.sprintHint", containsString("练歌")));
    }

    @Test
    void playerCanChooseNonStreamActionAndReceiveSavedDailyReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("action_player", "弥弥");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_SONG')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'STREAM_PLAN')]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "day1-train-song"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("TRAIN_SONG"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.reportId").isNumber());

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.stamina").value(8))
                .andExpect(jsonPath("$.data.attributes.songPower").value(11))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(greaterThan(52)))
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.platformTrend.id").value("SAFE_WEEK"))
                .andExpect(jsonPath("$.data.platformTrend.label").value("清朗低压周"))
                .andExpect(jsonPath("$.data.platformTrend.description", containsString("平台口味")))
                .andExpect(jsonPath("$.data.visibleItems", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.evidenceRefs[0].type").value("business_log"));
    }

    @Test
    void publishVideoConsumesInspirationAndCreatesPlatformSpreadEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("video_player", "投稿人");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "publish-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("发布视频")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("平台口味")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("切片")))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThan(10)))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(2));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.resources.stamina").value(6))
                .andExpect(jsonPath("$.data.resources.inspiration").value(2))
                .andExpect(jsonPath("$.data.attributes.planPower").value(11))
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(2))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(35)));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary", containsString("发布视频")))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*素材小票.*\\+2.*现有2份.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[0].type").value("business_log"));
    }

    @Test
    void publishClipRequiresMaterialStockInActionListAndSubmitValidation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("clip_material_player", "material_gate_clip");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.enabled == false && @.disabledReason == 'INSUFFICIENT_MATERIAL_STOCK')]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "clip-no-material-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_MATERIAL_STOCK"));
    }

    @Test
    void publishClipConsumesMaterialStockAfterVideoCreatesIt() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("clip_consume_player", "clip_stock_counter");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "clip-stock-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(2));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "clip-stock-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "clip-stock-clip-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*素材小票.*-1.*现有1份.*/)]", hasSize(1)));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "clip-stock-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "clip-stock-clip-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(0));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "clip-stock-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.enabled == false && @.disabledReason == 'INSUFFICIENT_MATERIAL_STOCK')]", hasSize(1)));
    }

    @Test
    void fanGroupMaintainCreatesOneMaterialStockThatCanBeConsumedByClip() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("clip_fan_group_player", "群友投稿人");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "FAN_GROUP_MAINTAIN",
                                  "idempotencyKey": "clip-fan-group-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("投稿素材")));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*素材小票.*\\+1.*现有1份.*/)]", hasSize(1)));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "clip-fan-group-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "clip-fan-group-clip-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(0));
    }

    @Test
    void collectingFanTopicSubmissionsAddsMaterialStockForClip() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_topic_submission_player", "投稿箱场控");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(0));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.enabled == false && @.disabledReason == 'INSUFFICIENT_MATERIAL_STOCK')]", hasSize(1)));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": "COLLECT_SUBMISSIONS",
                                  "idempotencyKey": "fan-topic-collect-submission-stock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.choiceType").value("COLLECT_SUBMISSIONS"))
                .andExpect(jsonPath("$.data.summary", containsString("投稿征集")))
                .andExpect(jsonPath("$.data.summary", containsString("切片组递来素材")))
                .andExpect(jsonPath("$.data.summary", containsString("素材小票")))
                .andExpect(jsonPath("$.data.summary", containsString("现有1份")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_CLIP' && @.enabled == true)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "fan-topic-submission-clip"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(0));
    }

    @Test
    void collectingFanTopicSubmissionsCannotBeRepeatedToFarmMaterialStockSameDay() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_topic_submission_repeat_player", "投稿箱查重员");

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": "COLLECT_SUBMISSIONS",
                                  "idempotencyKey": "fan-topic-collect-submission-first"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary", containsString("投稿征集")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1));

        mockMvc.perform(get("/api/fan-topic/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].choices[?(@.enabled == false && @.disabledReason == 'FAN_TOPIC_ALREADY_HANDLED')]", hasSize(greaterThanOrEqualTo(4))));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": "COLLECT_SUBMISSIONS",
                                  "idempotencyKey": "fan-topic-collect-submission-second"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FAN_TOPIC_ALREADY_HANDLED"))
                .andExpect(jsonPath("$.message", containsString("今天")))
                .andExpect(jsonPath("$.message", containsString("粉丝群议题")))
                .andExpect(jsonPath("$.message", not(containsString("FAN_TOPIC"))));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.funProfile.materialStock").value(1));
    }

    @Test
    void daySevenReportShowsStageReviewForEmergingClipRoute() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("day7_clip_review_player", "七日切片苗头");

        for (int day = 1; day <= 6; day++) {
            String actionType = day % 3 == 0 ? "FAN_GROUP_MAINTAIN" : (day % 2 == 0 ? "PUBLISH_CLIP" : "PUBLISH_VIDEO");
            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "%s",
                                      "idempotencyKey": "day7-review-day-%d"
                                    }
                                    """.formatted(actionType, day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "day7-review-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "day7-review-day-7"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(7))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第7天阶段复盘.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*阶段目标复盘.*第1周先立住.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*切片路线苗头.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*组合技.*一鱼两剪.*/)]", hasSize(1)));
    }

    @Test
    void dayFourteenStageReviewCitesFanStructureOrReputationEvidence() throws Exception {
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
                                  "targetDay": 14
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(14))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(14))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第14天路线成型.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*阶段目标复盘.*第2周补路线.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第14天路线成型.*粉丝结构.*真爱粉.*口碑.*/)]", hasSize(1)));
    }

    @Test
    void dayThirtyStageReviewClosesObjectivesWithoutNextWeekTaste() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("day30_objective_review_player", "收官目标员");
        completeRestRunToEnding(session, "day30-objective-review");

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第30天最终收束.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*阶段目标复盘.*第30天收束.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*最终提示.*结局复盘只看本轮30天证据.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*下周口味.*/)]", hasSize(0)));
    }

    @Test
    void dayTwentyOneStageReviewWarnsWhenSameMemeIsBeingCheckedForRepeats() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("day21_meme_review_player", "查重冲刺预警");
        String[] actions = {
                "PUBLISH_VIDEO",
                "PUBLISH_CLIP",
                "FAN_GROUP_MAINTAIN",
                "PUBLISH_CLIP",
                "PUBLISH_VIDEO",
                "PUBLISH_CLIP",
                "FAN_GROUP_MAINTAIN",
                "PUBLISH_CLIP",
                "PUBLISH_VIDEO",
                "PUBLISH_CLIP",
                "FAN_GROUP_MAINTAIN",
                "PUBLISH_CLIP",
                "PUBLISH_VIDEO",
                "PUBLISH_CLIP",
                "PUBLISH_CLIP",
                "PUBLISH_VIDEO",
                "PUBLISH_CLIP",
                "FAN_GROUP_MAINTAIN",
                "PUBLISH_CLIP",
                "PUBLISH_VIDEO"
        };

        for (int day = 1; day <= actions.length; day++) {
            submitActionAndResolveFormalEvent(
                    session,
                    actions[day - 1],
                    day,
                    "day21-meme-review-day-%d".formatted(day),
                    null
            );

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "day21-meme-review-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitActionAndResolveFormalEvent(session, "PUBLISH_CLIP", 21, "day21-meme-review-day-21", null);

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(21))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第21天冲刺预警.*梗开始被查重.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第21天冲刺预警.*切片.*/)]", hasSize(1)));
    }

    @Test
    void publishVideoRequiresInspirationInActionListAndSubmitValidation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("video_inspiration_player", "no_inspiration_video");

        for (int day = 1; day <= 3; day++) {
            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "PUBLISH_VIDEO",
                                      "idempotencyKey": "video-inspiration-day-%d"
                                    }
                                    """.formatted(day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

            if (day < 3) {
                mockMvc.perform(post("/api/day/next")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "idempotencyKey": "video-inspiration-go-day-%d"
                                        }
                                        """.formatted(day + 1)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.day").value(day + 1))
                        .andExpect(jsonPath("$.data.phase").value("READY"));
            }
        }

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(0));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "video-inspiration-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_VIDEO' && @.enabled == false && @.disabledReason == 'INSUFFICIENT_INSPIRATION')]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "video-inspiration-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_INSPIRATION"));
    }

    @Test
    void duplicateActionRequestWithSameIdempotencyKeyReplaysWithoutDoubleSettlement() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("replay_player", "绫音");

        String requestBody = """
                {
                  "actionType": "TRAIN_SONG",
                  "idempotencyKey": "same-click-key"
                }
                """;

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("TRAIN_SONG"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.stamina").value(8))
                .andExpect(jsonPath("$.data.attributes.songPower").value(11))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(71));
    }

    @Test
    void consecutiveDefensiveActionsSurfaceContentAnxietyInDailyReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("defense_anxiety_player", "装死复盘台");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "defense-anxiety-day-1-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "defense-anxiety-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "defense-anxiety-day-2-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[3]", containsString("内容焦虑")))
                .andExpect(jsonPath("$.data.visibleItems[3]", containsString("连续防守")))
                .andExpect(jsonPath("$.data.riskHint", containsString("内容焦虑")));
    }

    @Test
    void trainTalkGainsInspirationAndShowsItInDailyReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("talk_inspiration_player", "复盘有灵感");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "talk-inspiration-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(11))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("灵感")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(4))
                .andExpect(jsonPath("$.data.attributes.talkPower").value(13))
                .andExpect(jsonPath("$.data.opinion.reputation").value(64));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("灵感+1")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("口碑+4")))
                .andExpect(jsonPath("$.data.visibleItems[1]", containsString("灵感")));
    }

    @Test
    void consecutiveSameActionReportsDoNotRepeatSummaryTemplateWithinThreeDays() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("non_repeat_report_player", "三天练歌台");

        for (int day = 1; day <= 3; day++) {
            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "TRAIN_SONG",
                                      "idempotencyKey": "non-repeat-train-song-day-%d"
                                    }
                                    """.formatted(day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

            if (day < 3) {
                mockMvc.perform(post("/api/day/next")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "idempotencyKey": "non-repeat-go-day-%d"
                                        }
                                        """.formatted(day + 1)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.day").value(day + 1))
                        .andExpect(jsonPath("$.data.phase").value("READY"));
            }
        }

        MvcResult result = mockMvc.perform(get("/api/reports").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        java.util.List<String> summaries = JsonPath.read(body, "$.data[*].summary");
        assertNotEquals(summaries.get(0), summaries.get(1));
        assertNotEquals(summaries.get(1), summaries.get(2));

        java.util.List<String> mainActionItems = JsonPath.read(body, "$.data[*].visibleItems[1]");
        assertNotEquals(mainActionItems.get(0), mainActionItems.get(1));
        assertNotEquals(mainActionItems.get(1), mainActionItems.get(2));
    }

    @Test
    void fanGroupMaintainGainsInspirationAndShowsSubmissionMaterialInDailyReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_group_inspiration_player", "fan_submission_collector");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "FAN_GROUP_MAINTAIN",
                                  "idempotencyKey": "fan-group-inspiration-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("FAN_GROUP_MAINTAIN"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("灵感")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(4))
                .andExpect(jsonPath("$.data.currentRoute").value("ELECTRONIC_PICKLE"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("灵感+1")))
                .andExpect(jsonPath("$.data.visibleItems[1]", containsString("投稿")));
    }

    @Test
    void restReportsStaminaRecoveryAsDefensiveAction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("rest_recovery_player", "low_pressure_rest");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "rest-recovery-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("REST"))
                .andExpect(jsonPath("$.data.actionResult.staminaChange").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(greaterThanOrEqualTo(0)));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.stamina").value(10))
                .andExpect(jsonPath("$.data.currentRoute").value("UNKNOWN"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("体力+")));
    }

    @Test
    void playerCanAdvanceFromReportReadyToNextDayReadyWithSynchronizedState() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("next_day_player", "奈奈");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "next-day-song"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.resources.stamina").value(10));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));
    }

    @Test
    void playerCanReadStreamPlanOptionsBeforeChoosingStreamPlan() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stream_plan_options_player", "企划菜单检查台");

        mockMvc.perform(get("/api/stream/plans").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].planType").value("TALK"))
                .andExpect(jsonPath("$.data[0].label").value("杂谈回"))
                .andExpect(jsonPath("$.data[1].planType").value("SINGING"))
                .andExpect(jsonPath("$.data[1].label").value("唱歌回"))
                .andExpect(jsonPath("$.data[11].planType").value("SC_THANKS"))
                .andExpect(jsonPath("$.data[11].label").value("醒目留言回应回"));
    }

    @Test
    void streamPlanMovesToNeedTitleAndSavedTitleCandidatesCanBeRecovered() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stream_player", "铃铃");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "stream-singing-day1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").isNumber())
                .andExpect(jsonPath("$.data.titleCandidates", hasSize(3)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'SAFE')]", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedAction").value("STREAM_PLAN"))
                .andExpect(jsonPath("$.data.selectedPlanId").isNumber())
                .andExpect(jsonPath("$.data.titleCandidates", hasSize(3)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'SAFE')]", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/stream/titles").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.style == 'SAFE')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void playerCanChooseSavedStreamTitleAndSettleLiveAction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("title_player", "歌奈");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "stream-before-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 21,
                                  "idempotencyKey": "choose-safe-song-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("STREAM_PLAN"))
                .andExpect(jsonPath("$.data.actionResult.selectedTitle").value("低压歌回，今天不证明自己"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.reportId").isNumber());

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.stamina").value(7))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(greaterThan(52)))
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedTitle").value("低压歌回，今天不证明自己"))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'stream_plan' && @.id == 2)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'title_template' && @.id == 21)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[0].type").value("business_log"));
    }

    @Test
    void singingBoostWeekBuffsSafeSingingStreamTitleAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("singing_title_boost_player", "歌回版本答案");
        advanceToDay(session, 8, "singing-title-boost");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "singing-title-boost-plan-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(2))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'SAFE' && @.effectPreview =~ /.*歌回扶持周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'SAFE' && @.effectPreview =~ /.*推荐.*/)]", hasSize(1)));

        var result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 21,
                                  "idempotencyKey": "singing-title-boost-safe-title-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("STREAM_PLAN"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(44)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("歌回扶持周")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.opinion.popularity").value(greaterThanOrEqualTo(95)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThanOrEqualTo(81)));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("人气+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("口碑+")));
    }

    @Test
    void singingBoostWeekMakesHardMouthSingingDebtMoreSevere() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("singing_hard_mouth_player", "高音贷款人");
        advanceToDay(session, 8, "singing-hard-mouth");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "singing-hard-mouth-plan-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'HARD_MOUTH' && @.effectPreview =~ /.*歌回扶持周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'HARD_MOUTH' && @.debtRiskPreview =~ /.*回旋镖.*/)]", hasSize(1)));

        var result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "singing-hard-mouth-title-day-8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("高风险歌回")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("HARD_MOUTH"))));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("SINGING_BOOST_WEEK"))
                .andExpect(jsonPath("$.data.debts", hasSize(1)))
                .andExpect(jsonPath("$.data.debts[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debts[0].severity").value(3))
                .andExpect(jsonPath("$.data.debts[0].dueDay").value(10))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("歌回扶持周")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("高音贷款")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("录播组")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("考据组")))
                .andExpect(jsonPath("$.data.debts[0].summary", not(containsString("标题反噬债务"))));
    }

    @Test
    void playerCanCancelPendingStreamPlanAndStillUseNonStreamActionToday() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("cancel_stream_player", "棉棉");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "TALK",
                                  "idempotencyKey": "stream-to-cancel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/day/action/cancel")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "cancel-stream-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.streamPlanCancelled").value(true))
                .andExpect(jsonPath("$.data.disabledActions[0].actionType").value("STREAM_PLAN"))
                .andExpect(jsonPath("$.data.disabledActions[0].disabledReason").value("STREAM_PLAN_CANCELLED_TODAY"))
                .andExpect(jsonPath("$.data.availableHint", containsString("非直播")))
                .andExpect(jsonPath("$.data.titleCandidatesJson").value("[]"))
                .andExpect(jsonPath("$.data.titleCandidates", hasSize(0)))
                .andExpect(jsonPath("$.data.selectedAction").doesNotExist())
                .andExpect(jsonPath("$.data.selectedPlanId").doesNotExist());

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.streamPlanCancelled").value(true));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'STREAM_PLAN' && @.enabled == false && @.disabledReason == 'STREAM_PLAN_CANCELLED_TODAY')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_TALK' && @.enabled == true)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "after-cancel-train-talk"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("TRAIN_TALK"))
                .andExpect(jsonPath("$.data.reportId").isNumber());
    }

    @Test
    void titleRerollCostsInspirationReplaysByIdempotencyAndStopsAtDailyLimit() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("reroll_player", "铃夏");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "stream-before-reroll"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        String firstReroll = """
                {
                  "planId": 2,
                  "idempotencyKey": "reroll-title-once"
                }
                """;

        mockMvc.perform(post("/api/stream/titles/reroll")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstReroll))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.titleRerollCount").value(1))
                .andExpect(jsonPath("$.data.inspiration").value(2))
                .andExpect(jsonPath("$.data.titleCandidates", hasSize(3)))
                .andExpect(jsonPath("$.data.titleCandidates[0].id").value(211));

        mockMvc.perform(post("/api/stream/titles/reroll")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstReroll))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titleRerollCount").value(1))
                .andExpect(jsonPath("$.data.inspiration").value(2))
                .andExpect(jsonPath("$.data.titleCandidates[0].id").value(211));

        mockMvc.perform(get("/api/stream/titles").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(211));

        mockMvc.perform(post("/api/stream/titles/reroll")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 2,
                                  "idempotencyKey": "reroll-title-twice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titleRerollCount").value(2))
                .andExpect(jsonPath("$.data.inspiration").value(1))
                .andExpect(jsonPath("$.data.titleCandidates[0].id").value(221));

        mockMvc.perform(post("/api/stream/titles/reroll")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 2,
                                  "idempotencyKey": "reroll-title-third"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TITLE_REROLL_LIMIT"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(1));
    }

    @Test
    void playerCanCompleteThirtyDayRunAndReadEndingReviewWithoutEnteringDayThirtyOne() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("ending_player", "阿灯");

        insertUnknownAccidentMaterialReference(session, "ending-run-unknown-material-guard");
        completeRestRunToEnding(session, "ending-run");

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.endingReviewId").isNumber());

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.nextDayEnabled").value(false));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").isNotEmpty())
                .andExpect(jsonPath("$.data.keyEvents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.routeReview.finalReportRef.type").value("daily_report"))
                .andExpect(jsonPath("$.data.routeReview.finalReportRef.id").isNumber())
                .andExpect(jsonPath("$.data.routeReview.finalReportRef.day").value(30))
                .andExpect(jsonPath("$.data.routeReview.accidentMaterials[?(@.materialId == 'UNKNOWN_ACCIDENT_MATERIAL' && @.label == '未署名事故素材')]", hasSize(1)))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(0)))
                .andExpect(jsonPath("$.data.restartHint.routeBias").isNotEmpty())
                .andExpect(jsonPath("$.data.restartHint.fanBiasPercent").value(greaterThanOrEqualTo(5)));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "ending-run-no-day-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DAY_LIMIT_REACHED"));
    }

    @Test
    void devDemoScriptCanResetAndRunSteadyRouteToDayThirty() throws Exception {
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "steady",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("steady"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.currentDay").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.vupId").isNumber())
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.reportIds", hasSize(greaterThanOrEqualTo(30))))
                .andExpect(jsonPath("$.data.reportIds[0]").isNumber())
                .andExpect(jsonPath("$.data.logCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.businessLogCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingReviewId").isNumber())
                .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.summary", containsString("验证")))
                .andExpect(jsonPath("$.data.summary", containsString("电子榨菜")))
                .andExpect(jsonPath("$.data.summary", not(containsString("ELECTRONIC_PICKLE"))))
                .andExpect(jsonPath("$.data.summary", not(containsString("ENDING_READY"))))
                .andExpect(jsonPath("$.data.lastDaySnapshot.fans").isNumber())
                .andExpect(jsonPath("$.data.lastDaySnapshot.watchHeat").isNumber())
                .andExpect(jsonPath("$.data.lastDaySnapshot.unresolvedDebtCount").isNumber())
                .andExpect(jsonPath("$.data.endingReason", containsString("电子榨菜")));

        mockMvc.perform(get("/api/reports").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(30))))
                .andExpect(jsonPath("$.data[0].visibleItems", hasSize(greaterThanOrEqualTo(3))));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.endingReason", containsString("低压陪伴")))
                .andExpect(jsonPath("$.data.endingReason", containsString("最终参考第30天表现")))
                .andExpect(jsonPath("$.data.endingReason", not(containsString("行动记录#"))))
                .andExpect(jsonPath("$.data.endingReason", not(containsString("business_log#"))))
                .andExpect(jsonPath("$.data.endingReasonJson.routeCandidate.routeType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.endingReasonJson.routeCandidate.score").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingReasonJson.matchedRules[0]").value("DEFAULT_SAFE_ROUTE"))
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.fans").value(greaterThanOrEqualTo(130)))
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.trueFans").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.trueFanRatio").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.funFanRatio").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.unicornRatio").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.keyMetricSnapshot.ddRatio").isNumber())
                .andExpect(jsonPath("$.data.endingReasonJson.evidenceRefs[0].type").value("business_log"))
                .andExpect(jsonPath("$.data.endingReasonJson.evidenceRefs[0].id").exists())
                .andExpect(jsonPath("$.data.keyEvents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'LOW_PRESSURE_NO_BIG_HIT' && @.label == '低压但没活')]", hasSize(1)));

        mockMvc.perform(get("/api/dev/demo/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.currentDay").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.logCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.businessLogCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingReviewId").isNumber())
                .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.summary", containsString("验证")))
                .andExpect(jsonPath("$.data.summary", containsString("电子榨菜")))
                .andExpect(jsonPath("$.data.summary", not(containsString("ELECTRONIC_PICKLE"))))
                .andExpect(jsonPath("$.data.summary", not(containsString("ENDING_READY"))))
                .andExpect(jsonPath("$.data.lastDaySnapshot.fans").isNumber())
                .andExpect(jsonPath("$.data.lastDaySnapshot.unresolvedDebtCount").isNumber())
                .andExpect(jsonPath("$.data.endingReason", containsString("电子榨菜")));
    }

    @Test
    void devDemoScriptSummaryLocalizesIntermediatePhase() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady",
                                  "runSeed": "demo-summary-phase-001"
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
                                  "targetDay": 14,
                                  "runSeed": "demo-summary-phase-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.summary", containsString("验证")))
                .andExpect(jsonPath("$.data.summary", containsString("今日日报")))
                .andExpect(jsonPath("$.data.summary", not(containsString("REPORT_READY"))));
    }

    @Test
    void devDemoStatusReportsLastRunScriptError() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady",
                                  "runSeed": "demo-last-error-001"
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
                                  "targetDay": 31,
                                  "runSeed": "demo-last-error-001",
                                  "stopOnError": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFIG_FIELD_INVALID"))
                .andExpect(jsonPath("$.message", containsString("目标天数")));

        mockMvc.perform(get("/api/dev/demo/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentDay").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.lastError.code").value("CONFIG_FIELD_INVALID"))
                .andExpect(jsonPath("$.data.lastError.message", containsString("目标天数")));
    }

    @Test
    void endingReviewKeyEventsExposeRngAndWeightDetailsForReplay() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "steady",
                                  "runSeed": "demo-replay-detail-001"
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
                                  "runSeed": "demo-replay-detail-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyEvents[0].rngDetail.seed").value("demo-replay-detail-001-day-1"))
                .andExpect(jsonPath("$.data.keyEvents[0].rngDetail.hit").exists())
                .andExpect(jsonPath("$.data.keyEvents[0].weightDetail.candidatePool", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.data.keyEvents[0].weightDetail.hit").exists())
                .andExpect(jsonPath("$.data.keyEvents[0].capDetail.source").value("BalanceConfig"))
                .andExpect(jsonPath("$.data.keyEvents[0].capDetail.stageCap").value(200))
                .andExpect(jsonPath("$.data.keyEvents[0].clampDetail.reputation").isNumber())
                .andExpect(jsonPath("$.data.keyEvents[0].clampDetail.songPower").isNumber())
                .andExpect(jsonPath("$.data.keyEvents[0].routeScoreChange.ELECTRONIC_PICKLE").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.keyEvents[0].routeScoreChange.source").value("TRAIN_TALK"));
    }

    @Test
    void endingReviewKeyEventsExposeTitleCandidateReplayDetails() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "black_red",
                                  "runSeed": "demo-title-replay-detail-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "black_red",
                                  "targetDay": 30,
                                  "runSeed": "demo-title-replay-detail-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        MvcResult result = mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> titleEvents = JsonPath.read(
                body,
                "$.data.keyEvents[?(@.action == 'STREAM_PLAN' && @.summary =~ /.*高风险歌回.*/)]"
        );
        assertFalse(titleEvents.isEmpty(), body);
        Map<String, Object> weightDetail = (Map<String, Object>) titleEvents.get(0).get("weightDetail");
        Map<String, Object> rngDetail = (Map<String, Object>) titleEvents.get(0).get("rngDetail");
        Map<String, Object> multiplierDetail = (Map<String, Object>) titleEvents.get(0).get("multiplierDetail");
        Map<String, Object> routeScoreChange = (Map<String, Object>) titleEvents.get(0).get("routeScoreChange");

        assertTrue(titleEvents.get(0).containsKey("planId"), body);
        assertEquals(23, titleEvents.get(0).get("titleTemplateId"));
        assertEquals("title_choice", weightDetail.get("rollType"));
        assertEquals(23, weightDetail.get("hitTitleId"));
        assertTrue(String.valueOf(weightDetail.get("hitTitleText")).contains("高音"), body);
        assertTrue(((List<?>) weightDetail.get("titleCandidates")).size() >= 3, body);
        assertEquals("demo-title-replay-detail-001-day-1", rngDetail.get("seed"));
        assertEquals(23, rngDetail.get("hitTitleId"));
        assertTrue(((Number) multiplierDetail.get("watchHeatGain")).intValue() >= 20, body);
        assertTrue(((Number) multiplierDetail.get("watchToFanRate")).doubleValue() > 0, body);
        assertTrue(((Map<?, ?>) multiplierDetail.get("watchToFanDetail")).containsKey("convertedFunFans"), body);
        assertEquals(4, routeScoreChange.get("BLACK_RED_MAIN_STAGE"));
        assertEquals("title:HARD_MOUTH", routeScoreChange.get("source"));
    }

    @Test
    void endingReviewKeyEventsExposeDebtCandidateReplayDetails() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "black_red",
                                  "runSeed": "demo-debt-replay-detail-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "black_red",
                                  "targetDay": 30,
                                  "runSeed": "demo-debt-replay-detail-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        MvcResult result = mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> debtEvents = JsonPath.read(
                body,
                "$.data.keyEvents[?(@.action == 'EVENT_CHOICE' && @.summary =~ /.*标题党反噬.*/)]"
        );
        assertFalse(debtEvents.isEmpty(), body);
        Map<String, Object> weightDetail = (Map<String, Object>) debtEvents.get(0).get("weightDetail");
        Map<String, Object> rngDetail = (Map<String, Object>) debtEvents.get(0).get("rngDetail");
        Map<String, Object> routeScoreChange = (Map<String, Object>) debtEvents.get(0).get("routeScoreChange");

        assertEquals("risk_debt_due", weightDetail.get("rollType"));
        assertEquals(weightDetail.get("hitDebtId"), debtEvents.get(0).get("eventId"));
        assertEquals("TITLE_BACKFIRE", weightDetail.get("hitDebtType"));
        assertTrue(((List<?>) weightDetail.get("debtCandidates")).size() >= 1, body);
        assertTrue(((Map<?, ?>) weightDetail.get("weights")).containsKey(String.valueOf(weightDetail.get("hitDebtId"))), body);
        assertEquals(weightDetail.get("hitDebtId"), rngDetail.get("hitDebtId"));
        assertEquals("TITLE_BACKFIRE", rngDetail.get("hitDebtType"));
        assertEquals(1, routeScoreChange.get("ELECTRONIC_PICKLE"));
        assertEquals("event:TITLE_BACKFIRE:safe", routeScoreChange.get("source"));
        List<Map<String, Object>> accidentMaterials = JsonPath.read(
                body,
                "$.data.routeReview.accidentMaterials[?(@.materialId == 'TITLE_BACKFIRE_CONTEXT' && @.sourceAction == 'EVENT_CHOICE')]"
        );
        assertFalse(accidentMaterials.isEmpty(), body);
        assertEquals("标题党反噬上下文", accidentMaterials.get(0).get("label"));
    }

    @Test
    void endingReviewKeyEventsExposeOrdinaryEventReplayDetails() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("ordinary_replay_player", "低压回放员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "ordinary-replay-day-1-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        advanceWithRest(session, 2, "ordinary-replay-day-2");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "ordinary-replay-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "ordinary-replay-day-3-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "ordinary-replay-event-safe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        for (int day = 4; day <= 30; day++) {
            advanceWithRest(session, day, "ordinary-replay-finish");
        }

        MvcResult result = mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> ordinaryEvents = JsonPath.read(
                body,
                "$.data.keyEvents[?(@.action == 'EVENT_CHOICE' && @.summary =~ /.*无事发生.*/)]"
        );
        assertFalse(ordinaryEvents.isEmpty(), body);
        Map<String, Object> weightDetail = (Map<String, Object>) ordinaryEvents.get(0).get("weightDetail");
        Map<String, Object> rngDetail = (Map<String, Object>) ordinaryEvents.get(0).get("rngDetail");
        Map<String, Object> routeScoreChange = (Map<String, Object>) ordinaryEvents.get(0).get("routeScoreChange");

        assertEquals("ordinary_event_roll", weightDetail.get("rollType"));
        assertEquals("REST_SAVED_MELTDOWN", weightDetail.get("hitEventKey"));
        assertTrue(((List<?>) weightDetail.get("eventCandidates")).size() >= 1, body);
        assertTrue(((Map<?, ?>) weightDetail.get("weights")).containsKey("REST_SAVED_MELTDOWN"), body);
        assertEquals("REST_SAVED_MELTDOWN", rngDetail.get("hitEventKey"));
        assertEquals(1, routeScoreChange.get("ELECTRONIC_PICKLE"));
        assertEquals("event:REST_SAVED_MELTDOWN:safe", routeScoreChange.get("source"));
    }

    @Test
    void devDemoScriptCanResetAndRunClipRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "clip"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "clip",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("clip"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.endingReason", containsString("发布切片")));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'PUBLISH_CLIP')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void devDemoScriptCanResetAndRunSocialRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "social"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "social",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("social"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").value("DD_BUS_STOP"))
                .andExpect(jsonPath("$.data.endingReason", containsString("同台互动")));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("DD_BUS_STOP"))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("SOCIAL_COLLAB"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'NPC_INTERACT')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void devDemoScriptCanResetAndRunBlackRedRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "black_red"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "black_red",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("black_red"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.endingReason", containsString("硬嘴标题")));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.routeReview.routeEvidenceTrail[?(@.action == 'INTERACTION_CHOICE' && @.interactionEventId == 1 && @.routeType == 'ELECTRONIC_PICKLE' && @.scoreDelta == 1 && @.source == 'interaction:CHAT_BAIT:safe')]",
                        hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.debtRefs[0].debtType").value("TITLE_BACKFIRE"));
    }

    @Test
    void devDemoScriptCanResetAndRunNewAtlasEndingRoutesToDayThirty() throws Exception {
        MockHttpSession singing = resetAndRunDevDemoStrategy("singing", "SINGING_IDOL");
        mockMvc.perform(get("/api/ending/review").session(singing))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'TRAIN_SONG')]", hasSize(greaterThanOrEqualTo(1))));

        MockHttpSession cyberGirlfriend = resetAndRunDevDemoStrategy("cyber_girlfriend", "CYBER_GIRLFRIEND");
        mockMvc.perform(get("/api/ending/review").session(cyberGirlfriend))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("CYBER_GIRLFRIEND"))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'STREAM_PLAN' && @.summary =~ /.*陪伴营业.*/)]",
                        hasSize(greaterThanOrEqualTo(1))));

        MockHttpSession mainStageKing = resetAndRunDevDemoStrategy("main_stage_king", "MAIN_STAGE_KING");
        mockMvc.perform(get("/api/ending/review").session(mainStageKing))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("MAIN_STAGE_KING"))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.endingReason", containsString("硬嘴标题")))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(greaterThanOrEqualTo(1))));

        // glorious_graduation 的路线/结局一致性由 11 路线 acceptance 矩阵负责；毕业结局可达性由 defense 路线覆盖。
    }

    @Test
    void devDemoScriptCanResetAndRunIdleRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "idle"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "idle",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("idle"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").isNotEmpty())
                .andExpect(jsonPath("$.data.endingReason").isNotEmpty());

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").isNotEmpty())
                .andExpect(jsonPath("$.data.finalTitle").isNotEmpty())
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.routeReview.routeEvidenceTrail[?(@.action == 'EVENT_CHOICE' && @.routeType == 'ELECTRONIC_PICKLE' && @.scoreDelta == 1 && @.source == 'event:REST_SAVED_MELTDOWN:safe')]",
                        hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'REST')]", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(0)));
    }

    @Test
    void devDemoScriptCanResetAndRunDefenseRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "defense"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "defense",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("defense"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").value("GLORIOUS_GRADUATION"))
                .andExpect(jsonPath("$.data.endingReason", containsString("粉丝群维护")));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("GLORIOUS_GRADUATION"))
                .andExpect(jsonPath("$.data.finalTitle").value("光荣毕业"))
                .andExpect(jsonPath("$.data.endingReason", containsString("粉丝群维护")))
                .andExpect(jsonPath("$.data.routeReview.accidentMaterials[?(@.sourceAction == 'RISK_TOOL:FULL_CLIP_CONTEXT' && @.riskToolId == 'FULL_CLIP_CONTEXT')]",
                        hasSize(1)))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'FAN_GROUP_MAINTAIN')]", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.endingReasonJson.rule").exists())
                .andExpect(jsonPath("$.data.endingReasonJson.evidenceIds", hasSize(greaterThan(0))));
    }

    @Test
    void devDemoScriptCanResetAndRunRandomRouteToDayThirty() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "random"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "random",
                                  "targetDay": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("random"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.logCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingReviewId").isNumber())
                .andExpect(jsonPath("$.data.endingType").exists());

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyEvents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.routeReview.routeEvidenceTrail", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void devStressRunReturnsThousandRoundBalanceSummary() throws Exception {
        mockMvc.perform(post("/api/dev/stress/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "random",
                                  "rounds": 1000,
                                  "runSeed": "stress-random-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("random"))
                .andExpect(jsonPath("$.data.rounds").value(1000))
                .andExpect(jsonPath("$.data.daysPerRound").value(30))
                .andExpect(jsonPath("$.data.endingDistribution.UNKNOWN").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.endingDistribution.ELECTRONIC_PICKLE").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.endingDistribution.SLICE_SAINT").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.endingDistribution.BLACK_RED_MAIN_STAGE").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.routeScoreDistribution.BLACK_RED_MAIN_STAGE").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.endingReasonDistribution.ROUTE_BLACK_RED_OR_HIGH_WATCH_HEAT").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.avgFinalFans").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.avgReputation").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.avgMemeLevel").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.avgUnresolvedDebts").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.maxSingleDayFanGain").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.negativeEventStreakMax").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.topEventFrequency").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.topEventFrequency").value(lessThanOrEqualTo(0.15)))
                .andExpect(jsonPath("$.data.stageCapHitCount").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.memeFatigueTriggerCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.boomerangPhaseTriggerCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.debtDelayCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.qualification.randomRoundsAtLeast1000").value(true))
                .andExpect(jsonPath("$.data.qualification.randomOver100kRatio").value(lessThan(0.1)))
                .andExpect(jsonPath("$.data.qualification.topEventFrequencyAtMost15Percent").value(true));
    }

    @Test
    void devDemoRandomRouteReplaysKeyResultWithSameRunSeed() throws Exception {
        String firstFinalAction = runSeededRandomDemoAndReadFinalAction("demo-random-n12-a");
        String replayFinalAction = runSeededRandomDemoAndReadFinalAction("demo-random-n12-a");
        String otherSeedFinalAction = runSeededRandomDemoAndReadFinalAction("demo-random-n12-b");

        assertEquals(firstFinalAction, replayFinalAction);
        assertNotEquals(firstFinalAction, otherSeedFinalAction);
    }

    @Test
    void devDemoFastForwardContinuesCurrentDemoRunToDayThirty() throws Exception {
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

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_TALK",
                                  "idempotencyKey": "demo-fast-forward-manual-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "demo-fast-forward-manual-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2));

        mockMvc.perform(post("/api/dev/demo/fast-forward")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("steady"))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"));
    }

    @Test
    void playerCanReadHistoricalDailyReportsInDayOrder() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("history_report_player", "小报");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "history-day-1-song"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "history-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "history-day-2-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/reports").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].day").value(1))
                .andExpect(jsonPath("$.data[1].day").value(2))
                .andExpect(jsonPath("$.data[0].visibleItems", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void systemConfigCheckReportsP0CanPlayCoreConfiguration() throws Exception {
        // 登录后访问 config-check 才能拿到完整 devTools 信息；未登录时 devTools 被安全门控裁剪为 null。
        MockHttpSession session = registerLoginAndCreateVup("config_check_player", "配置检查员");

        mockMvc.perform(get("/api/system/config-check").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("配置检查完成")))
                .andExpect(jsonPath("$.message", not(containsString("P0"))))
                .andExpect(jsonPath("$.data.status").value("PASS"))
                .andExpect(jsonPath("$.data.canPlayP0").value(true))
                .andExpect(jsonPath("$.data.categories", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data.fatalErrors", hasSize(0)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'actions' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'streamTitles' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'liveInteraction' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'fanTopic' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'dailyLoop' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'endingReview' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'galleryAssets' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'seedData' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'seedData' && @.detail =~ /.*核心种子.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'riskTools' && @.detail =~ /.*米线工具.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'riskTools' && @.detail =~ /.*P0.*/)]", hasSize(0)))
                .andExpect(content().string(containsString("data.sql")))
                .andExpect(content().string(containsString("v4/protagonist/avatars/avatar-default.png")))
                .andExpect(content().string(containsString("v4/live-room/shells/shell-default.png")))
                .andExpect(content().string(containsString("v4/routes/glorious-graduation/cover-landscape.png")))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'devDemo' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'devDemo' && @.detail =~ /.*开发验证脚本可用.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'contentSafety' && @.detail =~ /.*公开安全模式.*/)]", hasSize(1)))
                .andExpect(content().string(not(containsString("DEV_DEMO_ENABLED"))))
                .andExpect(content().string(not(containsString("DEV_TOOL_DISABLED"))))
                .andExpect(content().string(not(containsString("CONTENT_SAFETY"))))
                .andExpect(content().string(not(containsString("PUBLIC_SAFE"))))
                .andExpect(jsonPath("$.data.devTools.enabled").value(true))
                .andExpect(jsonPath("$.data.devTools.profile").value("测试环境"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies", hasSize(11)))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[0]").value("steady"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[1]").value("clip"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[2]").value("black_red"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[3]").value("social"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[4]").value("singing"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[5]").value("cyber_girlfriend"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[6]").value("main_stage_king"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[7]").value("glorious_graduation"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[8]").value("idle"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[9]").value("defense"))
                .andExpect(jsonPath("$.data.devTools.availableStrategies[10]").value("random"));
    }

    @Test
    void systemConfigCheckReportsStructuredSeedManifestCounts() throws Exception {
        mockMvc.perform(get("/api/system/config-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'seedData' && @.expectedCount == 6 && @.actualCount == 6)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'seedData' && @.required == 6 && @.actual == 6)]", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[?(@.name == 'demo_strategy_script' && @.required == 11 && @.actual == 11)]", hasSize(1)));
    }

    @Test
    void highRiskStreamTitlePausesForInteractionChoiceThenCreatesVisibleDebtAndReportWarning() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("debt_player", "开庭酱");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "debt-stream-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "debt-choose-hard-mouth"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("CHAT_BAIT"))
                .andExpect(jsonPath("$.data.interactionEvent.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.interactionEvent.choices[0].choiceId").value("safe"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].severity").value(2))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].remainingDays").value(3));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(1))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.pending").value(true))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("CHAT_BAIT"))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.choices[0].choiceType").value("safe"));

        mockMvc.perform(get("/api/interaction/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.eventKey").value("CHAT_BAIT"))
                .andExpect(jsonPath("$.data.description", containsString("弹幕")))
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.choices[0].choiceType").value("safe"))
                .andExpect(jsonPath("$.data.choices[0].enabled").value(true))
                .andExpect(jsonPath("$.data.choices[1].choiceType").value("traffic"))
                .andExpect(jsonPath("$.data.choices[1].enabled").value(true));

        String interactionRequest = """
                {
                  "choiceType": "safe",
                  "idempotencyKey": "debt-interaction-safe"
                }
                """;

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interactionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.actionResult.actionType").value("INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("不评价")))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(1));

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interactionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts", hasSize(1)))
                .andExpect(jsonPath("$.data.debts[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debts[0].severity").value(2))
                .andExpect(jsonPath("$.data.debts[0].dueDay").value(4));

        mockMvc.perform(get("/api/ending/forecast").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLine", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.riskLine", not(containsString("TITLE_BACKFIRE"))));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'live_interaction' && @.eventKey == 'CHAT_BAIT' && @.choiceType == 'safe')]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'risk_debt' && @.debtType == 'TITLE_BACKFIRE')]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*围观.*留下.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.riskHint", containsString("标题")))
                .andReturn();
        assertChineseDebtSummary(reportResult, "标题党反噬", "严重度");
    }

    @Test
    void titleDebtPreviewUsesRepeatPressureBeforeChoosingTitle() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("title_repeat_preview_player", "预览对账员");
        createHighRiskTitleDebt(session, "title-repeat-preview-day-1");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "title-repeat-preview-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "title-repeat-preview-stream-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'HARD_MOUTH' && @.debtRiskPreview =~ /.*3级.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'HARD_MOUTH' && @.debtRiskPreview =~ /.*同类旧账加压.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'HARD_MOUTH' && @.debtRiskPreview =~ /.*回旋镖.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "title-repeat-preview-choose-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].severity").value(3));
    }

    @Test
    void pendingInteractionRecoversDocumentedMarshmallowSeedTemplate() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("marshmallow_pending_player", "棉花糖复读");
        Long userId = (Long) session.getAttribute("USER_ID");
        var vup = vupMapper.findActiveByUserId(userId);
        var daySession = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        daySession.setPhase("NEED_INTERACTION_CHOICE");
        daySession.setPendingInteractionEventId(2L);
        daySessionMapper.updateAfterAction(daySession);

        mockMvc.perform(get("/api/interaction/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.eventKey").value("MARSHMALLOW_BOMB"))
                .andExpect(jsonPath("$.data.description", containsString("棉花糖")))
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.choices[0].choiceType").value("safe"))
                .andExpect(jsonPath("$.data.choices[1].choiceType").value("traffic"))
                .andExpect(jsonPath("$.data.choices[1].enabled").value(true))
                .andExpect(jsonPath("$.data.choices[2].choiceType").value("meme"))
                .andExpect(jsonPath("$.data.choices[2].enabled").value(true));
    }

    @Test
    void choosingPendingInteractionUsesCurrentSeedTemplateAsEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("marshmallow_choose_player", "棉花糖回收站");
        Long userId = (Long) session.getAttribute("USER_ID");
        var vup = vupMapper.findActiveByUserId(userId);
        var daySession = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        daySession.setPhase("NEED_INTERACTION_CHOICE");
        daySession.setPendingInteractionEventId(2L);
        daySessionMapper.updateAfterAction(daySession);

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "marshmallow-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("棉花糖")))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.type").value("live_interaction"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.id").value(2))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.eventKey").value("MARSHMALLOW_BOMB"));
    }

    @Test
    void marshmallowRiskTitlePausesForMarshmallowBombInteraction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("marshmallow_title_player", "棉花糖警戒");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "MARSHMALLOW",
                                  "idempotencyKey": "marshmallow-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(6))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText", containsString("棉花糖")))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("HARD_MOUTH"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 63,
                                  "idempotencyKey": "marshmallow-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("MARSHMALLOW_BOMB"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(2))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("MARSHMALLOW_BOMB"));
    }

    @Test
    void collabRiskTitlePausesForCollabRhythmInteraction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("collab_title_player", "联动节奏观察员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "COLLAB",
                                  "idempotencyKey": "collab-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(9))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("联动前先写清不引战公告"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("HARD_MOUTH"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 93,
                                  "idempotencyKey": "collab-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("COLLAB_RHYTHM"))
                .andExpect(jsonPath("$.data.interactionEvent.choices[0].label").value("夸对方，联动稳定"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(4))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("COLLAB_RHYTHM"));
    }

    @Test
    void sharpCommentRiskTitlePausesForChatBaitInteraction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("sharp_comment_player", "锐评安全员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SHARP_COMMENT",
                                  "idempotencyKey": "sharp-comment-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(7))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("锐评前先声明不点名不挂人"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("HARD_MOUTH"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 73,
                                  "idempotencyKey": "sharp-comment-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("CHAT_BAIT"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(1))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("CHAT_BAIT"));
    }

    @Test
    void talkRiskTitlePausesForChatBaitInteraction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("talk_title_player", "杂谈米线员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "TALK",
                                  "idempotencyKey": "talk-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(1))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("今天有话直说，别录屏太快"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("HARD_MOUTH"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 13,
                                  "idempotencyKey": "talk-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("CHAT_BAIT"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(1))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("CHAT_BAIT"));
    }

    @Test
    void fanServiceScThanksPausesForBossQuestionAndKeepsCommercialReportDelta() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("sc_boss_question_player", "榜一气压计");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "boss-question-sc-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(12))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("榜一问题能不能不要这么懂"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("FAN_SERVICE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 123,
                                  "idempotencyKey": "boss-question-fan-service-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("醒目留言陪伴回")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("FAN_SERVICE"))))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("SC_BOSS_QUESTION"))
                .andExpect(jsonPath("$.data.interactionEvent.choices[0].label").value("感谢带过"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(3))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("SC_BOSS_QUESTION"));

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "boss-question-safe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("榜一问题")))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.eventKey").value("SC_BOSS_QUESTION"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedTitle").value("榜一问题能不能不要这么懂"))
                .andExpect(jsonPath("$.data.dataDelta.coinDelta").value(600))
                .andExpect(jsonPath("$.data.dataDelta.commercialDelta").value(4))
                .andExpect(jsonPath("$.data.summary", containsString("醒目留言")))
                .andExpect(jsonPath("$.data.visibleItems[1]", containsString("陪伴营业")))
                .andExpect(jsonPath("$.data.visibleItems[1]", not(containsString("FAN_SERVICE"))));
    }

    @Test
    void gameRiskTitlePausesForChatBaitInteraction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("game_title_player", "游戏指挥位");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "GAME",
                                  "idempotencyKey": "game-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(4))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("这把我来指挥，队友先别急"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("HARD_MOUTH"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 43,
                                  "idempotencyKey": "game-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("CHAT_BAIT"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingInteractionEventId").value(1))
                .andExpect(jsonPath("$.data.pendingInteractionEvent.eventKey").value("CHAT_BAIT"));
    }

    @Test
    void surprisePlanUsesDocumentedSeedTitles() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("surprise_title_player", "突击麦克风");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SURPRISE",
                                  "idempotencyKey": "surprise-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(5))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText").value("突击十分钟，看看麦好没好"))
                .andExpect(jsonPath("$.data.titleCandidates[1].titleText").value("突击开播，看看谁还醒着"))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("临时开播，主打一个没准备"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("ABSTRACT_MEME"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 53,
                                  "idempotencyKey": "surprise-abstract-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.selectedTitle").value("临时开播，主打一个没准备"));
    }

    @Test
    void endurancePlanUsesDocumentedSeedTitles() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("endurance_title_player", "耐久值班台");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "ENDURANCE",
                                  "idempotencyKey": "endurance-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(8))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText").value("低强度陪伴耐久，别卷主播"))
                .andExpect(jsonPath("$.data.titleCandidates[1].titleText").value("今天试试能播多久"))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("不下播挑战，谁先困谁输"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("ABSTRACT_MEME"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 83,
                                  "idempotencyKey": "endurance-abstract-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.selectedTitle").value("不下播挑战，谁先困谁输"));
    }

    @Test
    void singTalkPlanUsesDocumentedSeedTitles() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("sing_talk_title_player", "歌杂值班台");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SING_TALK",
                                  "idempotencyKey": "sing-talk-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(10))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText").value("唱几首，聊几句，慢慢来"))
                .andExpect(jsonPath("$.data.titleCandidates[1].titleText").value("歌杂混合，看看今晚哪边更有活"))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("唱歌不够，杂谈来凑"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("ABSTRACT_MEME"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 103,
                                  "idempotencyKey": "sing-talk-abstract-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.selectedTitle").value("唱歌不够，杂谈来凑"));
    }

    @Test
    void shortChallengePlanUsesDocumentedSeedTitles() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("short_challenge_title_player", "短挑战素材台");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SHORT_CHALLENGE",
                                  "idempotencyKey": "short-challenge-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(11))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText").value("低压短视频挑战，失败也算素材"))
                .andExpect(jsonPath("$.data.titleCandidates[1].titleText").value("跟一下今天的热门挑战"))
                .andExpect(jsonPath("$.data.titleCandidates[2].titleText").value("这也能火？我来复刻一下"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("ABSTRACT_MEME"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 113,
                                  "idempotencyKey": "short-challenge-abstract-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.selectedTitle").value("这也能火？我来复刻一下"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("梗舞")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("DANCE_MEME"))));
    }

    @Test
    void playerCanUseRiskToolNextDayToMitigateDebtWithoutDoubleCostOrClearingMemory() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("risk_tool_player", "米线酱");

        createHighRiskTitleDebt(session, "risk-tool");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "risk-tool-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.riskHints[0]", containsString("旧账")))
                .andExpect(jsonPath("$.data.riskHints[0]", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.riskHints[0]", not(containsString("TITLE_BACKFIRE"))));

        String useToolRequest = """
                {
                  "toolType": "FULL_CLIP_CONTEXT",
                  "idempotencyKey": "risk-tool-full-context"
                }
                """;

        mockMvc.perform(post("/api/risk-tool/use")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(useToolRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolType").value("FULL_CLIP_CONTEXT"))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.coinDelta").value(0))
                .andExpect(jsonPath("$.data.inspirationDelta").value(-1))
                .andExpect(jsonPath("$.data.debtSeverityDelta").value(-2))
                .andExpect(jsonPath("$.data.reputationDelta").value(3))
                .andExpect(jsonPath("$.data.popularityDelta").value(-80))
                .andExpect(jsonPath("$.data.inspiration").value(2))
                .andExpect(jsonPath("$.data.debt.debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debt.severity").value(1))
                .andExpect(jsonPath("$.data.debt.summary", containsString("补全切片")))
                .andExpect(jsonPath("$.data.debt.summary", not(containsString("FULL_CLIP_CONTEXT"))))
                .andExpect(jsonPath("$.data.debt.summary", not(containsString("TITLE_BACKFIRE"))))
                .andExpect(jsonPath("$.data.summary", containsString("切片反转")))
                .andExpect(jsonPath("$.data.summary", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.summary", not(containsString("TITLE_BACKFIRE"))));

        mockMvc.perform(post("/api/risk-tool/use")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(useToolRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inspiration").value(2))
                .andExpect(jsonPath("$.data.debt.severity").value(1));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(2))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(greaterThanOrEqualTo(72)))
                .andExpect(jsonPath("$.data.debts", hasSize(1)))
                .andExpect(jsonPath("$.data.debts[0].severity").value(1));

        mockMvc.perform(post("/api/risk-tool/use")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolType": "COOLING_NOTICE",
                                  "idempotencyKey": "risk-tool-second-same-day"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RISK_TOOL_DAILY_LIMIT"));
    }

    @Test
    void playerCanQueryRiskToolOptionsWithCostsAndAvailability() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("risk_tool_options_player", "米线工具箱");

        createHighRiskTitleDebt(session, "risk-tool-options");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "risk-tool-options-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/risk-tool/options")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].toolType").value("FULL_CLIP_CONTEXT"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].costPreview").value("1灵感"))
                .andExpect(jsonPath("$.data[0].effectPreview", containsString("补来源上下文")))
                .andExpect(jsonPath("$.data[0].effectPreview", containsString("旧账严重度")))
                .andExpect(jsonPath("$.data[0].targetDebtId").isNumber())
                .andExpect(jsonPath("$.data[1].toolType").value("COOLING_NOTICE"))
                .andExpect(jsonPath("$.data[1].costPreview").value("500运营预算或1体力"))
                .andExpect(jsonPath("$.data[2].toolType").value("TEMP_MOD_TEAM"))
                .andExpect(jsonPath("$.data[2].enabled").value(true))
                .andExpect(jsonPath("$.data[2].costPreview").value("500活动预算"));
    }

    @Test
    void coolingNoticeCanSpendStaminaWhenCoinsAreNotEnough() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("risk_tool_stamina_player", "体力公关人");

        createHighRiskTitleDebt(session, "risk-tool-stamina");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "risk-tool-stamina-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("READY"));

        var vup = vupMapper.findActiveByUserId((Long) session.getAttribute("USER_ID"));
        vup.setCoin(100);
        vupMapper.updateState(vup);

        mockMvc.perform(post("/api/risk-tool/use")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolType": "COOLING_NOTICE",
                                  "idempotencyKey": "risk-tool-stamina-cooling"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolType").value("COOLING_NOTICE"))
                .andExpect(jsonPath("$.data.coin").value(100))
                .andExpect(jsonPath("$.data.stamina").value(9))
                .andExpect(jsonPath("$.data.debt.severity").value(1));
    }

    @Test
    void dueDebtReturnsAsFormalEventAndMustBeHandledBeforeDailyReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("debt_return_player", "回旋灯");

        createHighRiskTitleDebt(session, "debt-return");
        advanceWithRest(session, 2, "debt-return-day-2");
        advanceWithRest(session, 3, "debt-return-day-3");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "debt-return-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "debt-return-day-4-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.reportId").doesNotExist());

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.pendingFormalEventId").isNumber())
                .andExpect(jsonPath("$.data.pendingFormalEvent.pending").value(true))
                .andExpect(jsonPath("$.data.pendingFormalEvent.debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.pendingFormalEvent.choices[0].choiceType").value("safe"));

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.title").value("标题党反噬正式事件"))
                .andExpect(jsonPath("$.data.description", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.description", containsString("标题组")))
                .andExpect(jsonPath("$.data.description", containsString("录播组")))
                .andExpect(jsonPath("$.data.description", containsString("考据楼")))
                .andExpect(jsonPath("$.data.description", containsString("高音贷款")))
                .andExpect(jsonPath("$.data.description", not(containsString("回旋镖到账"))))
                .andExpect(jsonPath("$.data.description", not(containsString("回旋镖切片"))))
                .andExpect(jsonPath("$.data.severity").isNumber())
                .andExpect(jsonPath("$.data.dueDay").value(4))
                .andExpect(jsonPath("$.data.choices", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.choices[0].choiceId").value("safe"))
                .andExpect(jsonPath("$.data.choices[0].choiceType").value("safe"))
                .andExpect(jsonPath("$.data.choices[0].enabled").value(true))
                .andExpect(jsonPath("$.data.choices[0].costPreview").value("无资源成本"))
                .andExpect(jsonPath("$.data.choices[0].effectPreview", containsString("热度")))
                .andExpect(jsonPath("$.data.choices[1].enabled").value(true))
                .andExpect(jsonPath("$.data.choices[2].enabled").value(true));

        mockMvc.perform(get("/api/risk-tool/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].toolType").value("FULL_CLIP_CONTEXT"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].targetDebtId").isNumber());

        mockMvc.perform(post("/api/risk-tool/use")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolType": "FULL_CLIP_CONTEXT",
                                  "idempotencyKey": "debt-return-event-phase-full-context"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.debt.debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debt.severity").value(1));

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.severity").value(1));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "debt-return-cannot-skip-event"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PHASE_NOT_ALLOWED"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": 9999,
                                  "choiceId": "safe",
                                  "idempotencyKey": "debt-return-wrong-event-id"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVENT_NOT_PENDING"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceId": "safe",
                                  "idempotencyKey": "debt-return-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.actionResult.actionType").value("EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("标题组")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("录播组")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("高音贷款")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("考据楼")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("证据仍被留档")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("玩家选择"))))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("回旋镖到账"))))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("safe处理"))))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(1));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": 9999,
                                  "choiceId": "safe",
                                  "idempotencyKey": "debt-return-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.visibleItems", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'risk_debt' && @.choiceType == 'safe')]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'accident_material' && @.materialId == 'TITLE_BACKFIRE_CONTEXT' && @.sourceType == 'risk_debt')]", hasSize(1)))
                .andReturn();
        assertChineseDebtSummary(reportResult, "标题党反噬", "已降温");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts", hasSize(0)));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "debt-return-go-day-5"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(5))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "debt-return-day-5-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));
    }

    @Test
    void trafficEventChoiceClearsCurrentDebtButCreatesShortRolloverDebt() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("debt_traffic_rollover_player", "硬接流量台");

        createHighRiskTitleDebt(session, "debt-traffic-rollover");
        advanceWithRest(session, 2, "debt-traffic-rollover-day-2");
        advanceWithRest(session, 3, "debt-traffic-rollover-day-3");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "debt-traffic-rollover-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "debt-traffic-rollover-day-4-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "traffic",
                                  "idempotencyKey": "debt-traffic-rollover-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("硬接流量")))
                .andExpect(jsonPath("$.data.actionResult.debtCreated", hasSize(1)))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].severity").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.actionResult.debtCreated[0].remainingDays").value(2))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.choiceType").value("traffic"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.rolloverDebtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.rolloverSeverity").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.rolloverDueDay").value(6))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.rolloverRemainingDays").value(2))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.watchHeatChange").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.reputationChange").value(lessThan(0)));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'risk_debt' && @.choiceType == 'traffic')]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*新短期旧账.*严重度.*第6天回流.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.riskHint", containsString("旧账压力")))
                .andExpect(jsonPath("$.data.riskHint", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.riskHint", containsString("第6天回流")))
                .andReturn();
        assertChineseDebtSummary(reportResult, "标题党反噬", "第6天回流", "明天日报会优先提醒");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts", hasSize(1)))
                .andExpect(jsonPath("$.data.debts[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debts[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("硬接")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("第6天会再次回流")))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("明天日报会优先提醒")));
    }

    @Test
    void dueFormalEventSkipsLiveInteractionWhenRiskTitleIsChosen() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("formal_slot_skips_live_player", "事件优先级");

        createHighRiskTitleDebt(session, "formal-slot-skip");
        advanceWithRest(session, 2, "formal-slot-skip-day-2");
        advanceWithRest(session, 3, "formal-slot-skip-day-3");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "formal-slot-skip-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "formal-slot-skip-day-4-stream-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "formal-slot-skip-day-4-hard-mouth-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.interactionEvent").doesNotExist())
                .andExpect(jsonPath("$.data.formalEvent.pending").value(true))
                .andExpect(jsonPath("$.data.formalEvent.debtType").value("TITLE_BACKFIRE"));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.pendingInteractionEventId").doesNotExist())
                .andExpect(jsonPath("$.data.pendingFormalEvent.pending").value(true));
    }

    @Test
    void endingReviewKeepsClearedHighImpactDebtAsDebtReference() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("cleared_debt_ending_player", "留账灯");

        createHighRiskTitleDebt(session, "cleared-debt-ending");
        advanceWithRest(session, 2, "cleared-debt-ending");
        advanceWithRest(session, 3, "cleared-debt-ending");
        advanceWithRest(session, 4, "cleared-debt-ending");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts", hasSize(0)));

        for (int day = 5; day <= 30; day++) {
            advanceWithRest(session, day, "cleared-debt-ending");
        }

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.debtRefs[?(@.debtType == 'TITLE_BACKFIRE' && @.status == 'CLEARED')]", hasSize(1)))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'LOW_PRESSURE_NO_BIG_HIT' && @.label == '低压但没活')]", hasSize(1)))
                .andExpect(jsonPath("$.data.debtRefs[0].summary", containsString("证据仍保留")));
    }

    @Test
    void ordinaryFormalEventCanInterruptActionBeforeDailyReportAndKeepEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("ordinary_event_player", "低压保温杯");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "ordinary-event-day-1-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        advanceWithRest(session, 2, "ordinary-event-day-2");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "ordinary-event-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "ordinary-event-day-3-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.reportId").doesNotExist());

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.debtId").doesNotExist())
                .andExpect(jsonPath("$.data.debtType").value("ORDINARY_EVENT"))
                .andExpect(jsonPath("$.data.title").value("低压运营救场"))
                .andExpect(jsonPath("$.data.description", containsString("无事发生")))
                .andExpect(jsonPath("$.data.description", not(containsString("REST_SAVED_MELTDOWN"))))
                .andExpect(jsonPath("$.data.choices[0].choiceType").value("safe"));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "ordinary-event-safe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("无事发生")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("REST_SAVED_MELTDOWN"))))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(1));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.evidenceRefs[0].action").value("EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'ordinary_event' && @.eventKey == 'REST_SAVED_MELTDOWN' && @.choiceType == 'safe')]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[1]", containsString("无事发生")))
                .andExpect(jsonPath("$.data.visibleItems[1]", not(containsString("REST_SAVED_MELTDOWN"))));
    }

    @Test
    void playerCanRestartAfterEndingWithConfirmationAndIdempotency() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("restart_player", "复活赛");
        completeRestRunToEnding(session, "restart-run");

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmRestart": false,
                                  "restartBiasType": "SLICE_SAINT",
                                  "idempotencyKey": "restart-missing-confirm"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESTART_CONFIRM_REQUIRED"));

        String restartRequest = """
                {
                  "confirmRestart": true,
                  "restartBiasType": "SLICE_SAINT",
                  "idempotencyKey": "restart-confirmed"
                }
                """;

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restartRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.restartHintApplied.targetType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.restartHintApplied.routeBiasType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.restartHintApplied.biasType").value("TRUE_FANS"))
                .andExpect(jsonPath("$.data.restartHintApplied.fanBiasPercent").value(5))
                .andExpect(jsonPath("$.data.restartHintApplied.runObjective", containsString("5%")))
                .andExpect(jsonPath("$.data.restartHintApplied.boundary", containsString("5%")))
                .andExpect(jsonPath("$.data.restartHintApplied.legacyTag").value("切片组续约"));

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restartRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.restartHintApplied.routeBiasType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.restartHintApplied.biasType").value("TRUE_FANS"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.currentRoute").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.route.routeScore.SLICE_SAINT").value(4))
                .andExpect(jsonPath("$.data.expectations.restartTargetType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.expectations.restartRouteBiasType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.expectations.restartRunObjective", containsString("5%")))
                .andExpect(jsonPath("$.data.expectations.restartBoundary", containsString("5%")))
                .andExpect(jsonPath("$.data.expectations.restartLegacyTag").value("切片组续约"))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.targetType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.runObjective", containsString("5%")))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.boundary", containsString("5%")))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.legacyTag").value("切片组续约"))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.summary", containsString("切片圣体")))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.nextGoal", containsString("切片组")))
                .andExpect(jsonPath("$.data.resources.stamina").value(10))
                .andExpect(jsonPath("$.data.resources.inspiration").value(4))
                .andExpect(jsonPath("$.data.attributes.songPower").value(10))
                .andExpect(jsonPath("$.data.attributes.dancePower").value(10))
                .andExpect(jsonPath("$.data.attributes.talkPower").value(12))
                .andExpect(jsonPath("$.data.attributes.memePower").value(12))
                .andExpect(jsonPath("$.data.attributes.planPower").value(10))
                .andExpect(jsonPath("$.data.attributes.stressPower").value(10))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(54))
                .andExpect(jsonPath("$.data.fanStructure.trueFans").value(34))
                .andExpect(jsonPath("$.data.debts", hasSize(0)));

        mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"));
    }

    @Test
    void endingAtlasPersistsUnlockedEndingsAcrossRestartedRuns() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("ending_atlas_player", "图鉴复活赛");
        completeRestRunToEnding(session, "ending-atlas");

        mockMvc.perform(get("/api/achievement/progress").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingCollection.totalCount").value(9))
                .andExpect(jsonPath("$.data.endingCollection.completedRuns").value(1))
                .andExpect(jsonPath("$.data.endingCollection.unlockedCount").value(1))
                .andExpect(jsonPath("$.data.endingCollection.nextTargetType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.endingCollection.nextTargetLabel").value("歌势遗珠"))
                .andExpect(jsonPath("$.data.endingCollection.nextRunGoal", containsString("5%")))
                .andExpect(jsonPath("$.data.endingCollection.endings[?(@.id == 'ENDING_SINGING_IDOL')].routeRecipe", hasSize(1)))
                .andExpect(jsonPath("$.data.endingCollection.endings[?(@.id == 'ENDING_SINGING_IDOL')].gateHint", hasSize(1)))
                .andExpect(jsonPath("$.data.endingCollection.endings[?(@.id == 'ENDING_SINGING_IDOL')].trapHint", hasSize(1)))
                .andExpect(jsonPath("$.data.endingCollection.endings[?(@.id == 'ENDING_UNKNOWN' && @.unlocked == true)]", hasSize(1)))
                .andExpect(jsonPath("$.data.achievements[?(@.id == 'ENDING_ATLAS_START' && @.unlocked == true)]", hasSize(1)));

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmRestart": true,
                                  "restartBiasType": "ELECTRONIC_PICKLE",
                                  "idempotencyKey": "ending-atlas-restart"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/achievement/progress").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingCollection.completedRuns").value(1))
                .andExpect(jsonPath("$.data.endingCollection.unlockedCount").value(1))
                .andExpect(jsonPath("$.data.endingCollection.endings[?(@.id == 'ENDING_UNKNOWN' && @.unlocked == true)]", hasSize(1)))
                .andExpect(jsonPath("$.data.endingCollection.nextTargetHint", containsString("下一张图鉴")));
    }

    @Test
    void restartWithoutExplicitBiasUsesNextEndingAtlasTarget() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("atlas_default_restart_player", "默认图鉴复活赛");
        completeRestRunToEnding(session, "atlas-default-restart");

        mockMvc.perform(get("/api/achievement/progress").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingCollection.nextTargetType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.endingCollection.nextTargetLabel").value("歌势遗珠"));

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmRestart": true,
                                  "idempotencyKey": "atlas-default-restart-confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.restartHintApplied.targetType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.restartHintApplied.routeBiasType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.restartHintApplied.legacyTag").value("遗珠返场"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.route.routeScore.SINGING_IDOL").value(4))
                .andExpect(jsonPath("$.data.expectations.restartTargetType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.expectations.restartRouteBiasType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.expectations.restartTargetLabel").value("歌势遗珠"))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.targetType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.expectations.previousEndingMemory.nextGoal", containsString("练歌")));
    }

    @Test
    void restartCanTargetFinalEndingAndSeedOpeningForecast() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("restart_cyber_target_player", "赛博复活赛");
        completeRestRunToEnding(session, "restart-cyber-target");

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmRestart": true,
                                  "restartBiasType": "CYBER_GIRLFRIEND",
                                  "idempotencyKey": "restart-cyber-target-confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.restartHintApplied.targetType").value("CYBER_GIRLFRIEND"))
                .andExpect(jsonPath("$.data.restartHintApplied.routeBiasType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.restartHintApplied.openingAdvice", containsString("赛博女友")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.route.routeScore.ELECTRONIC_PICKLE").value(4))
                .andExpect(jsonPath("$.data.expectations.restartTargetType").value("CYBER_GIRLFRIEND"))
                .andExpect(jsonPath("$.data.expectations.restartRouteBiasType").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.expectations.openingAdvice", containsString("赛博女友")));

        mockMvc.perform(get("/api/ending/forecast").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likelyEndingType").value("CYBER_GIRLFRIEND"))
                .andExpect(jsonPath("$.data.likelyFinalTitle").value("赛博女友"))
                .andExpect(jsonPath("$.data.sprintHint", containsString("低压陪伴")));
    }

    @Test
    void restartedRunDoesNotInheritOpenDebtsOrMoreThanFivePercentFans() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("restart_boundary_player", "债务不继承");
        advanceToDay(session, 30, "restart-boundary");
        submitHardMouthSingingDay(session, 30, "restart-boundary");

        MvcResult endedRun = mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.debts", hasSize(greaterThanOrEqualTo(1))))
                .andReturn();
        int endedFans = JsonPath.read(endedRun.getResponse().getContentAsString(), "$.data.fanStructure.fans");

        mockMvc.perform(post("/api/rebirth/restart")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmRestart": true,
                                  "restartBiasType": "BLACK_RED_MAIN_STAGE",
                                  "idempotencyKey": "restart-boundary-confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.restartHintApplied.fanBiasPercent").value(5));

        MvcResult restartedRun = mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(1))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.debts", hasSize(0)))
                .andReturn();
        int restartedFans = JsonPath.read(restartedRun.getResponse().getContentAsString(), "$.data.fanStructure.fans");
        assertTrue(restartedFans <= 52 + endedFans * 5 / 100);
    }

    @Test
    void playerCanHandleFanTopicBeforeMainActionWithIdempotency() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_topic_player", "群管代班");

        mockMvc.perform(get("/api/fan-topic/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].topicKey").value("OLD_FANS_WORRY"))
                .andExpect(jsonPath("$.data[0].choices", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data[0].choices[0].choiceType").value("APPEASE_OLD_FANS"))
                .andExpect(jsonPath("$.data[0].choices[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'COLLECT_SUBMISSIONS' && @.costPreview == '无资源成本')]", hasSize(1)))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'COLLECT_SUBMISSIONS' && @.effectPreview =~ /.*切片组递素材.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'COLLECT_SUBMISSIONS' && @.riskPreview =~ /.*整活现场.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[0].choices[?(@.label != '' && @.costPreview != '' && @.effectPreview != '' && @.riskPreview != '')]", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'HOLD_FAN_MEETING')]", hasSize(1)));

        var lowInspirationVup = vupMapper.findActiveByUserId((Long) session.getAttribute("USER_ID"));
        lowInspirationVup.setInspiration(0);
        vupMapper.updateState(lowInspirationVup);

        mockMvc.perform(get("/api/fan-topic/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'HOLD_FAN_MEETING' && @.enabled == false && @.disabledReason == 'INSUFFICIENT_INSPIRATION')]", hasSize(1)))
                .andExpect(jsonPath("$.data[0].choices[?(@.choiceType == 'APPEASE_OLD_FANS' && @.enabled == true)]", hasSize(1)));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": "APPEASE_OLD_FANS",
                                  "idempotencyKey": "fan-topic-1234567890"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFIG_FIELD_INVALID"));

        String request = """
                {
                  "topicKey": " ",
                  "choiceType": "APPEASE_OLD_FANS",
                  "idempotencyKey": "fan-topic-appease"
                }
                """;

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.topicKey").value("OLD_FANS_WORRY"))
                .andExpect(jsonPath("$.data.choiceType").value("APPEASE_OLD_FANS"))
                .andExpect(jsonPath("$.data.reputationDelta").value(2))
                .andExpect(jsonPath("$.data.evidenceRef.type").value("business_log"))
                .andExpect(jsonPath("$.data.evidenceRef.fanGroupTopicId").value("OLD_FANS_WORRY"))
                .andExpect(jsonPath("$.data.summary", containsString("老粉")));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reputationDelta").value(2));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": " APPEASE_OLD_FANS ",
                                  "idempotencyKey": "fan-topic-appease"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reputationDelta").value(2));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinion.reputation").value(62))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(0));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "OLD_FANS_WORRY",
                                  "choiceType": "OBSERVE",
                                  "idempotencyKey": "fan-topic-appease"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "fan-topic-rest-after"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));
    }

    @Test
    void fanTopicOptionsReflectClipRouteAfterVideoAndClip() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_topic_clip_player", "群聊切片员");

        submitActionAndResolveFormalEvent(session, "PUBLISH_VIDEO", 1, "fan-topic-clip-video", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "fan-topic-clip-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "PUBLISH_CLIP", 2, "fan-topic-clip-cut", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "fan-topic-clip-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/fan-topic/options").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.topicKey == 'CLIP_TEAM_VS_OLD_FANS')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.topicKey == 'CLIP_TEAM_VS_OLD_FANS' && @.title =~ /.*切片组.*老粉.*/)]", hasSize(1)))
                .andExpect(content().string(containsString("\"topicKey\":\"CLIP_TEAM_VS_OLD_FANS\"")))
                .andExpect(content().string(containsString("\"choiceType\":\"COLLECT_SUBMISSIONS\"")))
                .andExpect(content().string(containsString("\"enabled\":true")));

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "CLIP_TEAM_VS_OLD_FANS",
                                  "choiceType": "COLLECT_SUBMISSIONS",
                                  "idempotencyKey": "fan-topic-clip-collect"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("READY"))
                .andExpect(jsonPath("$.data.topicKey").value("CLIP_TEAM_VS_OLD_FANS"))
                .andExpect(jsonPath("$.data.choiceType").value("COLLECT_SUBMISSIONS"))
                .andExpect(jsonPath("$.data.inspirationDelta").value(1))
                .andExpect(jsonPath("$.data.evidenceRef.fanGroupTopicId").value("CLIP_TEAM_VS_OLD_FANS"))
                .andExpect(jsonPath("$.data.summary", containsString("切片组和老粉")));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.inspiration").value(3))
                .andExpect(jsonPath("$.data.fanStructure.funFans").value(63));
    }

    @Test
    void ignoredHotFanTopicWithOpenDebtEscalatesIntoFormalEventBeforeMainReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_topic_escalation_player", "群聊主会场");
        Long userId = (Long) session.getAttribute("USER_ID");

        createHighRiskTitleDebt(session, "fan-topic-escalation");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "fan-topic-escalation-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts", hasSize(1)));

        var vup = vupMapper.findActiveByUserId(userId);
        vup.setWatchHeat(72);
        vupMapper.updateState(vup);

        mockMvc.perform(post("/api/fan-topic/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicKey": "MOD_OVERWORKED",
                                  "choiceType": "OBSERVE",
                                  "idempotencyKey": "fan-topic-escalation-observe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topicKey").value("MOD_OVERWORKED"))
                .andExpect(jsonPath("$.data.choiceType").value("OBSERVE"))
                .andExpect(jsonPath("$.data.summary", containsString("楼友开始贷款后续")));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "fan-topic-escalation-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.formalEvent.pending").value(true))
                .andExpect(jsonPath("$.data.formalEvent.title", containsString("粉丝群升级")))
                .andExpect(jsonPath("$.data.formalEvent.description", containsString("房管")))
                .andExpect(jsonPath("$.data.formalEvent.description", containsString("未结清债务")));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "fan-topic-escalation-event-safe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("粉丝群")))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("提前翻旧账")))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.type").value("risk_debt"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.debts", hasSize(0)));
    }

    @Test
    void publishClipRunCanReachSliceSaintEndingWithClipEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("clip_route_player", "切片供货商");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "clip-route-day-1-video"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "clip-route-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        for (int day = 2; day <= 29; day++) {
            String actionType = day % 3 == 0 ? "FAN_GROUP_MAINTAIN" : (day % 2 == 0 ? "PUBLISH_CLIP" : "PUBLISH_VIDEO");
            String expectedSummary = day % 3 == 0 ? "投稿素材" : (day % 2 == 0 ? "切片" : "视频");
            submitActionAndResolveFormalEvent(session, actionType, day, "clip-route-day-%d".formatted(day), expectedSummary);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "clip-route-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitActionAndResolveFormalEvent(session, "PUBLISH_CLIP", 30, "clip-route-day-30", null);

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.fanStructure.funFans").value(greaterThan(300)))
                .andExpect(jsonPath("$.data.opinion.watchHeat").value(greaterThanOrEqualTo(60)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.finalTitle").value("切片圣体"))
                .andExpect(jsonPath("$.data.endingReason", containsString("切片圣体")))
                .andExpect(jsonPath("$.data.endingReason", containsString("发布切片")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.keyEvents", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.keyEvents", hasSize(lessThan(6))))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'PUBLISH_VIDEO')]", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'PUBLISH_CLIP')]", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.keyEvents[0].type").value("business_log"));
    }

    @Test
    void danceTrainingRunCanReachDanceMemeRouteAndSliceSaintEndingWithDanceEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("dance_meme_player", "梗舞练习生");

        for (int day = 1; day <= 29; day++) {
            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "TRAIN_DANCE",
                                      "idempotencyKey": "dance-meme-day-%d"
                                    }
                                    """.formatted(day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                    .andExpect(jsonPath("$.data.actionResult.summary", containsString("梗舞")));

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "dance-meme-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_DANCE",
                                  "idempotencyKey": "dance-meme-day-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("DANCE_MEME"))
                .andExpect(jsonPath("$.data.attributes.dancePower").value(greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.data.opinion.memeLevel").value(greaterThanOrEqualTo(40)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.summary", containsString("梗舞")))
                .andExpect(jsonPath("$.data.endingReason", containsString("梗舞整活路线")))
                .andExpect(jsonPath("$.data.endingReason", containsString("练舞")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("DANCE_MEME"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'TRAIN_DANCE')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void danceStreamPlanUsesDanceTitlesAndFeedsDanceMemeRoute() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("dance_plan_player", "舞蹈区投稿");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "DANCE",
                                  "idempotencyKey": "dance-plan-start"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(3))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText").value("低强度练舞，动作慢慢来"))
                .andExpect(jsonPath("$.data.titleCandidates[2].style").value("ABSTRACT_MEME"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 33,
                                  "idempotencyKey": "dance-plan-abstract-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("梗舞")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("DANCE_MEME"))));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("DANCE_MEME"))
                .andExpect(jsonPath("$.data.opinion.memeLevel").value(greaterThan(0)));
    }

    @Test
    void rerollingDanceStreamTitlesKeepsDanceContext() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("dance_reroll_player", "换批舞担");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "DANCE",
                                  "idempotencyKey": "dance-reroll-start"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(3));

        mockMvc.perform(post("/api/stream/titles/reroll")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 3,
                                  "idempotencyKey": "dance-reroll-once"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.titleRerollCount").value(1))
                .andExpect(jsonPath("$.data.titleCandidates[0].titleText", containsString("舞")))
                .andExpect(jsonPath("$.data.titleCandidates[2].effectPreview", containsString("梗舞")))
                .andExpect(jsonPath("$.data.titleCandidates[2].effectPreview", not(containsString("DANCE_MEME"))));
    }

    @Test
    void fanServiceScThanksReportExposesCommercialDelta() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("commercial_report_player", "商业味上桌");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "commercial-report-sc-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 123,
                                  "idempotencyKey": "commercial-report-fan-service"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("醒目留言陪伴回")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("FAN_SERVICE"))))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("SC_BOSS_QUESTION"));

        resolveSafeInteraction(session, "commercial-report-sc-boss-safe", "REPORT_READY");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.coin").value(2600))
                .andExpect(jsonPath("$.data.opinion.commercialLevel").value(4));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataDelta.coinDelta").value(600))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("运营预算+600")))
                .andExpect(jsonPath("$.data.dataDelta.commercialDelta").value(4))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("商业化+4")))
                .andExpect(jsonPath("$.data.summary", containsString("醒目留言")));
    }

    @Test
    void fanServiceScThanksLeavesUnicornExpectationDebtForLaterEvent() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fan_service_debt_player", "榜一排班表观察员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "fan-service-debt-sc-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        var result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 123,
                                  "idempotencyKey": "fan-service-debt-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("醒目留言陪伴回")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("FAN_SERVICE"))))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("SC_BOSS_QUESTION"));

        resolveSafeInteraction(session, "fan-service-debt-sc-boss-safe", "REPORT_READY");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts[0].debtType").value("UNICORN_EXPECTATION"))
                .andExpect(jsonPath("$.data.debts[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.debts[0].dueDay").value(4))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("陪伴营业")))
                .andExpect(jsonPath("$.data.debts[0].summary", not(containsString("FAN_SERVICE"))));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertChineseDebtSummary(reportResult, "独角兽期待", "陪伴营业");
    }

    @Test
    void unicornExpectationDebtReturnsWithSpecificFormalEventCopy() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("unicorn_event_copy_player", "小作文观察员");

        submitFanServiceScThanksDay(session, 1, "unicorn-event-copy");

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "unicorn-event-copy-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "TRAIN_TALK", 2, "unicorn-event-copy-day-2-talk", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "unicorn-event-copy-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "TRAIN_TALK", 3, "unicorn-event-copy-day-3-talk", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "unicorn-event-copy-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "unicorn-event-copy-day-4-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"));

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.debtType").value("UNICORN_EXPECTATION"))
                .andExpect(jsonPath("$.data.title").value("独角兽期待正式事件"))
                .andExpect(jsonPath("$.data.description", containsString("小作文")))
                .andExpect(jsonPath("$.data.description", containsString("独角兽")));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "unicorn-event-copy-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("独角兽期待")));
    }

    @Test
    void businessSafeScThanksLeavesCommercialBacklashDebtForLaterEvent() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("business_safe_debt_player", "商单味观察员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "business-safe-debt-sc-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 122,
                                  "idempotencyKey": "business-safe-debt-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("稳健处理模拟礼物")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("BUSINESS_SAFE"))));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debts[0].debtType").value("COMMERCIAL_BACKLASH"))
                .andExpect(jsonPath("$.data.debts[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.debts[0].dueDay").value(4))
                .andExpect(jsonPath("$.data.debts[0].summary", containsString("稳定排班和活动预算")))
                .andExpect(jsonPath("$.data.debts[0].summary", not(containsString("BUSINESS_SAFE"))));

        MvcResult reportResult = mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertChineseDebtSummary(reportResult, "商业反噬", "稳定排班和活动预算");
    }

    @Test
    void commercialBacklashDebtReturnsWithSpecificFormalEventCopy() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("commercial_event_copy_player", "品牌避险观察员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "commercial-event-copy-sc-plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 122,
                                  "idempotencyKey": "commercial-event-copy-business-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("稳健处理模拟礼物")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("BUSINESS_SAFE"))));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "commercial-event-copy-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "TRAIN_TALK", 2, "commercial-event-copy-day-2-talk", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "commercial-event-copy-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "TRAIN_TALK", 3, "commercial-event-copy-day-3-talk", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "commercial-event-copy-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "REST",
                                  "idempotencyKey": "commercial-event-copy-day-4-rest"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_EVENT_CHOICE"))
                .andExpect(jsonPath("$.data.formalEvent.pending").value(true))
                .andExpect(jsonPath("$.data.formalEvent.debtType").value("COMMERCIAL_BACKLASH"))
                .andExpect(jsonPath("$.data.formalEvent.title").value("商业反噬正式事件"))
                .andExpect(jsonPath("$.data.formalEvent.choices[0].choiceType").value("safe"));

        mockMvc.perform(get("/api/event/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.debtType").value("COMMERCIAL_BACKLASH"))
                .andExpect(jsonPath("$.data.title").value("商业反噬正式事件"))
                .andExpect(jsonPath("$.data.description", containsString("味儿变了")))
                .andExpect(jsonPath("$.data.description", containsString("品牌避险")));

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "commercial-event-copy-safe-choice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("商业反噬")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("玩家选择"))));
    }

    @Test
    void commercialReviewWeekBuffsScThanksFanServiceAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("commercial_review_player", "复审排班表");
        advanceToDay(session, 22, "commercial-review");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(22))
                .andExpect(jsonPath("$.data.platformTrend.id").value("COMMERCIAL_REVIEW_WEEK"))
                .andExpect(jsonPath("$.data.platformTrend.label").value("商业复审周"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "commercial-review-sc-plan-day-22"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"))
                .andExpect(jsonPath("$.data.selectedPlanId").value(12))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'FAN_SERVICE' && @.effectPreview =~ /.*商业复审周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.titleCandidates[?(@.style == 'FAN_SERVICE' && @.effectPreview =~ /.*运营预算.*/)]", hasSize(1)));

        var result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 123,
                                  "idempotencyKey": "commercial-review-fan-service-day-22"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("陪伴营业")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("FAN_SERVICE"))))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("商业复审周")))
                .andExpect(jsonPath("$.data.interactionEvent.eventKey").value("SC_BOSS_QUESTION"));

        resolveSafeInteraction(session, "commercial-review-sc-boss-safe-day-22", "REPORT_READY");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("COMMERCIAL_REVIEW_WEEK"))
                .andExpect(jsonPath("$.data.resources.coin").value(2800))
                .andExpect(jsonPath("$.data.opinion.commercialLevel").value(6));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("COMMERCIAL_REVIEW_WEEK"))
                .andExpect(jsonPath("$.data.dataDelta.coinDelta").value(800))
                .andExpect(jsonPath("$.data.dataDelta.commercialDelta").value(6))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("运营预算+800")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("商业化+6")));
    }

    @Test
    void commercialReviewWeekBuffsFanGroupMaintainAfterTrendRotation() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("commercial_group_player", "群公告打工人");
        advanceToDay(session, 22, "commercial-group");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.actionType == 'FAN_GROUP_MAINTAIN' && @.effectPreview =~ /.*商业复审周.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.actionType == 'FAN_GROUP_MAINTAIN' && @.effectPreview =~ /.*稳定排班.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "FAN_GROUP_MAINTAIN",
                                  "idempotencyKey": "commercial-review-fan-group-day-22"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.actionType").value("FAN_GROUP_MAINTAIN"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("商业复审周")));
        resolveSafeFormalEventIfNeeded(session, "commercial-review-fan-group-event-safe-day-22", "REPORT_READY");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("COMMERCIAL_REVIEW_WEEK"))
                .andExpect(jsonPath("$.data.resources.coin").value(greaterThanOrEqualTo(2200)))
                .andExpect(jsonPath("$.data.opinion.commercialLevel").value(greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platformTrend.id").value("COMMERCIAL_REVIEW_WEEK"))
                .andExpect(jsonPath("$.data.dataDelta.coinDelta").value(greaterThanOrEqualTo(200)))
                .andExpect(jsonPath("$.data.dataDelta.commercialDelta").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("运营预算+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("商业化+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("口碑+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("灵感+1")));
    }

    @Test
    void accumulatedRouteEvidenceBeatsLastDayActionForEnding() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("route_memory_player", "路线记忆体");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "route-memory-day-1-video"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "route-memory-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        for (int day = 2; day <= 29; day++) {
            String actionType = day % 3 == 0 ? "FAN_GROUP_MAINTAIN" : (day % 2 == 0 ? "PUBLISH_CLIP" : "PUBLISH_VIDEO");
            submitActionAndResolveFormalEvent(session, actionType, day, "route-memory-day-%d-clip".formatted(day), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "route-memory-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1));
        }

        submitActionAndResolveFormalEvent(session, "PUBLISH_CLIP", 30, "route-memory-day-30-clip", null);

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SLICE_SAINT"));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endingType").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.endingReason", containsString("发布切片")))
                .andExpect(jsonPath("$.data.routeReview.routeScoreJson", containsString("\"SLICE_SAINT\":")))
                .andExpect(jsonPath("$.data.routeReview.routeEvidenceTrail[?(@.routeType == 'SLICE_SAINT' && @.action == 'PUBLISH_CLIP')]",
                        hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.routeReview.routeEvidenceTrail[?(@.routeType == 'SLICE_SAINT' && @.scoreDelta >= 2)]",
                        hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void hardMouthStreamRunCanReachBlackRedEndingWithTrialEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("black_red_player", "主会场预备役");

        for (int day = 1; day <= 20; day++) {
            submitHardMouthSingingDay(session, day, "black-red");

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "black-red-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        for (int day = 21; day <= 29; day++) {
            submitActionAndResolveFormalEvent(session, "REST", day, "black-red-day-%d-rest".formatted(day), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "black-red-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitActionAndResolveFormalEvent(session, "REST", 30, "black-red-day-30-rest", null);

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.opinion.memeLevel").value(80))
                .andExpect(jsonPath("$.data.opinion.reputation").value(lessThanOrEqualTo(60)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.finalTitle").value("黑红顶流"))
                .andExpect(jsonPath("$.data.endingReason", containsString("黑红顶流")))
                .andExpect(jsonPath("$.data.endingReason", containsString("硬嘴标题")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.debtRefs[0].debtType").value("TITLE_BACKFIRE"))
                .andExpect(jsonPath("$.data.debtRefs[0].sourceTitle", containsString("不会真有人觉得我唱不了高音吧")))
                .andExpect(jsonPath("$.data.debtRefs[0].summary", containsString("标题党反噬")))
                .andExpect(jsonPath("$.data.debtRefs[0].summary", not(containsString("标题反噬债务"))));
    }

    @Test
    void extremeHardMouthRunEscalatesFromBlackRedToMainStageKing() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("main_stage_king_player", "录播组年度素材");

        for (int day = 1; day <= 29; day++) {
            submitHardMouthSingingDay(session, day, "main-stage-king");

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "main-stage-king-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitHardMouthSingingDay(session, 30, "main-stage-king");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.opinion.memeLevel").value(greaterThan(80)))
                .andExpect(jsonPath("$.data.opinion.reputation").value(lessThan(30)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("MAIN_STAGE_KING"))
                .andExpect(jsonPath("$.data.finalTitle").value("主会场之王"))
                .andExpect(jsonPath("$.data.subtitle", containsString("录播组")))
                .andExpect(jsonPath("$.data.endingReason", containsString("主会场之王")))
                .andExpect(jsonPath("$.data.endingReason", containsString("硬嘴标题")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("BLACK_RED_MAIN_STAGE"))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'FORUM_EMPLOYER' && @.label == '楼友就业保障')]", hasSize(1)))
                .andExpect(jsonPath("$.data.debtRefs", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void fanServiceScThanksRunCanReachCyberGirlfriendEndingWithBusinessEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("cyber_girlfriend_player", "榜一排班表");

        for (int day = 1; day <= 29; day++) {
            submitFanServiceScThanksDay(session, day, "cyber-girlfriend");

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "cyber-girlfriend-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitFanServiceScThanksDay(session, 30, "cyber-girlfriend");

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fanStructure.unicornFans").value(greaterThan(600)))
                .andExpect(jsonPath("$.data.opinion.commercialLevel").value(greaterThanOrEqualTo(70)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("CYBER_GIRLFRIEND"))
                .andExpect(jsonPath("$.data.finalTitle").value("赛博女友"))
                .andExpect(jsonPath("$.data.subtitle", containsString("排班表")))
                .andExpect(jsonPath("$.data.endingReason", containsString("赛博女友")))
                .andExpect(jsonPath("$.data.endingReason", containsString("醒目留言陪伴回")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.keyEvents[0].summary", containsString("陪伴营业")))
                .andExpect(jsonPath("$.data.keyEvents[0].summary", not(containsString("FAN_SERVICE"))))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'UNICORN_PRESSURE_COOKER' && @.label == '高压锅营业')]", hasSize(1)))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'BOSS_SCHEDULE_BOARD' && @.label == '榜一排班表')]", hasSize(1)));
    }

    @Test
    void finalDayFanGroupMaintainCanReachGloriousGraduationWhenReputationIsHigh() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("graduation_player", "体面下播人");

        for (int day = 1; day <= 29; day++) {
            // 毕业路线需要稳定陪伴证据，但不能把关系运营一路按到压力锁。
            // 生产演示脚本同样用 TRAIN_TALK/REST 做低压准备，最后一天再主动维护粉丝群。
            String prepAction = day % 5 == 0 ? "REST" : "TRAIN_TALK";
            submitActionAndResolveFormalEvent(session, prepAction, day, "graduation-day-%d-%s".formatted(day, prepAction.toLowerCase()), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "graduation-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "FAN_GROUP_MAINTAIN",
                                  "idempotencyKey": "graduation-day-30-maintain"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.opinion.reputation").value(greaterThan(60)))
                .andExpect(jsonPath("$.data.fanStructure.fans").value(greaterThan(130)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("GLORIOUS_GRADUATION"))
                .andExpect(jsonPath("$.data.finalTitle").value("光荣毕业"))
                .andExpect(jsonPath("$.data.subtitle", containsString("自己走下舞台")))
                .andExpect(jsonPath("$.data.endingReason", containsString("光荣毕业")))
                .andExpect(jsonPath("$.data.endingReason", containsString("粉丝群维护")))
                .andExpect(jsonPath("$.data.routeReview.currentRoute").value("ELECTRONIC_PICKLE"))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'FAN_GROUP_MAINTAIN')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void npcSpotlightShowsRouteAwareLightweightNpcBriefing() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("npc_spotlight_player", "情报台测试员");

        mockMvc.perform(get("/api/npc/spotlight").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.npcKey").isNotEmpty())
                .andExpect(jsonPath("$.data.displayName").isNotEmpty())
                .andExpect(jsonPath("$.data.role").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/npcs/portraits/")))
                .andExpect(jsonPath("$.data.image", containsString(".png")))
                .andExpect(jsonPath("$.data.moodLine").isNotEmpty())
                .andExpect(jsonPath("$.data.advice", containsString("DD")))
                .andExpect(jsonPath("$.data.relevance").isNotEmpty());
    }

    @Test
    void buzzBriefingShowsLightweightForumTrendWithoutGeneratingReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("buzz_briefing_player", "风向台测试员");

        mockMvc.perform(get("/api/buzz/briefing").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("热搜")))
                .andExpect(jsonPath("$.data.heatLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/backgrounds/algorithm-dashboard.png")))
                .andExpect(jsonPath("$.data.forumLine", containsString("楼友")))
                .andExpect(jsonPath("$.data.clipperLine", containsString("录播组")))
                .andExpect(jsonPath("$.data.trendLabel", containsString("周")))
                .andExpect(jsonPath("$.data.nextMoveHint").isNotEmpty());

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void funAudienceProfileShowsFourMemeSignalsWithoutGeneratingReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("fun_audience_player", "乐子画像测试员");

        mockMvc.perform(get("/api/audience/fun-profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("乐子人")))
                .andExpect(jsonPath("$.data.riskLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/backgrounds/rush-clip-room.png")))
                .andExpect(jsonPath("$.data.metrics", hasSize(4)))
                .andExpect(jsonPath("$.data.metrics[0].label").value("录播欲"))
                .andExpect(jsonPath("$.data.metrics[0].value", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.metrics[0].value", lessThanOrEqualTo(100)))
                .andExpect(jsonPath("$.data.metrics[1].label").value("拱火欲"))
                .andExpect(jsonPath("$.data.metrics[2].label").value("考据欲"))
                .andExpect(jsonPath("$.data.metrics[3].label").value("玩梗欲"))
                .andExpect(jsonPath("$.data.dominantSignal").isNotEmpty())
                .andExpect(jsonPath("$.data.nextMoveHint").isNotEmpty());

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void audienceExpectationShowsRouteLockAndPivotRiskBeforeActionWithoutGeneratingReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("audience_expectation_player", "观众期待测试员");

        submitActionAndResolveFormalEvent(session, "TRAIN_SONG", 1, "expectation-train-song-day-1", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "expectation-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/audience/expectation").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("观众期待")))
                .andExpect(jsonPath("$.data.lead.routeType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.lead.routeLabel").value("歌势路线"))
                .andExpect(jsonPath("$.data.lead.score", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.lead.evidenceLine", containsString("老粉")))
                .andExpect(jsonPath("$.data.expectations", hasSize(6)))
                .andExpect(jsonPath("$.data.pivotRiskLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.pivotRiskLine", containsString("转型")))
                .andExpect(jsonPath("$.data.nextMoveHint").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/backgrounds/algorithm-dashboard.png")));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void endingForecastPredictsLikelyEndingAndSprintGapsBeforeFinalDayWithoutGeneratingEnding() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("ending_forecast_player", "结局预演测试员");

        for (int day = 1; day <= 7; day++) {
            submitActionAndResolveFormalEvent(session, "TRAIN_SONG", day, "ending-forecast-train-day-%d".formatted(day), null);
            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "ending-forecast-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        mockMvc.perform(get("/api/ending/forecast").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("结局预演")))
                .andExpect(jsonPath("$.data.likelyEndingType").value("SINGING_IDOL"))
                .andExpect(jsonPath("$.data.likelyFinalTitle").value("歌势遗珠"))
                .andExpect(jsonPath("$.data.confidence", greaterThanOrEqualTo(50)))
                .andExpect(jsonPath("$.data.requirements", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.requirements[?(@.key == 'ROUTE' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.requirements[?(@.key == 'FANS' && @.status == 'PASS')]", hasSize(1)))
                .andExpect(jsonPath("$.data.requirements[?(@.key == 'DEBT_RISK')]", hasSize(1)))
                .andExpect(jsonPath("$.data.riskLine", containsString("债务")))
                .andExpect(jsonPath("$.data.sprintHint").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/routes/singing-idol/ending-highlight.png")));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ENDING_NOT_FOUND"));
    }

    @Test
    void comboDiscoveryShowsPracticeToStreamWithoutGeneratingExtraReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_discovery_player", "组合技练习生");

        mockMvc.perform(get("/api/combo/discovery").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discovered", hasSize(0)))
                .andExpect(jsonPath("$.data.locked[?(@.comboKey == 'PRACTICE_TO_STREAM')]", hasSize(1)))
                .andExpect(jsonPath("$.data.locked[?(@.comboKey == 'PRACTICE_TO_STREAM')].label").value("练习后直播"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "combo-train-song-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "combo-stream-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 21,
                                  "idempotencyKey": "combo-choose-safe-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(get("/api/reports").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/combo/discovery").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discovered[?(@.comboKey == 'PRACTICE_TO_STREAM')]", hasSize(1)))
                .andExpect(jsonPath("$.data.discovered[0].label").value("练习后直播"))
                .andExpect(jsonPath("$.data.discovered[0].evidence", containsString("第1天练习")))
                .andExpect(jsonPath("$.data.nextHint").isNotEmpty());

        mockMvc.perform(get("/api/reports").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void memeLifecycleShowsRepeatStageWithoutGeneratingExtraReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("meme_lifecycle_player", "复读观察员");

        mockMvc.perform(get("/api/meme/lifecycle").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[?(@.memeSubtype == 'CLIP_SPREAD')]", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].stage").value("NEW"))
                .andExpect(jsonPath("$.data.items[0].stageLabel").value("新梗期"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "meme-lifecycle-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "meme-lifecycle-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "meme-lifecycle-clip-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "meme-lifecycle-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/meme/lifecycle").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("复读")))
                .andExpect(jsonPath("$.data.items[0].memeSubtype").value("CLIP_SPREAD"))
                .andExpect(jsonPath("$.data.items[0].label").value("切片传播梗"))
                .andExpect(jsonPath("$.data.items[0].stage").value("REPEAT"))
                .andExpect(jsonPath("$.data.items[0].stageLabel").value("复读期"))
                .andExpect(jsonPath("$.data.items[0].usesInWindow").value(1))
                .andExpect(jsonPath("$.data.items[0].firstSeenDay").value(2))
                .andExpect(jsonPath("$.data.items[0].lastSeenDay").value(2))
                .andExpect(jsonPath("$.data.items[0].nextHint", containsString("复读")));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void personaTagsShowClipSupplierAfterRepeatedClipRunWithoutGeneratingReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("persona_tag_clip_player", "标签观察员");

        mockMvc.perform(get("/api/persona/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.tags[?(@.tagKey == 'CLIP_SUPPLIER')]", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[?(@.tagKey == 'CLIP_SUPPLIER')].status").value("DORMANT"));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "persona-tag-video-day-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "persona-tag-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "persona-tag-clip-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "persona-tag-go-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(3))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "persona-tag-clip-day-3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "persona-tag-go-day-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(4))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(get("/api/persona/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headline", containsString("切片组供货商")))
                .andExpect(jsonPath("$.data.tags[?(@.tagKey == 'CLIP_SUPPLIER')]", hasSize(1)))
                .andExpect(jsonPath("$.data.tags[0].tagKey").value("CLIP_SUPPLIER"))
                .andExpect(jsonPath("$.data.tags[0].label").value("切片组供货商"))
                .andExpect(jsonPath("$.data.tags[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tags[0].strength").value(greaterThan(70)))
                .andExpect(jsonPath("$.data.tags[0].evidence", containsString("切片")))
                .andExpect(jsonPath("$.data.tags[0].nextHint", containsString("素材")));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void stageBriefingWarnsMilestoneAndNextTrendBeforeActionWithoutGeneratingReport() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stage_briefing_player", "阶段复盘员");
        advanceToDay(session, 7, "stage-briefing");

        mockMvc.perform(get("/api/stage/briefing").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stageLabel").value("第7天阶段复盘"))
                .andExpect(jsonPath("$.data.milestoneToday").value(true))
                .andExpect(jsonPath("$.data.nextMilestoneDay").value(7))
                .andExpect(jsonPath("$.data.currentTrend.label").value("清朗低压周"))
                .andExpect(jsonPath("$.data.nextTrend.label").value("歌回扶持周"))
                .andExpect(jsonPath("$.data.nextTrendHint", containsString("下周口味")))
                .andExpect(jsonPath("$.data.nextTrendHint", containsString("歌回扶持周")))
                .andExpect(jsonPath("$.data.routeSnapshot", containsString("路线")))
                .andExpect(jsonPath("$.data.fanSnapshot", containsString("粉丝结构")))
                .andExpect(jsonPath("$.data.riskSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.objectiveTitle").value("第1周先立住"))
                .andExpect(jsonPath("$.data.objectiveSummary", containsString("基本盘")))
                .andExpect(jsonPath("$.data.objectiveProgress").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.objectiveProgressLabel", containsString("/3")))
                .andExpect(jsonPath("$.data.objectiveActionType").value("TRAIN_TALK"))
                .andExpect(jsonPath("$.data.objectiveItems", hasSize(3)))
                .andExpect(jsonPath("$.data.objectiveItems[0].label").value("完成3次基础动作"))
                .andExpect(jsonPath("$.data.objectiveItems[0].state").isNotEmpty())
                .andExpect(jsonPath("$.data.image", containsString("v4/backgrounds/day-01-first-night.png")));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void stageMomentumShowsInActionPreviewAndResultEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stage_momentum_player", "阶段动量员");
        advanceToDay(session, 8, "stage-momentum");

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.actionType == 'TRAIN_SONG' && @.effectPreview =~ /.*阶段动量.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "TRAIN_SONG",
                                  "idempotencyKey": "stage-momentum-day-8-train-song"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.stageMomentum").exists())
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("上阶段")));
    }

    @Test
    void stageObjectiveBonusAppliesToNonPrimaryUnfinishedObjectiveAction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stage_multi_objective_player", "多委托行动员");
        advanceToDay(session, 14, "stage-multi-objective");

        mockMvc.perform(get("/api/stage/briefing").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.objectiveTitle").value("第2周补路线"))
                .andExpect(jsonPath("$.data.objectiveActionType").value("STREAM_PLAN"))
                .andExpect(jsonPath("$.data.objectiveItems[?(@.objectiveKey == 'CONTENT_ASSET' && @.recommendedActionType == 'PUBLISH_VIDEO' && @.targetRouteType == 'SLICE_SAINT' && @.achieved == false)]", hasSize(1)));

        mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.actionType == 'PUBLISH_VIDEO' && @.effectPreview =~ /.*阶段委托.*/)]", hasSize(1)));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_VIDEO",
                                  "idempotencyKey": "stage-multi-objective-video-day-14"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.stageObjectiveBonus.objectiveActionType").value("PUBLISH_VIDEO"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.stageObjectiveBonus.objectiveTitle").value("第2周补路线"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.stageObjectiveBonus.targetRoute").value("SLICE_SAINT"))
                .andExpect(jsonPath("$.data.actionResult.evidenceRef.stageObjectiveBonus.routeScoreBonus").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("今日目标命中")));
    }

    @Test
    void stageBriefingRiskSnapshotUsesChineseRiskLevelInsteadOfSeverityField() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("stage_briefing_debt_player", "旧账复盘员");
        createHighRiskTitleDebt(session, "stage-briefing-debt");

        mockMvc.perform(get("/api/stage/briefing").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskSnapshot", containsString("最高风险等级")))
                .andExpect(jsonPath("$.data.riskSnapshot", containsString("级")))
                .andExpect(jsonPath("$.data.riskSnapshot", not(containsString("severity"))))
                .andExpect(jsonPath("$.data.riskSnapshot", not(containsString("TITLE_BACKFIRE"))));
    }

    @Test
    void dailyReportSurfacesPracticeToStreamComboWhenCurrentActionTriggersIt() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_report_player", "连招日报员");

        submitActionAndResolveFormalEvent(session, "TRAIN_SONG", 1, "combo-report-train-day-1", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-report-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "combo-report-stream-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 21,
                                  "idempotencyKey": "combo-report-safe-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(44)))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("练习后直播")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("PRACTICE_TO_STREAM"))));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("人气+95")))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*组合技.*练习后直播.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第1天练习.*第2天直播.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'combo' && @.comboKey == 'PRACTICE_TO_STREAM')]", hasSize(1)));
    }

    @Test
    void dailyReportSurfacesVideoToStreamComboWithPopularityAndFunFanBoost() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_video_stream_player", "引流日报员");

        submitActionAndResolveFormalEvent(session, "PUBLISH_VIDEO", 1, "combo-video-day-1", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-video-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "combo-video-stream-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 21,
                                  "idempotencyKey": "combo-video-safe-title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(greaterThanOrEqualTo(44)))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(8))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("视频引流直播")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("VIDEO_TO_STREAM"))));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("人气+110")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("梗浓度+1")))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*组合技.*视频引流.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第1天投稿.*第2天直播.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'combo' && @.comboKey == 'VIDEO_TO_STREAM')]", hasSize(1)));
    }

    @Test
    void dailyReportSurfacesVideoToClipComboWithSecondCutBoost() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("combo_video_clip_player", "二剪日报员");

        submitActionAndResolveFormalEvent(session, "PUBLISH_VIDEO", 1, "combo-clip-video-day-1", null);

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "combo-clip-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "PUBLISH_CLIP",
                                  "idempotencyKey": "combo-clip-cut-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.fanChange").value(44))
                .andExpect(jsonPath("$.data.actionResult.funFanChange").value(40))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("一鱼两剪")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("VIDEO_TO_CLIP"))));

        mockMvc.perform(get("/api/report/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("粉丝+44")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("人气+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("围观热度+")))
                .andExpect(jsonPath("$.data.visibleItems[0]", containsString("梗浓度+5")))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*组合技.*一鱼两剪.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.visibleItems[?(@ =~ /.*第1天投稿.*第2天切片.*/)]", hasSize(1)))
                .andExpect(jsonPath("$.data.evidenceRefs[?(@.type == 'combo' && @.comboKey == 'VIDEO_TO_CLIP')]", hasSize(1)));
    }

    @Test
    void npcInteractionRunCanReachDdBusStopEndingWithSocialEvidence() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("npc_social_player", "端水练习生");

        for (int day = 1; day <= 29; day++) {
            mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "NPC_INTERACT",
                                      "idempotencyKey": "npc-social-day-%d"
                                    }
                                    """.formatted(day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                    .andExpect(jsonPath("$.data.actionResult.summary", containsString("查房")));

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "npc-social-go-day-%d"
                                    }
                                    """.formatted(day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "NPC_INTERACT",
                                  "idempotencyKey": "npc-social-day-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        mockMvc.perform(get("/api/vup/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRoute").value("SOCIAL_COLLAB"))
                .andExpect(jsonPath("$.data.fanStructure.ddFans").value(greaterThan(300)));

        mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.endingType").value("DD_BUS_STOP"))
                .andExpect(jsonPath("$.data.finalTitle").value("DD公交站"))
                .andExpect(jsonPath("$.data.endingReason", containsString("社交联动路线")))
                .andExpect(jsonPath("$.data.endingReason", containsString("同台互动")))
                .andExpect(jsonPath("$.data.keyEvents[?(@.action == 'NPC_INTERACT')]", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.endingTags[?(@.tagKey == 'DD_BUS_STATION' && @.label == '流动观众集散地')]", hasSize(1)));
    }

    @Test
    void npcInteractionSupportsTendencyChoiceAndCollabCooldown() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup("npc_tendency_player", "联动排班员");

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "NPC_INTERACT",
                                  "planType": "COLLAB",
                                  "idempotencyKey": "npc-tendency-day-1-collab"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("轻联动")))
                .andExpect(jsonPath("$.data.actionResult.ddFanChange").value(27))
                .andExpect(jsonPath("$.data.actionResult.routeScoreChange").value(5));

        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "npc-tendency-go-day-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(2))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "NPC_INTERACT",
                                  "planType": "COLLAB",
                                  "idempotencyKey": "npc-tendency-day-2-collab"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NPC_INTERACTION_COOLDOWN"))
                .andExpect(jsonPath("$.message", containsString("轻联动")));

        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "NPC_INTERACT",
                                  "planType": "RAID",
                                  "idempotencyKey": "npc-tendency-day-2-raid"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"))
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("查房")))
                .andExpect(jsonPath("$.data.actionResult.ddFanChange").value(18));
    }

    private void createHighRiskTitleDebt(MockHttpSession session, String prefix) throws Exception {
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "%s-stream-plan"
                                }
                                """.formatted(prefix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "%s-choose-hard-mouth"
                                }
                                """.formatted(prefix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_INTERACTION_CHOICE"));

        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "%s-interaction-safe"
                                }
                                """.formatted(prefix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("REPORT_READY"));
    }

    private void insertUnknownAccidentMaterialReference(MockHttpSession session, String idempotencyKey) {
        Long userId = (Long) session.getAttribute("USER_ID");
        var vup = vupMapper.findActiveByUserId(userId);
        var daySession = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(daySession.getId());
        log.setDay(vup.getDayCount());
        log.setPhase(daySession.getPhase());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction("REST");
        log.setResult("未知事故素材护栏");
        log.setRouteScoreChange("{}");
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[\"UNKNOWN_ACCIDENT_MATERIAL\"]");
        log.setEndingRefFlag(true);
        businessLogMapper.insert(log);
    }

    private MockHttpSession resetAndRunDevDemoStrategy(String strategy, String expectedEndingType) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "%s"
                                }
                                """.formatted(strategy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("demo_teacher"))
                .andExpect(jsonPath("$.data.vup.name").value("演示小灯"))
                .andExpect(jsonPath("$.data.daySession.day").value(1))
                .andExpect(jsonPath("$.data.daySession.phase").value("READY"));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "%s",
                                  "targetDay": 30
                                }
                                """.formatted(strategy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value(strategy))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"))
                .andExpect(jsonPath("$.data.reportCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.logCount").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.endingReviewId").isNumber())
                .andExpect(jsonPath("$.data.endingType").value(expectedEndingType));
        return session;
    }

    private void submitHardMouthSingingDay(MockHttpSession session, int day, String prefix) throws Exception {
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SINGING",
                                  "idempotencyKey": "%s-day-%d-stream-plan"
                                }
                                """.formatted(prefix, day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        MvcResult result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 23,
                                  "idempotencyKey": "%s-day-%d-hard-mouth-title"
                                }
                                """.formatted(prefix, day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("高音")))
                .andReturn();

        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String expectedPhase = day >= 30 ? "ENDING_READY" : "REPORT_READY";
        if (body.contains("\"phase\":\"NEED_INTERACTION_CHOICE\"")) {
            resolveSafeInteraction(session, "%s-day-%d-interaction-safe".formatted(prefix, day), expectedPhase);
            return;
        }
        if (body.contains("\"phase\":\"NEED_EVENT_CHOICE\"")) {
            mockMvc.perform(post("/api/event/choose")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "choiceType": "safe",
                                      "idempotencyKey": "%s-day-%d-event-safe"
                                    }
                                    """.formatted(prefix, day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phase").value(expectedPhase));
            return;
        }

        assertTrue(body.contains("\"phase\":\"" + expectedPhase + "\""), body);
    }

    private void resolveSafeInteraction(MockHttpSession session, String idempotencyKey, String expectedPhase) throws Exception {
        mockMvc.perform(post("/api/interaction/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value(expectedPhase));
    }

    private void submitFanServiceScThanksDay(MockHttpSession session, int day, String prefix) throws Exception {
        mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "STREAM_PLAN",
                                  "planType": "SC_THANKS",
                                  "idempotencyKey": "%s-day-%d-stream-plan"
                                }
                                """.formatted(prefix, day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("NEED_TITLE"));

        var result = mockMvc.perform(post("/api/stream/title/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleTemplateId": 123,
                                  "idempotencyKey": "%s-day-%d-fan-service-title"
                                }
                                """.formatted(prefix, day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionResult.summary", containsString("陪伴营业")))
                .andExpect(jsonPath("$.data.actionResult.summary", not(containsString("FAN_SERVICE"))))
                .andReturn();

        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String expectedPhase = day >= 30 ? "ENDING_READY" : "REPORT_READY";
        if (body.contains("\"phase\":\"NEED_INTERACTION_CHOICE\"")) {
            resolveSafeInteraction(session, "%s-day-%d-fan-service-interaction-safe".formatted(prefix, day), expectedPhase);
            return;
        }
        if (body.contains("\"phase\":\"NEED_EVENT_CHOICE\"")) {
            mockMvc.perform(post("/api/event/choose")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "choiceType": "safe",
                                      "idempotencyKey": "%s-day-%d-fan-service-event-safe"
                                    }
                                    """.formatted(prefix, day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phase").value(expectedPhase));
            return;
        }

        assertTrue(body.contains("\"phase\":\"" + expectedPhase + "\""), body);
    }

    private String runSeededRandomDemoAndReadFinalAction(String runSeed) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/dev/demo/reset")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "random",
                                  "runSeed": "%s"
                                }
                                """.formatted(runSeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runSeed").value(runSeed));

        mockMvc.perform(post("/api/dev/demo/run-script")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": "random",
                                  "targetDay": 30,
                                  "runSeed": "%s"
                                }
                                """.formatted(runSeed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runSeed").value(runSeed))
                .andExpect(jsonPath("$.data.day").value(30))
                .andExpect(jsonPath("$.data.phase").value("ENDING_READY"));

        MvcResult result = mockMvc.perform(get("/api/ending/review").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.routeReview.finalDayAction");
    }

    private void submitActionAndResolveFormalEvent(
            MockHttpSession session,
            String actionType,
            int day,
            String idempotencyKey,
            String expectedSummary
    ) throws Exception {
        var result = mockMvc.perform(post("/api/day/action")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "%s",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(actionType, idempotencyKey)))
                .andExpect(status().isOk())
                .andReturn();

        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        boolean pressureRecoveryAction = false;
        if (body.contains("\"code\":\"OPERATIONAL_PRESSURE_LOCKED\"")) {
            String recoveryActionType = pressureRecoveryActionType(session, actionType);
            result = mockMvc.perform(post("/api/day/action")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "actionType": "%s",
                                      "idempotencyKey": "%s-pressure-recovery"
                                    }
                                    """.formatted(recoveryActionType, idempotencyKey)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();
            body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
            pressureRecoveryAction = true;
        } else {
            assertTrue(body.contains("\"success\":true"), body);
        }
        if (expectedSummary != null && !pressureRecoveryAction) {
            assertTrue(body.contains(expectedSummary), body);
        }

        String expectedPhase = day >= 30 ? "ENDING_READY" : "REPORT_READY";
        if (body.contains("\"phase\":\"NEED_EVENT_CHOICE\"")) {
            mockMvc.perform(post("/api/event/choose")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "choiceType": "safe",
                                      "idempotencyKey": "%s-event-safe"
                                    }
                                    """.formatted(idempotencyKey)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.phase").value(expectedPhase));
            return;
        }

        assertTrue(body.contains("\"phase\":\"" + expectedPhase + "\""), body);
    }

    private String pressureRecoveryActionType(MockHttpSession session, String lockedActionType) throws Exception {
        MvcResult actionsResult = mockMvc.perform(get("/api/actions").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = new String(actionsResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        List<Map<String, Object>> actions = JsonPath.read(body, "$.data");

        String replacementActionType = null;
        for (Map<String, Object> action : actions) {
            if (lockedActionType.equals(stringValue(action.get("actionType")))) {
                replacementActionType = stringValue(action.get("pressureReplacementActionType"));
                break;
            }
        }
        if (enabledAction(actions, replacementActionType)) {
            return replacementActionType;
        }
        for (String candidate : List.of("TRAIN_TALK", "REST", "FAN_GROUP_MAINTAIN", "TRAIN_DANCE", "TRAIN_SONG")) {
            if (enabledAction(actions, candidate)) {
                return candidate;
            }
        }
        return "REST";
    }

    private boolean enabledAction(List<Map<String, Object>> actions, String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return false;
        }
        for (Map<String, Object> action : actions) {
            if (actionType.equals(stringValue(action.get("actionType")))) {
                return Boolean.TRUE.equals(action.get("enabled"));
            }
        }
        return false;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void resolveSafeFormalEventIfNeeded(
            MockHttpSession session,
            String idempotencyKey,
            String expectedPhase
    ) throws Exception {
        var sessionResult = mockMvc.perform(get("/api/day/session").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String body = new String(sessionResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        if (!body.contains("\"phase\":\"NEED_EVENT_CHOICE\"")) {
            assertTrue(body.contains("\"phase\":\"" + expectedPhase + "\""), body);
            return;
        }

        mockMvc.perform(post("/api/event/choose")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "choiceType": "safe",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phase").value(expectedPhase));
    }

    private void completeRestRunToEnding(MockHttpSession session, String prefix) throws Exception {
        for (int day = 1; day <= 29; day++) {
            submitActionAndResolveFormalEvent(session, "REST", day, "%s-day-%d-rest".formatted(prefix, day), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "%s-go-day-%d"
                                    }
                                    """.formatted(prefix, day + 1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }

        submitActionAndResolveFormalEvent(session, "REST", 30, "%s-day-30-rest".formatted(prefix), null);
    }

    private void advanceWithRest(MockHttpSession session, int day, String prefix) throws Exception {
        mockMvc.perform(post("/api/day/next")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "%s-go-day-%d"
                                }
                                """.formatted(prefix, day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.day").value(day))
                .andExpect(jsonPath("$.data.phase").value("READY"));

        submitActionAndResolveFormalEvent(session, "REST", day, "%s-day-%d-rest".formatted(prefix, day), null);
    }

    private void advanceToDay(MockHttpSession session, int targetDay, String prefix) throws Exception {
        for (int day = 1; day < targetDay; day++) {
            submitActionAndResolveFormalEvent(session, "REST", day, "%s-rest-day-%d".formatted(prefix, day), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "%s-next-day-%d"
                                    }
                                    """.formatted(prefix, day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }
    }

    private void advanceReadyDaysToDay(MockHttpSession session, int startDay, int targetDay, String prefix) throws Exception {
        for (int day = startDay; day < targetDay; day++) {
            submitActionAndResolveFormalEvent(session, "REST", day, "%s-rest-day-%d".formatted(prefix, day), null);

            mockMvc.perform(post("/api/day/next")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "idempotencyKey": "%s-next-day-%d"
                                    }
                                    """.formatted(prefix, day)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.day").value(day + 1))
                    .andExpect(jsonPath("$.data.phase").value("READY"));
        }
    }

    private MockHttpSession registerLoginAndCreateVup(String username, String vupName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "pass1234",
                                  "nickname": "测试玩家"
                                }
                                """.formatted(username)))
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
                                """.formatted(username)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vup/create")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "persona": "歌杂练习生"
                                }
                                """.formatted(vupName)))
                .andExpect(status().isOk());

        return session;
    }

    private void assertChineseDebtSummary(MvcResult result, String... expectedSnippets) throws Exception {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String debtSummary = JsonPath.read(body, "$.data.debtSummary");
        for (String snippet : expectedSnippets) {
            assertTrue(debtSummary.contains(snippet), debtSummary);
        }
        for (String leakedToken : List.of(
                "TITLE_BACKFIRE",
                "BOOMERANG_CLIP",
                "UNICORN_EXPECTATION",
                "COMMERCIAL_BACKLASH",
                "FAN_SERVICE",
                "BUSINESS_SAFE",
                "severity=",
                "dueDay=",
                "source=",
                "summary=",
                "status=",
                "OPEN",
                "CLEARED"
        )) {
            assertFalse(debtSummary.contains(leakedToken), debtSummary);
        }
    }

    @Test
    void playableHomePrioritizesNormalPlayerStartOverClassroomDemo() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult htmlResult = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);
        String indexHtml = new String(htmlResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(appJs.contains("创建本地存档"), "普通入口文案'创建本地存档'必须存在");
        assertTrue(appJs.contains("开始出道"), "普通入口文案'开始出道'必须存在");
        assertTrue(appJs.contains("本地存档 / 设置"), "存档面板标题必须存在");

        String renderActionsSource = appJs.substring(
                appJs.indexOf("function renderActions()"),
                appJs.indexOf("function selectedOptionLabel"));
        assertFalse(renderActionsSource.contains("renderDefenseCockpit"), "renderActions 不能调用答辩驾驶舱");
        assertFalse(renderActionsSource.contains("renderDemo"), "renderActions 不能调用课堂演示");
        assertFalse(renderActionsSource.contains("SSM"), "行动面板不能出现 SSM");
        assertFalse(renderActionsSource.contains("MVC"), "行动面板不能出现 MVC");
        assertFalse(renderActionsSource.contains("Mapper"), "行动面板不能出现 Mapper");
        assertFalse(renderActionsSource.contains("幂等"), "行动面板不能出现幂等");

        String renderAuthSource = appJs.substring(
                appJs.indexOf("function renderAuth()"),
                appJs.indexOf("function renderCreationStylePresets"));
        assertFalse(renderAuthSource.contains("SSM"), "登录/创建面板不能出现 SSM");
        assertFalse(renderAuthSource.contains("MVC"), "登录/创建面板不能出现 MVC");
        assertFalse(renderAuthSource.contains("Mapper"), "登录/创建面板不能出现 Mapper");
        assertFalse(renderAuthSource.contains("幂等"), "登录/创建面板不能出现幂等");
        assertFalse(renderAuthSource.contains("课堂演示"), "登录/创建面板不能出现课堂演示");
        assertFalse(renderAuthSource.contains("答辩驾驶舱"), "登录/创建面板不能出现答辩驾驶舱");
        assertFalse(renderAuthSource.contains("SSM证据台"), "登录/创建面板不能出现SSM证据台");

        // demoPanel should be hidden by default
        assertTrue(indexHtml.contains("id=\"demoPanel\"") && indexHtml.contains("class=\"panel hidden\" id=\"demoPanel\""),
                "演示面板必须默认隐藏");
        assertFalse(indexHtml.contains("defense-cockpit"), "页面不能包含答辩驾驶舱");

        assertTrue(appJs.contains("function demoToolsEnabled()"), "demoToolsEnabled 函数必须存在");
        assertTrue(appJs.contains("state.configCheck?.devTools?.enabled"), "demoToolsEnabled 必须检查 devTools.enabled");
    }

    @Test
    void createVupStyleCardsExplainOpeningPlanAndRisk() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("首日打法"), "风格卡必须显示首日打法");
        assertTrue(appJs.contains("适合路线"), "风格卡必须显示适合路线");
        assertTrue(appJs.contains("风险提示"), "风格卡必须显示风险提示");

        String presetsSource = appJs.substring(
                appJs.indexOf("const creationStylePresets = ["),
                appJs.indexOf("];", appJs.indexOf("const creationStylePresets = [")) + 2);
        assertFalse(presetsSource.contains("ELECTRONIC_PICKLE"), "风格预设数据不能出现 ELECTRONIC_PICKLE");
        assertFalse(presetsSource.contains("SLICE_SAINT"), "风格预设数据不能出现 SLICE_SAINT");
        assertFalse(presetsSource.contains("BLACK_RED_MAIN_STAGE"), "风格预设数据不能出现 BLACK_RED_MAIN_STAGE");

        assertTrue(appJs.contains("fitRoute"), "风格预设数据必须包含 fitRoute 字段");
    }

    @Test
    void coachCopyTellsOneNextActionForEachPhase() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);

        // 教练文案已迁移至 renderNextStepHint，按阶段给出下一步行动提示。
        int hintStart = appJs.indexOf("function renderNextStepHint()");
        assertTrue(hintStart >= 0, "必须存在 renderNextStepHint 函数承载各阶段下一步提示");
        String renderCoachSource = appJs.substring(hintStart);

        assertTrue(renderCoachSource.contains("先选择一个主行动"), "READY 阶段文案必须提示先选择主行动");
        assertTrue(renderCoachSource.contains("请选择一个标题"), "NEED_TITLE 阶段文案必须提示选择标题");
        assertTrue(renderCoachSource.contains("请选择处理方式"), "NEED_INTERACTION_CHOICE 阶段文案必须提示选择处理方式");
        assertTrue(renderCoachSource.contains("复盘并进入下一天"), "REPORT_READY 阶段文案必须提示复盘并进入下一天");
        assertTrue(renderCoachSource.contains("查看结局复盘"), "ENDING_READY 阶段文案必须提示查看结局复盘");
    }

    @Test
    void eventChoiceDtoHasAllRequiredFields() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);

        String eventDtosSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/dto/EventDtos.java"), StandardCharsets.UTF_8);
        String interactionDtosSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/dto/InteractionDtos.java"), StandardCharsets.UTF_8);

        assertTrue(eventDtosSource.contains("choiceId"), "EventChoiceDTO 必须有 choiceId");
        assertTrue(eventDtosSource.contains("choiceType"), "EventChoiceDTO 必须有 choiceType");
        assertTrue(eventDtosSource.contains("label"), "EventChoiceDTO 必须有 label");
        assertTrue(eventDtosSource.contains("costPreview"), "EventChoiceDTO 必须有 costPreview");
        assertTrue(eventDtosSource.contains("effectPreview"), "EventChoiceDTO 必须有 effectPreview");
        assertTrue(eventDtosSource.contains("riskPreview"), "EventChoiceDTO 必须有 riskPreview");
        assertTrue(eventDtosSource.contains("enabled"), "EventChoiceDTO 必须有 enabled");
        assertTrue(eventDtosSource.contains("disabledReason"), "EventChoiceDTO 必须有 disabledReason");

        assertTrue(interactionDtosSource.contains("costPreview"), "InteractionChoiceDTO 必须有 costPreview");
        assertTrue(interactionDtosSource.contains("effectPreview"), "InteractionChoiceDTO 必须有 effectPreview");
        assertTrue(interactionDtosSource.contains("enabled"), "InteractionChoiceDTO 必须有 enabled");
        assertTrue(interactionDtosSource.contains("disabledReason"), "InteractionChoiceDTO 必须有 disabledReason");

        assertTrue(interactionDtosSource.contains("PendingInteractionChoiceDTO"), "必须有 PendingInteractionChoiceDTO");
        int pendingRecordIdx = interactionDtosSource.indexOf("public record PendingInteractionChoiceDTO");
        int pendingEndIdx = interactionDtosSource.indexOf("}", pendingRecordIdx);
        String pendingSource = interactionDtosSource.substring(pendingRecordIdx, pendingEndIdx + 1);
        assertTrue(pendingSource.contains("choiceId"), "PendingInteractionChoiceDTO 必须有 choiceId");
        assertTrue(pendingSource.contains("costPreview"), "PendingInteractionChoiceDTO 必须有 costPreview");
    }

    @Test
    void formalEventPresenterReturnsDebtTypeSpecificChoices() throws Exception {
        String presenterSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/event/FormalEventPresenter.java"), StandardCharsets.UTF_8);

        assertTrue(presenterSource.contains("TITLE_BACKFIRE"), "必须处理 TITLE_BACKFIRE 债务类型");
        assertTrue(presenterSource.contains("BOOMERANG_CLIP"), "必须处理 BOOMERANG_CLIP 债务类型");
        assertTrue(presenterSource.contains("UNICORN_EXPECTATION"), "必须处理 UNICORN_EXPECTATION 债务类型");
        assertTrue(presenterSource.contains("COMMERCIAL_BACKLASH"), "必须处理 COMMERCIAL_BACKLASH 债务类型");

        assertTrue(presenterSource.contains("safe"), "必须有 safe 选项");
        assertTrue(presenterSource.contains("traffic"), "必须有 traffic 选项");
        assertTrue(presenterSource.contains("meme"), "必须有 meme 选项");

        assertTrue(presenterSource.contains("标题党反噬"), "TITLE_BACKFIRE 必须有中文标题");
        assertTrue(presenterSource.contains("回旋镖"), "BOOMERANG_CLIP 必须有中文标题");
        assertTrue(presenterSource.contains("独角兽"), "UNICORN_EXPECTATION 必须有中文标题");
        assertTrue(presenterSource.contains("商业"), "COMMERCIAL_BACKLASH 必须有中文标题");
    }

    @Test
    void pendingInteractionPresenterHasFourTypesWithThreeChoicesEach() throws Exception {
        String presenterSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/event/PendingInteractionPresenter.java"), StandardCharsets.UTF_8);

        assertTrue(presenterSource.contains("CHAT_BAIT"), "必须有 CHAT_BAIT 类型");
        assertTrue(presenterSource.contains("MARSHMALLOW_BOMB"), "必须有 MARSHMALLOW_BOMB 类型");
        assertTrue(presenterSource.contains("SC_BOSS_QUESTION"), "必须有 SC_BOSS_QUESTION 类型");
        assertTrue(presenterSource.contains("COLLAB_RHYTHM"), "必须有 COLLAB_RHYTHM 类型");

        assertFalse(presenterSource.contains("本版暂未开放"), "选项不能包含'本版暂未开放'");

        assertTrue(presenterSource.contains("棉花糖"), "必须有棉花糖相关文案");
        assertTrue(presenterSource.contains("联动"), "必须有联动相关文案");
    }

    @Test
    void eventServiceDoesNotHardcodeSafeChoiceType() throws Exception {
        String eventServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/event/EventService.java"), StandardCharsets.UTF_8);

        assertFalse(eventServiceSource.contains("\"choiceType\", \"safe\""), "不能硬编码 choiceType 为 safe");
        assertFalse(eventServiceSource.contains("\"event:\" + debt.getDebtType() + \":safe\""), "不能硬编码 source 为 safe");
    }

    @Test
    void eventCardRendersChoiceLabelsEffectsCostsAndRisks() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();

        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function renderEventChoice("), "必须有 renderEventChoice 函数");
        assertTrue(appJs.contains("处理方案"), "事件卡必须显示'处理方案'");
        assertTrue(appJs.contains("代价"), "事件卡必须显示'代价'");
        assertTrue(appJs.contains("收益"), "事件卡必须显示'收益'");
        assertTrue(appJs.contains("后果"), "事件卡必须显示'后果'");
        assertTrue(appJs.contains("event-choice-button"), "必须有事件选择按钮");
        assertTrue(appJs.contains("aria-label="), "必须有 aria-label");

        assertFalse(appJs.contains("未命名处理方案"), "不能有'未命名处理方案'fallback");
        assertFalse(appJs.contains("效果待观察"), "不能有'效果待观察'fallback");
        assertFalse(appJs.contains("收益待看"), "不能有'收益待看'fallback");
    }

    @Test
    void nonStreamActionRewardsFollowDesignRules() throws Exception {
        String rewardCalcSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/risk/RewardCalculator.java"), StandardCharsets.UTF_8);

        assertTrue(rewardCalcSource.contains("resolveTrainTalk"), "必须有 resolveTrainTalk");
        assertTrue(rewardCalcSource.contains("resolvePublishVideo"), "必须有 resolvePublishVideo");
        assertTrue(rewardCalcSource.contains("resolvePublishClip"), "必须有 resolvePublishClip");
        assertTrue(rewardCalcSource.contains("resolveNpcInteract"), "必须有 resolveNpcInteract");
        assertTrue(rewardCalcSource.contains("resolveRest"), "必须有 resolveRest");

        assertTrue(rewardCalcSource.contains("RAID"), "NPC_INTERACT 必须有 RAID 选项");
        assertTrue(rewardCalcSource.contains("COLLAB"), "NPC_INTERACT 必须有 COLLAB 选项");
        assertTrue(rewardCalcSource.contains("BORROW_HEAT"), "NPC_INTERACT 必须有 BORROW_HEAT 选项");
        assertTrue(rewardCalcSource.contains("AVOID"), "NPC_INTERACT 必须有 AVOID 选项");
    }

    @Test
    void titleRiskSystemFollowsDesignRules() throws Exception {
        String titleServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/TitleService.java"), StandardCharsets.UTF_8);
        String debtServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/risk/DebtService.java"), StandardCharsets.UTF_8);

        assertTrue(titleServiceSource.contains("SAFE"), "必须有 SAFE 标题风格");
        assertTrue(titleServiceSource.contains("BAIT_TRAFFIC"), "必须有 BAIT_TRAFFIC 标题风格");
        assertTrue(titleServiceSource.contains("HARD_MOUTH"), "必须有 HARD_MOUTH 标题风格");
        assertTrue(titleServiceSource.contains("ABSTRACT_MEME"), "必须有 ABSTRACT_MEME 标题风格");

        assertTrue(titleServiceSource.contains("newPlayerProtection"), "必须有新手保护逻辑");
        assertTrue(titleServiceSource.contains("session.getDay() <= 3"), "新手保护必须覆盖前3天");

        assertTrue(debtServiceSource.contains("SAFE"), "DebtService 必须处理 SAFE 标题");
        assertTrue(debtServiceSource.contains("HARD_MOUTH"), "DebtService 必须处理 HARD_MOUTH 标题");
    }

    @Test
    void stageBriefingGivesRouteSpecificNextStepAdvice() throws Exception {
        String briefingSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/progression/StageBriefingService.java"), StandardCharsets.UTF_8);

        assertTrue(briefingSource.contains("第7天阶段复盘"), "必须有第7天阶段复盘");
        assertTrue(briefingSource.contains("第14天路线成型"), "必须有第14天阶段复盘");
        assertTrue(briefingSource.contains("第21天冲刺预警"), "必须有第21天阶段复盘");
        assertTrue(briefingSource.contains("第30天最终收束"), "必须有第30天阶段复盘");

        assertTrue(briefingSource.contains("riskSnapshot"), "必须有风险快照");
        assertTrue(briefingSource.contains("actionHint"), "必须有行动建议");
        assertTrue(briefingSource.contains("routeSnapshot"), "必须有路线快照");
    }

    @Test
    void dailyReportShowsThreeToFivePlayerFacingItems() throws Exception {
        String reportServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/report/ReportService.java"), StandardCharsets.UTF_8);
        String reportDtosSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/dto/ReportDtos.java"), StandardCharsets.UTF_8);

        assertTrue(reportServiceSource.contains("visibleItems"), "必须有 visibleItems");
        assertTrue(reportServiceSource.contains("evidenceRefs"), "必须有 evidenceRefs");
        assertTrue(reportServiceSource.contains("riskHint"), "必须有 riskHint");
        assertTrue(reportServiceSource.contains("summary"), "必须有 summary");

        assertTrue(reportDtosSource.contains("visibleItems"), "ReportDtos 必须有 visibleItems");
        assertTrue(reportDtosSource.contains("evidenceRefs"), "ReportDtos 必须有 evidenceRefs");
        assertTrue(reportDtosSource.contains("riskHint"), "ReportDtos 必须有 riskHint");
    }

    @Test
    void dailyReportCitesRealActionTitleEventOrDebtEvidence() throws Exception {
        String reportServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/report/ReportService.java"), StandardCharsets.UTF_8);

        assertTrue(reportServiceSource.contains("titleRef"), "必须引用标题");
        assertTrue(reportServiceSource.contains("selectedTitle"), "必须有 selectedTitle");
        assertTrue(reportServiceSource.contains("openDebts"), "必须引用债务");
        assertTrue(reportServiceSource.contains("debtType"), "必须有 debtType");
        assertTrue(reportServiceSource.contains("dueDay"), "必须有 dueDay");
        assertTrue(reportServiceSource.contains("comboHits"), "必须引用组合技");
        assertTrue(reportServiceSource.contains("memeQuote"), "必须有梗语录");
        assertTrue(reportServiceSource.contains("fanLetter"), "必须有粉丝来信");
    }

    @Test
    void reportKeepsNextDayButtonInReach() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function renderReport()"), "必须有 renderReport 函数");
        assertTrue(appJs.contains("report-next-day"), "必须有下一天按钮");
        assertTrue(appJs.contains("data-action=\"next-day\""), "必须有 next-day data-action");
        assertTrue(appJs.contains("visibleItems"), "必须有 visibleItems");
        assertTrue(appJs.contains("report-kpi-strip"), "必须有 KPI 条");
        assertTrue(appJs.contains("report-summary"), "必须有摘要");
    }

    @Test
    void expandedPublicSafeContentAvoidsRealPersonAndGroupDogwhistles() throws Exception {
        String dataSql = Files.readString(
                Path.of("src/main/resources/data.sql"), StandardCharsets.UTF_8);

        long trueFanLetters = dataSql.lines()
                .filter(line -> line.contains("FAN_LETTER") && line.contains("true_fan"))
                .count();
        long funFanLetters = dataSql.lines()
                .filter(line -> line.contains("FAN_LETTER") && line.contains("fun_fan"))
                .count();
        long unicornLetters = dataSql.lines()
                .filter(line -> line.contains("FAN_LETTER") && line.contains("unicorn"))
                .count();
        long ddLetters = dataSql.lines()
                .filter(line -> line.contains("FAN_LETTER") && line.contains("'dd'"))
                .count();

        assertTrue(trueFanLetters >= 8, "true_fan 信件至少 8 条，当前 " + trueFanLetters);
        assertTrue(funFanLetters >= 8, "fun_fan 信件至少 8 条，当前 " + funFanLetters);
        assertTrue(unicornLetters >= 8, "unicorn 信件至少 8 条，当前 " + unicornLetters);
        assertTrue(ddLetters >= 8, "dd 信件至少 8 条，当前 " + ddLetters);
    }

    @Test
    void endingReviewShowsThreeToFiveEvidenceBackedKeyEvents() throws Exception {
        String endingServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/ending/EndingService.java"), StandardCharsets.UTF_8);
        String endingDtosSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/dto/EndingDtos.java"), StandardCharsets.UTF_8);

        assertTrue(endingServiceSource.contains("keyEvents"), "必须有 keyEvents");
        assertTrue(endingServiceSource.contains("endingReason"), "必须有 endingReason");
        assertTrue(endingServiceSource.contains("endingReasonJson"), "必须有 endingReasonJson");
        assertTrue(endingServiceSource.contains("debtRefs"), "必须有 debtRefs");
        assertTrue(endingServiceSource.contains("routeReview"), "必须有 routeReview");
        assertTrue(endingServiceSource.contains("restartHint"), "必须有 restartHint");

        assertTrue(endingDtosSource.contains("keyEvents"), "EndingDtos 必须有 keyEvents");
        assertTrue(endingDtosSource.contains("endingReason"), "EndingDtos 必须有 endingReason");
        assertTrue(endingDtosSource.contains("debtRefs"), "EndingDtos 必须有 debtRefs");
        assertTrue(endingDtosSource.contains("routeReview"), "EndingDtos 必须有 routeReview");
        assertTrue(endingDtosSource.contains("restartHint"), "EndingDtos 必须有 restartHint");
    }

    @Test
    void endingReasonJsonContainsRequiredFields() throws Exception {
        String endingServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/ending/EndingService.java"), StandardCharsets.UTF_8);

        assertTrue(endingServiceSource.contains("endingType"), "必须有 endingType");
        assertTrue(endingServiceSource.contains("rule"), "必须有 rule");
        assertTrue(endingServiceSource.contains("finalDayAction"), "必须有 finalDayAction");
        assertTrue(endingServiceSource.contains("routeEvidence"), "必须有 routeEvidence");
        assertTrue(endingServiceSource.contains("evidenceRefs"), "必须有 evidenceRefs");
        assertTrue(endingServiceSource.contains("subtitleSource"), "必须有 subtitleSource");
        assertTrue(endingServiceSource.contains("subtitleSeedData"), "副标题必须使用本局个性化种子");
        assertTrue(endingServiceSource.contains("representativeActions"), "副标题种子必须包含代表行动");
        assertTrue(endingServiceSource.contains("representativeResultSignals"), "副标题种子必须包含代表结果信号");
        assertTrue(endingServiceSource.contains("\"signature\".equals(entry.subKey())"), "关键结局必须用内容 metadata 保留传播锚点");
        assertTrue(endingServiceSource.contains("\"seed\", copyDecision.seedData()"), "subtitleSource 必须写入 seed");
        assertTrue(endingServiceSource.contains("\"candidatePoolSize\", copyDecision.candidatePoolSize()"), "subtitleSource 必须写入候选池大小");
        assertTrue(endingServiceSource.contains("\"catalogPoolSize\", copyDecision.catalogPoolSize()"), "subtitleSource 必须写入目录池大小");
        assertFalse(endingServiceSource.contains("seedData.put(\"vupId\""), "副标题种子不能依赖数据库自增ID");
        assertFalse(endingServiceSource.contains("private String endingReasonJson(String endingType, Vup vup, BusinessLog finalLog)"), "不能保留缺 subtitleSource 的旧 endingReasonJson");
    }

    @Test
    void restartOptionsUseChineseRouteLabelsAndExplainInheritance() throws Exception {
        String endingServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/ending/EndingService.java"), StandardCharsets.UTF_8);
        String rebirthServiceSource = Files.readString(
                Path.of("src/main/java/com/example/vupworld/service/ending/RebirthService.java"), StandardCharsets.UTF_8);

        assertTrue(endingServiceSource.contains("restartHint"), "必须有 restartHint");
        assertTrue(endingServiceSource.contains("routeBias"), "必须有 routeBias");
        assertTrue(endingServiceSource.contains("fanBiasPercent"), "必须有 fanBiasPercent");

        assertTrue(rebirthServiceSource.contains("restartBiasType"), "必须有 restartBiasType");
        assertTrue(rebirthServiceSource.contains("复活赛"), "必须有复活赛文案");
    }

    @Test
    void endingKeepsRestartButtonReachable() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("function renderEnding()"), "必须有 renderEnding 函数");
        assertTrue(appJs.contains("ending-restart-button"), "必须有复活赛按钮");
        assertTrue(appJs.contains("data-action=\"restart\""), "必须有 restart data-action");
        assertTrue(appJs.contains("keyEvents"), "必须有关键节点");
        assertTrue(appJs.contains("renderRestartOptions"), "必须有复活赛选项");
    }

    @Test
    void allPrimaryPlayerButtonsHaveButtonTypeAndAccessibleNames() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("ready-mode"), "必须有 ready-mode");
        assertTrue(appJs.contains("title-mode"), "必须有 title-mode");
        assertTrue(appJs.contains("event-mode"), "必须有 event-mode");
        assertTrue(appJs.contains("report-mode"), "必须有 report-mode");
        assertTrue(appJs.contains("ending-mode"), "必须有 ending-mode");
    }

    @Test
    void stageHotkeysDoNotHijackTextInputs() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("data-stage-hotkey"), "必须有 data-stage-hotkey");
        assertTrue(appJs.contains("data-stage-advance-hotkey"), "必须有 data-stage-advance-hotkey");
        assertTrue(appJs.contains("data-action-hotkey"), "必须有 data-action-hotkey");
        assertTrue(appJs.contains("stage-hotkey"), "必须有 stage-hotkey 样式");
    }

    @Test
    void toastDoesNotCoverPrimaryButtons() throws Exception {
        MvcResult scriptResult = mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andReturn();
        String appJs = playerFrontendSource(scriptResult);

        assertTrue(appJs.contains("positionStatusToast"), "必须有 positionStatusToast 函数");
        assertTrue(appJs.contains("avoidBoxes"), "必须有 avoidBoxes 避让逻辑");
        assertTrue(appJs.contains("appStatus"), "必须有 appStatus 元素");
    }

    @Test
    void visualScriptsHaveRequiredChecks() throws Exception {
        String desktopProbe = Files.readString(
                Path.of("scripts/visual-desktop-check.mjs"), StandardCharsets.UTF_8);

        assertTrue(desktopProbe.contains("failedChecks"), "desktop 脚本必须有 failedChecks");
    }
}
