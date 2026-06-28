package com.example.vupworld.service.ending;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndingRuleEnginePriorityTest {
    private final JsonService jsonService = new JsonService(new ObjectMapper());
    private final EndingRuleEngine engine = new EndingRuleEngine(new BalanceConfig(30, true, "STANDARD"), jsonService);

    @Test
    void cyberGirlfriendTakesPriorityOverDdSliceAndBlackRedWhenFanServiceGateIsReady() {
        Vup vup = baseVup("ELECTRONIC_PICKLE", routeScores(Map.of(
                "ELECTRONIC_PICKLE", 120,
                "SOCIAL_COLLAB", 20,
                "SLICE_SAINT", 18,
                "BLACK_RED_MAIN_STAGE", 24
        )));
        setFans(vup, 160, 40, 200, 200);
        vup.setCommercialLevel(60);
        vup.setMemeLevel(70);
        vup.setWatchHeat(80);
        vup.setReputation(60);

        List<BusinessLog> logs = new ArrayList<>();
        logs.addAll(fanServiceLogs(4, 22));
        logs.addAll(stableLogs(2, 26));
        logs.add(log(28, "NPC_INTERACT", "{}", "{\"SOCIAL_COLLAB\":2}", "{}"));
        logs.add(log(29, "PUBLISH_VIDEO", "{}", "{\"SLICE_SAINT\":2}", "{}"));
        logs.addAll(blackRedLogs(4, 24));

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "STREAM_PLAN", "FAN_SERVICE", "{}", "{}"), List.of());

        assertEquals("CYBER_GIRLFRIEND", result.endingType());
        assertEquals("HIGH_UNICORN_COMMERCIAL_AND_FAN_SERVICE", result.rule());
        assertEquals(true, result.conditions().get("cyberReady"));
        assertEquals(true, result.conditions().get("collabRouteReady"));
        assertEquals(true, result.conditions().get("sliceRouteReady"));
        assertEquals(true, result.conditions().get("blackRedRouteReady"));
    }

    @Test
    void ddBusStopTakesPriorityOverSliceAndBlackRedWhenSocialGateIsReady() {
        Vup vup = baseVup("ELECTRONIC_PICKLE", routeScores(Map.of(
                "ELECTRONIC_PICKLE", 70,
                "SOCIAL_COLLAB", 20,
                "SLICE_SAINT", 20,
                "BLACK_RED_MAIN_STAGE", 24
        )));
        setFans(vup, 250, 125, 5, 220);
        vup.setMemeLevel(80);
        vup.setWatchHeat(90);
        vup.setReputation(50);

        List<BusinessLog> logs = new ArrayList<>();
        logs.addAll(stableLogs(2, 23));
        logs.add(log(28, "NPC_INTERACT", "{}", "{\"SOCIAL_COLLAB\":2}", "{}"));
        logs.add(log(29, "PUBLISH_VIDEO", "{}", "{\"SLICE_SAINT\":2}", "{}"));
        logs.addAll(blackRedLogs(4, 24));

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "STREAM_PLAN", "{}", "{}", "{}"), List.of());

        assertEquals("DD_BUS_STOP", result.endingType());
        assertEquals("SOCIAL_COLLAB_ROUTE_AND_HIGH_DD_RATIO", result.rule());
        assertEquals(true, result.conditions().get("collabRouteReady"));
        assertEquals(true, result.conditions().get("sliceRouteReady"));
        assertEquals(true, result.conditions().get("blackRedRouteReady"));
    }

    @Test
    void gloriousGraduationTakesPriorityOverCyberAndDdWhenFinalFanGroupMaintainIsClean() {
        Vup vup = baseVup("ELECTRONIC_PICKLE", routeScores(Map.of(
                "ELECTRONIC_PICKLE", 80,
                "SOCIAL_COLLAB", 20
        )));
        setFans(vup, 250, 0, 180, 220);
        vup.setCommercialLevel(60);
        vup.setWatchHeat(0);
        vup.setReputation(100);

        List<BusinessLog> logs = new ArrayList<>();
        logs.addAll(fanServiceLogs(4, 20));
        logs.addAll(stableLogs(4, 24));
        logs.add(log(29, "NPC_INTERACT", "{}", "{\"SOCIAL_COLLAB\":2}", "{}"));

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "FAN_GROUP_MAINTAIN", "{}", "{\"ELECTRONIC_PICKLE\":2}", "{}"), List.of());

        assertEquals("GLORIOUS_GRADUATION", result.endingType());
        assertEquals("GRADUATION_ACTION_HIGH_REPUTATION_CLEAN_RECORD", result.rule());
        assertEquals(true, result.conditions().get("graduationReady"));
        assertEquals(true, result.conditions().get("cyberReady"));
        assertEquals(true, result.conditions().get("collabRouteReady"));
    }

    @Test
    void danceMemeRouteUsesSliceBucketAndResolvesToSliceSaint() {
        Vup vup = baseVup("DANCE_MEME", routeScores(Map.of(
                "DANCE_MEME", 22
        )));
        setFans(vup, 250, 140, 5, 60);
        vup.setMemeLevel(70);
        vup.setWatchHeat(50);
        vup.setReputation(90);

        List<BusinessLog> logs = List.of(
                log(28, "TRAIN_DANCE", "{}", "{\"DANCE_MEME\":4}", "{}"),
                log(29, "TRAIN_DANCE", "{}", "{\"DANCE_MEME\":4}", "{}")
        );

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "TRAIN_DANCE", "{}", "{\"DANCE_MEME\":4}", "{}"), List.of());

        assertEquals("SLICE_SAINT", result.endingType());
        assertEquals(true, result.conditions().get("sliceRouteReady"));
        assertTrue(((Number) result.conditions().get("sliceRouteScore")).intValue() >= 16);
    }

    @Test
    void sliceSaintTakesPriorityOverBlackRedCrisisWhenSliceGateIsReady() {
        Vup vup = baseVup("SLICE_SAINT", routeScores(Map.of(
                "SLICE_SAINT", 24
        )));
        setFans(vup, 200, 220, 5, 80);
        vup.setMemeLevel(80);
        vup.setWatchHeat(90);
        vup.setReputation(20);

        List<BusinessLog> logs = List.of(
                log(28, "PUBLISH_VIDEO", "{}", "{\"SLICE_SAINT\":2}", "{}"),
                log(29, "PUBLISH_CLIP", "{}", "{\"SLICE_SAINT\":2}", "{}")
        );

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "PUBLISH_CLIP", "{}", "{\"SLICE_SAINT\":2}", "{}"), List.of(debt(2)));

        assertEquals("SLICE_SAINT", result.endingType());
        assertEquals(true, result.conditions().get("sliceRouteReady"));
        assertEquals(true, result.conditions().get("blackRedCrisisReady"));
    }

    @Test
    void blackRedMainStageRemainsReachableWhenNoHigherPriorityGateIsReady() {
        Vup vup = baseVup("BLACK_RED_MAIN_STAGE", routeScores(Map.of(
                "BLACK_RED_MAIN_STAGE", 24
        )));
        setFans(vup, 200, 180, 5, 80);
        vup.setMemeLevel(70);
        vup.setWatchHeat(85);
        vup.setReputation(50);

        List<BusinessLog> logs = blackRedLogs(4, 24);

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "PUBLISH_CLIP", "{}", "{\"BLACK_RED_MAIN_STAGE\":2}", "{}"), List.of());

        assertEquals("BLACK_RED_MAIN_STAGE", result.endingType());
        assertEquals("BLACK_RED_EVIDENCE_OR_HEAT_CRISIS", result.rule());
        assertEquals(true, result.conditions().get("blackRedRouteReady"));
    }

    @Test
    void mainStageKingTakesPriorityOverBlackRedMainStageForExtremeBlackRedRun() {
        Vup vup = baseVup("BLACK_RED_MAIN_STAGE", routeScores(Map.of(
                "BLACK_RED_MAIN_STAGE", 40
        )));
        setFans(vup, 200, 300, 5, 120);
        vup.setMemeLevel(95);
        vup.setWatchHeat(120);
        vup.setReputation(10);

        List<BusinessLog> logs = blackRedLogs(4, 24);

        EndingRuleEngine.EndingResult result = evaluate(vup, logs, log(30, "PUBLISH_CLIP", "{}", "{\"BLACK_RED_MAIN_STAGE\":2}", "{}"), List.of(debt(2)));

        assertEquals("MAIN_STAGE_KING", result.endingType());
        assertEquals("HIGH_MEME_LOW_REPUTATION_WITH_DEBT", result.rule());
    }

    private EndingRuleEngine.EndingResult evaluate(Vup vup, List<BusinessLog> logs, BusinessLog finalLog, List<RiskDebt> debts) {
        return engine.evaluate(vup, null, null, finalLog, debts, logs, true);
    }

    private Vup baseVup(String currentRoute, Map<String, Integer> routeScores) {
        Vup vup = new Vup();
        vup.setId(1001L);
        vup.setUserId(2001L);
        vup.setName("priority-test");
        vup.setStatus("ACTIVE");
        vup.setDayCount(30);
        vup.setCurrentRoute(currentRoute);
        vup.setRouteScoreJson(jsonService.write(routeScores));
        vup.setSongPower(12);
        vup.setReputation(70);
        vup.setWatchHeat(0);
        vup.setMemeLevel(0);
        vup.setCommercialLevel(0);
        return vup;
    }

    private void setFans(Vup vup, int trueFans, int funFans, int unicornFans, int ddFans) {
        vup.setTrueFans(trueFans);
        vup.setFunFans(funFans);
        vup.setUnicornFans(unicornFans);
        vup.setDdFans(ddFans);
        vup.setFans(trueFans + funFans + unicornFans + ddFans);
    }

    private Map<String, Integer> routeScores(Map<String, Integer> overrides) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), overrides.getOrDefault(routeType.name(), 0));
        }
        return scores;
    }

    private List<BusinessLog> fanServiceLogs(int count, int startDay) {
        List<BusinessLog> logs = new ArrayList<>();
        for (int index = 0; index < count; index += 1) {
            logs.add(log(startDay + index, "STREAM_PLAN", "FAN_SERVICE", "{}", "FAN_SERVICE"));
        }
        return logs;
    }

    private List<BusinessLog> stableLogs(int count, int startDay) {
        List<BusinessLog> logs = new ArrayList<>();
        for (int index = 0; index < count; index += 1) {
            logs.add(log(startDay + index, "FAN_GROUP_MAINTAIN", "{}", "{\"ELECTRONIC_PICKLE\":2}", "{}"));
        }
        return logs;
    }

    private List<BusinessLog> blackRedLogs(int count, int startDay) {
        List<BusinessLog> logs = new ArrayList<>();
        for (int index = 0; index < count; index += 1) {
            logs.add(log(startDay + index, "PUBLISH_CLIP", "{}", "{\"BLACK_RED_MAIN_STAGE\":2}", "{}"));
        }
        return logs;
    }

    private BusinessLog log(int day, String action, String weightDetail, String routeScoreChange, String result) {
        BusinessLog log = new BusinessLog();
        log.setDay(day);
        log.setAction(action);
        log.setWeightDetail(weightDetail);
        log.setRouteScoreChange(routeScoreChange);
        log.setResult(result);
        return log;
    }

    private RiskDebt debt(int severity) {
        RiskDebt debt = new RiskDebt();
        debt.setDebtType("TITLE_BACKFIRE");
        debt.setStatus("OPEN");
        debt.setSeverity(severity);
        debt.setCreateDay(28);
        debt.setDueDay(30);
        return debt;
    }
}
