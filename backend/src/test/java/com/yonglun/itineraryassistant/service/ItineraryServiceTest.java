package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    private static final String VALID = """
            {"destination":"Kyoto","days":[{"dayNumber":1,"theme":"t","activities":[
            {"time":"09:00 AM","location":"KIX","description":"d","gettingThere":"g","travelMinutesFromPrevious":0,"durationMinutes":60,"operatingHours":"h","category":"ARRIVAL"},
            {"time":"12:00 PM","location":"Kinkaku-ji","description":"d","gettingThere":"g","travelMinutesFromPrevious":90,"durationMinutes":120,"operatingHours":"h","category":"SIGHTSEEING"},
            {"time":"06:00 PM","location":"KIX","description":"d","gettingThere":"g","travelMinutesFromPrevious":90,"durationMinutes":60,"operatingHours":"h","category":"DEPARTURE"}]}]}""";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ItineraryService itineraryService;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(chatClient, new ItineraryValidator(new ObjectMapper()));
    }

    private void stubResponses(Flux<String> first, Flux<String> second) {
        when(chatClient.prompt().system(anyString()).user(anyString()).stream().content())
                .thenReturn(first, second);
    }

    @Test
    void validDraftIsStreamedWithoutRetry() {
        stubResponses(Flux.just(VALID.substring(0, 20), VALID.substring(20)), Flux.just("unused"));

        List<String> chunks = itineraryService.streamItinerary("Days: 1").collectList().block();

        assertThat(chunks).containsExactly(VALID.substring(0, 20), VALID.substring(20));
    }

    @Test
    void invalidDraftIsFollowedByRetryMarkerAndRepair() {
        stubResponses(Flux.just("{\"destination\":\"Kyoto\"}"), Flux.just(VALID));

        List<String> chunks = itineraryService.streamItinerary("Days: 1").collectList().block();

        assertThat(chunks).containsExactly("{\"destination\":\"Kyoto\"}", ItineraryService.RETRY_MARKER, VALID);
    }

    @Test
    void validatorReportsRuleViolations() {
        ItineraryValidator validator = new ItineraryValidator(new ObjectMapper());
        String bad = """
                {"days":[{"activities":[
                {"time":"09:00 AM","location":"Kinkaku-ji","description":"d","gettingThere":"g","travelMinutesFromPrevious":0,"durationMinutes":60,"operatingHours":"h","category":"SIGHTSEEING"}]},
                {"activities":[
                {"time":"09:00 AM","location":"kinkaku-ji","description":"d","gettingThere":"","travelMinutesFromPrevious":0,"durationMinutes":60,"operatingHours":"h","category":"SIGHTSEEING"}]}]}""";

        assertThat(validator.violations(VALID, 1)).isEmpty();
        assertThat(validator.violations(bad, 3)).containsExactlyInAnyOrder(
                "The 'days' array has 2 days but exactly 3 were requested. Add day 3 with new venues, and move CHECK_OUT and DEPARTURE to day 3.",
                "Day 2 activity 'kinkaku-ji' is missing 'gettingThere'.",
                "Day 2 repeats venue 'kinkaku-ji', which already appears earlier in the trip.",
                "Day 1 must begin with an ARRIVAL activity.",
                "The last day must end with a DEPARTURE activity.");
        assertThat(validator.violations("not json", 1)).hasSize(1);
        assertThat(ItineraryValidator.expectedDays("Destination: Kyoto\nDays: 5\nPace: x")).isEqualTo(5);
        assertThat(ItineraryValidator.expectedDays("no days line")).isNull();
    }

    @Test
    void dayChecklistSpellsOutEveryDayNumber() {
        assertThat(ItineraryService.dayChecklist(3))
                .contains("exactly 3 days", "dayNumber 1, 2, 3, in that order", "Only day 3 has CHECK_OUT");
        assertThat(ItineraryService.dayChecklist(null)).isEmpty();
    }

    @Test
    void validatorFlagsActivitiesScheduledWithoutTravelTime() {
        ItineraryValidator validator = new ItineraryValidator(new ObjectMapper());
        String rushed = VALID.replace("\"time\":\"12:00 PM\"", "\"time\":\"10:00 AM\"");

        assertThat(validator.violations(rushed, 1)).containsExactly(
                "Day 1 activity 'Kinkaku-ji' starts at 10:00 AM, but 'KIX' ends at 10:00 AM and travel takes 90 min, so it cannot start before 11:30 AM.");
        assertThat(ItineraryValidator.minutesOfDay("12:30 AM")).isEqualTo(30);
        assertThat(ItineraryValidator.minutesOfDay("12:15 PM")).isEqualTo(735);
        assertThat(ItineraryValidator.minutesOfDay("18:05")).isEqualTo(1085);
        assertThat(ItineraryValidator.minutesOfDay("Evening")).isNull();
    }
}
