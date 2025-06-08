package kopo.jeonnam.controller.api;

import kopo.jeonnam.dto.csv.MediaSpotMapDTO;
import kopo.jeonnam.model.MediaSpot;
import kopo.jeonnam.service.csv.IMediaSpotService;
import kopo.jeonnam.service.api.ITmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/media-spots")
@RequiredArgsConstructor
public class MediaSpotController {

    private final IMediaSpotService mediaSpotService;
    private final ITmdbService tmdbService;

    public record MediaSpotResponse(
            String spotId,
            String spotNm,
            String spotArea,
            String posterUrl
    ) {}

    @GetMapping("/list")
    public List<MediaSpotResponse> getMediaSpotList() {
        List<MediaSpot> spots = mediaSpotService.getAllJeonnamSpots();
        log.info("촬영지 목록 조회, 총 {}개", spots.size());

        return spots.stream()
                .filter(spot -> spot.getPosterUrl() != null) // 👈 없는 애들 제외
                .map(spot -> {
                    String posterUrl = tmdbService.getPosterUrlByTitle(spot.getSpotNm()).orElse(null);
                    return new MediaSpotResponse(
                            spot.getId().toHexString(),
                            spot.getSpotNm(),
                            spot.getSpotArea(),
                            spot.getPosterUrl() // 👈 이제 여기선 바로 사용
                    );
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/map")
    public List<MediaSpotMapDTO> getMapReadyMediaSpots() {
        return mediaSpotService.getAllMapReadySpots(); // returns MediaSpotMapDTO list
    }
}
