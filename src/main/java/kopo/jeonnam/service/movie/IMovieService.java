package kopo.jeonnam.service.movie;

import kopo.jeonnam.dto.movie.MovieDTO;
import kopo.jeonnam.dto.movie.MovieSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IMovieService {
    /**
     * 모든 영화 정보를 조회합니다.
     * @return 영화 DTO 리스트
     */
    List<MovieDTO> getAllMovies();

    /**
     * 특정 ID의 영화 상세 정보를 조회합니다.
     * @param movieId 조회할 영화의 ID
     * @return 영화 상세 DTO 또는 null
     */
    MovieDTO getMovieDetail(String movieId);

    /**
     * 영화를 검색하고 페이징 처리하여 반환합니다.
     * 다양한 검색 조건(키워드, 제목, 장소, 주소) 및 정렬을 지원합니다.
     *
     * @param searchRequest 검색 조건을 담은 {@link MovieSearchRequest} 객체
     * @param pageable      페이징 및 정렬 정보를 담은 {@link Pageable} 객체
     * @return 검색 조건에 맞는 영화들의 페이징 처리된 {@link MovieDTO} 목록
     */
    Page<MovieDTO> searchMovies(MovieSearchRequest searchRequest, Pageable pageable);
}
