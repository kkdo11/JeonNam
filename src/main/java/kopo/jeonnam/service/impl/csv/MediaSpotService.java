package kopo.jeonnam.service.impl.csv;

import kopo.jeonnam.dto.csv.MediaSpotDTO;
import kopo.jeonnam.model.MediaSpot;
import kopo.jeonnam.repository.mongo.csv.MediaSpotRepository;
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

    @Override
    public void loadMediaSpotsFromCsv(InputStream csvInputStream) throws Exception {
        List<MediaSpotDTO> dtos = CsvParserUtil.parseMediaSpots(csvInputStream);

        List<MediaSpot> spots = dtos.stream()
                .map(dto -> {
                    String spotNm = dto.spotNm().replace("촬영지", "").trim();
                    return MediaSpot.builder()
                            .spotNm(spotNm)
                            .spotArea(dto.spotArea())
                            .spotLegalDong(dto.spotLegalDong())
                            .spotRi(dto.spotRi())
                            .spotBunji(dto.spotBunji())
                            .spotRoadAddr(dto.spotRoadAddr())
                            .spotLon(dto.spotLon())
                            .spotLat(dto.spotLat())
                            .build();
                })
                .collect(Collectors.toList());

        repository.saveAll(spots);
        log.info("✅ MediaSpot 데이터 저장 완료, 총 {}개", spots.size());
    }

    @Override
    public List<MediaSpot> getAllJeonnamSpots() {
        return repository.findBySpotLegalDongContaining("전라남도");
    }

    @Override
    public List<MediaSpot> searchBySpotNm(String keyword) {
        return repository.findBySpotNmContainingIgnoreCase(keyword);
    }

    @Override
    public boolean existsAny() {
        return repository.count() > 0;
    }
}
