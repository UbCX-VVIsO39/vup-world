package com.example.vupworld;

import com.example.vupworld.dto.EndingDtos.EndingReviewDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DailyReportMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DailyReport;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.EndingReview;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.ending.EndingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndingServiceCopyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VupMapper vupMapper;

    @Autowired
    private DaySessionMapper daySessionMapper;

    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private BusinessLogMapper businessLogMapper;

    @Autowired
    private EndingService endingService;

    @Test
    void endingReviewReasonUsesPlayerFacingFallbackForUnknownFinalAction() throws Exception {
        MockHttpSession session = registerLoginAndCreateVup();
        Long userId = (Long) session.getAttribute("USER_ID");
        Vup vup = vupMapper.findActiveByUserId(userId);
        tuneVupForStableEnding(vup);
        DaySession daySession = daySessionMapper.findByVupIdAndDay(vup.getId(), 1);
        DailyReport finalReport = insertFinalReport(vup);
        BusinessLog finalLog = insertUnknownActionLog(vup, daySession, finalReport);

        EndingReview review = endingService.createEndingReview(vup, daySession, finalReport, finalLog);

        assertPlayerFacingFallback(review.getEndingReason());
        EndingReviewDTO latestReview = endingService.latestReview(vup);
        assertPlayerFacingFallback(latestReview.endingReason());
    }

    private MockHttpSession registerLoginAndCreateVup() throws Exception {
        String username = "ending_copy_unknown_action_player";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "pass1234",
                                  "nickname": "结局复盘校对"
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
                                  "name": "复盘台",
                                  "persona": "结局文案校对"
                                }
                                """))
                .andExpect(status().isOk());
        return session;
    }

    private void tuneVupForStableEnding(Vup vup) {
        vup.setFans(180);
        vup.setTrueFans(90);
        vup.setFunFans(40);
        vup.setUnicornFans(20);
        vup.setDdFans(30);
        vup.setWatchHeat(12);
        vup.setReputation(72);
        vup.setMemeLevel(10);
        vup.setCommercialLevel(5);
        vup.setSongPower(6);
        vup.setCurrentRoute("ELECTRONIC_PICKLE");
        vup.setRouteScoreJson("{\"ELECTRONIC_PICKLE\":6}");
    }

    private DailyReport insertFinalReport(Vup vup) {
        DailyReport report = new DailyReport();
        report.setVupId(vup.getId());
        report.setDay(30);
        report.setSummary("第30天收官复盘");
        report.setSelectedTitle("月底复盘回");
        report.setReportTone("ENDING_REVIEW");
        report.setFanDelta(0);
        report.setCoinDelta(0);
        report.setPopularityDelta(0);
        report.setWatchHeatDelta(0);
        report.setReputationDelta(0);
        report.setMemeDelta(0);
        report.setCommercialDelta(0);
        report.setDebtSummary("本轮旧账已归档");
        report.setRiskHint("结局复盘生成中");
        report.setVisibleItemsJson("[]");
        report.setEvidenceRefsJson("[]");
        report.setTemplateRefsJson("[]");
        report.setRenderVersion("test");
        dailyReportMapper.insert(report);
        return report;
    }

    private BusinessLog insertUnknownActionLog(Vup vup, DaySession daySession, DailyReport finalReport) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(daySession.getId());
        log.setDay(30);
        log.setPhase("ENDING_READY");
        log.setIdempotencyKey("ending-copy-unknown-action-final-log");
        log.setAction("MYSTERY_BACKSTAGE_TASK");
        log.setResult("后台补档动作进入结局复盘");
        log.setRouteScoreChange("{\"ELECTRONIC_PICKLE\":1,\"source\":\"MYSTERY_BACKSTAGE_TASK\"}");
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setReportId(finalReport.getId());
        log.setEndingRefFlag(true);
        businessLogMapper.insert(log);
        return log;
    }

    private void assertPlayerFacingFallback(String endingReason) {
        assertTrue(endingReason.contains("未归档营业记录"), endingReason);
        assertFalse(endingReason.contains("行动记录"), endingReason);
        assertFalse(endingReason.contains("MYSTERY_BACKSTAGE_TASK"), endingReason);
    }
}
