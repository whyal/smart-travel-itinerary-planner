package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.ItineraryFormDataDto;
import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.entity.SavedItinerary;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.repository.SavedItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SavedItineraryService {

    private final SavedItineraryRepository repository;
    private final ObjectMapper objectMapper;

    public SavedItineraryService(SavedItineraryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedItineraryResponse saveItinerary(SaveItineraryRequest request) {
        SavedItinerary entity = new SavedItinerary(
                null,
                request.conversationId(),
                request.destination(),
                request.daysCount(),
                toJson(request.formData()),
                toJson(request.itinerary()),
                request.rawText(),
                parseInstant(request.createdAt())
        );

        return mapToResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<SavedItineraryResponse> getAllSavedItineraries() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SavedItineraryResponse> getSavedItinerariesByConversationId(String conversationId) {
        return repository.findByConversationIdOrderByCreatedAtDesc(conversationId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SavedItineraryResponse> getSavedItineraryById(Long id) {
        return repository.findById(id).map(this::mapToResponse);
    }

    @Transactional
    public boolean deleteSavedItinerary(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
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
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
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

