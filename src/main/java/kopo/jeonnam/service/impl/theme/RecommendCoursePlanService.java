package kopo.jeonnam.service.impl.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;
import kopo.jeonnam.repository.entity.theme.RecommendCoursePlanEntity;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseImageRepository;
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
import java.util.Optional;

/**
 * 특정 추천 코스(courseKey)에 대한 상세 계획(Plan) 정보를 외부 API에서 가져와 MongoDB에 저장하는 서비스 구현체
 */
@Service
public class RecommendCoursePlanService implements IRecommendCoursePlanService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendCoursePlanService.class);

    private final RecommendCoursePlanRepository recommendCoursePlanRepository;
    private final RecommendCourseImageRepository imageRepository;


    @Value("${recommendcourse.api.planlist.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    @Override
    public boolean existsAnyByCourseKey(String courseKey) {
        return recommendCoursePlanRepository.countByPlanCourseId(courseKey) > 0;
    }

    @Override
    public boolean existsAny() {
        return recommendCoursePlanRepository.count() > 0;
    }


    public RecommendCoursePlanService(RecommendCoursePlanRepository recommendCoursePlanRepository,
                                      RecommendCourseImageRepository imageRepository) {
        this.recommendCoursePlanRepository = recommendCoursePlanRepository;
        this.imageRepository = imageRepository;
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
            String url = String.format("%s?serviceKey=%s&startPage=1&pageSize=1000&planCourseId=%s", apiUrl, encodedKey, courseKey);
            logger.info("API 호출 URL: {}", url);

            // API 호출 및 응답 수신
            String response = NetworkUtil.get(url);
            logger.info("[DEBUG] API 원본 응답: \n{}", response);

            // 1. API 응답 유효성 검사 (null이거나 XML 형식이 아니면 오류)
            if (response == null || response.trim().isEmpty() || !response.trim().startsWith("<")) {
                logger.error("API 응답이 유효하지 않습니다 (null, 빈 문자열 또는 비XML 형식): {}", response);
                return 0;
            }

            // XML 파싱 및 JsonNode 변환
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode root = xmlMapper.readTree(response.getBytes(StandardCharsets.UTF_8));

            // ✅ 비표준 에러 구조 대응 (e.g. <OpenAPI_ServiceResponse><cmmMsgHeader>...)
            if (root.has("cmmMsgHeader")) {
                JsonNode errHeader = root.path("cmmMsgHeader");
                String errMsg = errHeader.path("errMsg").asText("서비스 오류");
                String returnAuthMsg = errHeader.path("returnAuthMsg").asText("");
                String returnReasonCode = errHeader.path("returnReasonCode").asText("");

                logger.error("API 응답 에러 (제한 초과 등) - errMsg: {}, returnAuthMsg: {}, reasonCode: {} (courseKey: {})",
                        errMsg, returnAuthMsg, returnReasonCode, courseKey);
                return 0;
            }

            // ✅ 표준 응답 구조 처리
            JsonNode headerNode = root.path("header");
            String resultCode = headerNode.path("resultCode").asText("");
            String resultMsg = headerNode.path("resultMsg").asText("알 수 없는 오류");

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

    @Override
    public List<RecommendCoursePlanDTO> getAllPlansWithImages() {
        List<RecommendCoursePlanEntity> plans = recommendCoursePlanRepository.findAll();
        List<RecommendCoursePlanDTO> result = new ArrayList<>();

        for (RecommendCoursePlanEntity plan : plans) {
            RecommendCoursePlanDTO dto = new RecommendCoursePlanDTO();
            dto.setPlanInfoId(plan.getPlanInfoId());
            dto.setPlanName(plan.getPlanName());
            dto.setPlanArea(plan.getPlanArea());
            dto.setPlanAddr(plan.getPlanAddr());
            dto.setPlanPhone(plan.getPlanPhone());
            dto.setPlanHomepage(plan.getPlanHomepage());
            dto.setPlanParking(plan.getPlanParking());
            dto.setPlanContents(plan.getPlanContents());

            try {
                dto.setPlanLatitude(Double.parseDouble(plan.getPlanLatitude()));
                dto.setPlanLongitude(Double.parseDouble(plan.getPlanLongitude()));
            } catch (NumberFormatException e) {
                logger.warn("잘못된 위경도: {}", plan.getPlanInfoId());
                dto.setPlanLatitude(0);
                dto.setPlanLongitude(0);
            }

            List<String> imageUrls = imageRepository.findByCourseInfoId(plan.getPlanInfoId())
                    .stream()
                    .map(image -> image.getCourseFileUrl())
                    .toList();
            dto.setImageUrls(imageUrls);

            result.add(dto);
        }

        return result;
    }


    @Override
    public Optional<RecommendCoursePlanDTO> getPlanWithImagesById(String planInfoId) {
        return recommendCoursePlanRepository.findById(planInfoId).map(plan -> {
            RecommendCoursePlanDTO dto = new RecommendCoursePlanDTO();
            dto.setPlanInfoId(plan.getPlanInfoId());
            dto.setPlanName(plan.getPlanName());
            dto.setPlanArea(plan.getPlanArea());
            dto.setPlanAddr(plan.getPlanAddr());
            dto.setPlanPhone(plan.getPlanPhone());
            dto.setPlanHomepage(plan.getPlanHomepage());
            dto.setPlanParking(plan.getPlanParking());
            dto.setPlanContents(plan.getPlanContents());

            try {
                dto.setPlanLatitude(Double.parseDouble(plan.getPlanLatitude()));
                dto.setPlanLongitude(Double.parseDouble(plan.getPlanLongitude()));
            } catch (NumberFormatException e) {
                logger.warn("잘못된 위경도: {}", planInfoId);
                dto.setPlanLatitude(0);
                dto.setPlanLongitude(0);
            }

            List<String> imageUrls = imageRepository.findByCourseInfoId(plan.getPlanInfoId())
                    .stream()
                    .map(image -> image.getCourseFileUrl())
                    .toList();
            dto.setImageUrls(imageUrls);

            return dto;
        });
    }


    /**
     * JsonNode에서 데이터를 추출하여 RecommendCoursePlanEntity 객체로 매핑합니다.
     *
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