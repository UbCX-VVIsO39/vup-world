package com.example.vupworld;

import com.example.vupworld.dto.ComboDtos.ComboItemDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComboDiscoveryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetComboDiscovery() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // Register and login
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("{\"username\":\"combo_test\",\"password\":\"test123\",\"nickname\":\"ComboTester\"}")
                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"combo_test\",\"password\":\"test123\"}")
                .session(session))
                .andExpect(status().isOk());

        // Create VUP
        mockMvc.perform(post("/api/vup/create")
                .contentType("application/json")
                .content("{\"name\":\"ComboVup\",\"persona\":\"Test combo\"}")
                .session(session))
                .andExpect(status().isOk());

        // Call the combo discovery endpoint and verify the DTO structure
        mockMvc.perform(get("/api/combo/discovery").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.headline").isString())
                .andExpect(jsonPath("$.data.headline").isNotEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(8))
                .andExpect(jsonPath("$.data.collectionLabel").value("账号图鉴"))
                .andExpect(jsonPath("$.data.discovered").isArray())
                .andExpect(jsonPath("$.data.locked").isArray())
                .andExpect(jsonPath("$.data.locked").isNotEmpty())
                .andExpect(jsonPath("$.data.nextHint").isString());
    }

    @Test
    void shouldHaveComboItemStructure() {
        // Test the DTO structure
        ComboItemDTO item = new ComboItemDTO(
                "test_combo",
                "Test Combo",
                "Test hint",
                true,
                "Test evidence",
                "teal"
        );
        assertEquals("test_combo", item.comboKey());
        assertEquals("Test Combo", item.label());
        assertTrue(item.discovered());
    }
}
