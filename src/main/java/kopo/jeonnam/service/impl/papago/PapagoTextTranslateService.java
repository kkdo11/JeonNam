package kopo.jeonnam.service.impl.papago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.jeonnam.service.IPapagoTextTranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PapagoTextTranslateService implements IPapagoTextTranslateService {

    @Value("${papago.client-id}")
    private String clientId;

    @Value("${papago.client-secret}")
    private String clientSecret;

    // ✅ 공식 URL로 변경
    private static final String PAPAGO_TEXT_TRANSLATE_URL = "https://papago.apigw.ntruss.com/nmt/v1/translation";

    @Override
    public String translateText(String sourceLang, String targetLang, String text) throws Exception {
        log.info("[PapagoTextTranslateService] 번역 요청 시작: {} → {}, 원문: {}", sourceLang, targetLang, text);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        // ✅ 헤더 세팅
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
        headers.set("X-NCP-APIGW-API-KEY", clientSecret);

        // ✅ 요청 파라미터 세팅
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("source", sourceLang); // 예: "ko"
        params.add("target", targetLang); // 예: "en"
        params.add("text", text); // 인코딩 필요 없음

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    PAPAGO_TEXT_TRANSLATE_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("[PapagoTextTranslateService] 응답 상태: {}", response.getStatusCode());
            log.info("[PapagoTextTranslateService] 응답 바디: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String translatedText = json.path("message").path("result").path("translatedText").asText();
                log.info("[PapagoTextTranslateService] 번역 결과: {}", translatedText);
                return translatedText;
            } else {
                throw new Exception("Papago API 오류: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("[PapagoTextTranslateService] 번역 중 예외 발생", e);
            throw e;
        }
    }
}
