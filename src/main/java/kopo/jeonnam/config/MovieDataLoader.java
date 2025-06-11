package kopo.jeonnam.config;

import kopo.jeonnam.service.impl.csv.MovieCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring Boot 애플리케이션 시작 시 CSV 데이터를 자동으로 임포트하는 Runner 클래스입니다.
 * CommandLineRunner 인터페이스를 구현하여 애플리케이션 컨텍스트가 로드된 후 run 메서드가 실행되도록 합니다.
 */
@Slf4j
@RequiredArgsConstructor // final 필드에 대한 생성자 주입을 자동으로 처리합니다.
@Component // 스프링 빈으로 등록하여 컨테이너가 관리하도록 합니다.
public class MovieDataLoader implements CommandLineRunner {

    // MovieCsvService를 주입받아 CSV 임포트 기능을 사용합니다.
    private final MovieCsvService movieCsvService;

    /**
     * 애플리케이션 시작 시 자동으로 호출되는 메서드입니다.
     * 여기에서 CSV 파일 임포트 로직을 실행합니다.
     *
     * @param args 커맨드 라인 인자들 (여기서는 사용하지 않습니다)
     * @throws Exception CSV 임포트 중 발생할 수 있는 예외
     */
    @Override
    public void run(String... args) throws Exception {
        log.info(this.getClass().getName() + ".run() CSV 데이터 임포트 시작!");

        // CSV 파일 경로. 프로젝트 실행 디렉토리 기준 상대경로 또는 절대경로를 사용합니다.
        // Spring Boot JAR 파일로 패키징될 경우, 'src/main/resources' 경로는 접근 방식이 달라질 수 있습니다.
        // JAR 내부 리소스에 접근하려면 ClassPathResource 등을 사용해야 합니다.
        // 여기서는 개발 환경(IDE에서 실행)을 가정하여 파일 시스템 경로를 사용합니다.
        // 배포 시에는 application.properties 등에서 경로를 설정하는 것이 좋습니다.
        final String csvFilePath = "src/main/resources/data/mediaInfo.csv";

        try {
            log.info("CSV 파일 경로: {}", csvFilePath);
            movieCsvService.importCsv(csvFilePath);
            log.info("CSV 데이터가 성공적으로 MongoDB에 저장되었습니다.");
        } catch (Exception e) {
            log.error("CSV 데이터 임포트 중 에러 발생: {}", e.getMessage(), e);
            // 에러 발생 시 애플리케이션이 중단되지 않도록 예외를 다시 던지지 않거나, 적절히 처리합니다.
            // throw e; // 만약 임포트 실패 시 애플리케이션 시작을 중단하려면 주석 해제
        } finally {
            log.info(this.getClass().getName() + ".run() CSV 데이터 임포트 종료.");
        }
    }
}