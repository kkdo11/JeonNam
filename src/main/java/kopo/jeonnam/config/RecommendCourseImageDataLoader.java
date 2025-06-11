package kopo.jeonnam.config;

import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
import kopo.jeonnam.service.theme.IRecommendCourseImageService;
import kopo.jeonnam.service.theme.IRecommendCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order; // ⭐ Order 어노테이션을 추가하여 실행 순서를 명확히 해주는 게 좋아요!
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // ⭐ RecommendCourseDataLoader (Order 1) 다음으로 실행되도록 순서를 지정해주는 게 좋아요!
public class RecommendCourseImageDataLoader implements CommandLineRunner {

    private final IRecommendCourseService recommendCourseService;
    private final IRecommendCourseImageService recommendCourseImageService;

    @Override
    public void run(String... args) throws Exception {
        log.info(">>> [자동실행] 추천 코스 이미지 저장 프로세스 시작");

        // ⭐ 컬렉션이 존재하면 실행 안 하도록 추가된 로직!
        if (recommendCourseImageService.existsAnyImages()) {
            log.info("ℹ️ 이미 추천 코스 이미지 데이터가 존재하여 자동 저장을 건너뜝니다.");
            return; // 이미 데이터가 있으면 여기서 메서드 종료!
        }

        List<RecommendCourseEntity> allCourses = recommendCourseService.getAllRecommendCourses();
        if (allCourses.isEmpty()) {
            log.warn("  추천 코스 데이터가 없습니다. 먼저 코스 데이터를 저장하세요.");
            return;
        }

        try { // ⭐ 에러 처리를 위한 try-catch 블록 추가!
            int totalSavedImages = 0;
            for (RecommendCourseEntity course : allCourses) {
                String courseInfoIds = course.getCourseInfoIds();
                if (courseInfoIds != null && !courseInfoIds.trim().isEmpty()) {
                    totalSavedImages += recommendCourseImageService.fetchAndSaveRecommendCourseImages(courseInfoIds);
                } else {
                    log.debug("  코스 ID '{}'에 courseInfoIds가 없어 이미지 처리를 건너뜀.", course.get_id());
                }
            }
            log.info(">>> 추천 코스 이미지 저장 완료 - 총 {}개의 이미지가 저장됨", totalSavedImages);
        } catch (Exception e) {
            log.error("❌ 추천 코스 이미지 저장 중 에러 발생", e);
        }
    }
}