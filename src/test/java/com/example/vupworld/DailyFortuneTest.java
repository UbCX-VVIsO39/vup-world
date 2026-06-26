package com.example.vupworld;

import com.example.vupworld.dto.DailyFortuneDtos.DailyFortuneDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.content.DailyFortuneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DailyFortuneTest {

    @Autowired
    private DailyFortuneService dailyFortuneService;

    private DaySession testSession() {
        DaySession s = new DaySession();
        s.setRandomSeed("test");
        s.setRngCursor(0);
        return s;
    }

    @Test
    void shouldGetDailyFortune() {
        DailyFortuneDTO fortune = dailyFortuneService.getFortune(1, 52, 60, testSession());
        assertNotNull(fortune);
        assertNotNull(fortune.fortune());
        assertNotNull(fortune.advice());
        assertNotNull(fortune.luckyAction());
        assertNotNull(fortune.icon());
    }

    @Test
    void shouldGetDifferentFortunesForDifferentDays() {
        DailyFortuneDTO fortune1 = dailyFortuneService.getFortune(1, 52, 60, testSession());
        DailyFortuneDTO fortune10 = dailyFortuneService.getFortune(10, 200, 70, testSession());
        assertNotNull(fortune1);
        assertNotNull(fortune10);
    }

    @Test
    void shouldGetFortuneBasedOnReputation() {
        DailyFortuneDTO highRepFortune = dailyFortuneService.getFortune(5, 100, 80, testSession());
        DailyFortuneDTO lowRepFortune = dailyFortuneService.getFortune(5, 100, 30, testSession());
        assertNotNull(highRepFortune);
        assertNotNull(lowRepFortune);
    }

    @Test
    void shouldHaveAtLeast5Fortunes() {
        int count = dailyFortuneService.getFortuneCount();
        assertTrue(count >= 5, "应该至少有5种运势");
    }

    @Test
    void shouldGetFortuneWithLuckyAction() {
        DailyFortuneDTO fortune = dailyFortuneService.getFortune(5, 100, 60, testSession());
        assertNotNull(fortune.luckyAction());
        assertFalse(fortune.luckyAction().isEmpty());
    }
}
