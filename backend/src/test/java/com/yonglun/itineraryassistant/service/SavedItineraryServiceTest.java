package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.ItineraryFormDataDto;
import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.entity.SavedItinerary;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.repository.SavedItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedItineraryServiceTest {

    @Mock
    private SavedItineraryRepository repository;

    private ObjectMapper objectMapper;
    private SavedItineraryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new SavedItineraryService(repository, objectMapper);
    }

    @Test
    void testSaveItinerary() {
        ItineraryFormDataDto formData = new ItineraryFormDataDto("Tokyo", 4, "Fast", "Shopping", "High");
        Itinerary itinerary = new Itinerary("Tokyo", List.of());
        SaveItineraryRequest request = new SaveItineraryRequest(
                "session-1",
                "Tokyo",
                4,
                formData,
                itinerary,
                "Tokyo raw plan",
                "2026-08-08T10:00:00Z"
        );

        SavedItinerary savedEntity = new SavedItinerary(
                100L,
                "session-1",
                "Tokyo",
                4,
                "{}",
                "{}",
                "Tokyo raw plan",
                Instant.parse("2026-08-08T10:00:00Z")
        );

        when(repository.save(any(SavedItinerary.class))).thenReturn(savedEntity);

        SavedItineraryResponse response = service.saveItinerary(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.conversationId()).isEqualTo("session-1");
        assertThat(response.destination()).isEqualTo("Tokyo");
        assertThat(response.daysCount()).isEqualTo(4);

        ArgumentCaptor<SavedItinerary> captor = ArgumentCaptor.forClass(SavedItinerary.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDestination()).isEqualTo("Tokyo");
    }

    @Test
    void testGetAllSavedItineraries() {
        SavedItinerary entity = new SavedItinerary(
                1L, "conv-1", "Paris", 3, "{}", "{}", "raw", Instant.now()
        );
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entity));

        List<SavedItineraryResponse> result = service.getAllSavedItineraries();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).destination()).isEqualTo("Paris");
    }

    @Test
    void testGetSavedItineraryById() {
        SavedItinerary entity = new SavedItinerary(
                1L, "conv-1", "Paris", 3, "{}", "{}", "raw", Instant.now()
        );
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        Optional<SavedItineraryResponse> result = service.getSavedItineraryById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().destination()).isEqualTo("Paris");
    }

    @Test
    void testDeleteSavedItinerary() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean deleted = service.deleteSavedItinerary(1L);

        assertThat(deleted).isTrue();
        verify(repository, times(1)).deleteById(1L);
    }
}
