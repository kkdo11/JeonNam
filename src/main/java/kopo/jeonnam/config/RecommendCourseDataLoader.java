package kopo.jeonnam.config;

import kopo.jeonnam.service.theme.IRecommendCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Order(1)
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendCourseDataLoader implements CommandLineRunner {

    private final IRecommendCourseService recommendCourseService;

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ 남도 추천 여행 코스 자동 로딩 시작");

        // 기존 데이터가 있으면 저장하지 않음
        if (recommendCourseService.existsAny()) {
            log.info("ℹ️ 이미 추천 코스 데이터가 존재하여 자동 저장을 건너뜁니다.");
            return;
        }

        try {
            int savedCount = recommendCourseService.fetchAndSaveRecommendCourses();
            log.info("✅ 남도 추천 여행 코스 자동 저장 완료: {}건 저장됨", savedCount);
        } catch (Exception e) {
            log.error("❌ 남도 추천 여행 코스 저장 중 에러 발생", e);
        }
    }

}
