package kopo.jeonnam.controller.movie;

import kopo.jeonnam.dto.movie.MovieDTO;
import kopo.jeonnam.repository.entity.movie.MovieEntity;
import kopo.jeonnam.repository.mongo.movie.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트 추가 (로그 사용 시)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus; // HttpStatus 임포트 추가
import org.springframework.http.ResponseEntity; // ResponseEntity 임포트 추가
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional; // Optional 임포트 추가

@Slf4j // 로그 사용을 위한 어노테이션
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies") // 모든 API에 공통적으로 붙는 경로
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping("/list") // 전체 영화 리스트 (페이징) API
    public Page<MovieDTO> getPagedMovies(
            @RequestParam(defaultValue = "0") int page, // 현재 페이지 번호 (0부터 시작)
            @RequestParam(defaultValue = "16") int size) { // 페이지당 항목 수

        log.info(this.getClass().getName() + ".getPagedMovies Start! Page: " + page + ", Size: " + size);

        // Pageable 객체 생성 (페이지 번호, 페이지당 항목 수)
        Pageable pageable = PageRequest.of(page, size);

        // 페이지네이션된 데이터 조회
        Page<MovieEntity> moviePage = movieRepository.findAll(pageable);

        // DTO로 변환하여 반환
        Page<MovieDTO> movieDTOPage = moviePage.map(movie -> new MovieDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getLocation(),
                movie.getPosterUrl(),
                movie.getX(),
                movie.getY()
        ));

        log.info(this.getClass().getName() + ".getPagedMovies End! Total Elements: " + movieDTOPage.getTotalElements());
        return movieDTOPage;
    }

    // 특정 영화의 상세 정보를 JSON으로 반환하는 API
    // URL 예시: /api/movies/detail?id=60c72b2f9c9a5b0015f8e2a3
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
            return new ResponseEntity<>(dto, HttpStatus.OK); // 200 OK 응답과 데이터 반환
        } else {
            log.warn(this.getClass().getName() + ".getMovieDetailApi End! Movie not found for ID: " + id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 Not Found 응답 반환
        }
    }
}