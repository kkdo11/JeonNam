package kopo.jeonnam.controller.favorite;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import kopo.jeonnam.repository.mongo.favorite.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteViewController {

    private final FavoriteRepository favoriteRepository;

    @GetMapping("/testAllList")
    public String showFavorites(Model model, HttpSession session) {

        String userId = "user01"; // 추후 세션에서 가져올 수 있음

        List<FavoriteEntity> favorites = favoriteRepository.findAll()
                .stream()
                .filter(fav -> userId.equals(fav.getUserId()))
                .toList();

        List<FavoriteEntity> movieFavorites = favorites.stream()
                .filter(f -> "movie".equalsIgnoreCase(f.getType()))
                .toList();

        List<FavoriteEntity> courseFavorites = favorites.stream()
                .filter(f -> "theme".equalsIgnoreCase(f.getType()))
                .toList();

        // ✅ 슬프로그 디버깅
        log.info("🎬 movieFavorites: {}개", movieFavorites.size());
        for (FavoriteEntity fav : movieFavorites) {
            log.info("  - {} / {}", fav.getName(), fav.getLocation());
        }

        log.info("🌄 courseFavorites: {}개", courseFavorites.size());
        for (FavoriteEntity fav : courseFavorites) {
            log.info("  - {} / {}", fav.getName(), fav.getLocation());
        }

        // 모델 전달
        model.addAttribute("movieFavorites", movieFavorites);
        model.addAttribute("courseFavorites", courseFavorites);
        model.addAttribute("favoriteList", favorites); // JS에서 사용

        return "theme/favoriteTest-listAll";
    }
}
