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

    @GetMapping("/list") // 전체 영화 리스트 (페이징) API
    public Page<MovieDTO> getPagedMovies(
            @RequestParam(defaultValue = "0") int page, // 현재 페이지 번호 (0부터 시작)
            @RequestParam(defaultValue = "16") int size) { // 페이지당 항목 수

        log.info(this.getClass().getName() + ".getPagedMovies Start! Page: " + page + ", Size: " + size);

        Pageable pageable = PageRequest.of(page, size);

        Page<MovieEntity> moviePage = movieRepository.findAll(pageable);

        // ✨✨✨ 여기에 로그를 추가해주세요! ✨✨✨
        log.info("--- Debugging Page Object ---");
        log.info("Total Elements: " + moviePage.getTotalElements());
        log.info("Total Pages: " + moviePage.getTotalPages());
        log.info("Number (current page): " + moviePage.getNumber());
        log.info("Size (items per page): " + moviePage.getSize());
        log.info("Has Content: " + moviePage.hasContent());
        log.info("Is Empty: " + moviePage.isEmpty());
        log.info("--- End Debugging Page Object ---");
        // ✨✨✨ 여기까지 ✨✨✨


        if (moviePage.getTotalElements() == 0) {
            log.info(this.getClass().getName() + ".getPagedMovies: No movies found. Returning empty page.");
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

        log.info(this.getClass().getName() + ".getPagedMovies End! Total Elements: " + movieDTOPage.getTotalElements() + ", Total Pages: " + movieDTOPage.getTotalPages());
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