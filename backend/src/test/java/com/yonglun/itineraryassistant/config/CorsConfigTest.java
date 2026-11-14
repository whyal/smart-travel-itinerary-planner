package com.yonglun.itineraryassistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    static class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    @Test
    void allowsOnlyTheConfiguredOrigins() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();

        new CorsConfig(new String[]{"https://app.example.com", "http://localhost:3000"}).addCorsMappings(registry);

        assertThat(registry.configurations().get("/api/**").getAllowedOrigins())
                .containsExactly("https://app.example.com", "http://localhost:3000");
    }
}
