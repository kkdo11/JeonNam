//package kopo.jeonnam.config;
//
//import kopo.jeonnam.service.theme.IRecommendCoursePlanService;
//import kopo.jeonnam.repository.mongo.theme.RecommendCourseRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class RecommendCoursePlanDataLoader implements CommandLineRunner {
//
//    private final IRecommendCoursePlanService recommendCoursePlanService;
//    private final RecommendCourseRepository recommendCourseRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("▶ 추천 코스 상세 정보 자동 로딩 시작");
//
//        var courseList = recommendCourseRepository.findAll();
//        int totalSaved = 0;
//
//        for (var course : courseList) {
//            String courseKey = course.getCourseKey();
//            if (courseKey != null && !courseKey.isEmpty()) {
//                // 이미 데이터 존재 여부 체크 (count > 0이면 skip)
//                boolean exists = recommendCoursePlanService.existsAnyByCourseKey(courseKey);
//                if (exists) {
//                    log.info("[RecommendCoursePlanDataLoader] courseKey={} 데이터 이미 존재, 저장 건너뜀", courseKey);
//                    continue;
//                }
//
//                try {
//                    int saved = recommendCoursePlanService.fetchAndSaveRecommendCoursePlans(courseKey);
//                    totalSaved += saved;
//                    log.info("[RecommendCoursePlanDataLoader] courseKey={} 저장 성공: {}건", courseKey, saved);
//                } catch (Exception e) {
//                    log.error("[RecommendCoursePlanDataLoader] courseKey={} 저장 실패: {}", courseKey, e.getMessage(), e);
//                }
//                // ✅ Add delay between calls to avoid API throttling
//                Thread.sleep(500); // sleep for 500 milliseconds (adjust as needed)
//            } else {
//                log.warn("[RecommendCoursePlanDataLoader] courseKey가 null 또는 빈 값입니다. course={}", course);
//            }
//        }
//    }
//}
