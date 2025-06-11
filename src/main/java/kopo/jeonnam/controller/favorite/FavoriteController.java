package kopo.jeonnam.controller.favorite;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.service.impl.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/save")
    @ResponseBody
    public String saveFavorite(@RequestBody FavoriteDTO dto, HttpSession session) {
        String userId = (String) session.getAttribute("SS_USER_ID");
        if (userId == null) return "Not logged in";

        FavoriteDTO updatedDto = new FavoriteDTO(
                userId,
                dto.type(),
                dto.name(),
                dto.location(),
                dto.x(),
                dto.y(),
                dto.planPhone(),
                dto.planHomepage(),
                dto.planParking(),
                dto.planContents()
        );

        return favoriteService.saveFavorite(updatedDto);
    }

    @PostMapping("/test-insert")
    @ResponseBody
    public String testInsert() {
        String userId = "user01";

        // Theme (Course) Data
        List<FavoriteDTO> themes = List.of(
                new FavoriteDTO(userId, "theme", "죽녹원", "전라남도 담양군 담양읍 죽녹원로 119", "126.98661759410753", "35.32535674999729",
                        "061-380-2680", "http://www.juknokwon.go.kr/index.juknok", "가능",
                        "담양군에서 조성한 담양읍 향교리의 죽녹원이 죽림욕장으로 인기다..."),

                new FavoriteDTO(userId, "theme", "메타세콰이어 가로수길", "전라남도 담양군 담양읍 메타세쿼이아로 12", "127.00318935284791", "35.323406959344254",
                        "061-380-3149", "https://www.damyang.go.kr/tour/board/view.damyang?boardId=BBS_0000169&menuCd=DOM_000000301001001000&paging=ok&startPage=1&dataSid=319428", "가능",
                        "전국 제일의 가로수길로 설명되는 '메타세쿼이아길'은 숲이 만들어 놓은 터널처럼..."),

                new FavoriteDTO(userId, "theme", "대원사 벚꽃길", "전라남도 보성군 문덕면 죽산길 506-8", "127.13334338635387", "34.96133951236614",
                        "", "https://www.boseong.go.kr/tour/theme/roadtour/beautiful_road/cherry_blossom", "-",
                        "전남 보성군 문덕면 죽산리 대원사 벚꽃길은 1980년에 조성하여 지금은 터널을 이루어...")
        );

        // Movie Data
        List<FavoriteDTO> movies = List.of(
                new FavoriteDTO(userId, "movie", "언니네 산지직송2", "완도 신지도", "126.848666458827", "34.333793723869", "", "", "", ""),
                new FavoriteDTO(userId, "movie", "악연", "(구) 장흥교도소", "126.90022014505392", "34.62939199157511", "", "", "", ""),
                new FavoriteDTO(userId, "movie", "계시록", "(구) 장흥교도소", "126.90022014505392", "34.62939199157511", "", "", "", ""),
                new FavoriteDTO(userId, "movie", "하이퍼나이프", "국립목포해양대학교", "126.36295973474314", "34.79120153921345", "", "", "", ""),
                new FavoriteDTO(userId, "movie", "폭싹 속았수다", "순천드라마촬영장", "127.538244213124", "34.9584569682125", "", "", "", "")
        );

        // Save all
        themes.forEach(favoriteService::saveFavorite);
        movies.forEach(favoriteService::saveFavorite);

        return "Test insert completed for user01";
    }
}