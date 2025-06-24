package kopo.jeonnam.controller.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.dto.gpt.GptRequestDTO;
import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import kopo.jeonnam.service.gpt.IGptService;
import kopo.jeonnam.service.impl.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Corrected to slf4j
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet; // HashSet import added
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/gpt")
public class GptController {

    private final FavoriteService favoriteService;
    private final IGptService gptService;
    private final ObjectMapper objectMapper;

    /**
     * Helper method to create a JSON error response string.
     * 클라이언트에 일관된 JSON 형식의 에러 메시지를 반환하기 위한 헬퍼 메서드.
     * @param message 주된 에러 메시지
     * @param details 에러에 대한 추가 상세 정보 (선택 사항)
     * @return JSON 형식의 에러 문자열
     */
    private String createErrorJson(String message, String details) {
        return String.format("{\"error\": \"%s\", \"details\": \"%s\"}",
                message.replace("\"", "\\\""),
                details != null ? details.replace("\"", "\\\"") : ""
        );
    }

    /**
     * [GET] 세션 기반 찜 목록으로 GPT 추천 일정 생성 (사용되지 않거나 테스트용)
     * URL: /gpt/recommend-schedule (GET)
     * @param session HTTP 세션 (로그인 정보 확인용)
     * @return GPT가 생성한 JSON 일정 또는 JSON 에러 메시지
     */
    @GetMapping("/recommend-schedule")
    @ResponseBody
    public ResponseEntity<String> recommendScheduleBySession(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            log.warn("GET /recommend-schedule 요청 - 로그인 필요: 세션에 email 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Unauthorized", "로그인이 필요합니다."));
        }

        log.info("GET /recommend-schedule 요청 수신 - userEmail: {}", email);

        List<PlaceInfoDTO> placeInfoList = favoriteService.getFavoritesByUserId(email).stream()
                .map(fav -> {
                    String nameToUse = null;
                    String addrToUse = fav.addr();

                    if (fav.type() != null) {
                        String type = fav.type().trim().toLowerCase();
                        switch (type) {
                            case "media":
                                nameToUse = fav.location();
                                if (nameToUse == null || nameToUse.isBlank()) {
                                    log.warn("찜한 장소 (media 타입)에 location 필드가 비어있음: {}. fav.name()으로 대체 시도.", fav);
                                    nameToUse = fav.name();
                                }
                                break;
                            case "theme":
                                nameToUse = fav.name();
                                break;
                            default:
                                log.warn("알 수 없는 찜한 장소 타입 감지됨: {}. fav.name() 사용 시도.", fav.type());
                                nameToUse = fav.name();
                                break;
                        }
                    } else {
                        log.warn("찜한 장소의 type 필드가 null임. fav.name() 사용 시도: {}", fav);
                        nameToUse = fav.name();
                    }

                    if (nameToUse == null || nameToUse.isBlank() || addrToUse == null || addrToUse.isBlank()) {
                        log.warn("찜한 장소 '{}' (주소: '{}')의 이름 또는 주소가 유효하지 않아 GPT 일정 생성에서 제외됩니다. 원본 FavoriteDTO: {}", nameToUse, addrToUse, fav);
                        return null;
                    }

                    log.debug("최종 PlaceInfoDTO 변환 완료: Name='{}', Addr='{}'", nameToUse, addrToUse);
                    return new PlaceInfoDTO(nameToUse, addrToUse);
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (placeInfoList.isEmpty()) {
            log.warn("GET /recommend-schedule 요청 - 유효한 찜한 장소가 없어 일정 생성 불가 (userEmail: {})", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Bad Request", "유효한 찜한 장소가 없습니다. 일정 생성을 위해 찜을 추가해 주세요."));
        }

        // ⭐ 여기서는 GPTService.createScheduleWithFavorites를 호출하기 위한 단일 찜 장소 선택 로직이 없습니다.
        // 이 엔드포인트는 주로 테스트용으로 남겨두고, 실제 사용은 POST /recommend-schedule로 가정합니다.
        // 따라서 이 GET 요청을 통해 호출한다면, 여전히 allPlaces를 다루지 않을 수 있습니다.
        // 현재는 POST 요청에 집중하여 수정합니다.

        try {
            // 이 경로는 createScheduleWithFavorites를 직접 호출하지 않으므로,
            // 확장된 장소 목록을 GPT에 전달하는 로직이 여기에 직접 구현되어야 합니다.
            // 하지만 이 메서드는 사용자가 단일 찜을 선택하도록 하는 UI와 연결되지 않으므로,
            // POST 메서드에 집중하는 것이 더 합리적입니다.
            String gptResponse = gptService.createScheduleFromRequest(
                    placeInfoList, // 현재는 이 리스트에 찜한 장소만 포함되어 있습니다.
                    "2025-06-24", // 예시 날짜 (필요에 따라 사용자 입력으로 변경)
                    2,            // 예시 일수 (필요에 따라 사용자 입력으로 변경)
                    "순천역",       // 예시 출발지 (필요에 따라 사용자 입력으로 변경)
                    "09:00",      // 예시 출발 시간 (필요에 따라 사용자 입력으로 변경)
                    ""            // 예시 추가 프롬프트 (필요에 따라 사용자 입력)
            );

            log.info("GET /recommend-schedule 요청 - GPT 서비스 정상 호출 완료");
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(gptResponse);

        } catch (Exception e) {
            log.error("GET /recommend-schedule 요청 - GPT 서비스 호출 중 서버 내부 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Internal Server Error", "일정 생성 중 서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }


    /**
     * [POST] 사용자 입력 기반 GPT 일정 생성
     * URL: /gpt/recommend-schedule (POST)
     * 클라이언트에서 넘어온 'places' 리스트의 모든 장소를 GptService.createScheduleWithFavorites에 전달합니다.
     * @param request GptRequestDTO (사용자 입력 데이터)
     * @param session HTTP 세션 (로그인 정보 확인용)
     * @return GPT가 생성한 JSON 일정 또는 JSON 에러 메시지
     */
    @PostMapping("/recommend-schedule")
    @ResponseBody
    public ResponseEntity<String> recommendScheduleWithUserInput(
            @RequestBody GptRequestDTO request,
            HttpSession session
    ) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            log.warn("POST /recommend-schedule 요청 - 로그인 필요: 세션에 email 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Unauthorized", "로그인이 필요합니다."));
        }

        log.info("POST /recommend-schedule 요청 수신: {}", request);

        // 클라이언트가 보낸 'places' 리스트가 비어있는지 확인
        if (request.places() == null || request.places().isEmpty()) {
            log.warn("POST /recommend-schedule 요청 - 선택한 장소가 없습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Bad Request", "선택한 장소가 없습니다. 일정 생성을 위해 찜을 추가해 주세요."));
        }

        // ⭐⭐⭐ 핵심 수정: 클라이언트에서 받은 모든 찜 장소 이름을 Set으로 변환하여 GptService에 전달 ⭐⭐⭐
        // request.places()는 List<String>이므로, 이를 HashSet의 생성자로 바로 전달하여 Set<String>으로 변환합니다.
        Set<String> allSelectedFavorites = new HashSet<>(request.places());

        // 로깅 추가 (기존 '기준 찜 장소' 로그 대체)
        log.info("POST /recommend-schedule 요청 - 전달될 찜 장소 (전체): {}", allSelectedFavorites);


        try {
            // GptService의 createScheduleWithFavorites 메서드를 호출하여
            // 선택된 모든 찜 장소를 활용하도록 합니다.
            String gptResponse = gptService.createScheduleWithFavorites(
                    allSelectedFavorites, // Set<String>으로 모든 찜 장소 이름을 전달
                    request.startDate(),
                    request.tripDays(),
                    request.departurePlace(),
                    request.departureTime(),
                    request.additionalPrompt()
            );

            log.info("POST /recommend-schedule 요청 - GPT 서비스 정상 호출 완료");
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(gptResponse);

        } catch (Exception e) {
            log.error("POST /recommend-schedule 요청 - 일정 생성 중 서버 내부 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Internal Server Error", "일정 생성 중 서버 내부 오류가 발생했습니다: " + e.getMessage()));
        }
    }


    /**
     * [GET] 찜 목록 조회 (프론트엔드 호환용)
     * URL: /gpt/favorite/list (GET)
     * @param session HTTP 세션 (로그인 정보 확인용)
     * @return 찜 목록 JSON 또는 JSON 에러 메시지
     */
    @GetMapping("/favorite/list")
    @ResponseBody // JSON 응답을 위해 @ResponseBody 추가
    public ResponseEntity<String> getFavoriteList(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            log.warn("GET /favorite/list 요청 - 로그인 필요: 세션에 email 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Unauthorized", "로그인이 필요합니다."));
        }

        try {
            List<FavoriteDTO> favoriteList = favoriteService.getFavoritesByUserId(email);
            log.info("GET /favorite/list 요청 - userEmail: {}, 찜 목록 개수: {}", email, favoriteList.size());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(objectMapper.writeValueAsString(favoriteList));
        } catch (Exception e) {
            log.error("GET /favorite/list 요청 - 찜 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(createErrorJson("Internal Server Error", "찜 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * [GET] GPT 일정 뷰 페이지 로딩
     * URL: /gpt/view (GET)
     * @return Thymeleaf 템플릿 경로
     */
    @GetMapping("/view")
    public String gptView(Model model) {
        log.info(this.getClass().getName() + ".gpt/view page Start!");
        log.info(this.getClass().getName() + ".gpt/view page End!");
        return "gpt/view";
    }
}
