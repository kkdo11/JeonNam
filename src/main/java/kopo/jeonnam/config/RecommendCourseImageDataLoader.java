//package kopo.jeonnam.config;
//
//import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
//import kopo.jeonnam.service.theme.IRecommendCourseImageService;
//import kopo.jeonnam.service.theme.IRecommendCourseService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RecommendCourseImageDataLoader implements CommandLineRunner {
//
//    private final IRecommendCourseService recommendCourseService;
//    private final IRecommendCourseImageService recommendCourseImageService;
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info(">>> [자동실행] 추천 코스 이미지 저장 프로세스 시작");
//
//        List<RecommendCourseEntity> allCourses = recommendCourseService.getAllRecommendCourses();
//        if (allCourses.isEmpty()) {
//            log.warn("  추천 코스 데이터가 없습니다. 먼저 코스 데이터를 저장하세요.");
//            return;
//        }
//
//        int totalSavedImages = 0;
//        for (RecommendCourseEntity course : allCourses) {
//            String courseInfoIds = course.getCourseInfoIds();
//            if (courseInfoIds != null && !courseInfoIds.trim().isEmpty()) {
//                totalSavedImages += recommendCourseImageService.fetchAndSaveRecommendCourseImages(courseInfoIds);
//            } else {
//                log.debug("  코스 ID '{}'에 courseInfoIds가 없어 이미지 처리를 건너뜀.", course.get_id());
//            }
//        }
//
//        log.info(">>> 추천 코스 이미지 저장 완료 - 총 {}개의 이미지가 저장됨", totalSavedImages);
//    }
//}