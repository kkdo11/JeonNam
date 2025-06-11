package kopo.jeonnam.service.movie;

import kopo.jeonnam.dto.movie.MovieDTO;

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
}