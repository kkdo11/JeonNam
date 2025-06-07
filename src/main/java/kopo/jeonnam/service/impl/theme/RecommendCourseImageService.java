package kopo.jeonnam.service.impl.theme;

import kopo.jeonnam.repository.entity.theme.RecommendCourseImageEntity;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseImageRepository;
import kopo.jeonnam.service.theme.IRecommendCourseImageService;
import kopo.jeonnam.util.NetworkUtil;
import kopo.jeonnam.util.XmlParserUtil; // XmlParserUtil 임포트
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set; // 중복 방지를 위해 Set 임포트
import java.util.HashSet; // 중복 방지를 위해 HashSet 임포트
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecommendCourseImageService implements IRecommendCourseImageService {

    private final RecommendCourseImageRepository recommendCourseImageRepository;

    @Value("${recommendcourse.api.imglist.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    /**
     * 특정 courseInfoIds에 해당하는 이미지 데이터를 외부 API에서 받아와 MongoDB에 저장합니다.
     * @param courseInfoIds 쉼표로 구분된 courseInfoId 문자열
     * @return 저장된 이미지 데이터 개수를 반환
     */
    @Override
    public int fetchAndSaveRecommendCourseImages(String courseInfoIds) {
        log.info(">> fetchAndSaveRecommendCourseImages 서비스 시작: courseInfoIds={}", courseInfoIds);
        if (courseInfoIds == null || courseInfoIds.trim().isEmpty()) {
            log.warn("  courseInfoIds가 null 또는 비어있어 이미지 데이터를 가져올 수 없습니다.");
            return 0;
        }

        int savedCount = 0;
        Set<String> processedImageIds = new HashSet<>(); // 중복 이미지 방지를 위한 Set

        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            // 쉼표로 구분된 courseInfoIds를 개별 ID로 분리
            List<String> individualCourseInfoIds = Arrays.asList(courseInfoIds.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // 각 개별 courseInfoId에 대해 API 호출
            for (String singleCourseInfoId : individualCourseInfoIds) {
                String url = String.format("%s?serviceKey=%s&numOfRows=10&pageNo=1&courseInfoId=%s",
                        apiUrl, encodedKey, URLEncoder.encode(singleCourseInfoId, StandardCharsets.UTF_8));
                log.info("  이미지 API 호출 URL: {}", url);

                String response = NetworkUtil.get(url);

                if (response == null || !response.trim().startsWith("<")) {
                    log.error("  이미지 API 응답 오류(비XML 또는 null): {}", response);
                    continue;
                }

                // 응답 원문 로그 추가
                log.info("  이미지 API 원문 응답: {}", response);
                JsonNode rootNode = XmlParserUtil.parseXmlToJsonNode(response).orElse(null);
                if (rootNode == null) {
                    log.error("  XML 파싱 실패 또는 빈 응답 (courseInfoId: {})", singleCourseInfoId);
                    continue;
                }

                // header/body가 없을 때 예외 처리
                JsonNode header = rootNode.path("header");
                JsonNode body = rootNode.path("body");
                if (header.isMissingNode() || body.isMissingNode()) {
                    // 혹시 cmmMsgHeader 구조일 경우도 체크
                    JsonNode cmmHeader = rootNode.path("cmmMsgHeader");
                    if (!cmmHeader.isMissingNode()) {
                        String errMsg = cmmHeader.path("errMsg").asText();
                        String returnAuthMsg = cmmHeader.path("returnAuthMsg").asText();
                        String returnReasonCode = cmmHeader.path("returnReasonCode").asText();
                        log.error("  이미지 API 인증 오류: {}, {}, {} (courseInfoId: {})", errMsg, returnAuthMsg, returnReasonCode, singleCourseInfoId);
                    } else {
                        log.warn("  이미지 API 응답에 header/body가 없음. 원문: {}", response);
                    }
                    continue;
                }

                String resultCode = header.path("resultCode").asText();
                String resultMsg = header.path("resultMsg").asText("알 수 없는 오류");
                if (!"00".equals(resultCode)) {
                    log.error("  이미지 API 응답 오류 - 코드: {}, 메시지: {} (courseInfoId: {})", resultCode, resultMsg, singleCourseInfoId);
                    continue;
                }

                JsonNode items = body.path("items").path("item");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        RecommendCourseImageEntity entity = mapJsonNodeToImageEntity(item, singleCourseInfoId);
                        if (entity != null && processedImageIds.add(entity.get_id())) {
                            recommendCourseImageRepository.save(entity);
                            savedCount++;
                            log.debug("    이미지 저장 완료: _id={}", entity.get_id());
                        } else if (entity != null) {
                            log.debug("    이미지 _id: {} 는 이미 처리된 항목이므로 스킵합니다.", entity.get_id());
                        }
                    }
                } else if (!items.isMissingNode()) {
                    RecommendCourseImageEntity entity = mapJsonNodeToImageEntity(items, singleCourseInfoId);
                    if (entity != null && processedImageIds.add(entity.get_id())) {
                        recommendCourseImageRepository.save(entity);
                        savedCount++;
                        log.debug("    단일 이미지 저장 완료: _id={}", entity.get_id());
                    } else if (entity != null) {
                        log.debug("    이미지 _id: {} 는 이미 처리된 항목이므로 스킵합니다.", entity.get_id());
                    }
                } else {
                    log.info("  courseInfoId '{}'에 대한 이미지 데이터가 없습니다.", singleCourseInfoId);
                }
            }
        } catch (Exception e) {
            log.error("!! 이미지 데이터 처리 중 예상치 못한 예외 발생: {}", e.getMessage(), e);
            return 0;
        }
        log.info(">> fetchAndSaveRecommendCourseImages 서비스 종료. 총 {}개 이미지 저장.", savedCount);
        return savedCount;
    }

    /**
     * 모든 이미지 엔티티를 조회합니다.
     * @return 모든 RecommendCourseImageEntity 리스트
     */
    @Override
    public List<RecommendCourseImageEntity> getAllRecommendCourseImages() {
        log.info(">> getAllRecommendCourseImages 서비스 호출");
        return recommendCourseImageRepository.findAll();
    }

    /**
     * JsonNode에서 데이터를 추출하여 RecommendCourseImageEntity 객체로 매핑합니다.
     * @param item JsonNode 형태의 단일 아이템 데이터
     * @param courseInfoId 원본 코스의 courseInfoId (매핑을 위해 전달)
     * @return 매핑된 RecommendCourseImageEntity 객체
     */
    private RecommendCourseImageEntity mapJsonNodeToImageEntity(JsonNode item, String courseInfoId) {
        String courseFileUrl = item.path("courseFileUrl").asText(null);
        if (courseFileUrl == null || courseFileUrl.isEmpty()) {
            log.warn("  API 응답에서 courseFileUrl이 없어 이미지 엔티티를 생성할 수 없습니다. item: {}", item);
            return null; // ID로 사용할 수 없는 경우 null 반환
        }

        return RecommendCourseImageEntity.builder()
                ._id(courseFileUrl) // courseFileUrl을 _id로 사용 (고유해야 함)
                .courseInfoId(courseInfoId) // 상위 코스 정보 ID
                .courseFileUrl(courseFileUrl)
                .courseFileNm(item.path("courseFileNm").asText(null))
                .courseFilePath(item.path("courseFilePath").asText(null))
                .build();
    }
}