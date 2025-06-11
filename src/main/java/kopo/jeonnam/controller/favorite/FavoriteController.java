package kopo.jeonnam.controller.favorite;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import kopo.jeonnam.service.impl.favorite.FavoriteService;
import kopo.jeonnam.service.impl.theme.RecommendCoursePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final RecommendCoursePlanService recommendCoursePlanService;

    @PostMapping
    public ResponseEntity<FavoriteEntity> addFavorite(@RequestBody FavoriteDTO favoriteDTO, HttpSession session) {
        String email = (String) session.getAttribute("email");
        System.out.println("세션에서 가져온 email: " + email);  // 로그 출력

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String planPhone = favoriteDTO.planPhone();
        String planHomepage = favoriteDTO.planHomepage();
        String planParking = favoriteDTO.planParking();
        String planContents = favoriteDTO.planContents();

        if ("media".equals(favoriteDTO.type())) {
            planPhone = "";
            planHomepage = "";
            planParking = "";
            planContents = "";
        }

        FavoriteDTO dtoWithUserId = new FavoriteDTO(
                email,
                favoriteDTO.type(),
                favoriteDTO.name(),
                favoriteDTO.location(),
                favoriteDTO.posterUrl(),
                favoriteDTO.x(),
                favoriteDTO.y(),
                planPhone,
                planHomepage,
                planParking,
                planContents
        );

        FavoriteEntity savedEntity = favoriteService.saveFavorite(dtoWithUserId);
        return ResponseEntity.ok(savedEntity);
    }


    @GetMapping("/check")
    public ResponseEntity<Boolean> checkFavorite(
            @RequestParam String type,
            @RequestParam String name,
            @RequestParam String location,
            HttpSession session) {

        String email = (String) session.getAttribute("email");
        System.out.println("[CHECK] Email: " + email);
        System.out.println("[CHECK] Type: " + type);
        System.out.println("[CHECK] Name: " + name);
        System.out.println("[CHECK] Location: " + location);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean exists = favoriteService.existsByUserIdAndTypeAndNameAndLocation(email, type, name, location);
        System.out.println("[CHECK] Exists: " + exists);

        return ResponseEntity.ok(exists);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFavorite(
            @RequestParam String type,
            @RequestParam String name,
            @RequestParam String location,
            HttpSession session) {

        String email = (String) session.getAttribute("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean deleted = favoriteService.deleteByTypeAndNameAndLocation(type, name, location, email);

        if (deleted) {
            return ResponseEntity.ok("찜 취소 완료");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("찜 항목을 찾을 수 없습니다.");
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RecommendCoursePlanDTO>> getNearbyRecommendations(
            @RequestParam double latMin,
            @RequestParam double latMax,
            @RequestParam double lngMin,
            @RequestParam double lngMax) {

        List<RecommendCoursePlanDTO> dtoList = recommendCoursePlanService.findNearby(latMin, latMax, lngMin, lngMax).stream()
                .map(entity -> {
                    RecommendCoursePlanDTO dto = new RecommendCoursePlanDTO();
                    dto.setPlanInfoId(entity.getPlanInfoId());
                    dto.setPlanName(entity.getPlanName());
                    dto.setPlanArea(entity.getPlanArea());
                    dto.setPlanAddr(entity.getPlanAddr());
                    dto.setPlanPhone(entity.getPlanPhone());
                    dto.setPlanHomepage(entity.getPlanHomepage());
                    dto.setPlanParking(entity.getPlanParking());
                    dto.setPlanContents(entity.getPlanContents());

                    try {
                        dto.setPlanLatitude(Double.parseDouble(String.valueOf(entity.getPlanLatitude())));
                        dto.setPlanLongitude(Double.parseDouble(String.valueOf(entity.getPlanLongitude())));
                    } catch (NumberFormatException e) {
                        dto.setPlanLatitude(0);
                        dto.setPlanLongitude(0);
                    }

                    dto.setImageUrls(List.of()); // 주변 찜 추천에선 이미지 제외

                    return dto;
                }).toList();

        return ResponseEntity.ok(dtoList);
    }
}