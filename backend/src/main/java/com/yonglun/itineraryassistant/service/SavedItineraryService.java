package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.ItineraryFormDataDto;
import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.entity.SavedItinerary;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.repository.SavedItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SavedItineraryService {

    private static final Logger log = LoggerFactory.getLogger(SavedItineraryService.class);

    private final SavedItineraryRepository repository;
    private final ObjectMapper objectMapper;

    public SavedItineraryService(SavedItineraryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedItineraryResponse saveItinerary(SaveItineraryRequest request) {
        String formDataJson = toJson(request.formData());
        String itineraryJson = toJson(request.itinerary());
        String conversationId = sanitizeText(request.conversationId());
        SavedItinerary entity = conversationId == null ? new SavedItinerary()
                : repository.findFirstByConversationIdOrderByIdDesc(conversationId).orElseGet(SavedItinerary::new);
        entity.setConversationId(conversationId);
        entity.setDestination(sanitizeText(request.destination()));
        entity.setDaysCount(request.daysCount());
        entity.setFormDataJson(formDataJson);
        entity.setItineraryJson(itineraryJson);
        entity.setRawText(sanitizeText(request.rawText()));
        entity.setCreatedAt(parseInstant(request.createdAt()));

        return mapToResponse(repository.save(entity));
    }

    private SavedItineraryResponse mapToResponse(SavedItinerary entity) {
        return new SavedItineraryResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getDestination(),
                entity.getDaysCount(),
                fromJson(entity.getFormDataJson(), ItineraryFormDataDto.class),
                fromJson(entity.getItineraryJson(), Itinerary.class),
                entity.getRawText(),
                entity.getCreatedAt()
        );
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return sanitizeText(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize itinerary data for saving", e);
        }
    }

    private String sanitizeText(String input) {
        return input != null ? input.replace("\u0000", "") : null;
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Stored itinerary JSON could not be read as {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Instant parseInstant(String text) {
        if (text != null && !text.isBlank()) {
            try {
                return Instant.parse(text);
            } catch (Exception ignored) {
            }
        }
        return Instant.now();
    }
}

