package kopo.jeonnam.controller.theme;

import kopo.jeonnam.service.theme.IRecommendCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 남도 추천 여행 코스 관련 API 컨트롤러
 * 외부 API에서 코스 데이터를 가져와 저장하는 역할을 담당합니다
 * @author
 * @since 2025-06-07
 */
@RestController
@RequestMapping("/api/recommend-course")
public class RecommendCourseController {

    private final IRecommendCourseService recommendCourseService;

    @Autowired
    public RecommendCourseController(IRecommendCourseService recommendCourseService) {
        this.recommendCourseService = recommendCourseService;
    }

    /**
     * 외부 API에서 남도 추천 여행 코스 데이터를 가져와 MongoDB에 저장
     * 저장된 건수와 함께 로그를 남깁니다.
     * @return 저장 결과 메시지
     */
    @GetMapping("/fetch")
    public ResponseEntity<String> fetchAndSaveRecommendCourses() {
        int savedCount;
        try {
            savedCount = recommendCourseService.fetchAndSaveRecommendCourses();
            org.slf4j.LoggerFactory.getLogger(getClass()).info("[RecommendCourseController] fetchAndSaveRecommendCourses 성공: {}건 저장", savedCount);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("[RecommendCourseController] fetchAndSaveRecommendCourses 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("저장 중 오류 발생: " + e.getMessage());
        }
        return ResponseEntity.ok(savedCount + "건 저장 완료");
    }
}
