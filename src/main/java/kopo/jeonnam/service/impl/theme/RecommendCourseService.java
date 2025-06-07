package kopo.jeonnam.service.impl.theme;

import kopo.jeonnam.repository.entity.RecommendCourseEntity;
import kopo.jeonnam.repository.entity.RecommendCourseImageEntity; // 새로 추가
import kopo.jeonnam.repository.mongo.theme.RecommendCourseRepository;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseImageRepository; // 새로 추가
import kopo.jeonnam.service.theme.IRecommendCourseService;
import kopo.jeonnam.util.NetworkUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays; // 새로 추가
import java.util.Collections; // 새로 추가
import java.util.List;
import java.util.HashSet;
import java.util.Optional; // 새로 추가
import java.util.Set;
import java.util.stream.Collectors; // 새로 추가


@Service
public class RecommendCourseService implements IRecommendCourseService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendCourseService.class);

    private final RecommendCourseRepository recommendCourseRepository;
    private final RecommendCourseImageRepository recommendCourseImageRepository; // 새로 추가: 이미지 레포지토리 주입

    @Value("${recommendcourse.api.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    // 생성자 업데이트: RecommendCourseImageRepository도 주입받도록 변경
    public RecommendCourseService(RecommendCourseRepository recommendCourseRepository,
                                  RecommendCourseImageRepository recommendCourseImageRepository) {
        this.recommendCourseRepository = recommendCourseRepository;
        this.recommendCourseImageRepository = recommendCourseImageRepository;
    }

    // fetchAndSaveRecommendCourses 기존 코드 그대로 유지
    @Override
    public int fetchAndSaveRecommendCourses() {
        // ... (이전 코드와 동일)
        // mapJsonNodeToEntity 메서드 호출 부분도 이전과 동일하게 유지
        // ...
        return 0; // 예시
    }

    /**
     * 특정 추천 코스 (RecommendCourseEntity)의 상세 정보를 조회합니다.
     * courseKey는 RecommendCourseEntity의 _id에 매핑되므로 findById를 사용합니다.
     *
     * @param courseId 조회할 추천 코스의 _id
     * @return 조회된 RecommendCourseEntity (Optional로 감싸져 있음)
     */
    @Override
    public Optional<RecommendCourseEntity> getRecommendCourseDetail(String courseId) {
        logger.info(">> getRecommendCourseDetail 호출: courseId={}", courseId);
        return recommendCourseRepository.findById(courseId);
    }

    /**
     * 쉼표로 구분된 courseInfoId 문자열을 받아 해당 ID들에 연결된 이미지 데이터를 조회합니다.
     * 이 메서드는 RecommendCourseImageRepository의 findByCourseInfoIdIn 메서드를 호출합니다.
     *
     * @param courseInfoIds 쉼표로 구분된 courseInfoId 문자열
     * @return 조회된 RecommendCourseImageEntity 리스트
     */
    @Override
    public List<RecommendCourseImageEntity> getImagesByCourseInfoIds(String courseInfoIds) {
        logger.info(">> getImagesByCourseInfoIds 호출: courseInfoIds={}", courseInfoIds);
        if (courseInfoIds == null || courseInfoIds.trim().isEmpty()) {
            return Collections.emptyList(); // 유효한 ID가 없으면 빈 리스트 반환
        }

        List<String> idList = Arrays.asList(courseInfoIds.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // RecommendCourseImageRepository에 findByCourseInfoIdIn 메서드가 필요합니다.
        return recommendCourseImageRepository.findByCourseInfoIdIn(idList);
    }

    /**
     * JsonNode에서 데이터를 추출하여 RecommendCourseEntity 객체로 매핑합니다.
     * @param item JsonNode 형태의 단일 아이템 데이터
     * @return 매핑된 RecommendCourseEntity 객체
     */
    private RecommendCourseEntity mapJsonNodeToEntity(JsonNode item) {
        // ... (이전 코드와 동일하게 유지)
        // 이 mapJsonNodeToEntity는 RecommendCourseEntity를 위한 것이므로,
        // 이미지 엔티티의 필드와 혼동하지 않도록 주의하세요.
        return new RecommendCourseEntity(
                item.path("planCourseId").asText(null),
                item.path("planName").asText(null),
                item.path("planArea").asText(null),
                item.path("planAddr").asText(null),
                item.path("spotNm").asText(null),
                item.path("spotAddr").asText(null),
                item.path("courseKey").asText(null),
                item.path("courseInfoIds").asText(null),
                item.path("courseCategory").asText(null),
                item.path("courseName").asText(null),
                item.path("coursePeriod").asText(null),
                item.path("coursePersonType").asText(null),
                item.path("coursePersonCount").asText(null),
                item.path("courseContents").asText(null),
                item.path("courseArea").asText(null)
        );
    }
}