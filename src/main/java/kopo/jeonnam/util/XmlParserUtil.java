package kopo.jeonnam.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class XmlParserUtil {

    private static final Logger logger = LoggerFactory.getLogger(XmlParserUtil.class);
    private static final XmlMapper xmlMapper = new XmlMapper(); // 한 번만 생성

    /**
     * XML 문자열을 JsonNode로 파싱합니다.
     * 유효하지 않은 XML이거나 파싱 중 오류 발생 시 Optional.empty()를 반환합니다.
     *
     * @param xmlString 파싱할 XML 문자열
     * @return 파싱된 JsonNode를 포함하는 Optional, 오류 발생 시 Optional.empty()
     */
    public static Optional<JsonNode> parseXmlToJsonNode(String xmlString) {
        if (xmlString == null || xmlString.trim().isEmpty()) {
            logger.warn("parseXmlToJsonNode: 입력 XML 문자열이 null이거나 비어 있습니다.");
            return Optional.empty();
        }
        // XML 시작 태그로 시작하는지 기본적인 유효성 검사
        if (!xmlString.trim().startsWith("<")) {
            logger.warn("parseXmlToJsonNode: 입력 문자열이 XML 형식이 아닙니다. 시작: {}", xmlString.trim().substring(0, Math.min(xmlString.trim().length(), 50)));
            return Optional.empty();
        }
        try {
            return Optional.of(xmlMapper.readTree(xmlString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error("XML 파싱 중 오류 발생: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 특정 경로의 JsonNode를 안전하게 가져옵니다.
     *
     * @param parentNode 시작할 JsonNode
     * @param pathParts 찾을 노드의 경로 (예: "body", "items", "item")
     * @return 찾은 JsonNode를 포함하는 Optional, 없거나 오류 발생 시 Optional.empty()
     */
    public static Optional<JsonNode> getNodeAtPath(JsonNode parentNode, String... pathParts) {
        if (parentNode == null || pathParts == null || pathParts.length == 0) {
            return Optional.empty();
        }

        JsonNode currentNode = parentNode;
        for (String pathPart : pathParts) {
            if (currentNode == null || currentNode.isMissingNode()) {
                return Optional.empty(); // 경로 중간에 노드가 없으면 종료
            }
            currentNode = currentNode.path(pathPart);
        }
        // 최종 노드가 MissingNode가 아니면 반환 (null 체크는 path()가 처리)
        return currentNode.isMissingNode() ? Optional.empty() : Optional.of(currentNode);
    }

    /**
     * 특정 경로의 텍스트 값을 안전하게 가져옵니다.
     *
     * @param parentNode 시작할 JsonNode
     * @param defaultValue 값을 찾지 못했을 때 반환할 기본값
     * @param pathParts 찾을 텍스트의 경로 (예: "header", "resultCode")
     * @return 찾은 텍스트 값 또는 기본값
     */
    public static String getTextAtPath(JsonNode parentNode, String defaultValue, String... pathParts) {
        return getNodeAtPath(parentNode, pathParts)
                .map(JsonNode::asText) // Optional에 값이 있으면 텍스트로 변환
                .orElse(defaultValue); // 없으면 기본값 반환
    }
}