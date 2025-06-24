package kopo.jeonnam.service.impl.gpt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import kopo.jeonnam.service.gpt.IGptService;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GptService implements IGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * [1] 사용자가 찜한 장소명 목록 (단일 선택된 기준 장소)을 기반으로 일정 생성.
     * places.json의 모든 장소 중 기준 장소를 제외한 나머지 장소들을 GPT에 전달합니다.
     */
    public String createScheduleWithFavorites(
            Set<String> favoriteNames, // 클라이언트에서 단일 선택된 찜 장소 이름만 포함되어야 함
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    ) {
        List<PlaceInfoDTO> allPlaces = loadAllPlaces(); // places.json의 모든 장소 로드

        // GPT에 전달할 최종 장소 목록을 기본적으로 allPlaces로 초기화
        List<PlaceInfoDTO> finalPlacesForGpt = new ArrayList<>(allPlaces);

        // ⭐⭐⭐ 핵심 수정: primaryFavoritedPlace, clientPrimaryFavoriteName, primaryFavoriteNameForPrompt를
        //      단 한 번만 할당하도록 변경하여 final 또는 effectively final 상태를 보장합니다. ⭐⭐⭐
        final String clientPrimaryFavoriteName;
        final PlaceInfoDTO primaryFavoritedPlace;
        final String primaryFavoriteNameForPrompt;

        if (favoriteNames != null && !favoriteNames.isEmpty()) {
            clientPrimaryFavoriteName = favoriteNames.iterator().next();
            primaryFavoriteNameForPrompt = clientPrimaryFavoriteName; // 클라이언트가 보낸 이름을 먼저 프롬프트용으로 설정

            primaryFavoritedPlace = allPlaces.stream()
                    .filter(p -> Objects.equals(p.name(), clientPrimaryFavoriteName))
                    .findFirst()
                    .orElse(null); // 찾지 못하면 null 할당
        } else {
            // favoriteNames가 비어있으면 이 경로로 들어와 각 변수가 한 번씩 할당됩니다.
            clientPrimaryFavoriteName = null; // 이 경로에서 clientPrimaryFavoriteName은 null
            primaryFavoriteNameForPrompt = "선택된 찜 장소 없음"; // 이 경로에서 프롬프트용 이름
            primaryFavoritedPlace = null; // 이 경로에서 primaryFavoritedPlace는 null
            System.err.println("WARN: 일정 생성을 위한 기준 찜 장소가 선택되지 않았습니다. places.json의 모든 장소를 사용합니다.");
        }

        // ⭐⭐⭐ primaryFavoritedPlace의 null 여부에 따라 로직 분기 (이제 primaryFavoritedPlace는 final 변수) ⭐⭐⭐
        if (primaryFavoritedPlace != null) {
            // 기준 장소를 places.json에서 찾았다면, 해당 장소를 제외한 목록을 생성
            finalPlacesForGpt = allPlaces.stream()
                    .filter(p -> !(Objects.equals(p.name(), primaryFavoritedPlace.name()) &&
                            Objects.equals(p.addr(), primaryFavoritedPlace.addr())))
                    .collect(Collectors.toList());
            System.out.println("DEBUG: 기준 찜 장소 '" + clientPrimaryFavoriteName + "'을 places.json에서 성공적으로 찾았고 제외했습니다.");
        } else if (clientPrimaryFavoriteName != null) { // 찜 장소는 있었지만 places.json에서 매칭되지 않은 경우
            System.err.println("WARN: 기준 찜 장소 '" + clientPrimaryFavoriteName + "'을 places.json에서 찾을 수 없습니다. " +
                    "일정을 생성하기 위해 places.json의 모든 장소를 사용합니다.");
            System.err.println("DEBUG: places.json의 첫 5개 장소 (매칭 확인용):");
            allPlaces.stream().limit(5).forEach(p ->
                    System.err.println("  - name: " + p.name() + ", addr: " + p.addr())
            );
            // 이 경우 finalPlacesForGpt는 이미 allPlaces로 초기화되어 있으므로 추가 작업 없음.
            // primaryFavoriteNameForPrompt도 이미 위에서 clientPrimaryFavoriteName으로 설정됨.
        }
        // favoriteNames가 처음부터 비어있었던 경우는 위 else 블록에서 처리되며, finalPlacesForGpt는 allPlaces로 유지됨.

        if (finalPlacesForGpt.isEmpty()) {
            System.err.println("WARN: GPT에 전달할 장소가 없습니다. places.json 파일에 유효한 장소가 있는지 확인해주세요.");
            return "{\"error\": \"일정 생성을 위한 장소 데이터가 부족합니다. places.json 파일을 확인해주세요.\"}";
        }

        System.out.println("DEBUG: GPT에 전달할 최종 장소 개수 (기준 찜 제외 또는 전체): " + finalPlacesForGpt.size());
        finalPlacesForGpt.stream().limit(10).forEach(p -> System.out.println("  - 최종 전달 장소: " + p.name() + " (" + p.addr() + ")"));


        return createScheduleFromRequest(
                finalPlacesForGpt, // 이제 이 리스트는 기준 찜 장소를 제외한 (또는 찜 장소를 찾지 못하면 전체) places.json 장소들
                startDate,
                tripDays,
                departurePlace,
                departureTime,
                additionalPrompt,
                primaryFavoriteNameForPrompt // 프롬프트에 활용될 기준 장소 이름 (매칭 실패 시 클라이언트가 보낸 이름)
        );
    }

    /**
     * [2] GPT에게 일정 생성 요청 (오버로드된 메서드)
     */
    @Override
    public String createScheduleFromRequest(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    ) {
        // 이 메서드는 직접 호출되지 않고, 아래 오버로드된 메서드로 라우팅되거나 기존 로직의 호환성을 위해 남겨둠.
        // 여기서는 임시로 빈 문자열을 넘겨주어 아래 오버로드된 메서드를 호출
        return createScheduleFromRequest(places, startDate, tripDays, departurePlace, departureTime, additionalPrompt, "");
    }


    /**
     * [2-2] GPT에게 일정 생성 요청 (기준 장소 컨텍스트 포함)
     */
    public String createScheduleFromRequest(
            List<PlaceInfoDTO> places, // 확장되었지만 기준 장소가 제외된 장소 목록
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt,
            String contextPrimaryFavoriteName // GPT 프롬프트에 포함될 기준 장소 이름
    ) {
        System.out.println("=== 📌 GPT 요청 전 장소 정보 (제외된 장소: " + contextPrimaryFavoriteName + ", 목록 개수: " + places.size() + ") ===");
        if (places.isEmpty()) {
            System.out.println("  - 전달할 장소 목록이 비어있습니다.");
        } else {
            places.stream().limit(10).forEach(p -> System.out.println("  - name: " + p.name() + ", addr: [" + p.addr() + "]"));
        }
        System.out.println("==========================================================");


        String prompt = generatePromptFromLocations(
                places,
                startDate,
                tripDays,
                departurePlace,
                departureTime,
                additionalPrompt,
                contextPrimaryFavoriteName // 기준 장소 이름을 프롬프트 생성기에 전달
        );

        System.out.println("=== GPT 프롬프트 ===");
        System.out.println(prompt);
        System.out.println("===================");

        try {
            String jsonBody = objectMapper.writeValueAsString(
                    new ChatRequest("gpt-4o", new Message[]{ new Message("user", prompt) }, 1500, 0.2f)
            );

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    System.err.println("🚨 GPT API 호출 실패! HTTP Status: " + response.code() + ", 응답 본문: " + errorBody);
                    return "{\"error\": \"GPT API 호출 실패\", \"details\": \"HTTP " + response.code() + ": " + errorBody.replace("\"", "\\\"") + "\"}";
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                String rawContent = root.path("choices").get(0).path("message").path("content").asText();

                System.out.println("=== GPT 응답 (원문) ===");
                System.out.println(rawContent);
                System.out.println("================");

                String cleanedContent = rawContent;
                if (rawContent.startsWith("```json") && rawContent.endsWith("```")) {
                    cleanedContent = rawContent.substring("```json".length(), rawContent.length() - "```".length()).trim();
                } else if (rawContent.startsWith("```") && rawContent.endsWith("```")) {
                    cleanedContent = rawContent.substring("```".length(), rawContent.length() - "```".length()).trim();
                }

                JsonNode scheduleNode;
                try {
                    scheduleNode = objectMapper.readTree(cleanedContent);
                } catch (IOException e) {
                    System.err.println("🚨 오류: GPT 응답이 유효한 JSON 형식이 아닙니다 (클린징 후): " + cleanedContent);
                    e.printStackTrace();
                    return "{\"error\": \"GPT 응답 파싱 실패\", \"details\": \"유효하지 않은 JSON 형식: " + e.getMessage().replace("\"", "\\\"") + "\"}";
                }

                ObjectNode filteredSchedule = objectMapper.createObjectNode();

                scheduleNode.fieldNames().forEachRemaining(date -> {
                    JsonNode dailyActivities = scheduleNode.get(date);
                    if (dailyActivities.isArray()) {
                        ArrayNode filteredDailyActivities = objectMapper.createArrayNode();
                        for (JsonNode activity : dailyActivities) {
                            String placeName = activity.path("place").asText("");
                            String placeAddr = activity.path("addr").asText("");

                            boolean isAllowed = false;
                            // ⭐⭐ 변경: 출발지 필터링 로직 수정 (출발지이거나 places 목록에 있으면 허용) ⭐⭐
                            if (placeName.equals(departurePlace)) { // placeName이 departurePlace와 정확히 일치하면 허용
                                isAllowed = true;
                            } else { // 그 외의 경우에만 places 목록에서 확인
                                for (PlaceInfoDTO p : places) {
                                    if (Objects.equals(p.name(), placeName) && Objects.equals(p.addr(), placeAddr)) {
                                        isAllowed = true;
                                        break;
                                    }
                                }
                            }

                            if (isAllowed) {
                                filteredDailyActivities.add(activity);
                            } else {
                                System.out.println("🚨 경고: 허용되지 않은 장소 '" + placeName + "' (주소: '" + placeAddr + "')가 감지되어 일정에서 제외합니다.");
                            }
                        }
                        if (filteredDailyActivities.size() > 0) {
                            filteredSchedule.set(date, filteredDailyActivities);
                        }
                    }
                });

                String finalScheduleJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredSchedule);
                System.out.println("=== GPT 응답 (필터링 후) ===");
                System.out.println(finalScheduleJson);
                System.out.println("================");

                return finalScheduleJson;
            }

        } catch (IOException e) {
            System.err.println("🚨 GPT API 호출 중 심각한 IO 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"GPT API 통신 오류\", \"details\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        } catch (Exception e) {
            System.err.println("🚨 GPT 서비스 처리 중 예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"예상치 못한 서버 오류\", \"details\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    private String generatePromptFromLocations(
            List<PlaceInfoDTO> places,
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt,
            String contextPrimaryFavoriteName
    ) {
        String jsonAvailablePlaces = places.stream()
                .map(p -> String.format("{\"name\": \"%s\", \"addr\": \"%s\"}", p.name(), p.addr()))
                .collect(Collectors.joining(", ", "[", "]"));

        String additionalRequirements = (additionalPrompt != null && !additionalPrompt.trim().isEmpty())
                ? "✅ [사용자 요청사항 - 반드시 반영]\n- " + additionalPrompt.trim() + "\n\n"
                : "";

        String primaryFavoriteContext = "";
        if (contextPrimaryFavoriteName != null && !contextPrimaryFavoriteName.isEmpty() && !contextPrimaryFavoriteName.equals("선택된 찜 장소 없음")) {
            primaryFavoriteContext = String.format(
                    "당신이 선택한 기준 장소는 '%s'입니다. 이 일정은 해당 장소 '%s'가 위치한 지역을 포함하여 전라남도 전역의 장소들을 활용하여 구성됩니다. " +
                            "제공된 '사용 가능한 장소 목록'에는 '%s'이 직접 포함되어 있지 않습니다. " +
                            "대신, '%s' 주변 및 전라남도의 다른 매력적인 장소들을 다양하게 포함하여 일정을 만드십시오.",
                    contextPrimaryFavoriteName, contextPrimaryFavoriteName, contextPrimaryFavoriteName, contextPrimaryFavoriteName
            );
        } else {
            primaryFavoriteContext = "사용자가 특정 기준 장소를 선택하지 않았거나, 선택한 장소를 찾을 수 없었습니다. 전라남도 전역의 장소들을 다양하게 활용하여 일정을 구성하십시오.";
        }


        return String.format("""
%s
당신은 전라남도 여행 전문가입니다. %s아래에 명시된 '사용 가능한 장소 목록'을 **최대한 다양하게 활용**하여, 요청된 기간과 조건에 맞는 전라남도 여행 일정을 JSON 형식으로 생성하십시오.

--- 🚨 필수 지침: 이 지침을 최우선으로 준수하십시오 🚨 ---
1.  **'place' 필드에 '사용 가능한 장소 목록'에 없는 장소는 절대로 추가하지 마십시오.**
    * 단, '출발지'(%s)는 예외적으로 일정의 시작/끝 지점 또는 경유지로 포함될 수 있습니다. (예: '출발지'에서 '목적지'로 이동)
2.  **일정 내 각 장소의 'name'과 'addr'는 '사용 가능한 장소 목록'에 있는 정보와 정확히 일치해야 합니다.** 어떠한 변형이나 혼용도 허용되지 않습니다.
3.  **'사용 가능한 장소 목록'에 있는 장소들을 균형 있고 다양하게 조합하여 일정을 만드십시오.** 하루에 같은 장소를 두 번 이상 반복하는 것은 피하고, 여러 날에 걸쳐 같은 장소를 사용할 때는 합리적인 이유(예: 숙박)를 명시하십시오.

--- 📍 기본 여행 정보 ---
출발일: %s
기간: %d일
출발지: %s
출발 시간: %s

--- 🟦 사용 가능한 장소 목록 (JSON 형식) ---
%s

--- 🧭 일정 구성 규칙 ---
1.  날짜별로 "YYYY-MM-DD" 키를 사용하여 일정을 구성합니다.
2.  각 날짜의 일정은 [{"time":"HH:MM", "place":"장소명", "activity":"활동 내용", "addr":"정확한 주소"}] 형식의 JSON 배열이어야 합니다.
3.  하루에 6개의 활동을 포함하되, **각 날짜에는 물리적으로 이동 가능한 1~3개의 주요 장소를 '사용 가능한 장소 목록'에서 선정하고, 그 장소 내에서 또는 그 장소를 중심으로 심층적이고 구체적인 활동을 상세하게 작성하십시오. 이동 동선을 고려하여 효율적인 일정을 만드십시오.**
4.  활동은 반드시 시간 순서대로 정렬하십시오.
5.  모든 동선은 효율적이고 경제적인 경로로 계획하며, 이동 시간과 거리를 최소화하십시오.

--- ⚠️ 출력 형식 제한 사항 ---
* 출력은 오직 JSON 형식으로만 제공해야 합니다. 어떠한 추가 설명이나 주석도 포함하지 마십시오.
* '사용 가능한 장소 목록'에 있는 장소 중 주소가 없거나 "undefined"인 경우가 있다면, 해당 장소는 일정에서 제외하거나 목록 내의 다른 유효한 장소로 대체하십시오.
""",
                additionalRequirements,
                primaryFavoriteContext,
                departurePlace, // 출발지를 프롬프트에 넘겨줍니다.
                startDate,
                tripDays,
                departurePlace,
                departureTime,
                jsonAvailablePlaces
        );
    }

    private List<PlaceInfoDTO> loadAllPlaces() {
        System.out.println("📥 loadAllPlaces() 호출됨 - 장소 JSON 로드 시도");

        try (InputStream is = getClass().getResourceAsStream("/data/places.json")) {
            if (is == null) {
                System.err.println("❌ InputStream이 null입니다. 경로: /data/places.json 확인 필요");
                throw new RuntimeException("📛 장소 정보를 불러오는 데 실패했습니다. (리소스를 찾을 수 없음)");
            }

            List<PlaceInfoDTO> placeList = objectMapper.readValue(is, new TypeReference<List<PlaceInfoDTO>>() {});
            System.out.println("✅ 장소 정보 로딩 성공! 총 " + placeList.size() + "개 장소 로드됨");

            placeList.stream().limit(5).forEach(p -> {
                System.out.println(" - name: " + p.name() + ", addr: " + p.addr());
            });

            return placeList;

        } catch (IOException e) {
            System.err.println("❌ 장소 정보 JSON 파싱 중 오류 발생");
            e.printStackTrace();
            throw new RuntimeException("📛 장소 정보를 불러오는 데 실패했습니다.", e);
        }
    }

    private static class ChatRequest {
        public String model;
        public Message[] messages;
        public int max_tokens;
        public Float temperature;

        public ChatRequest(String model, Message[] messages, int max_tokens, Float temperature) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = max_tokens;
            this.temperature = temperature;
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
