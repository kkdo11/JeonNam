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
import java.util.HashSet; // HashSet 추가

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
     * [1] 사용자가 찜한 장소명 목록 (클라이언트에서 선택된 모든 장소)을 기반으로 일정 생성.
     * places.json의 모든 장소와 사용자가 선택한 찜 장소(places.json에 없어도 추가)를 GPT에 전달합니다.
     */
    public String createScheduleWithFavorites(
            Set<String> favoriteNames, // 클라이언트에서 선택된 모든 찜 장소 이름
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt
    ) {
        List<PlaceInfoDTO> allPlaces = loadAllPlaces(); // places.json의 모든 장소 로드

        // GPT에 전달할 최종 장소 목록을 places.json의 모든 장소로 시작
        List<PlaceInfoDTO> finalPlacesForGpt = new ArrayList<>(allPlaces);

        // 프롬프트에 사용될 '기준' 장소 이름 (첫 번째 찜 장소 또는 기본값)
        final String primaryFavoriteNameForPrompt;
        final String firstClientFavoriteName;

        if (favoriteNames != null && !favoriteNames.isEmpty()) {
            firstClientFavoriteName = favoriteNames.iterator().next(); // 첫 번째 찜 장소만 프롬프트 컨텍스트용으로 사용
            primaryFavoriteNameForPrompt = firstClientFavoriteName;

            // 선택된 각 찜 장소를 순회하며 finalPlacesForGpt에 추가 (places.json에 없으면 가상 장소로)
            for (String selectedFavName : favoriteNames) {
                boolean foundInAllPlaces = allPlaces.stream()
                        .anyMatch(p -> Objects.equals(p.name(), selectedFavName));

                if (!foundInAllPlaces) {
                    System.err.println("WARN: 찜한 장소 '" + selectedFavName + "'을 places.json에서 찾을 수 없습니다. " +
                            "가상의 장소 정보를 생성하여 GPT에 전달합니다. places.json 파일의 이름과 일치하는지 확인해 주세요.");
                    // 가상의 PlaceInfoDTO를 생성하여 finalPlacesForGpt에 추가
                    // 주소는 departurePlace를 사용하여 GPT가 출발지와 연관지어 생각하도록 유도합니다.
                    finalPlacesForGpt.add(new PlaceInfoDTO(selectedFavName, departurePlace));
                } else {
                    System.out.println("DEBUG: 찜한 장소 '" + selectedFavName + "'을 places.json에서 찾았습니다. 일정에 포함됩니다.");
                }
            }
        } else {
            firstClientFavoriteName = null;
            primaryFavoriteNameForPrompt = "선택된 찜 장소 없음";
            System.err.println("WARN: 일정 생성을 위한 찜 장소가 선택되지 않았습니다. places.json의 모든 장소를 사용합니다.");
        }

        if (finalPlacesForGpt.isEmpty()) {
            System.err.println("WARN: GPT에 전달할 장소가 없습니다. places.json 파일에 유효한 장소가 있는지 확인해주세요.");
            return "{\"error\": \"일정 생성을 위한 장소 데이터가 부족합니다. places.json 파일을 확인해주세요.\"}";
        }

        System.out.println("DEBUG: GPT에 전달할 최종 장소 개수 (선택된 모든 찜 장소 포함): " + finalPlacesForGpt.size());
        finalPlacesForGpt.stream().limit(10).forEach(p -> System.out.println("  - 최종 전달 장소: " + p.name() + " (" + p.addr() + ")"));


        return createScheduleFromRequest(
                finalPlacesForGpt, // 이 리스트는 이제 사용자가 선택한 모든 찜 장소(또는 가상 장소)를 포함합니다.
                startDate,
                tripDays,
                departurePlace,
                departureTime,
                additionalPrompt,
                primaryFavoriteNameForPrompt, // 프롬프트의 '기준 장소' 컨텍스트용 (첫 번째 찜 장소)
                favoriteNames // GPT 프롬프트에서 '사용자가 특별히 요청한 장소들'을 명시하기 위함
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
        return createScheduleFromRequest(places, startDate, tripDays, departurePlace, departureTime, additionalPrompt, "", Collections.emptySet());
    }


    /**
     * [2-2] GPT에게 일정 생성 요청 (기준 장소 및 선택된 모든 찜 장소 컨텍스트 포함)
     */
    public String createScheduleFromRequest(
            List<PlaceInfoDTO> places, // 이 목록에는 사용자의 찜 장소가 포함될 수 있습니다.
            String startDate,
            int tripDays,
            String departurePlace,
            String departureTime,
            String additionalPrompt,
            String contextPrimaryFavoriteName, // GPT 프롬프트에 포함될 '기준' 장소 이름 (선택한 찜 중 첫 번째)
            Set<String> actualSelectedFavoriteNames // 사용자가 클라이언트에서 선택한 모든 찜 장소 이름 목록
    ) {
        System.out.println("=== 📌 GPT 요청 전 장소 정보 (기준 장소: " + contextPrimaryFavoriteName + ", 선택된 찜 장소 수: " + (actualSelectedFavoriteNames != null ? actualSelectedFavoriteNames.size() : 0) + ", 목록 개수: " + places.size() + ") ===");
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
                contextPrimaryFavoriteName, // 프롬프트의 '기준 장소' 컨텍스트용
                actualSelectedFavoriteNames // 프롬프트에서 '사용자가 특별히 요청한 장소들'을 명시하기 위함
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
                            // ⭐⭐⭐ 핵심 수정: 찜한 장소를 최우선으로 허용 ⭐⭐⭐
                            if (actualSelectedFavoriteNames != null && actualSelectedFavoriteNames.contains(placeName)) {
                                // 사용자가 특별히 요청한 찜 장소는 주소 일치 여부와 관계없이 허용합니다.
                                isAllowed = true;
                                System.out.println("DEBUG: 요청된 찜 장소 '" + placeName + "'가 일정에 포함됨 (주소: '" + placeAddr + "').");
                            } else if (placeName.equals(departurePlace)) {
                                // 출발지는 이전과 동일하게 허용합니다.
                                isAllowed = true;
                                System.out.println("DEBUG: 출발지 '" + departurePlace + "'가 일정에 포함됨 (주소: '" + placeAddr + "').");
                            } else {
                                // 그 외의 장소들은 places.json (또는 가상 장소 목록)에 이름과 주소가 모두 일치해야 허용합니다.
                                for (PlaceInfoDTO p : places) {
                                    if (Objects.equals(p.name(), placeName) && Objects.equals(p.addr(), placeAddr)) {
                                        isAllowed = true;
                                        System.out.println("DEBUG: places.json 또는 가상 장소 목록에 있는 장소 '" + placeName + "' (" + placeAddr + ")가 일정에 포함됨.");
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
            String contextPrimaryFavoriteName,
            Set<String> actualSelectedFavoriteNames // 사용자가 선택한 모든 찜 장소 이름
    ) {
        String jsonAvailablePlaces = places.stream()
                .map(p -> String.format("{\"name\": \"%s\", \"addr\": \"%s\"}", p.name(), p.addr()))
                .collect(Collectors.joining(", ", "[", "]"));

        String additionalRequirements = (additionalPrompt != null && !additionalPrompt.trim().isEmpty())
                ? "✅ [사용자 요청사항 - 반드시 반영]\n- " + additionalPrompt.trim() + "\n\n"
                : "";

        String favoriteInclusionContext = "";
        if (actualSelectedFavoriteNames != null && !actualSelectedFavoriteNames.isEmpty()) {
            String selectedFavsString = String.join(", ", actualSelectedFavoriteNames);
            favoriteInclusionContext = String.format(
                    "사용자가 특별히 포함을 요청한 장소들은 다음과 같습니다: **%s**. " +
                            "이 장소들을 포함하여 전라남도 전역의 다른 매력적인 장소들을 다양하게 활용하여 일정을 구성하십시오. " +
                            "제공된 '사용 가능한 장소 목록'에는 이 모든 요청 장소가 포함되어 있습니다.\n",
                    selectedFavsString
            );
        } else {
            favoriteInclusionContext = "사용자가 특정 기준 장소를 선택하지 않았습니다. 전라남도 전역의 장소들을 다양하게 활용하여 일정을 구성하십시오.\n";
        }


        return String.format("""
%s
당신은 전라남도 여행 전문가입니다. %s아래에 명시된 '사용 가능한 장소 목록'을 **최대한 다양하게 활용**하여, 요청된 기간과 조건에 맞는 전라남도 여행 일정을 JSON 형식으로 생성하십시오.

--- 🚨 필수 지침: 이 지침을 최우선으로 준수하십시오 🚨 ---
1.  **'place' 필드에 '사용 가능한 장소 목록'에 없는 장소는 절대로 추가하지 마십시오.**
    * 단, '출발지'(%s)는 예외적으로 일정의 시작/끝 지점 또는 경유지로 포함될 수 있습니다. 출발지의 정확한 주소는 명확히 제공되지 않으므로, 활동 내용은 이동과 관련된 추상적인 설명을 사용하고 주소는 '%s'로 표기하십시오.
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
3.  하루에 8~9개의 활동을 포함하되, **각 날짜에는 물리적으로 이동 가능한 1~3개의 주요 장소를 '사용 가능한 장소 목록'에서 선정하고, 그 장소 내에서 또는 그 장소를 중심으로 심층적이고 구체적인 활동을 상세하게 작성하십시오. 이동 동선을 고려하여 효율적인 일정을 만드십시오.**
4.  활동은 반드시 시간 순서대로 정렬하십시오.
5.  모든 동선은 효율적이고 경제적인 경로로 계획하며, 이동 시간과 거리를 최소화하십시오.

--- ⚠️ 출력 형식 제한 사항 ---
* 출력은 오직 JSON 형식으로만 제공해야 합니다. 어떠한 추가 설명이나 주석도 포함하지 마십시오.
* '사용 가능한 장소 목록'에 있는 장소 중 주소가 없거나 "undefined"인 경우가 있다면, 해당 장소는 일정에서 제외하거나 목록 내의 다른 유효한 장소로 대체하십시오.
""",
                additionalRequirements,
                favoriteInclusionContext, // 새로운 컨텍스트 사용
                departurePlace,
                departurePlace,
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
