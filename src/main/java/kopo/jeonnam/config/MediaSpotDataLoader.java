package kopo.jeonnam.config;

import kopo.jeonnam.service.csv.IMediaSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaSpotDataLoader implements CommandLineRunner {

    private final IMediaSpotService mediaSpotService;

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ MediaSpot CSV 데이터 로딩 시작");

        if (mediaSpotService.existsAny()) {
            log.info("▶ 이미 MediaSpot 데이터가 존재하여 저장을 건너뜁니다.");
            return;
        }

        ClassPathResource resource = new ClassPathResource("data/KC_502_LLR_MOVIE_DRMA_PLCE_2023.csv");
        try (InputStream inputStream = resource.getInputStream()) {
            mediaSpotService.loadMediaSpotsFromCsv(inputStream);
            log.info("▶ MediaSpot 데이터 저장 완료");
        } catch (Exception e) {
            log.error("❌ MediaSpot CSV 데이터 로딩 중 에러 발생", e);
        }
    }
}
