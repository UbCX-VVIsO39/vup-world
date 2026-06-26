package com.example.vupworld.common;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void imageEndpointExceptionReturnsJsonErrorInsteadOfPresetPngContentType() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokenGalleryController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/gallery/broken.png"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void committedImageResponseExceptionDoesNotReturnJsonBody() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/gallery/broken.png");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.flushBuffer();

        ApiResponse<Object> errorBody = handler.handleException(
                new IllegalStateException("image stream failed after commit"),
                request,
                response
        );

        assertNotNull(errorBody);
        assertEquals("RESPONSE_COMMITTED", errorBody.code());
        assertEquals(MediaType.IMAGE_PNG_VALUE, response.getContentType());
    }

    @RestController
    private static class BrokenGalleryController {
        @GetMapping("/gallery/broken.png")
        void brokenPng(HttpServletResponse response) {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            throw new IllegalStateException("image stream failed");
        }
    }
}
