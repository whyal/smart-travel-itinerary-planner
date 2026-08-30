package com.yonglun.itineraryassistant.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that {@link GlobalExceptionHandler} returns the standardized
 * {@link com.yonglun.itineraryassistant.dto.ApiErrorResponse} envelope for
 * each handled exception type.
 *
 * <p>A minimal stub controller is registered alongside the advice so that
 * MockMvc can trigger real exception paths without spinning up a full
 * Spring application context.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    /** Minimal stub controller that throws on demand, used only in this test. */
    @RestController
    @RequestMapping("/test")
    static class StubController {

        @GetMapping("/generic-error")
        public String throwGeneric() {
            throw new RuntimeException("something went wrong internally");
        }

        @GetMapping("/missing-param")
        public String requireParam(@RequestParam String required) {
            return required;
        }

        @PostMapping(value = "/bad-body", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String readBody(@RequestBody java.util.Map<String, Object> body) {
            return body.toString();
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void genericException_returns500WithErrorEnvelope() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void missingRequiredParam_returns400WithErrorEnvelope() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("required")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void malformedJsonBody_returns400WithErrorEnvelope() throws Exception {
        mockMvc.perform(post("/test/bad-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed or missing request body")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
