package kopo.jeonnam.config;

import kopo.jeonnam.repository.mongo.movie.MovieRepository; // MovieRepository 임포트
import kopo.jeonnam.service.impl.csv.MovieCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class MovieDataLoader implements CommandLineRunner {

    private final MovieCsvService movieCsvService;
    private final MovieRepository movieRepository; // MovieRepository 주입
    // MovieRepository를 주입받아 MongoDB에 영화 데이터를 저장하는 역할을 합니다.
    @Override
    public void run(String... args) throws Exception {
        log.info(this.getClass().getName() + ".run() CSV 데이터 임포트 시작!");

        final String csvFilePath = "src/main/resources/data/mediaInfo.csv";

        try {
            // ✨✨✨ 여기에 조건 추가! ✨✨✨
            long existingMovieCount = movieRepository.count();
            if (existingMovieCount == 0) { // 데이터베이스에 영화가 하나도 없을 때만 임포트
                log.info("CSV 파일 경로: {}", csvFilePath);
                log.info("데이터베이스에 영화 데이터가 없어 CSV 임포트를 실행합니다.");
                movieCsvService.importCsv(csvFilePath);
                log.info("CSV 데이터가 성공적으로 MongoDB에 저장되었습니다.");
            } else {
                log.info("데이터베이스에 이미 {}개의 영화 데이터가 존재하여 CSV 임포트를 건너뜁니다.", existingMovieCount);
            }
            // ✨✨✨ 여기까지 ✨✨✨

        } catch (Exception e) {
            log.error("CSV 데이터 임포트 중 에러 발생: {}", e.getMessage(), e);
        } finally {
            log.info(this.getClass().getName() + ".run() CSV 데이터 임포트 종료.");
        }
    }
}