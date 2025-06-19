package kopo.jeonnam.controller.movie;

import kopo.jeonnam.dto.movie.MovieDTO;
import kopo.jeonnam.repository.entity.movie.MovieEntity;
import kopo.jeonnam.repository.mongo.movie.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping("/list") // 영화 리스트 (검색 + 페이징)
    public Page<MovieDTO> getPagedMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(required = false) String keyword) {

        log.info("🎬 getPagedMovies Start! page={}, size={}, keyword={}", page, size, keyword);

        Pageable pageable = PageRequest.of(page, size);
        Page<MovieEntity> moviePage;

        // 🔍 검색 키워드가 있을 경우 title 필터링
        if (keyword != null && !keyword.trim().isEmpty()) {
            moviePage = movieRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
            log.info("🔍 Filtering by keyword: '{}'", keyword);
        } else {
            moviePage = movieRepository.findAll(pageable);
            log.info("📦 No keyword, returning full list.");
        }

        if (moviePage.isEmpty()) {
            log.info("🕳 No matching movies. Returning empty page.");
            return Page.empty(pageable);
        }

        Page<MovieDTO> movieDTOPage = moviePage.map(movie -> new MovieDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getLocation(),
                movie.getPosterUrl(),
                movie.getX(),
                movie.getY()
        ));

        log.info("✅ getPagedMovies End. TotalElements={}, TotalPages={}",
                movieDTOPage.getTotalElements(), movieDTOPage.getTotalPages());

        return movieDTOPage;
    }


    @GetMapping("/detail")
    public ResponseEntity<MovieDTO> getMovieDetailApi(@RequestParam String id) {
        log.info(this.getClass().getName() + ".getMovieDetailApi Start! movieId : " + id);

        Optional<MovieEntity> movieOptional = movieRepository.findById(id);

        if (movieOptional.isPresent()) {
            MovieEntity entity = movieOptional.get();
            MovieDTO dto = new MovieDTO(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getLocation(),
                    entity.getPosterUrl(),
                    entity.getX(),
                    entity.getY()
            );
            log.info(this.getClass().getName() + ".getMovieDetailApi End! Found movie: " + dto.getTitle());
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } else {
            log.warn(this.getClass().getName() + ".getMovieDetailApi End! Movie not found for ID: " + id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}