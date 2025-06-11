package kopo.jeonnam.controller.favorite;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.service.favorite.IFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteViewController {

    private final IFavoriteService favoriteService;

    @GetMapping("/testAllList")
    public String showFavorites(Model model, HttpSession session) {
        // ✅ 세션에서 userId 가져오기
//        String userId = (String) session.getAttribute("SS_USER_ID");
//        if (userId == null) {
//            log.warn("세션에 사용자 ID가 없습니다. 로그인 필요.");
//            return "redirect:/login"; // 로그인 페이지로 리디렉션
//        }

        String userId = "taeseung92l@gmail.com";
        // ✅ 즐겨찾기 전체 조회
        List<FavoriteDTO> favorites = favoriteService.getFavoritesByUserId(userId);

        // ✅ 필터링
        List<FavoriteDTO> movieFavorites = favorites.stream()
                .filter(f -> "media".equalsIgnoreCase(f.type()))
                .toList();

        List<FavoriteDTO> courseFavorites = favorites.stream()
                .filter(f -> "theme".equalsIgnoreCase(f.type()))
                .toList();

        // ✅ 디버깅 로그
        log.info("🎬 movieFavorites: {}개", movieFavorites.size());
        movieFavorites.forEach(fav -> log.info("  - {} / {}", fav.name(), fav.location()));

        log.info("🌄 courseFavorites: {}개", courseFavorites.size());
        courseFavorites.forEach(fav -> log.info("  - {} / {}", fav.name(), fav.location()));

        // ✅ 모델에 추가
        model.addAttribute("movieFavorites", movieFavorites);
        model.addAttribute("courseFavorites", courseFavorites);
        model.addAttribute("favoriteList", favorites); // JS에서 사용할 전체 목록

        return "theme/favoriteTest-listAll";
    }
}
