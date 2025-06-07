package kopo.jeonnam.service.impl.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kopo.jeonnam.repository.entity.RecommendCoursePlanEntity;
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
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = String.format("%s?serviceKey=%s&planCourseId=%s", apiUrl, encodedKey, courseKey);
            logger.info("API 호출 URL: {}", url);

            String response = NetworkUtil.get(url);

            // 1. API 응답 유효성 검사 (null이거나 XML 형식이 아니면 오류)
            if (response == null || response.trim().isEmpty() || !response.trim().startsWith("<")) {
                logger.error("API 응답이 유효하지 않습니다 (null, 빈 문자열 또는 비XML 형식): {}", response);
                return 0;
            }

            XmlMapper xmlMapper = new XmlMapper();
            JsonNode root = xmlMapper.readTree(response.getBytes(StandardCharsets.UTF_8));

            // 2. API 응답 헤더 파싱 및 결과 코드 확인
            // 제공된 XML 응답 구조: <response><header><resultCode>...</resultCode><resultMsg>...</resultMsg></header>
            JsonNode headerNode = root.path("header"); // <response> 바로 아래의 <header> 노드
            String resultCode = headerNode.path("resultCode").asText(""); // resultCode 노드의 텍스트 값
            String resultMsg = headerNode.path("resultMsg").asText("알 수 없는 오류"); // resultMsg 노드의 텍스트 값

            // API 호출 결과 코드 확인: "00"만 성공으로 간주
            if (!"00".equals(resultCode)) {
                logger.error("API 응답 오류 - 코드: {}, 메시지: {} (courseKey: {})", resultCode, resultMsg, courseKey);
                // API 호출 제한 횟수 초과 등 특정 오류 코드를 명확히 구분하고 싶다면 여기에 추가 로직 구현
                return 0;
            }

            // 3. 실제 아이템(데이터) 추출
            // API 응답 구조: <response><body><items><item>...</item></items></body>
            JsonNode itemsNode = root.path("body").path("items").path("item");

            List<RecommendCoursePlanEntity> entitiesToSave = new ArrayList<>();
            int parsedItemCount = 0; // 파싱된 아이템의 개수

            if (itemsNode.isArray()) { // itemsNode가 배열 형태일 경우
                logger.debug("courseKey {} 에 대한 아이템 (배열 형태): {}개", courseKey, itemsNode.size());
                for (JsonNode item : itemsNode) {
                    entitiesToSave.add(mapJsonNodeToEntity(item));
                    parsedItemCount++;
                }
            } else if (!itemsNode.isMissingNode()) { // itemsNode가 단일 객체 형태일 경우 (데이터가 하나일 때)
                logger.debug("courseKey {} 에 대한 아이템 (단일 객체 형태)", courseKey);
                entitiesToSave.add(mapJsonNodeToEntity(itemsNode));
                parsedItemCount++;
            } else { // items 노드가 아예 없거나 (데이터 없음)
                logger.info("courseKey {} 에 대한 아이템 데이터가 없습니다. (items 노드 부재 또는 비어있음)", courseKey);
            }

            // 4. 파싱된 데이터가 있을 경우 MongoDB에 저장
            if (parsedItemCount > 0) {
                logger.info("courseKey {} 에 대해 {}개의 RecommendCoursePlanEntity 저장/업데이트 시도...", courseKey, parsedItemCount);
                // saveAll은 @Id 필드를 기준으로 중복 시 업데이트, 없으면 삽입 (Upsert)
                recommendCoursePlanRepository.saveAll(entitiesToSave);
                logger.info("courseKey {} 에 대해 {}개의 RecommendCoursePlanEntity 저장/업데이트 완료.", courseKey, parsedItemCount);
            } else {
                logger.info("courseKey {} 에 대해 저장할 RecommendCoursePlanEntity가 없습니다.", courseKey);
            }

            return parsedItemCount; // 파싱되어 저장/업데이트 시도된 엔티티의 개수 반환

        } catch (Exception e) {
            logger.error("courseKey {} 데이터 처리 중 예상치 못한 예외 발생: {}", courseKey, e.getMessage(), e);
            return 0;
        } finally {
            logger.info(">> fetchAndSaveRecommendCoursePlans 서비스 종료 (courseKey: {})", courseKey);
        }
    }

    /**
     * JsonNode에서 데이터를 추출하여 RecommendCoursePlanEntity 객체로 매핑합니다.
     * 엔티티 생성자의 매개변수 순서와 API 응답 필드명을 일치시키는 것이 중요합니다.
     */
    private RecommendCoursePlanEntity mapJsonNodeToEntity(JsonNode item) {
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