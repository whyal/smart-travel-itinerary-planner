package com.yonglun.itineraryassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiKeyFilterTest {

    @RestController
    static class Probe {
        @PostMapping("/api/probe")
        String write() {
            return "ok";
        }

        @GetMapping("/health")
        String health() {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new Probe()).addFilters(new ApiKeyFilter("secret")).build();
    }

    @Test
    void rejectsApiCallWithoutKey() throws Exception {
        mockMvc.perform(post("/api/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsApiCallWithWrongKey() throws Exception {
        mockMvc.perform(post("/api/probe").header(ApiKeyFilter.HEADER, "nope")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsReadWithoutKey() throws Exception {
        mockMvc.perform(get("/api/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void allowsApiCallWithKey() throws Exception {
        mockMvc.perform(post("/api/probe").header(ApiKeyFilter.HEADER, "secret")).andExpect(status().isOk());
    }

    @Test
    void letsPreflightAndNonApiPathsThrough() throws Exception {
        mockMvc.perform(options("/api/probe")).andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }
}
