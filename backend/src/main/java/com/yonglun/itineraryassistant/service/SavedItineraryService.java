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

    public SavedItineraryService(SavedItineraryRepository repository, Optional<ObjectMapper> objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.orElseGet(ObjectMapper::new);
    }

    @Transactional
    public SavedItineraryResponse saveItinerary(SaveItineraryRequest request) {
        String formDataJson = null;
        if (request.formData() != null) {
            try {
                formDataJson = objectMapper.writeValueAsString(request.formData());
            } catch (JsonProcessingException e) {
                formDataJson = "{}";
            }
        }

        String itineraryJson = null;
        if (request.itinerary() != null) {
            try {
                itineraryJson = objectMapper.writeValueAsString(request.itinerary());
            } catch (JsonProcessingException e) {
                itineraryJson = "{}";
            }
        }

        Instant createdAt;
        if (request.createdAt() != null && !request.createdAt().isBlank()) {
            try {
                createdAt = Instant.parse(request.createdAt());
            } catch (Exception e) {
                createdAt = Instant.now();
            }
        } else {
            createdAt = Instant.now();
        }

        SavedItinerary entity = new SavedItinerary(
                null,
                request.conversationId(),
                request.destination(),
                request.daysCount(),
                formDataJson,
                itineraryJson,
                request.rawText(),
                createdAt
        );

        SavedItinerary saved = repository.save(entity);
        return mapToResponse(saved);
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
        ItineraryFormDataDto formData = null;
        if (entity.getFormDataJson() != null && !entity.getFormDataJson().isBlank()) {
            try {
                formData = objectMapper.readValue(entity.getFormDataJson(), ItineraryFormDataDto.class);
            } catch (JsonProcessingException e) {
                // ignore parsing failure
            }
        }

        Itinerary itinerary = null;
        if (entity.getItineraryJson() != null && !entity.getItineraryJson().isBlank()) {
            try {
                itinerary = objectMapper.readValue(entity.getItineraryJson(), Itinerary.class);
            } catch (JsonProcessingException e) {
                // ignore parsing failure
            }
        }

        return new SavedItineraryResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getDestination(),
                entity.getDaysCount(),
                formData,
                itinerary,
                entity.getRawText(),
                entity.getCreatedAt()
        );
    }
}
