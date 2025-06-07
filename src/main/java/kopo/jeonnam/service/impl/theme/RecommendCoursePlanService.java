package kopo.jeonnam.service.impl.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kopo.jeonnam.repository.entity.theme.RecommendCoursePlanEntity;
import kopo.jeonnam.repository.mongo.theme.RecommendCoursePlanRepository;
import kopo.jeonnam.service.theme.IRecommendCoursePlanService;
import kopo.jeonnam.util.NetworkUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 특정 추천 코스(courseKey)에 대한 상세 계획(Plan) 정보를 외부 API에서 가져와 MongoDB에 저장하는 서비스 구현체
 *
 * 협업 및 디버깅을 위해 주요 단계별로 상세 로그를 남기며,
 * 예외 상황, 데이터 파싱, 저장 등 모든 주요 로직에 주석을 추가하였습니다.
 */
@Service
public class RecommendCoursePlanService implements IRecommendCoursePlanService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendCoursePlanService.class);

    private final RecommendCoursePlanRepository recommendCoursePlanRepository;

    @Value("${recommendcourse.api.planlist.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    public RecommendCoursePlanService(RecommendCoursePlanRepository recommendCoursePlanRepository) {
        this.recommendCoursePlanRepository = recommendCoursePlanRepository;
    }

    /**
     * 특정 courseKey에 해당하는 추천 코스 계획 데이터를 외부 API로부터 가져와 MongoDB에 저장합니다.
     *
     * @param courseKey Plan 정보를 가져올 추천 코스의 고유 키 (예: "0000000455")
     * @return 성공적으로 저장되거나 업데이트된 추천 코스 계획 엔티티의 총 개수
     */
    @Override
    public int fetchAndSaveRecommendCoursePlans(String courseKey) {
        logger.info(">> fetchAndSaveRecommendCoursePlans 서비스 시작 (courseKey: {})", courseKey);
        try {
            // API 호출 URL 생성 및 로그
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = String.format("%s?serviceKey=%s&planCourseId=%s", apiUrl, encodedKey, courseKey);
            logger.info("API 호출 URL: {}", url);

            // API 호출 및 응답 수신
            String response = NetworkUtil.get(url);

            // 1. API 응답 유효성 검사 (null이거나 XML 형식이 아니면 오류)
            if (response == null || response.trim().isEmpty() || !response.trim().startsWith("<")) {
                logger.error("API 응답이 유효하지 않습니다 (null, 빈 문자열 또는 비XML 형식): {}", response);
                return 0;
            }

            // XML 파싱 및 JsonNode 변환
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode root = xmlMapper.readTree(response.getBytes(StandardCharsets.UTF_8));

            // 2. API 응답 헤더 파싱 및 결과 코드 확인
            JsonNode headerNode = root.path("header");
            String resultCode = headerNode.path("resultCode").asText("");
            String resultMsg = headerNode.path("resultMsg").asText("알 수 없는 오류");

            // API 호출 결과 코드 확인: "00"만 성공으로 간주
            if (!"00".equals(resultCode)) {
                logger.error("API 응답 오류 - 코드: {}, 메시지: {} (courseKey: {})", resultCode, resultMsg, courseKey);
                return 0;
            }

            // 3. 실제 아이템(데이터) 추출
            JsonNode itemsNode = root.path("body").path("items").path("item");
            List<RecommendCoursePlanEntity> entitiesToSave = new ArrayList<>();
            int parsedItemCount = 0;

            // 배열/단일 객체/없음 모두 처리
            if (itemsNode.isArray()) {
                logger.debug("courseKey {} 에 대한 아이템 (배열 형태): {}개", courseKey, itemsNode.size());
                for (JsonNode item : itemsNode) {
                    entitiesToSave.add(mapJsonNodeToEntity(item));
                    parsedItemCount++;
                }
            } else if (!itemsNode.isMissingNode()) {
                logger.debug("courseKey {} 에 대한 아이템 (단일 객체 형태)", courseKey);
                entitiesToSave.add(mapJsonNodeToEntity(itemsNode));
                parsedItemCount++;
            } else {
                logger.info("courseKey {} 에 대한 아이템 데이터가 없습니다. (items 노드 부재 또는 비어있음)", courseKey);
            }

            // 4. 파싱된 데이터가 있을 경우 MongoDB에 저장
            if (parsedItemCount > 0) {
                logger.info("courseKey {} 에 대해 {}개의 RecommendCoursePlanEntity 저장/업데이트 시도...", courseKey, parsedItemCount);
                recommendCoursePlanRepository.saveAll(entitiesToSave);
                logger.info("courseKey {} 에 대해 {}개의 RecommendCoursePlanEntity 저장/업데이트 완료.", courseKey, parsedItemCount);
            } else {
                logger.info("courseKey {} 에 대해 저장할 RecommendCoursePlanEntity가 없습니다.", courseKey);
            }
            return parsedItemCount;
        } catch (Exception e) {
            logger.error("courseKey {} 데이터 처리 중 예상치 못한 예외 발생: {}", courseKey, e.getMessage(), e);
            return 0;
        } finally {
            logger.info(">> fetchAndSaveRecommendCoursePlans 서비스 종료 (courseKey: {})", courseKey);
        }
    }

    /**
     * JsonNode에서 데이터를 추출하여 RecommendCoursePlanEntity 객체로 매핑합니다.
     * @param item JsonNode의 item 노드
     * @return RecommendCoursePlanEntity 객체
     */
    private RecommendCoursePlanEntity mapJsonNodeToEntity(JsonNode item) {
        // 각 필드별로 null-safe하게 매핑
        return new RecommendCoursePlanEntity(
                item.path("planInfoId").asText(null),
                item.path("planCourseId").asText(null),
                item.path("planDay").asText(null),
                item.path("planTime").asText(null),
                item.path("planName").asText(null),
                item.path("planArea").asText(null),
                item.path("planAddr").asText(null),
                item.path("planAddrDetail").asText(null),
                item.path("planLatitude").asText(null),
                item.path("planLongitude").asText(null),
                item.path("planPhone").asText(null),
                item.path("planFax").asText(null),
                item.path("planHomepage").asText(null),
                item.path("planParking").asText(null),
                item.path("planContents").asText(null)
        );
    }
}