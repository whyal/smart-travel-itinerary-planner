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
        ItineraryFormDataDto formData = new ItineraryFormDataDto("Tokyo", 4, "Fast", "Shopping", "High", "Arriving late", "2026-12-05");
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
    void testSaveItinerary_SanitizesNullBytes() {
        SaveItineraryRequest request = new SaveItineraryRequest(
                "session\u0000-1",
                "Tokyo\u0000",
                3,
                null,
                null,
                "raw\u0000 plan",
                null
        );

        SavedItinerary savedEntity = new SavedItinerary(
                1L, "session-1", "Tokyo", 3, null, null, "raw plan", Instant.now()
        );
        when(repository.save(any(SavedItinerary.class))).thenReturn(savedEntity);

        service.saveItinerary(request);

        ArgumentCaptor<SavedItinerary> captor = ArgumentCaptor.forClass(SavedItinerary.class);
        verify(repository, times(1)).save(captor.capture());
        SavedItinerary captured = captor.getValue();
        assertThat(captured.getConversationId()).isEqualTo("session-1");
        assertThat(captured.getDestination()).isEqualTo("Tokyo");
        assertThat(captured.getRawText()).isEqualTo("raw plan");
    }

    @Test
    void testSavingSameConversationTwiceUpdatesExistingRow() {
        SavedItinerary existing = new SavedItinerary(7L, "session-1", "Tokyo", 3, null, null, "old", Instant.now());
        when(repository.findFirstByConversationIdOrderByIdDesc("session-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedItinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedItineraryResponse response = service.saveItinerary(
                new SaveItineraryRequest("session-1", "Tokyo", 4, null, null, "new", null));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.rawText()).isEqualTo("new");
        assertThat(response.daysCount()).isEqualTo(4);
    }

    @Test
    void testSaveFailsInsteadOfStoringEmptyJsonWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        SavedItineraryService failingService = new SavedItineraryService(repository, failingMapper);
        SaveItineraryRequest request = new SaveItineraryRequest("s1", "Tokyo", 1, null, new Itinerary("Tokyo", List.of()), "raw", null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> failingService.saveItinerary(request));
        verifyNoInteractions(repository);
    }

    @Test
    void testReadReturnsNullItineraryForCorruptStoredJson() {
        SavedItinerary corrupt = new SavedItinerary(1L, "s1", "Tokyo", 1, null, "{not json", "raw", Instant.now());
        when(repository.save(any(SavedItinerary.class))).thenReturn(corrupt);

        SavedItineraryResponse response = service.saveItinerary(
                new SaveItineraryRequest("s1", "Tokyo", 1, null, null, "raw", null));

        assertThat(response.itinerary()).isNull();
        assertThat(response.destination()).isEqualTo("Tokyo");
    }
}
