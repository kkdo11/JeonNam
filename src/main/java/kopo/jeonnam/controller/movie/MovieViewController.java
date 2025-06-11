package kopo.jeonnam.controller.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MovieViewController {

    @Value("${KakaoJsApiKey}")
    private String kakaoJsKey;


    //http://localhost:8080/movie
    @GetMapping("/movie")
    public String showMovieListPage() {

        return "/movie/movieList";
    }



    @GetMapping("/movie/detail")
    public String showMovieDetailPage(@RequestParam(required = false) String id, Model model) {
        model.addAttribute("kakaoJsKey", kakaoJsKey);

        return "movie/movieDetail"; // movie/movieDetail.html 템플릿을 찾습니다.
    }
}