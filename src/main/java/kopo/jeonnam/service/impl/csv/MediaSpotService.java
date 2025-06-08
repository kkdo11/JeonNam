package kopo.jeonnam.service.impl.csv;

import kopo.jeonnam.dto.csv.MediaSpotDTO;
import kopo.jeonnam.dto.csv.MediaSpotMapDTO;
import kopo.jeonnam.model.MediaSpot;
import kopo.jeonnam.repository.mongo.csv.MediaSpotRepository;
import kopo.jeonnam.service.api.ITmdbService;
import kopo.jeonnam.service.csv.IMediaSpotService;
import kopo.jeonnam.util.CsvParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaSpotService implements IMediaSpotService {

    private final MediaSpotRepository repository;
    private final ITmdbService tmdbService;

    @Override
    public void loadMediaSpotsFromCsv(InputStream csvInputStream) throws Exception {
        List<MediaSpotDTO> dtos = CsvParserUtil.parseMediaSpots(csvInputStream);

        List<MediaSpot> spots = dtos.stream()
                .map(dto -> {
                    String spotNm = dto.spotNm().replace("촬영지", "").trim();
                    String posterUrl = tmdbService.getPosterUrlByTitle(spotNm).orElse(null);

                    return MediaSpot.builder()
                            .spotNm(spotNm)
                            .spotArea(dto.spotArea())
                            .spotLegalDong(dto.spotLegalDong())
                            .spotRi(dto.spotRi())
                            .spotBunji(dto.spotBunji())
                            .spotRoadAddr(dto.spotRoadAddr())
                            .spotLon(dto.spotLon())
                            .spotLat(dto.spotLat())
                            .posterUrl(posterUrl) // 👈 캐시 저장
                            .build();
                })
                .collect(Collectors.toList());

        repository.saveAll(spots);
        log.info("✅ MediaSpot 데이터 저장 완료, 총 {}개", spots.size());
    }

    @Override
    public List<MediaSpot> getAllJeonnamSpots() {
        List<MediaSpot> all = repository.findAll();
        log.info("전체 촬영지 개수: {}", all.size());
        return all;
    }

    @Override
    public List<MediaSpot> searchBySpotNm(String keyword) {
        return repository.findBySpotNmContainingIgnoreCase(keyword);
    }

    @Override
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    public List<MediaSpotMapDTO> getAllMapReadySpots() {
        return repository.findAll().stream().map(spot -> {
            String address = String.format("%s %s %s %s %s",
                    safe(spot.getSpotArea()),
                    safe(spot.getSpotLegalDong()),
                    safe(spot.getSpotRi()),
                    safe(spot.getSpotRoadAddr()),
                    safe(spot.getSpotBunji())
            ).trim().replaceAll(" +", " ");

            return MediaSpotMapDTO.builder()
                    .spotNm(spot.getSpotNm())
                    .address(address)
                    .lat(parseDouble(spot.getSpotLat()))
                    .lon(parseDouble(spot.getSpotLon()))
                    .posterUrl(spot.getPosterUrl())
                    .build();
        }).toList();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

}
