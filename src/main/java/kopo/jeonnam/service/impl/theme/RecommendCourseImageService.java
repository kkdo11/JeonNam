// kopo.jeonnam.service.impl.theme.RecommendCourseImageService.java
package kopo.jeonnam.service.impl.theme;

import com.fasterxml.jackson.databind.JsonNode;
import kopo.jeonnam.repository.entity.RecommendCourseImageEntity; // 이 엔티티를 사용합니다.
import kopo.jeonnam.repository.mongo.theme.RecommendCourseImageRepository;
import kopo.jeonnam.service.theme.IRecommendCourseImageService;
import kopo.jeonnam.util.NetworkUtil;
import kopo.jeonnam.util.XmlParserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class RecommendCourseImageService implements IRecommendCourseImageService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendCourseImageService.class);

    private final RecommendCourseImageRepository recommendCourseImageRepository;

    @Value("${recommendcourse.api.imglist.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    public RecommendCourseImageService(RecommendCourseImageRepository recommendCourseImageRepository) {
        this.recommendCourseImageRepository = recommendCourseImageRepository;
    }

    @Override
    public int fetchAndSaveRecommendCourseImages(String courseInfoIds) {
        logger.info(">> fetchAndSaveRecommendCourseImages 서비스 시작 (courseInfoIds: {})", courseInfoIds);

        if (courseInfoIds == null || courseInfoIds.trim().isEmpty()) {
            logger.warn("입력된 courseInfoIds가 비어있습니다. 처리를 중단합니다.");
            return 0;
        }

        List<String> idList = Arrays.asList(courseInfoIds.split(","));
        int totalSavedCount = 0;

        for (String courseInfoId : idList) {
            String trimmedId = courseInfoId.trim();
            if (trimmedId.isEmpty()) {
                continue;
            }

            logger.info("  >> courseInfoId: {} 에 대한 이미지 데이터 처리 시작", trimmedId);
            try {
                String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
                String url = String.format("%s?serviceKey=%s&courseInfoId=%s", apiUrl, encodedKey, trimmedId);
                logger.info("    API 호출 URL: {}", url);

                String response = NetworkUtil.get(url);

                Optional<JsonNode> rootNodeOptional = XmlParserUtil.parseXmlToJsonNode(response);

                if (rootNodeOptional.isEmpty()) {
                    logger.error("    API 응답 XML 파싱 실패 또는 유효하지 않은 응답 (courseInfoId: {})", trimmedId);
                    continue;
                }
                JsonNode root = rootNodeOptional.get();

                String resultCode = XmlParserUtil.getTextAtPath(root, "", "header", "resultCode");
                String resultMsg = XmlParserUtil.getTextAtPath(root, "알 수 없는 오류", "header", "resultMsg");

                if (!"00".equals(resultCode)) {
                    logger.error("    API 응답 오류 - 코드: {}, 메시지: {} (courseInfoId: {})", resultCode, resultMsg, trimmedId);
                    continue;
                }

                // 이미지 데이터 추출 경로: <response><body><items><item>
                Optional<JsonNode> itemsNodeOptional = XmlParserUtil.getNodeAtPath(root, "body", "items", "item");

                List<RecommendCourseImageEntity> entitiesToSave = new ArrayList<>();
                int currentSavedCount = 0;

                if (itemsNodeOptional.isPresent()) {
                    JsonNode itemsNode = itemsNodeOptional.get();

                    if (itemsNode.isArray()) {
                        logger.debug("    courseInfoId {} 에 대한 아이템 (배열 형태): {}개", trimmedId, itemsNode.size());
                        for (JsonNode item : itemsNode) {
                            // mapJsonNodeToEntity 호출 시 courseInfoId를 함께 넘깁니다.
                            entitiesToSave.add(mapJsonNodeToEntity(item, trimmedId));
                            currentSavedCount++;
                        }
                    } else { // 단일 객체 형태
                        logger.debug("    courseInfoId {} 에 대한 아이템 (단일 객체 형태)", trimmedId);
                        // mapJsonNodeToEntity 호출 시 courseInfoId를 함께 넘깁니다.
                        entitiesToSave.add(mapJsonNodeToEntity(itemsNode, trimmedId));
                        currentSavedCount++;
                    }
                } else {
                    logger.info("    courseInfoId {} 에 대한 아이템 데이터가 없습니다.", trimmedId);
                }

                if (currentSavedCount > 0) {
                    logger.info("    courseInfoId {} 에 대해 {}개의 RecommendCourseImageEntity 저장/업데이트 시도...", trimmedId, currentSavedCount);
                    recommendCourseImageRepository.saveAll(entitiesToSave);
                    logger.info("    courseInfoId {} 에 대해 {}개의 RecommendCourseImageEntity 저장/업데이트 완료.", trimmedId, currentSavedCount);
                    totalSavedCount += currentSavedCount;
                } else {
                    logger.info("    courseInfoId {} 에 대해 저장할 RecommendCourseImageEntity가 없습니다.", trimmedId);
                }

            } catch (Exception e) {
                logger.error("  courseInfoId {} 데이터 처리 중 예상치 못한 예외 발생: {}", trimmedId, e.getMessage(), e);
            }
        }

        logger.info(">> fetchAndSaveRecommendCourseImages 서비스 종료. 총 저장/업데이트 건수: {}", totalSavedCount);
        return totalSavedCount;
    }

    /**
     * JsonNode에서 데이터를 추출하여 RecommendCourseImageEntity 객체로 매핑합니다.
     * 실제 이미지 API 응답 필드명인 courseFileNm과 courseFileUrl을 사용합니다.
     * courseInfoId는 API 응답에 없으므로, 호출 시 외부에서 주입받습니다.
     */
    private RecommendCourseImageEntity mapJsonNodeToEntity(JsonNode item, String courseInfoId) {
        return RecommendCourseImageEntity.builder()
                .courseFileUrl(item.path("courseFileUrl").asText(null)) // URL을 ID로 사용
                .courseInfoId(courseInfoId) // 외부에서 주입받은 courseInfoId
                .courseFileNm(item.path("courseFileNm").asText(null)) // 파일명
                .build();
    }
}