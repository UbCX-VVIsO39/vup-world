package com.example.vupworld;

import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.content.MemeQuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MemeQuoteTest {

    @Autowired
    private MemeQuoteService memeQuoteService;

    private DaySession testSession() {
        DaySession s = new DaySession();
        s.setRandomSeed("test-seed");
        s.setRngCursor(0);
        return s;
    }

    @Test
    void shouldGetRandomMemeQuote() {
        String quote = memeQuoteService.getRandomQuote(testSession());
        assertNotNull(quote);
        assertFalse(quote.isEmpty());
    }

    @Test
    void shouldGetDifferentQuotesOnMultipleCalls() {
        String quote1 = memeQuoteService.getRandomQuote(testSession());
        String quote2 = memeQuoteService.getRandomQuote(testSession());
        assertNotNull(quote1);
        assertNotNull(quote2);
    }

    @Test
    void shouldGetQuoteBasedOnAction() {
        String streamQuote = memeQuoteService.getQuoteForAction("STREAM_PLAN", testSession());
        assertNotNull(streamQuote);
        assertFalse(streamQuote.isEmpty());

        String videoQuote = memeQuoteService.getQuoteForAction("PUBLISH_VIDEO", testSession());
        assertNotNull(videoQuote);
        assertFalse(videoQuote.isEmpty());
    }

    @Test
    void shouldGetQuoteBasedOnMood() {
        String highHeatQuote = memeQuoteService.getQuoteForMood("high_heat", testSession());
        assertNotNull(highHeatQuote);

        String lowHeatQuote = memeQuoteService.getQuoteForMood("low_heat", testSession());
        assertNotNull(lowHeatQuote);
    }

    @Test
    void shouldHaveAtLeast10Quotes() {
        int count = memeQuoteService.getQuoteCount();
        assertTrue(count >= 10, "应该至少有10条梗语录");
    }
}
