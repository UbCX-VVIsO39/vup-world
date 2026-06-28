package com.example.vupworld.service.risk;

import com.example.vupworld.dto.RewardDelta;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RewardCalculator, focusing on clampDetail label correctness.
 */
class RewardCalculatorTest {

    private RewardCalculator calculator;
    private Vup vup;

    @BeforeEach
    void setUp() {
        BalanceConfig config = new BalanceConfig(30, true, "STANDARD");
        JsonService jsonService = new JsonService(new ObjectMapper());
        calculator = new RewardCalculator(config, jsonService);

        vup = new Vup();
        vup.setReputation(60);
        vup.setStamina(8);
        vup.setSongPower(25);
        vup.setDancePower(15);
        vup.setTalkPower(10);
        vup.setMaxStamina(10);
    }

    /**
     * clampDetail must label vup.getStamina() as "stamina", not "songPower".
     * The JSON should contain both "reputation" and "stamina" keys.
     */
    @Test
    void clampDetail_labelsStaminaCorrectly() throws Exception {
        RewardDelta delta = calculator.resolveNonStreamAction(vup,
                com.example.vupworld.domain.ActionType.TRAIN_SONG, null);

        String clampJson = delta.clampDetail();
        assertNotNull(clampJson, "clampDetail should not be null");

        // Parse the JSON to verify keys
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> clamp = mapper.readValue(clampJson, Map.class);

        assertTrue(clamp.containsKey("reputation"),
                "clampDetail should contain 'reputation' key");
        assertTrue(clamp.containsKey("stamina"),
                "clampDetail should contain 'stamina' key but was: " + clampJson);
        assertFalse(clamp.containsKey("songPower"),
                "clampDetail should NOT contain 'songPower' key (bug: stamina was mislabeled). JSON was: " + clampJson);

        assertEquals(60, clamp.get("reputation"),
                "reputation value should match vup.getReputation()");
        assertEquals(8, clamp.get("stamina"),
                "stamina value should match vup.getStamina()");
    }

    /**
     * Verify clampDetail values reflect the actual vup state, not hardcoded values.
     */
    @Test
    void clampDetail_valuesMatchVupState() throws Exception {
        vup.setReputation(42);
        vup.setStamina(3);

        RewardDelta delta = calculator.resolveNonStreamAction(vup,
                com.example.vupworld.domain.ActionType.TRAIN_DANCE, null);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> clamp = mapper.readValue(delta.clampDetail(), Map.class);

        assertEquals(42, clamp.get("reputation"),
                "reputation should reflect vup state");
        assertEquals(3, clamp.get("stamina"),
                "stamina should reflect vup state");
    }
}
