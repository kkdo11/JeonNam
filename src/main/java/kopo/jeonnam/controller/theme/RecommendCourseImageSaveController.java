package kopo.jeonnam.controller.theme;

import kopo.jeonnam.repository.entity.RecommendCourseEntity;
import kopo.jeonnam.service.theme.IRecommendCourseImageService;
import kopo.jeonnam.service.theme.IRecommendCourseService; // RecommendCourseEntity 조회를 위해 필요
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j // Lombok을 이용한 로깅
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 주입
@RestController // RESTful API 컨트롤러
@RequestMapping("/data/recommend-course-image") // 이미지 데이터 저장에 특화된 URL 경로
public class RecommendCourseImageSaveController {

    private final IRecommendCourseService recommendCourseService; // 모든 코스 엔티티 조회를 위해 필요
    private final IRecommendCourseImageService recommendCourseImageService; // 이미지 데이터 서비스

    /**
     * 모든 저장된 추천 코스의 courseInfoIds를 기반으로 이미지 데이터를 초기화/업데이트합니다.
     * 이 엔드포인트는 기존 데이터에 기반한 새로운 데이터 저장/갱신 작업을 시작하므로 POST를 사용합니다.
     *
     * @return 처리 결과 메시지
     */
    @PostMapping("/fetch-and-save")
    public ResponseEntity<String> fetchAndSaveRecommendCourseImages() {
        log.info(">> 모든 추천 코스에 대한 이미지 데이터 가져오기 및 저장 요청 수신");
        try {
            int totalSavedImages = 0;
            // MongoDB에 저장된 모든 RecommendCourseEntity를 조회합니다.
            // 이전에 메인 코스 데이터를 먼저 저장했어야 합니다.
            List<RecommendCourseEntity> allCourses = recommendCourseService.getAllRecommendCourses();

            if (allCourses.isEmpty()) {
                log.warn("  저장된 추천 코스 데이터가 없어 이미지 데이터를 가져올 수 없습니다. 'fetch-courses'를 먼저 실행해주세요.");
                return ResponseEntity.ok("저장된 추천 코스 데이터가 없어 이미지 데이터를 초기화할 수 없습니다. 메인 코스 데이터를 먼저 저장해주세요.");
            }

            for (RecommendCourseEntity course : allCourses) {
                String courseInfoIds = course.getCourseInfoIds();
                if (courseInfoIds != null && !courseInfoIds.trim().isEmpty()) {
                    totalSavedImages += recommendCourseImageService.fetchAndSaveRecommendCourseImages(courseInfoIds);
                } else {
                    log.debug("  코스 ID '{}'에 courseInfoIds가 없어 이미지 처리를 건너뜜.", course.get_id());
                }
            }
            log.info(">> 모든 추천 코스에 대한 이미지 데이터 가져오기 및 저장 완료. 총 {}개의 이미지 데이터가 저장/업데이트됨.", totalSavedImages);
            return ResponseEntity.ok("총 " + totalSavedImages + "개의 이미지 데이터가 성공적으로 가져와 저장되었습니다.");

        } catch (Exception e) {
            log.error("!! 이미지 데이터 가져오기 및 저장 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 데이터 가져오기 및 저장 실패: " + e.getMessage());
        }
    }
}