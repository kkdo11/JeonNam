package kopo.jeonnam.dto.gpt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GptRequestDTO(
        @JsonProperty("locations")
        List<String> places,
        String startDate,
        int tripDays,
        String departurePlace,
        String departureTime,
        String additionalPrompt
) {}
