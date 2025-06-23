package kopo.jeonnam.service.impl.gpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import kopo.jeonnam.service.gpt.IGptService;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GptService implements IGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();


    @Override
    public String createScheduleFromRequest(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    ) {
        // ✅ 디버깅: 입력 장소 정보 확인
        System.out.println("=== 📌 GPT 요청 전 장소 정보 디버깅 ===");
        for (PlaceInfoDTO p : places) {
            System.out.println("name: " + p.name());
            System.out.println("addr: [" + p.addr() + "]");
        }
        System.out.println("=====================================");

        // 프롬프트 생성
        String prompt = generatePromptFromLocations(places, startDate, tripDays, departurePlace, departureTime, additionalPrompt);

        System.out.println("=== GPT 프롬프트 ===");
        System.out.println(prompt);
        System.out.println("===================");

        try {
            String jsonBody = objectMapper.writeValueAsString(
                    new ChatRequest(
                            "gpt-3.5-turbo",
                            new Message[]{ new Message("user", prompt) },
                            1500
                    )
            );

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                String content = root.path("choices").get(0).path("message").path("content").asText();

                System.out.println("=== GPT 응답 ===");
                System.out.println(content);
                System.out.println("================");

                return content;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "{\"error\": \"GPT 호출 중 오류 발생\"}";
        }
    }


    private String generatePromptFromLocations(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    ) {
        // 1. 사용자 선택 장소 목록을 JSON 문자열로 변환
        String jsonLocations = places.stream()
                .map(p -> String.format("{\"name\": \"%s\", \"addr\": \"%s\"}", p.name(), p.addr()))
                .collect(Collectors.joining(", ", "[", "]"));

        // 2. 사용자 추가 프롬프트 존재 시 삽입
        String additionalRequirements = (additionalPrompt != null && !additionalPrompt.trim().isEmpty())
                ? "✅ [사용자 요청사항 - 반드시 반영]\n- " + additionalPrompt.trim() + "\n\n"
                : "";

        // 3. GPT가 정확한 형식과 주소를 인식할 수 있도록 예시 추가
        String exampleJson = """
예시:
[
  {"name": "녹차밭", "addr": "전남 보성군 보성읍 녹차밭길 17"},
  {"name": "해양박물관", "addr": "전남 목포시 해양로 24"}
]
""";

        // 4. 최종 프롬프트 구성
        return String.format("""
%s당신은 전문 여행 일정 플래너입니다. 전라남도 %d일 여행 일정을 JSON 형식으로 구성하세요.

📍 기본 정보
출발일: %s
기간: %d일
출발지: %s (출발지 주소는 이름과 동일하게 작성 가능)
출발 시간: %s
선호 장소 목록:
%s

%s

🧭 일정 규칙
1. 날짜별 "YYYY-MM-DD" 키로 일정 구성
2. 각 날짜에 [{"time":"HH:MM", "place":"장소명", "activity":"활동 내용", "addr":"정확한 주소"}] 배열 포함
3. 무조건 하루 8~9개 활동, 시간순 정렬
4. 모든 장소는 실제 존재하며 네이버지도 검색 가능한 주소 사용
5. 식사 장소는 구체적 식당명과 주소 제공, 로컬 맛집 우선 추천
6. 숙소는 정확한 이름과 주소 필수, 추상적 표현 금지 (예: '호텔 체크인' 금지)
   주소 누락 시 일정 무효
7. 활동 내용은 구체적이고 상세하게 작성 (단순 방문/관람 금지)
8. 동선은 효율적이고 경제적인 경로로 계획, 이동 시간·거리 최소화, 왕복 이동 금지

⚠️ 출력은 JSON 형식만, 설명이나 주석 포함 금지
⚠️ 주소가 없거나 "undefined"이면 해당 일정 제외 또는 다른 장소로 대체
""",
                additionalRequirements,
                tripDays,
                startDate,
                tripDays,
                departurePlace,
                departureTime,
                jsonLocations,
                exampleJson
        );
    }


    private static class ChatRequest {
        public String model;
        public Message[] messages;
        public int max_tokens;

        public ChatRequest(String model, Message[] messages, int max_tokens) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = max_tokens;
        }
    }

    private static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
