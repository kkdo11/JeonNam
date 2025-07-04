package kopo.jeonnam.controller.theme;

import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseRepository;
import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
import kopo.jeonnam.service.theme.IRecommendCoursePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 남도 추천 여행 코스 Plan 관련 API 컨트롤러
 * recommend_course 컬렉션의 모든 courseKey별로 외부 API를 호출하여 Plan을 저장합니다
 * @author
 * @since 2025-06-07
 */
@RestController
@RequestMapping("/api/recommend-course-plan")
public class RecommendCoursePlanController {

    private final IRecommendCoursePlanService recommendCoursePlanService;
    private final RecommendCourseRepository recommendCourseRepository;

    @Autowired
    public RecommendCoursePlanController(IRecommendCoursePlanService recommendCoursePlanService, RecommendCourseRepository recommendCourseRepository) {
        this.recommendCoursePlanService = recommendCoursePlanService;
        this.recommendCourseRepository = recommendCourseRepository;
    }

    /**
     * recommend_course 컬렉션의 모든 courseKey(_id)별로 외부 API를 호출하여 Plan을 저장
     * 각 courseKey별 저장 성공/실패 로그를 남깁니다.
     * @return 전체 저장 결과 메시지
     */
    @GetMapping("/fetch/all-by-course-key")
    public ResponseEntity<String> fetchAndSaveAllByCourseKey() {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        List<RecommendCourseEntity> courseList = recommendCourseRepository.findAll();
        int totalSaved = 0;

        for (RecommendCourseEntity course : courseList) {
            String courseKey = course.getCourseKey();
            if (courseKey != null && !courseKey.isEmpty()) {
                try {
                    int saved = recommendCoursePlanService.fetchAndSaveRecommendCoursePlans(courseKey);
                    totalSaved += saved;
                    log.info("[RecommendCoursePlanController] courseKey={} 저장 성공: {}건", courseKey, saved);

                    // ✅ [NEW] Add delay between requests to avoid rate limiting
                    Thread.sleep(1000); // 0.5초 딜레이 (필요에 따라 1000ms로 조정)

                } catch (Exception e) {
                    log.error("[RecommendCoursePlanController] courseKey={} 저장 실패: {}", courseKey, e.getMessage(), e);
                }
            } else {
                log.warn("[RecommendCoursePlanController] courseKey가 null 또는 빈 값입니다. course={}", course);
            }
        }

        log.info("[RecommendCoursePlanController] 전체 저장 완료: {}건 (모든 courseKey)", totalSaved);
        return ResponseEntity.ok("총 " + totalSaved + "건 저장 완료 (모든 courseKey)");
    }

    /**
     * 전체 recommend_course_plan + 이미지 정보를 JSON 형태로 반환
     * (지도 마커용)
     */
    @GetMapping("/all")
    public ResponseEntity<List<RecommendCoursePlanDTO>> getAllCoursePlansWithImages() {
        List<RecommendCoursePlanDTO> result = recommendCoursePlanService.getAllPlansWithImages();
        return ResponseEntity.ok(result);
    }

    /**
     * 단일 recommend_course_plan + 이미지 조회
     * (상세 팝업용)
     */
    @GetMapping("/detail")
    public ResponseEntity<RecommendCoursePlanDTO> getPlanWithImagesById(@RequestParam String planInfoId) {
        return recommendCoursePlanService.getPlanWithImagesById(planInfoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
