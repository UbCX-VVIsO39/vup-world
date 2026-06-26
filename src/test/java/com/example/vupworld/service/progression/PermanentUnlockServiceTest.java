package com.example.vupworld.service.progression;

import com.example.vupworld.dto.MoodDtos.PermanentUnlockDTO;
import com.example.vupworld.mapper.GameUnlockMapper;
import com.example.vupworld.model.GameUnlock;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.JsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermanentUnlockServiceTest {

    @Mock private GameUnlockMapper gameUnlockMapper;
    @Mock private JsonService jsonService;
    private PermanentUnlockService service;

    @BeforeEach
    void setUp() {
        service = new PermanentUnlockService(gameUnlockMapper, jsonService);
    }

    // --- unlock() tests ---

    @Test
    void unlockEnding_insertsRecord() {
        when(gameUnlockMapper.existsByUserAndKey(eq(1L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(1L, "ELECTRONIC_PICKLE");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        GameUnlock saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("PERMANENT_UNLOCK", saved.getUnlockType());
        assertEquals("ELECTRONIC_PICKLE_ENDING", saved.getUnlockKey());
        assertEquals("ELECTRONIC_PICKLE", saved.getSourceEndingType());
    }

    @Test
    void unlockDuplicate_skipsInsert() {
        // Simulate the unlock already existing for any key in the unlockKeys list
        when(gameUnlockMapper.existsByUserAndKey(eq(1L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(true);

        service.unlock(1L, "ELECTRONIC_PICKLE");

        verify(gameUnlockMapper, never()).insert(any());
    }

    @Test
    void unlockUnknownEndingType_doesNothing() {
        service.unlock(1L, "NONEXISTENT_ENDING");

        verify(gameUnlockMapper, never()).insert(any());
    }

    @Test
    void unlockWithLegacyEndingType_normalizesAndInserts() {
        when(gameUnlockMapper.existsByUserAndKey(eq(1L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        // "LEGEND" should be normalized to "MAIN_STAGE_KING"
        service.unlock(1L, "LEGEND");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("MAIN_STAGE_KING", captor.getValue().getSourceEndingType());
        assertEquals("MAIN_STAGE_KING_ENDING", captor.getValue().getUnlockKey());
    }

    @Test
    void unlockWithLegacyEndingType_GRADUATION_normalizes() {
        when(gameUnlockMapper.existsByUserAndKey(eq(2L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(2L, "GRADUATION");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("GLORIOUS_GRADUATION", captor.getValue().getSourceEndingType());
    }

    @Test
    void unlockWithLegacyEndingType_BLACK_RED_normalizes() {
        when(gameUnlockMapper.existsByUserAndKey(eq(3L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(3L, "BLACK_RED");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("BLACK_RED_MAIN_STAGE", captor.getValue().getSourceEndingType());
    }

    @Test
    void unlockWithLegacyEndingType_CYBER_FAN_SERVICE_normalizes() {
        when(gameUnlockMapper.existsByUserAndKey(eq(4L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(4L, "CYBER_FAN_SERVICE");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("CYBER_GIRLFRIEND", captor.getValue().getSourceEndingType());
    }

    @Test
    void unlockWithLegacyEndingType_DANCE_MEME_normalizes() {
        when(gameUnlockMapper.existsByUserAndKey(eq(5L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(5L, "DANCE_MEME");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("SLICE_SAINT", captor.getValue().getSourceEndingType());
    }

    @Test
    void unlockWithLegacyEndingType_SOCIAL_COLLAB_normalizes() {
        when(gameUnlockMapper.existsByUserAndKey(eq(6L), eq("PERMANENT_UNLOCK"), anyString()))
                .thenReturn(false);

        service.unlock(6L, "SOCIAL_COLLAB");

        ArgumentCaptor<GameUnlock> captor = ArgumentCaptor.forClass(GameUnlock.class);
        verify(gameUnlockMapper).insert(captor.capture());
        assertEquals("DD_BUS_STOP", captor.getValue().getSourceEndingType());
    }

    @Test
    void unlockWithNullEndingType_doesNothing() {
        service.unlock(1L, null);

        verify(gameUnlockMapper, never()).insert(any());
    }

    @Test
    void unlockWithBlankEndingType_doesNothing() {
        service.unlock(1L, "   ");

        verify(gameUnlockMapper, never()).insert(any());
    }

    // --- getUnlocks() tests ---

    @Test
    void getUnlocks_empty_whenNoRecords() {
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUnlocks_returnsMatchingUnlock() {
        GameUnlock unlock = makeUnlock(1L, "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(unlock));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertEquals(1, result.size());
        PermanentUnlockDTO dto = result.get(0);
        assertEquals("ELECTRONIC_PICKLE_ENDING", dto.key());
        assertEquals("饭点固定席", dto.label());
        assertNotNull(dto.description());
        assertEquals("ELECTRONIC_PICKLE", dto.sourceEnding());
        assertNotNull(dto.effect());
        assertTrue(dto.effect().isEmpty(), "Effects should be empty by design (no stat inflation)");
    }

    @Test
    void getUnlocks_deduplicatesByEndingType() {
        // Two records for the same ending type (e.g. old legacy key + new key)
        GameUnlock old = makeUnlock(1L, "ELECTRONIC_PICKLE", "LEGACY_KEY_1");
        GameUnlock dup = makeUnlock(1L, "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(old, dup));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertEquals(1, result.size(), "Duplicate ending types should be deduplicated");
    }

    @Test
    void getUnlocks_normalizesLegacySourceEndingType() {
        // Record with legacy ending type "LEGEND" should normalize to "MAIN_STAGE_KING"
        GameUnlock unlock = makeUnlock(1L, "LEGEND", "LEGEND_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(unlock));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertEquals(1, result.size());
        assertEquals("MAIN_STAGE_KING", result.get(0).sourceEnding());
        assertEquals("MAIN_STAGE_KING_ENDING", result.get(0).key());
    }

    @Test
    void getUnlocks_handlesNullSourceEndingType_viaUnlockKey() {
        // Record with null sourceEndingType but a recognizable unlockKey
        GameUnlock unlock = makeUnlock(1L, null, "GRADUATION_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(unlock));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertEquals(1, result.size());
        assertEquals("GLORIOUS_GRADUATION", result.get(0).sourceEnding());
    }

    @Test
    void getUnlocks_skipsUnrecognizedRecords() {
        GameUnlock unknown = makeUnlock(1L, "FAKE_TYPE", "FAKE_KEY");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(unknown));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertTrue(result.isEmpty(), "Unrecognized ending types should be skipped");
    }

    @Test
    void getUnlocks_returnsMultipleDistinctEndings() {
        GameUnlock u1 = makeUnlock(1L, "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE_ENDING");
        GameUnlock u2 = makeUnlock(1L, "SINGING_IDOL", "SINGING_IDOL_ENDING");
        GameUnlock u3 = makeUnlock(1L, "UNKNOWN", "UNKNOWN_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(u1, u2, u3));

        List<PermanentUnlockDTO> result = service.getUnlocks(1L);

        assertEquals(3, result.size());
        List<String> keys = result.stream().map(PermanentUnlockDTO::key).toList();
        assertTrue(keys.contains("ELECTRONIC_PICKLE_ENDING"));
        assertTrue(keys.contains("SINGING_IDOL_ENDING"));
        assertTrue(keys.contains("UNKNOWN_ENDING"));
    }

    // --- applyBonuses() tests ---

    @Test
    void applyBonuses_noEffectWithEmptyMap() {
        // All current unlock definitions have empty effect maps
        GameUnlock unlock = makeUnlock(1L, "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE_ENDING");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(unlock));

        Vup vup = new Vup();
        vup.setFans(100);
        vup.setTrueFans(50);
        vup.setPopularity(30);

        service.applyBonuses(1L, vup);

        // No stat inflation by design
        assertEquals(100, vup.getFans());
        assertEquals(50, vup.getTrueFans());
        assertEquals(30, vup.getPopularity());
    }

    @Test
    void applyBonuses_deduplicatesMultipleRecordsForSameEnding() {
        GameUnlock u1 = makeUnlock(1L, "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE_ENDING");
        GameUnlock u2 = makeUnlock(1L, "ELECTRONIC_PICKLE", "LEGACY_KEY");
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(List.of(u1, u2));

        Vup vup = new Vup();
        vup.setFans(100);

        // Should not throw even with duplicates
        assertDoesNotThrow(() -> service.applyBonuses(1L, vup));
        assertEquals(100, vup.getFans());
    }

    @Test
    void applyBonuses_handlesEmptyUnlockList() {
        when(gameUnlockMapper.findByUserId(1L)).thenReturn(Collections.emptyList());

        Vup vup = new Vup();
        vup.setFans(100);

        assertDoesNotThrow(() -> service.applyBonuses(1L, vup));
        assertEquals(100, vup.getFans());
    }

    // --- isCurrentEndingType / normalizeEndingType / currentEndingTypes ---

    @Test
    void currentEndingTypes_containsAllNine() {
        List<String> types = service.currentEndingTypes();
        assertEquals(9, types.size());
        assertTrue(types.contains("ELECTRONIC_PICKLE"));
        assertTrue(types.contains("GLORIOUS_GRADUATION"));
        assertTrue(types.contains("SINGING_IDOL"));
        assertTrue(types.contains("SLICE_SAINT"));
        assertTrue(types.contains("BLACK_RED_MAIN_STAGE"));
        assertTrue(types.contains("MAIN_STAGE_KING"));
        assertTrue(types.contains("CYBER_GIRLFRIEND"));
        assertTrue(types.contains("DD_BUS_STOP"));
        assertTrue(types.contains("UNKNOWN"));
    }

    @Test
    void isCurrentEndingType_trueForKnownTypes() {
        assertTrue(service.isCurrentEndingType("ELECTRONIC_PICKLE"));
        assertTrue(service.isCurrentEndingType("GLORIOUS_GRADUATION"));
        assertTrue(service.isCurrentEndingType("SINGING_IDOL"));
    }

    @Test
    void isCurrentEndingType_normalizesLegacyTypes() {
        assertTrue(service.isCurrentEndingType("LEGEND"));
        assertTrue(service.isCurrentEndingType("GRADUATION"));
        assertTrue(service.isCurrentEndingType("BLACK_RED"));
        assertTrue(service.isCurrentEndingType("CYBER_FAN_SERVICE"));
        assertTrue(service.isCurrentEndingType("DANCE_MEME"));
        assertTrue(service.isCurrentEndingType("SOCIAL_COLLAB"));
    }

    @Test
    void isCurrentEndingType_falseForUnknown() {
        assertFalse(service.isCurrentEndingType("FAKE_ENDING"));
        assertFalse(service.isCurrentEndingType(""));
    }

    @Test
    void normalizeEndingType_mapsLegacyTypes() {
        assertEquals("MAIN_STAGE_KING", service.normalizeEndingType("LEGEND"));
        assertEquals("GLORIOUS_GRADUATION", service.normalizeEndingType("GRADUATION"));
        assertEquals("BLACK_RED_MAIN_STAGE", service.normalizeEndingType("BLACK_RED"));
        assertEquals("CYBER_GIRLFRIEND", service.normalizeEndingType("CYBER_FAN_SERVICE"));
        assertEquals("SLICE_SAINT", service.normalizeEndingType("DANCE_MEME"));
        assertEquals("DD_BUS_STOP", service.normalizeEndingType("SOCIAL_COLLAB"));
    }

    @Test
    void normalizeEndingType_preservesCurrentTypes() {
        assertEquals("ELECTRONIC_PICKLE", service.normalizeEndingType("ELECTRONIC_PICKLE"));
        assertEquals("SINGING_IDOL", service.normalizeEndingType("SINGING_IDOL"));
    }

    @Test
    void normalizeEndingType_isCaseInsensitive() {
        assertEquals("ELECTRONIC_PICKLE", service.normalizeEndingType("electronic_pickle"));
        assertEquals("MAIN_STAGE_KING", service.normalizeEndingType("legend"));
    }

    // --- unlock description completeness ---

    @Test
    void allUnlockDefinitions_haveNonEmptyDescriptions() {
        // Verify every ending type produces a DTO with meaningful description
        for (String endingType : service.currentEndingTypes()) {
            GameUnlock unlock = makeUnlock(1L, endingType, endingType + "_ENDING");
            when(gameUnlockMapper.findByUserId(99L)).thenReturn(List.of(unlock));

            List<PermanentUnlockDTO> dtos = service.getUnlocks(99L);
            assertEquals(1, dtos.size(), "Ending type " + endingType + " should produce a DTO");

            PermanentUnlockDTO dto = dtos.get(0);
            assertNotNull(dto.label(), "Label must not be null for " + endingType);
            assertFalse(dto.label().isBlank(), "Label must not be blank for " + endingType);
            assertNotNull(dto.description(), "Description must not be null for " + endingType);
            assertFalse(dto.description().isBlank(), "Description must not be blank for " + endingType);
            assertNotNull(dto.effect(), "Effect map must not be null for " + endingType);
            assertTrue(dto.effect().isEmpty(), "Effect map must be empty by design for " + endingType);

            reset(gameUnlockMapper);
        }
    }

    // --- helper ---

    private GameUnlock makeUnlock(Long userId, String sourceEndingType, String unlockKey) {
        GameUnlock u = new GameUnlock();
        u.setId(1L);
        u.setUserId(userId);
        u.setUnlockType("PERMANENT_UNLOCK");
        u.setUnlockKey(unlockKey);
        u.setSourceEndingType(sourceEndingType);
        u.setSourceRunScore(0);
        return u;
    }
}
