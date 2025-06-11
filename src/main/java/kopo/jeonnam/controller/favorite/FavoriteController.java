package kopo.jeonnam.controller.favorite;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import kopo.jeonnam.service.impl.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

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
}