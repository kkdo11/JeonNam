package kopo.jeonnam.service.gpt;

import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import java.util.List;

public interface IGptService {
    String createScheduleFromRequest(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    );
}
