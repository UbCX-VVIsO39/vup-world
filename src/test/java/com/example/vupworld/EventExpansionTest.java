package com.example.vupworld;

import com.example.vupworld.dto.EventDtos.EventDTO;
import com.example.vupworld.service.event.EventExpansionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EventExpansionTest {

    @Autowired
    private EventExpansionService eventExpansionService;

    @Test
    void shouldGetGoodEvents() {
        List<EventDTO> events = eventExpansionService.getGoodEvents();
        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertTrue(events.size() >= 5, "应该至少有5个好事事件");
    }

    @Test
    void shouldGetBadEvents() {
        List<EventDTO> events = eventExpansionService.getBadEvents();
        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertTrue(events.size() >= 3, "应该至少有3个坏事事件");
    }

    @Test
    void shouldGetChoiceEvents() {
        List<EventDTO> events = eventExpansionService.getChoiceEvents();
        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertTrue(events.size() >= 3, "应该至少有3个选择事件");
    }

    @Test
    void shouldGetMemeEvents() {
        List<EventDTO> events = eventExpansionService.getMemeEvents();
        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertTrue(events.size() >= 5, "应该至少有5个梗事件");
    }

    @Test
    void shouldGetRandomEvent() {
        EventDTO event = eventExpansionService.getRandomEvent(1, 52, 0, 60);
        assertNotNull(event);
        assertNotNull(event.title());
        assertNotNull(event.description());
        assertNotNull(event.effect());
    }

    @Test
    void shouldSelectExpansionEventDeterministicallyForSameInputs() {
        EventDTO first = eventExpansionService.getRandomEvent(12, 1200, 55, 40);
        EventDTO second = eventExpansionService.getRandomEvent(12, 1200, 55, 40);

        assertEquals(first.id(), second.id(), "同一局面下扩展事件必须可复现");
    }

    @Test
    void shouldExposeExplainableExpansionCandidatePool() {
        List<EventDTO> highHeatCandidates = eventExpansionService.candidateEventsFor(22, 1500, 72, 45);
        List<EventExpansionService.CandidateReason> reasons = eventExpansionService.candidateReasonsFor(22, 1500, 72, 45);

        assertTrue(highHeatCandidates.stream().anyMatch(event -> "BAD".equals(event.eventType())));
        assertTrue(highHeatCandidates.stream().anyMatch(event -> "CHOICE".equals(event.eventType())));
        assertTrue(highHeatCandidates.stream().anyMatch(event -> "MEME".equals(event.eventType())));
        assertTrue(reasons.stream().anyMatch(reason -> "CHOICE".equals(reason.eventType())));
        assertTrue(reasons.stream().anyMatch(reason -> reason.reason().contains("watch_heat")));
        assertTrue(reasons.stream().anyMatch(reason -> "ENDGAME".equals(reason.eventType())));
        assertTrue(reasons.stream().anyMatch(reason -> "FAN_SCALE".equals(reason.eventType())));
    }

    @Test
    void shouldHaveAtLeast15Events() {
        int count = eventExpansionService.getTotalEventCount();
        assertTrue(count >= 15, "应该至少有15个事件");
    }

    @Test
    void publicEventCopyShouldAvoidSponsorCodedFanserviceLanguage() {
        List<String> forbiddenTerms = List.of(
                "金主",
                "土豪",
                "大额SC",
                "大量SC",
                "赛博女友",
                "女友营业",
                "恋爱营业",
                "陪伴感营业",
                "高强度陪伴感"
        );

        List<EventDTO> events = Stream.of(
                        eventExpansionService.getGoodEvents(),
                        eventExpansionService.getBadEvents(),
                        eventExpansionService.getChoiceEvents(),
                        eventExpansionService.getMemeEvents()
                )
                .flatMap(List::stream)
                .toList();

        for (EventDTO event : events) {
            String publicCopy = String.join(" ", event.title(), event.description(), event.effect());
            for (String forbiddenTerm : forbiddenTerms) {
                assertFalse(
                        publicCopy.contains(forbiddenTerm),
                        () -> event.id() + " should not contain sponsor-coded fanservice copy: " + forbiddenTerm
                );
            }
        }
    }
}
