package com.example.vupworld;

import com.example.vupworld.dto.EventDtos.EventDTO;
import com.example.vupworld.service.content.ContentCatalogService;
import com.example.vupworld.service.event.EventExpansionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PublicSafeContentLanguageTest {

    private static final int MIN_ENDING_SUBTITLES_PER_ENDING = 12;
    private static final int MAX_SCREENSHOT_SUBTITLE_LENGTH = 32;

    private static final List<String> ENDING_TYPES = List.of(
            "SINGING_IDOL",
            "SLICE_SAINT",
            "BLACK_RED_MAIN_STAGE",
            "MAIN_STAGE_KING",
            "CYBER_GIRLFRIEND",
            "GLORIOUS_GRADUATION",
            "ELECTRONIC_PICKLE",
            "DD_BUS_STOP",
            "UNKNOWN"
    );

    private static final List<String> ENDING_SUBTITLE_UNSAFE_TERMS = List.of(
            "叔叔",
            "中之人",
            "真实身份",
            "现实身份",
            "真名",
            "开盒",
            "人肉",
            "住址",
            "身份证",
            "手机号",
            "曝光",
            "扒"
    );

    @Autowired
    private ContentCatalogService contentCatalogService;

    @Autowired
    private EventExpansionService eventExpansionService;

    @Test
    void publicSafeContentKeepsVupMemesWithoutPlatformDogwhistlesOrPrivacyAttacks() {
        String corpus = publicContentCorpus();

        assertFalse(corpus.contains("叔叔给量"));
        assertFalse(corpus.contains("黑粉组织攻击"));
        assertFalse(corpus.contains("恶意举报"));
        assertFalse(corpus.contains("试图曝光"));
        assertFalse(corpus.contains("中之人身份"));

        assertTrue(corpus.contains("平台算法给量"));
        assertTrue(corpus.contains("舰长应援"));
        assertTrue(corpus.contains("烤肉组"));
        assertTrue(corpus.contains("二创授权"));
        assertTrue(corpus.contains("录播组补时间轴"));
        assertTrue(corpus.contains("不讨论现实身份"));
    }

    @Test
    void endingSubtitlesHaveReplayablePublicSafeDensityForEveryEnding() {
        Map<String, List<ContentCatalogService.ContentEntry>> subtitles =
                contentCatalogService.getEntries("ENDING_SUBTITLE");

        for (String endingType : ENDING_TYPES) {
            List<ContentCatalogService.ContentEntry> entries = subtitles.get(endingType);
            assertNotNull(entries, () -> endingType + " should have ending subtitles");
            assertTrue(entries.size() >= MIN_ENDING_SUBTITLES_PER_ENDING,
                    () -> endingType + " should have at least "
                            + MIN_ENDING_SUBTITLES_PER_ENDING
                            + " ending subtitles after density expansion");

            Set<String> uniqueTexts = new HashSet<>();
            for (ContentCatalogService.ContentEntry entry : entries) {
                String text = entry.text();
                assertNotNull(text, () -> endingType + " subtitle text should not be null");
                assertFalse(text.isBlank(), () -> endingType + " subtitle text should not be blank");
                assertTrue(text.length() <= MAX_SCREENSHOT_SUBTITLE_LENGTH,
                        () -> endingType + " subtitle should stay screenshot-friendly: " + text);
                assertTrue(uniqueTexts.add(text), () -> endingType + " subtitle should be unique: " + text);
                assertFalse(text.matches(".*\\d{6,}.*"), () -> endingType + " subtitle should not look like private data: " + text);
                assertFalse(text.contains("@"), () -> endingType + " subtitle should not reference a handle: " + text);

                for (String unsafeTerm : ENDING_SUBTITLE_UNSAFE_TERMS) {
                    assertFalse(text.contains(unsafeTerm),
                            () -> endingType + " subtitle should not contain unsafe term " + unsafeTerm + ": " + text);
                }
            }
            assertEquals(entries.size(), uniqueTexts.size(), () -> endingType + " subtitles should be unique");
        }
    }

    private String publicContentCorpus() {
        List<String> lines = new ArrayList<>();
        for (Map<String, List<ContentCatalogService.ContentEntry>> category : contentCatalogService.getCatalog().values()) {
            for (List<ContentCatalogService.ContentEntry> entries : category.values()) {
                for (ContentCatalogService.ContentEntry entry : entries) {
                    lines.add(entry.text());
                    lines.add(entry.json());
                }
            }
        }
        addEvents(lines, eventExpansionService.getBadEvents());
        addEvents(lines, eventExpansionService.getChoiceEvents());
        addEvents(lines, eventExpansionService.getMemeEvents());
        return String.join("\n", lines);
    }

    private void addEvents(List<String> lines, List<EventDTO> events) {
        for (EventDTO event : events) {
            lines.add(event.title());
            lines.add(event.description());
            lines.add(event.effect());
        }
    }
}
