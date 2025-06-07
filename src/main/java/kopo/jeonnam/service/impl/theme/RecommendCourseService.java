package kopo.jeonnam.service.impl.theme;

// ... (기존 import 문)
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
import kopo.jeonnam.repository.entity.theme.RecommendCourseImageEntity;
import kopo.jeonnam.repository.mongo.theme.RecommendCourseRepository;
import kopo.jeonnam.service.theme.IRecommendCourseService;
import kopo.jeonnam.util.NetworkUtil; // NetworkUtil import 되어있는지 확인
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecommendCourseService implements IRecommendCourseService {

    private final RecommendCourseRepository recommendCourseRepository;

    @Value("${recommendcourse.api.url}")
    private String apiUrl;

    @Value("${recommendcourse.api.key}")
    private String apiKey;

    /**
     * 남도 추천 여행 코스 데이터를 외부 API에서 받아와 MongoDB에 저장합니다.
     * 계절별로 반복 호출하며, 중복 코스는 Set으로 필터링합니다.
     *
     * @return 저장된 고유 추천 코스 개수를 반환합니다
     */
    @Override
    public int fetchAndSaveRecommendCourses() {
        log.info(">> fetchAndSaveRecommendCourses 서비스 시작");
        recommendCourseRepository.deleteAll();
        int numOfRows = 10;
        Set<RecommendCourseEntity> uniqueCourses = new HashSet<>();
        List<String> seasons = Arrays.asList("봄", "여름", "가을", "겨울");
        XmlMapper xmlMapper = new XmlMapper();

        for (String season : seasons) {
            log.info("### 계절: {} 데이터 처리 시작 ###", season);
            String encodedSeason = encodeUtf8(season);
            String initialUrl = String.format("%s?serviceKey=%s&numOfRows=%d&pageNo=1&courseCategory=%s",
                    apiUrl, encodeUtf8(apiKey), numOfRows, encodedSeason);
            log.info("초기 API 호출 URL (Total Count 확인용): {}", initialUrl);
            String initialResponse = NetworkUtil.get(initialUrl);
            log.info("API 원문 응답: {}", initialResponse);
            JsonNode initialRoot = parseXml(xmlMapper, initialResponse);
            if (initialRoot == null) {
                log.warn("{} 계절의 XML 파싱 실패, 다음 계절로 진행", season);
                continue;
            }

            // 인증키 오류 등 예외 응답 처리
            if (initialRoot.has("cmmMsgHeader")) {
                String errMsg = initialRoot.path("cmmMsgHeader").path("errMsg").asText();
                String returnAuthMsg = initialRoot.path("cmmMsgHeader").path("returnAuthMsg").asText();
                log.error("API 인증 오류: {}, {}", errMsg, returnAuthMsg);
                continue;
            }

            JsonNode header = initialRoot.path("header");
            JsonNode body = initialRoot.path("body");
            String resultCode = header.path("resultCode").asText();
            String resultMsg = header.path("resultMsg").asText("알 수 없는 오류");
            if (!"00".equals(resultCode)) {
                log.error("API 응답 오류 - 코드: {}, 메시지: {} (계절: {})", resultCode, resultMsg, season);
                continue;
            }
            int totalCount = body.path("totalCount").asInt(0);
            log.info("계절: {} - 총 데이터 건수: {}", season, totalCount);
            if (totalCount == 0) {
                log.warn("계절: {} - API에서 가져올 데이터가 없습니다.", season);
                continue;
            }
            int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
            log.info("계절: {} - 총 페이지 수: {}", season, totalPages);
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                String pageUrl = String.format("%s?serviceKey=%s&numOfRows=%d&pageNo=%d&courseCategory=%s",
                        apiUrl, encodeUtf8(apiKey), numOfRows, pageNo, encodedSeason);
                log.info("데이터 호출 URL (계절: {}, 페이지: {}): {}", season, pageNo, pageUrl);
                String pageResponse = NetworkUtil.get(pageUrl);
                JsonNode pageRoot = parseXml(xmlMapper, pageResponse);
                if (pageRoot == null) {
                    log.warn("{} 계절 {}페이지 XML 파싱 실패, 다음 페이지로 진행", season, pageNo);
                    continue;
                }
                JsonNode itemsNode = pageRoot.path("body").path("items");
                // item이 배열 또는 단일 객체일 수 있으므로 모두 처리
                if (itemsNode.has("item")) {
                    JsonNode itemNode = itemsNode.path("item");
                    if (itemNode.isArray()) {
                        for (JsonNode item : itemNode) {
                            RecommendCourseEntity entity = mapJsonNodeToEntity(item);
                            if (entity != null) {
                                uniqueCourses.add(entity);
                                log.debug("코스 추가: {}", entity.getCourseKey());
                            }
                        }
                    } else if (itemNode.isObject()) {
                        RecommendCourseEntity entity = mapJsonNodeToEntity(itemNode);
                        if (entity != null) {
                            uniqueCourses.add(entity);
                            log.debug("코스 추가(단일): {}", entity.getCourseKey());
                        }
                    }
                }
            }
            log.info("### 계절: {} 데이터 처리 완료. 현재까지 {}개의 고유 코스 수집 ###", season, uniqueCourses.size());
        }
        log.info("총 {}개의 고유 추천 코스 데이터를 MongoDB에 저장 시도...", uniqueCourses.size());
        recommendCourseRepository.saveAll(uniqueCourses);
        log.info("총 {}개의 추천 코스 데이터 MongoDB 저장 완료.", uniqueCourses.size());
        log.info(">> fetchAndSaveRecommendCourses 서비스 종료");
        return uniqueCourses.size();
    }

    /**
     * 문자열을 UTF-8로 인코딩합니다.
     * @param value 인코딩할 문자열
     * @return 인코딩된 문자열
     */
    private String encodeUtf8(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("UTF-8 인코딩 오류: {}", e.getMessage());
            return "";
        }
    }

    /**
     * XML 문자열을 JsonNode로 파싱합니다.
     * @param xmlMapper XmlMapper 인스턴스
     * @param xml XML 문자열
     * @return 파싱된 JsonNode, 실패 시 null
     */
    private JsonNode parseXml(XmlMapper xmlMapper, String xml) {
        try {
            JsonNode node = xmlMapper.readTree(xml.getBytes(StandardCharsets.UTF_8));
            log.debug("파싱된 JSON 구조: {}", node.toPrettyString());
            return node;
        } catch (Exception e) {
            log.error("XML 파싱 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JsonNode에서 RecommendCourseEntity로 매핑합니다.
     * @param item JsonNode의 item 노드
     * @return RecommendCourseEntity 객체
     */
    private RecommendCourseEntity mapJsonNodeToEntity(JsonNode item) {
        if (item == null) return null;
        return RecommendCourseEntity.builder()
                ._id(item.path("courseKey").asText(null)) // _id를 courseKey로 설정
                .courseKey(item.path("courseKey").asText(null))
                .courseInfoIds(item.path("courseInfoIds").asText(null))
                .courseCategory(item.path("courseCategory").asText(null))
                .courseName(item.path("courseName").asText(null))
                .coursePeriod(item.path("coursePeriod").asText(null))
                .coursePersonType(item.path("coursePersonType").asText(null))
                .coursePersonCount(item.path("coursePersonCount").asText(null))
                .courseContents(item.path("courseContents").asText(null))
                .courseArea(item.path("courseArea").asText(null))
                .build();
    }

    /**
     * 모든 추천 코스 엔티티를 조회합니다.
     * @return RecommendCourseEntity 리스트
     */
    @Override
    public List<RecommendCourseEntity> getAllRecommendCourses() {
        log.info("getAllRecommendCourses 호출");
        return recommendCourseRepository.findAll();
    }

    /**
     * courseInfoIds로 이미지 리스트를 조회합니다. (구현 필요)
     * @param courseInfoIds 쉼표로 구분된 courseInfoId 문자열
     * @return RecommendCourseImageEntity 리스트
     */
    @Override
    public List<RecommendCourseImageEntity> getImagesByCourseInfoIds(String courseInfoIds) {
        log.info("getImagesByCourseInfoIds 호출: {}", courseInfoIds);
        // TODO: 실제 구현 필요. 임시로 빈 리스트 반환
        return List.of();
    }

    /**
     * 추천 코스 상세 정보를 조회합니다.
     * @param courseId 코스의 _id
     * @return RecommendCourseEntity (Optional)
     */
    @Override
    public Optional<RecommendCourseEntity> getRecommendCourseDetail(String courseId) {
        log.info("getRecommendCourseDetail 호출: {}", courseId);
        return recommendCourseRepository.findById(courseId);
    }
}