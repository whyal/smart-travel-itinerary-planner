package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryValidatorTest {

    private final ItineraryValidator validator = new ItineraryValidator(new ObjectMapper());

    @ParameterizedTest
    @CsvSource({
            "12:00 AM, 0",
            "12:30 PM, 750",
            "01:00 PM, 780",
            "9:15 am, 555",
            "14:00, 840"
    })
    void parsesTwelveAndTwentyFourHourClocks(String time, int minutes) {
        assertThat(ItineraryValidator.minutesOfDay(time)).isEqualTo(minutes);
    }

    @Test
    void flagsActivityThatStartsBeforePreviousOneEnds() {
        String json = """
                {"days":[{"activities":[
                  {"time":"09:00 AM","location":"Airport","description":"d","gettingThere":"g","operatingHours":"h","durationMinutes":60,"travelMinutesFromPrevious":0,"category":"ARRIVAL"},
                  {"time":"12:30 PM","location":"Castle","description":"d","gettingThere":"g","operatingHours":"h","durationMinutes":120,"travelMinutesFromPrevious":30,"category":"SIGHTSEEING"},
                  {"time":"01:00 PM","location":"Ramen","description":"d","gettingThere":"g","operatingHours":"h","durationMinutes":60,"travelMinutesFromPrevious":15,"category":"DINING"},
                  {"time":"06:00 PM","location":"Airport","description":"d","gettingThere":"g","operatingHours":"h","durationMinutes":60,"travelMinutesFromPrevious":40,"category":"DEPARTURE"}
                ]}]}""";

        List<String> problems = validator.violations(json, 1);

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst()).contains("'Ramen' starts at 01:00 PM").contains("cannot start before 02:45 PM");
    }
}
