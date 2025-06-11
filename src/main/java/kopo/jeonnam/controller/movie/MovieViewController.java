package kopo.jeonnam.controller.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MovieViewController {


    //http://localhost:8080/movie
    @GetMapping("/movie")
    public String showMovieListPage() {

        return "/movie/movieList";
    }



    @GetMapping("/movie/detail")
    public String showMovieDetailPage(@RequestParam(required = false) String id) {

        return "movie/movieDetail"; // movie/movieDetail.html 템플릿을 찾습니다.
    }
}