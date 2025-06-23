package kopo.jeonnam.controller.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.dto.gpt.GptRequestDTO;
import kopo.jeonnam.dto.gpt.PlaceInfoDTO;
import kopo.jeonnam.service.gpt.IGptService;
import kopo.jeonnam.service.impl.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@Controller
@RequiredArgsConstructor
public class GptController {

    private final FavoriteService favoriteService;
    private final IGptService gptService;

    // GET: 세션 기반 찜 목록으로 추천 일정 생성
    @GetMapping("/gpt/recommend-schedule")
    public ResponseEntity<String> recommendScheduleBySession(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            System.out.println("[ERROR] 로그인 필요 - 세션에 email 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        System.out.println("[INFO] 추천 일정 요청 - userEmail: " + email);

        List<PlaceInfoDTO> placeInfoList = favoriteService.getFavoritesByUserId(email).stream()
                .filter(fav -> (fav.type().equals("media") || fav.type().equals("theme"))
                        && fav.addr() != null && !fav.addr().isBlank())
                .map(fav -> {
                    String type = fav.type() != null ? fav.type().trim().toLowerCase() : "";
                    String name; // PlaceInfoDTO에 들어갈 이름
                    String addr = fav.addr(); // PlaceInfoDTO에 들어갈 주소

                    // ✅ 로그 추가: 원본 FavoriteDTO 정보와 가공될 정보 미리 보기
                    System.out.println(String.format("[DEBUG] Original Favorite - Type: %s, Name: %s, Location: %s, Addr: %s",
                            fav.type(), fav.name(), fav.location(), fav.addr()));

                    switch (type) {
                        case "media":
                            name = fav.location();
                            System.out.println(String.format("[DEBUG] Processed for MEDIA - PlaceInfoDTO.name: %s, PlaceInfoDTO.addr: %s", name, addr));
                            break;
                        case "theme":
                            name = fav.name();
                            System.out.println(String.format("[DEBUG] Processed for THEME - PlaceInfoDTO.name: %s, PlaceInfoDTO.addr: %s", name, addr));
                            break;
                        default:
                            name = "알수없는타입";
                            System.out.println(String.format("[DEBUG] Processed for UNKNOWN TYPE - PlaceInfoDTO.name: %s, PlaceInfoDTO.addr: %s", name, addr));
                            break;
                    }
                    return new PlaceInfoDTO(name, addr);
                })
                .filter(dto -> dto.name() != null && !dto.name().isBlank())
                .distinct()
                .toList();


        if (placeInfoList.isEmpty()) {
            System.out.println("[WARN] 찜한 장소 없음 - userEmail: " + email);
            return ResponseEntity.badRequest().body("찜한 장소가 없습니다.");
        }

        System.out.println("[INFO] 추천 장소 개수: " + placeInfoList.size());

        try {
            // 로그 추가: 실제 전송될 장소 목록 확인 (최종 PlaceInfoDTO 리스트)
            System.out.println("[INFO] 최종 GPT로 전달할 장소 목록 확인 (PlaceInfoDTO List)");
            for (PlaceInfoDTO place : placeInfoList) {
                System.out.println("[CHECK] Final PlaceInfoDTO - name: '" + place.name() + "', addr: '" + place.addr() + "'");
            }

            String gptResponse = gptService.createScheduleFromRequest(
                    placeInfoList,
                    "2025-06-19", // 예시 날짜
                    3, // 예시 일수
                    "출발지", // 예시 출발지
                    "09:00", // 예시 출발 시간
                    "" // 예시 추가 프롬프트
            );

            System.out.println("[INFO] GPT 서비스 정상 호출 완료");
            return ResponseEntity.ok(gptResponse);
        } catch (Exception e) {
            System.out.println("[ERROR] GPT 서비스 호출 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("일정 생성 중 오류 발생");
        }
    }


    // POST: 사용자 입력 기반 GPT 일정 생성
    @PostMapping("/gpt/recommend-schedule")
    public ResponseEntity<String> recommendScheduleWithUserInput(
            @RequestBody String rawJson,
            HttpSession session
    ) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            GptRequestDTO request = mapper.readValue(rawJson, GptRequestDTO.class);

            if (request.places() == null || request.places().isEmpty()) {
                return ResponseEntity.badRequest().body("선택한 장소가 없습니다.");
            }

            // 📌 찜 목록 전체 가져오기
            List<FavoriteDTO> favorites = favoriteService.getFavoritesByUserId(email);

            // 📌 선택된 장소이름 기반으로 주소까지 포함된 PlaceInfoDTO 생성
            List<PlaceInfoDTO> placeInfoList = request.places().stream()
                    .map(placeName -> {
                        FavoriteDTO matched = favorites.stream()
                                .filter(fav -> fav.name().equals(placeName) || fav.location().equals(placeName))
                                .findFirst()
                                .orElse(null);

                        if (matched != null) {
                            String type = matched.type() != null ? matched.type().trim().toLowerCase() : "";
                            String addr = matched.addr() != null ? matched.addr() : "";

                            String finalName;
                            switch (type) {
                                case "theme":
                                    finalName = matched.name(); // 실제 장소명
                                    break;
                                case "media":
                                    finalName = matched.location(); // 미디어 촬영지 지역
                                    break;
                                default:
                                    finalName = placeName; // fallback
                            }

                            return new PlaceInfoDTO(finalName, addr);
                        } else {
                            return new PlaceInfoDTO(placeName, ""); // fallback if no match
                        }
                    })
                    .toList();


            String gptResponse = gptService.createScheduleFromRequest(
                    placeInfoList,
                    request.startDate(),
                    request.tripDays(),
                    request.departurePlace(),
                    request.departureTime(),
                    request.additionalPrompt()
            );

            return ResponseEntity.ok(gptResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("일정 생성 중 오류 발생");
        }
    }



    // ✅ 프론트 fetch('/favorite/list')와 호환되는 경로
    @GetMapping("/favorite/list")
    public ResponseEntity<List<FavoriteDTO>> getFavoriteList(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FavoriteDTO> favoriteList = favoriteService.getFavoritesByUserId(email);
        return ResponseEntity.ok(favoriteList);
    }

    @GetMapping("/gpt/view")
    public String gptView() {
        log.info(this.getClass().getName() + ".gpt/view page Start!");
        log.info(this.getClass().getName() + ".gpt/view page End!");
        return "gpt/view"; // templates/gpt/view.html 경로를 반환
    }
}