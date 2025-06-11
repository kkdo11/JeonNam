package kopo.jeonnam.config;

import kopo.jeonnam.service.theme.IRecommendCoursePlanService;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order; // Order 어노테이션 추가

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3) // 실행 순서를 지정 (RecommendCourseDataLoader, RecommendCourseImageDataLoader 다음에 실행)
public class RecommendCoursePlanDataLoader implements CommandLineRunner {

    private final IRecommendCoursePlanService recommendCoursePlanService;
    private final RecommendCourseRepository recommendCourseRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ 추천 코스 상세 정보 자동 로딩 시작");

        // 컬렉션에 데이터가 하나라도 존재하는지 확인
        if (recommendCoursePlanService.existsAny()) {
            log.info("ℹ️ 이미 추천 코스 상세 정보 데이터가 존재하여 자동 로딩을 건너뜝니다.");
            return; // 데이터가 있으면 메서드 종료
        }

        var courseList = recommendCourseRepository.findAll();
        int totalSaved = 0;

        for (var course : courseList) {
            String courseKey = course.getCourseKey();
            if (courseKey != null && !courseKey.isEmpty()) {
                // 이미 데이터 존재 여부 체크 (count > 0이면 skip) - 이 부분은 그대로 유지
                boolean exists = recommendCoursePlanService.existsAnyByCourseKey(courseKey);
                if (exists) {
                    log.info("[RecommendCoursePlanDataLoader] courseKey={} 데이터 이미 존재, 저장 건너뜀", courseKey);
                    continue;
                }

                try {
                    int saved = recommendCoursePlanService.fetchAndSaveRecommendCoursePlans(courseKey);
                    totalSaved += saved;
                    log.info("[RecommendCoursePlanDataLoader] courseKey={} 저장 성공: {}건", courseKey, saved);
                } catch (Exception e) {
                    log.error("[RecommendCoursePlanDataLoader] courseKey={} 저장 실패: {}", courseKey, e.getMessage(), e);
                }
            } else {
                log.warn("[RecommendCoursePlanDataLoader] courseKey가 null 또는 빈 값입니다. course={}", course);
            }
        }
        log.info("▶ 추천 코스 상세 정보 자동 로딩 완료. 총 {}건 저장됨.", totalSaved);
    }
}