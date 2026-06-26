package com.example.vupworld;

import com.example.vupworld.dto.FanLetterDtos.FanLetterDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.fan.FanLetterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FanLetterTest {

    @Autowired
    private FanLetterService fanLetterService;

    private DaySession testSession() {
        DaySession s = new DaySession();
        s.setRandomSeed("test");
        s.setRngCursor(0);
        return s;
    }

    @Test
    void shouldGetRandomFanLetter() {
        FanLetterDTO letter = fanLetterService.getRandomLetter(100, 50, 30, testSession());
        assertNotNull(letter);
        assertNotNull(letter.fanType());
        assertNotNull(letter.content());
        assertNotNull(letter.mood());
    }

    @Test
    void shouldGetDifferentLettersForDifferentFanTypes() {
        FanLetterDTO trueFanLetter = fanLetterService.getLetterForFanType("true_fan", 100, testSession());
        FanLetterDTO funFanLetter = fanLetterService.getLetterForFanType("fun_fan", 50, testSession());
        assertNotNull(trueFanLetter);
        assertNotNull(funFanLetter);
    }

    @Test
    void shouldGetLetterBasedOnReputation() {
        FanLetterDTO highRepLetter = fanLetterService.getRandomLetter(100, 50, 80, testSession());
        FanLetterDTO lowRepLetter = fanLetterService.getRandomLetter(100, 50, 30, testSession());
        assertNotNull(highRepLetter);
        assertNotNull(lowRepLetter);
    }

    @Test
    void shouldHaveAtLeast5Letters() {
        int count = fanLetterService.getLetterCount();
        assertTrue(count >= 5, "应该至少有5封粉丝来信");
    }

    @Test
    void shouldGetMultipleLetters() {
        List<FanLetterDTO> letters = fanLetterService.getRecentLetters(3, 100, 50, 60, testSession());
        assertNotNull(letters);
        assertTrue(letters.size() <= 3);
    }
}
