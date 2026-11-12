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
                .user(anyString())
                .advisors(any(Consumer.class))
                .stream()
                .content())
                .thenReturn(Flux.just("{\"destination\":", "\"Kyoto\"}"));

        Flux<String> flux = itineraryService.streamItinerary("3 days in Kyoto", "conv-1");

        List<String> chunks = flux.collectList().block();
        assertThat(chunks).containsExactly("{\"destination\":", "\"Kyoto\"}");
    }

    @Test
    void testSanitizeItinerary_PreservesGenericDestinationActivities() {
        Itinerary.Activity act1 = new Itinerary.Activity("Morning", "Tokyo", "Visit the bustling morning market");
        Itinerary.Activity act2 = new Itinerary.Activity("Afternoon", "Tokyo", "Stroll through central park");
        Itinerary.DayPlan day1 = new Itinerary.DayPlan(1, "Tokyo Exploration", List.of(act1, act2));

        Itinerary raw = new Itinerary("Tokyo", List.of(day1));
        Itinerary sanitized = itineraryService.sanitizeItinerary(raw);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.days().get(0).activities()).hasSize(2);
    }

    @Test
    void testSanitizeItinerary_RemovesDuplicateLocationsAcrossDays() {
        Itinerary.Activity act1 = new Itinerary.Activity("Morning", "Fushimi Inari Taisha", "Walk torii gates");
        Itinerary.Activity act2 = new Itinerary.Activity("Afternoon", "Kiyomizu-dera", "Historic wooden temple");
        Itinerary.DayPlan day1 = new Itinerary.DayPlan(1, "Southern & Eastern Kyoto", List.of(act1, act2));

        Itinerary.Activity act3 = new Itinerary.Activity("Morning", "fushimi inari taisha ", "Torii path again");
        Itinerary.Activity act4 = new Itinerary.Activity("Afternoon", "Arashiyama Bamboo Grove", "Walk the grove");
        Itinerary.DayPlan day2 = new Itinerary.DayPlan(2, "Western Kyoto", List.of(act3, act4));

        Itinerary raw = new Itinerary("Kyoto", List.of(day1, day2));

        Itinerary sanitized = itineraryService.sanitizeItinerary(raw);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.days()).hasSize(2);
        assertThat(sanitized.days().get(0).activities()).hasSize(2);
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

    @Test
    void testSanitizeItinerary_CleansMetadataPrefixesAndWhitespace() {
        Itinerary.Activity messyAct = new Itinerary.Activity(
                "  Time:   09:00 AM  \n",
                "  Title:  Osaka Castle (District: Chuo)  ",
                "  Destination: Osaka\nTitle: Osaka Castle\nDistrict: Chuo\n\nWalk through the ancient fortress grounds.  ",
                "  Transit:  5 min walk from Station  \n",
                "  Operating Hours:  09:00 AM - 05:00 PM  "
        );
        Itinerary.DayPlan day1 = new Itinerary.DayPlan(1, "Day 1: Castle & History", List.of(messyAct));
        Itinerary raw = new Itinerary("  Osaka  \n", List.of(day1));

        Itinerary sanitized = itineraryService.sanitizeItinerary(raw);

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.destination()).isEqualTo("Osaka");
        assertThat(sanitized.days().get(0).theme()).isEqualTo("Castle & History");

        Itinerary.Activity clean = sanitized.days().get(0).activities().get(0);
        assertThat(clean.time()).isEqualTo("09:00 AM");
        assertThat(clean.location()).isEqualTo("Osaka Castle");
        assertThat(clean.description()).isEqualTo("Walk through the ancient fortress grounds.");
        assertThat(clean.gettingThere()).isEqualTo("5 min walk from Station");
        assertThat(clean.operatingHours()).isEqualTo("09:00 AM - 05:00 PM");
    }
}
