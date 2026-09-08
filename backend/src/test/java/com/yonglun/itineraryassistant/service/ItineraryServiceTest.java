package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ItineraryService itineraryService;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(chatClient);
    }

    @Test
    void testGenerateItinerary() {
        Itinerary expected = new Itinerary("Kyoto", List.of());

        when(chatClient.prompt()
                .user(anyString())
                .advisors(any(Consumer.class))
                .call()
                .entity(any(Class.class), any(Consumer.class)))
                .thenReturn(expected);

        Itinerary result = itineraryService.generateItinerary("3 days in Kyoto", "conv-1");

        assertThat(result).isNotNull();
        assertThat(result.destination()).isEqualTo("Kyoto");
    }

    @Test
    void testStreamItinerary() {
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .advisors(any(Consumer.class))
                .stream()
                .content())
                .thenReturn(Flux.just("{\"destination\":", "\"Kyoto\"}"));

        Flux<String> flux = itineraryService.streamItinerary("3 days in Kyoto", "conv-1");

        List<String> chunks = flux.collectList().block();
        assertThat(chunks).containsExactly("{\"destination\":", "\"Kyoto\"}");
    }

    @Test
    void testSanitizeItinerary_RemovesDuplicateLocationsAcrossDays() {
        Itinerary.Activity act1 = new Itinerary.Activity("Morning", "Fushimi Inari Taisha", "Walk torii gates");
        Itinerary.Activity act2 = new Itinerary.Activity("Afternoon", "Kiyomizu-dera", "Historic wooden temple");
        Itinerary.DayPlan day1 = new Itinerary.DayPlan(1, "Southern & Eastern Kyoto", List.of(act1, act2));

        // Day 2 mistakenly contains Fushimi Inari Taisha again (duplicate location)
        Itinerary.Activity act3 = new Itinerary.Activity("Morning", "fushimi inari taisha ", "Torii path again");
        Itinerary.Activity act4 = new Itinerary.Activity("Afternoon", "Arashiyama Bamboo Grove", "Walk the grove");
        Itinerary.DayPlan day2 = new Itinerary.DayPlan(2, "Western Kyoto", List.of(act3, act4));

        Itinerary raw = new Itinerary("Kyoto", List.of(day1, day2));

        Itinerary sanitized = itineraryService.sanitizeItinerary(raw);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.days()).hasSize(2);

        // Day 1 has both activities
        assertThat(sanitized.days().get(0).activities()).hasSize(2);

        // Day 2 has duplicate act3 removed, leaving only act4
        assertThat(sanitized.days().get(1).activities()).hasSize(1);
        assertThat(sanitized.days().get(1).activities().get(0).location()).isEqualTo("Arashiyama Bamboo Grove");
    }

    @Test
    void testSanitizeItinerary_PreservesCleanItinerary() {
        Itinerary.Activity act1 = new Itinerary.Activity("Morning", "Eiffel Tower", "Iconic view");
        Itinerary.Activity act2 = new Itinerary.Activity("Afternoon", "Louvre Museum", "Mona Lisa");
        Itinerary.DayPlan day1 = new Itinerary.DayPlan(1, "Paris Highlights", List.of(act1, act2));

        Itinerary raw = new Itinerary("Paris", List.of(day1));
        Itinerary sanitized = itineraryService.sanitizeItinerary(raw);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.days().get(0).activities()).hasSize(2);
    }

    @Test
    void testSanitizeItinerary_NullHandling() {
        assertThat(itineraryService.sanitizeItinerary(null)).isNull();

        Itinerary emptyDays = new Itinerary("Tokyo", null);
        assertThat(itineraryService.sanitizeItinerary(emptyDays)).isEqualTo(emptyDays);
    }
}
