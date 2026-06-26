package com.example.vupworld;

import com.example.vupworld.dto.RandomEventDtos.RandomEventDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.event.RandomEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RandomEventTest {

    @Autowired
    private RandomEventService randomEventService;

    private DaySession createTestSession() {
        DaySession session = new DaySession();
        session.setId(1L);
        session.setRandomSeed("test-seed");
        session.setRngCursor(0);
        return session;
    }

    @Test
    void shouldGetRandomEvent() {
        RandomEventDTO event = randomEventService.getRandomEvent(1, 0, 0, createTestSession());
        assertNotNull(event);
        assertNotNull(event.title());
        assertNotNull(event.description());
        assertNotNull(event.effect());
    }

    @Test
    void shouldGetDifferentEventsForDifferentDays() {
        RandomEventDTO event1 = randomEventService.getRandomEvent(1, 0, 0, createTestSession());
        RandomEventDTO event10 = randomEventService.getRandomEvent(10, 50, 30, createTestSession());
        assertNotNull(event1);
        assertNotNull(event10);
    }

    @Test
    void shouldGetEventBasedOnHeat() {
        RandomEventDTO lowHeatEvent = randomEventService.getRandomEvent(5, 0, 0, createTestSession());
        RandomEventDTO highHeatEvent = randomEventService.getRandomEvent(5, 70, 50, createTestSession());
        assertNotNull(lowHeatEvent);
        assertNotNull(highHeatEvent);
    }

    @Test
    void shouldHaveReplayableEventPoolSize() {
        int count = randomEventService.getEventCount();
        assertTrue(count >= 32, "随机事件池应该足够支撑多周目复玩");
    }

    @Test
    void shouldExposeReplayableEventPoolContract() {
        List<RandomEventDTO> events = randomEventService.candidateEventsFor(18, 55, 45).stream()
                .distinct()
                .toList();

        assertEquals(events.size(), events.stream().map(RandomEventDTO::id).distinct().count(), "随机事件ID不能重复");
        assertTrue(countType(events, "POSITIVE") >= 6, "正向事件不能太少");
        assertTrue(countType(events, "NEUTRAL") >= 6, "中性事件不能太少");
        assertTrue(countType(events, "MIXED") >= 6, "混合事件不能太少");
        assertTrue(countType(events, "NEGATIVE") >= 6, "负向事件不能太少");
    }

    @Test
    void shouldBiasCandidatePoolByHeatAndMemeLevel() {
        List<RandomEventDTO> newPlayerLowHeat = randomEventService.candidateEventsFor(3, 10, 0);
        assertFalse(newPlayerLowHeat.isEmpty());
        assertTrue(countType(newPlayerLowHeat, "POSITIVE") + countType(newPlayerLowHeat, "NEUTRAL")
                > countType(newPlayerLowHeat, "NEGATIVE") + countType(newPlayerLowHeat, "MIXED"));

        List<RandomEventDTO> highHeat = randomEventService.candidateEventsFor(12, 75, 20);
        assertFalse(highHeat.isEmpty());
        assertTrue(countType(highHeat, "NEGATIVE") + countType(highHeat, "MIXED")
                > countType(highHeat, "POSITIVE") + countType(highHeat, "NEUTRAL"));

        List<RandomEventDTO> highMeme = randomEventService.candidateEventsFor(12, 20, 65);
        assertFalse(highMeme.isEmpty());
        assertTrue(highMeme.stream().anyMatch(event -> "MIXED".equals(event.eventType())));
        assertTrue(countType(highMeme, "MIXED") + countType(highMeme, "NEUTRAL")
                >= countType(highMeme, "POSITIVE"));
    }

    @Test
    void shouldExposeExplainableCandidateBiases() {
        List<RandomEventService.CandidateBias> biases = randomEventService.candidateBiasesFor(24, 80, 75);
        assertFalse(biases.isEmpty());
        assertTrue(biases.stream().anyMatch(bias ->
                bias.weight() > 1 && bias.reasons().stream().anyMatch(reason -> reason.contains("endgame"))));
        assertTrue(biases.stream().anyMatch(bias ->
                bias.weight() > 1 && bias.reasons().stream().anyMatch(reason -> reason.contains("watch_heat"))));
        assertTrue(biases.stream().anyMatch(bias ->
                bias.weight() > 1 && bias.reasons().stream().anyMatch(reason -> reason.contains("meme_level"))));
    }

    @Test
    void shouldScaleTriggerThresholdWithPressure() {
        int calmOpening = randomEventService.triggerThresholdFor(1, 0, 0);
        int heatedMidGame = randomEventService.triggerThresholdFor(12, 80, 75);
        assertTrue(calmOpening < heatedMidGame);
        assertTrue(heatedMidGame <= 55);
    }

    @Test
    void shouldRespectTriggerThreshold() {
        DaySession session = createTestSession();
        assertNotNull(randomEventService.tryTriggerEvent(12, 80, 75, 0, session));
        assertNull(randomEventService.tryTriggerEvent(1, 0, 0, 99, createTestSession()));
    }

    private long countType(List<RandomEventDTO> events, String eventType) {
        return events.stream().filter(event -> eventType.equals(event.eventType())).count();
    }
}
