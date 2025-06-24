package kopo.jeonnam.service.gpt;

import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import java.util.List;
import java.util.Set;

public interface IGptService {
    String createScheduleFromRequest(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    );
    String createScheduleWithFavorites(
            Set<String> favoriteNames,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    );
}
