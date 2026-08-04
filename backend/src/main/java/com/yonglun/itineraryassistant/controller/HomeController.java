package com.yonglun.itineraryassistant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @GetMapping("/message")
    public Map<String, String> getMessage() {
        return Map.of(
                "status", "success",
                "message", "Hello from Spring Boot!"
        );
    }
}
