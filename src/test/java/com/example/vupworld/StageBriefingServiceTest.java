package com.example.vupworld;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.StageDtos.StageBriefingDTO;
import com.example.vupworld.dto.StageDtos.StageObjectiveItemDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.DailyFortuneService;
import com.example.vupworld.service.content.PlatformTrendService;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.progression.StageBriefingService;
import com.example.vupworld.service.progression.StageObjectiveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StageBriefingServiceTest {
    private final JsonService jsonService = new JsonService(new ObjectMapper());
    private final VupService vupService = mock(VupService.class);
    private final RiskDebtMapper riskDebtMapper = mock(RiskDebtMapper.class);
    private final DaySessionMapper daySessionMapper = mock(DaySessionMapper.class);
    private final DayFlowService dayFlowService = mock(DayFlowService.class);
    private final StageObjectiveService stageObjectiveService = mock(StageObjectiveService.class);
    private final StageBriefingService service = new StageBriefingService(
            vupService,
            new PlatformTrendService(),
            riskDebtMapper,
            daySessionMapper,
            jsonService,
            new DailyFortuneService(),
            dayFlowService,
            stageObjectiveService
    );

    StageBriefingServiceTest() {
        when(dayFlowService.routeLabel(anyString())).thenAnswer(invocation -> routeLabel(invocation.getArgument(0)));
    }

    @Test
    void briefingExposesRouteTendencyAndRouteFormingRhythm() {
        Map<String, Integer> routeScores = routeScores(Map.of(
                RouteType.SINGING_IDOL.name(), 8,
                RouteType.SLICE_SAINT.name(), 5,
                RouteType.DANCE_MEME.name(), 5,
                RouteType.ELECTRONIC_PICKLE.name(), 2
        ));
        Vup vup = vup(10, RouteType.SINGING_IDOL.name(), routeScores);
        givenBriefingDependencies(vup, routeScores);

        StageBriefingDTO briefing = service.briefing(1001L);

        assertEquals("路线成形", briefing.stageRhythm().phaseName());
        assertEquals(8, briefing.stageRhythm().startDay());
        assertEquals(14, briefing.stageRhythm().endDay());
        assertTrue(briefing.stageRhythm().reason().contains("路线证据"));
        assertEquals(RouteType.SINGING_IDOL.name(), briefing.routeTendency().primaryRoute().routeType());
        assertEquals("歌势路线", briefing.routeTendency().primaryRoute().label());
        assertEquals(8, briefing.routeTendency().primaryRoute().score());
        assertEquals(RouteType.SLICE_SAINT.name(), briefing.routeTendency().competingRoutes().get(0).routeType());
        assertEquals(RouteType.DANCE_MEME.name(), briefing.routeTendency().competingRoutes().get(1).routeType());
        assertEquals(3, briefing.routeTendency().competingRoutes().get(0).scoreGapToPrimary());
        assertTrue(briefing.routeTendency().reason().contains("分数最高"));
    }

    @Test
    void briefingKeepsUnknownPrimaryButStillShowsTwoCompetitionRoutesWhenScoresAreEmpty() {
        Map<String, Integer> routeScores = routeScores(Map.of());
        Vup vup = vup(17, RouteType.UNKNOWN.name(), routeScores);
        givenBriefingDependencies(vup, routeScores);

        StageBriefingDTO briefing = service.briefing(vup.getUserId());

        assertEquals("危机扩张", briefing.stageRhythm().phaseName());
        assertEquals(RouteType.UNKNOWN.name(), briefing.routeTendency().primaryRoute().routeType());
        assertEquals(0, briefing.routeTendency().primaryRoute().score());
        assertEquals(2, briefing.routeTendency().competingRoutes().size());
        assertEquals(RouteType.ELECTRONIC_PICKLE.name(), briefing.routeTendency().competingRoutes().get(0).routeType());
        assertEquals(RouteType.SINGING_IDOL.name(), briefing.routeTendency().competingRoutes().get(1).routeType());
        assertTrue(briefing.routeTendency().reason().contains("路线未定"));
    }

    private void givenBriefingDependencies(Vup vup, Map<String, Integer> routeScores) {
        when(vupService.requireActiveVup(vup.getUserId())).thenReturn(vup);
        when(dayFlowService.routeScores(vup)).thenReturn(routeScores);
        when(riskDebtMapper.findOpenByVupId(vup.getId())).thenReturn(List.of());
        when(stageObjectiveService.objectiveFor(vup)).thenReturn(new StageObjectiveService.StageObjectivePlan(
                "阶段目标",
                "保持节奏",
                0,
                "0/3",
                List.of(new StageObjectiveItemDTO("补路线证据", "未完成", "继续观察", false)),
                0,
                3
        ));
        when(stageObjectiveService.objectiveHitStreakBefore(vup, vup.getDayCount())).thenReturn(0);
        when(stageObjectiveService.objectiveActionTypeFor(vup, "杂谈复盘")).thenReturn("TRAIN_TALK");
    }

    private Vup vup(int day, String currentRoute, Map<String, Integer> routeScores) {
        Vup vup = new Vup();
        vup.setId(day + 100L);
        vup.setUserId(day + 991L);
        vup.setName("stage-briefing-vup");
        vup.setPersona("阶段节奏测试员");
        vup.setDayCount(day);
        vup.setCurrentRoute(currentRoute);
        vup.setRouteScoreJson(jsonService.write(routeScores));
        vup.setExpectationJson("{}");
        vup.setFans(120);
        vup.setTrueFans(50);
        vup.setFunFans(30);
        vup.setUnicornFans(20);
        vup.setDdFans(20);
        vup.setReputation(60);
        return vup;
    }

    private Map<String, Integer> routeScores(Map<String, Integer> overrides) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), overrides.getOrDefault(routeType.name(), 0));
        }
        return scores;
    }

    private String routeLabel(String routeType) {
        return switch (routeType) {
            case "SLICE_SAINT" -> "切片路线";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "SINGING_IDOL" -> "歌势路线";
            case "DANCE_MEME" -> "梗舞整活";
            default -> "路线仍在摇摆";
        };
    }
}
