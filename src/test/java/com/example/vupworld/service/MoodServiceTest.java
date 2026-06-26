package com.example.vupworld.service;

import com.example.vupworld.dto.MoodDtos.MoodType;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.MoodService;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.operating.OperatingPressureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {
    @Mock private BusinessLogMapper businessLogMapper;
    @Mock private RiskDebtMapper riskDebtMapper;
    @Mock private JsonService jsonService;
    @Mock private OperatingPressureService operatingPressureService;
    @InjectMocks private MoodService moodService;

    private Vup freshVup() {
        Vup vup = new Vup();
        vup.setId(1L);
        vup.setReputation(60);
        vup.setStamina(5);
        vup.setFans(100);
        vup.setWatchHeat(30);
        vup.setMemeLevel(20);
        return vup;
    }

    private DaySession freshSession() {
        DaySession session = new DaySession();
        session.setDay(0);
        session.setRandomSeed("test-seed");
        session.setRngCursor(0);
        return session;
    }

    private void stubDefaultMappers() {
        when(riskDebtMapper.findOpenByVupId(any())).thenReturn(Collections.emptyList());
        when(businessLogMapper.findRecentByVupId(any(), anyInt())).thenReturn(Collections.emptyList());
        when(operatingPressureService.pressureMoodEffect(any(), anyInt())).thenReturn(null);
    }

    @Test
    void returnsNonNullMood() {
        Vup vup = freshVup();
        DaySession session = freshSession();
        stubDefaultMappers();
        var result = moodService.getMood(vup, session);
        assertNotNull(result);
        assertNotNull(result.mood());
    }

    @Test
    void moodLineIsNonEmptyString() {
        Vup vup = freshVup();
        DaySession session = freshSession();
        stubDefaultMappers();
        var result = moodService.getMood(vup, session);
        assertFalse(result.moodLine().isBlank(),
                "moodLine should be a non-empty descriptive string");
    }

    @Test
    void moodTypeDependsOnVupState_highStamina_givesEnergized() {
        Vup vup = freshVup();
        vup.setStamina(10); // > 8 triggers ENERGIZED
        DaySession session = freshSession();
        stubDefaultMappers();
        var result = moodService.getMood(vup, session);
        assertEquals(MoodType.ENERGIZED, result.mood());
        assertNotNull(result.effect());
    }

    @Test
    void moodTypeDependsOnVupState_lowReputation_givesStressed() {
        Vup vup = freshVup();
        vup.setStamina(5); // not energized
        vup.setReputation(40); // < 45 triggers STRESSED
        vup.setWatchHeat(10); // not excited
        vup.setMemeLevel(10);
        DaySession session = freshSession();
        stubDefaultMappers();
        var result = moodService.getMood(vup, session);
        assertEquals(MoodType.STRESSED, result.mood());
    }

    @Test
    void moodTypeDependsOnVupState_highWatchHeat_givesExcited() {
        Vup vup = freshVup();
        vup.setStamina(5); // not energized
        vup.setReputation(60); // not stressed
        vup.setWatchHeat(70); // > 60 triggers EXCITED
        vup.setMemeLevel(10);
        DaySession session = freshSession();
        stubDefaultMappers();
        var result = moodService.getMood(vup, session);
        assertEquals(MoodType.EXCITED, result.mood());
    }

    @Test
    void moodIsDeterministicForSameInput() {
        Vup vup = freshVup();
        DaySession session = freshSession();
        stubDefaultMappers();
        var first = moodService.getMood(vup, session);
        var second = moodService.getMood(vup, session);
        assertEquals(first.mood(), second.mood(),
                "same VUP state and seed should yield the same mood type");
        assertEquals(first.moodLine(), second.moodLine(),
                "same VUP state and seed should yield the same mood line");
        assertEquals(first.effect(), second.effect(),
                "same VUP state and seed should yield the same effect description");
    }
}
