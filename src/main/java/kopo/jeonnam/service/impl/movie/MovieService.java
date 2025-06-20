package kopo.jeonnam.service.impl.movie;

import kopo.jeonnam.dto.movie.MovieDTO;
import kopo.jeonnam.repository.entity.movie.MovieEntity;
import kopo.jeonnam.repository.mongo.movie.MovieRepository;
import kopo.jeonnam.service.movie.IMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections; // 빈 리스트 반환을 위해 추가
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 영화 관련 비즈니스 로직을 처리하는 서비스 구현 클래스
 * {@link IMovieService} 인터페이스를 구현합니다.
 * Lombok의 @Slf4j를 통해 로깅 기능을 제공하고, @RequiredArgsConstructor를 통해
 * final 필드에 대한 생성자 주입(DI)을 자동으로 처리합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service("movieService") // 빈 이름을 명시하여 명확성을 높일 수 있습니다.
public class MovieService implements IMovieService {

    // MongoDB Movie 컬렉션에 접근하기 위한 Repository 주입
    private final MovieRepository movieRepository;

    /**
     * 모든 영화 목록을 조회합니다.
     * 데이터베이스에서 MovieEntity 리스트를 가져와 MovieDTO 리스트로 변환하여 반환합니다.
     *
     * @return 조회된 모든 영화 정보의 {@link MovieDTO} 리스트. 조회 결과가 없을 경우 빈 리스트를 반환합니다.
     */
    @Override
    public List<MovieDTO> getAllMovies() {
        // 로그 시작
        log.info(this.getClass().getName() + ".getAllMovies Start!");

        List<MovieEntity> movieEntities;
        try {
            // MovieRepository를 통해 모든 MovieEntity 데이터를 조회합니다.
            movieEntities = movieRepository.findAll();
            log.debug("조회된 MovieEntity 개수: {}", movieEntities.size()); // 디버그 레벨에서 조회된 개수 로깅
        } catch (Exception e) {
            // 데이터 조회 중 예외 발생 시 에러 로그를 기록하고 빈 리스트 반환
            log.error("getAllMovies 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            log.info(this.getClass().getName() + ".getAllMovies End! (Error occurred, returning empty list)");
            return Collections.emptyList(); // 오류 발생 시 빈 리스트 반환
        }

        // MovieEntity 리스트를 MovieDTO 리스트로 변환합니다.
        // Stream API를 사용하여 각 Entity를 DTO로 매핑합니다.
        List<MovieDTO> movieDTOs = movieEntities.stream()
                .map(entity -> {
                    // 각 MovieEntity를 MovieDTO 생성자를 통해 변환합니다.
                    // 변환 과정에서 null 값 등 예외 처리 로직을 추가할 수 있습니다.
                    MovieDTO dto = new MovieDTO(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getLocation(),
                            entity.getPosterUrl(),
                            entity.getAddr(),
                            entity.getX(),
                            entity.getY()
                    );
                    log.trace("Entity 변환: {} -> {}", entity.getId(), dto.getTitle()); // 상세 변환 로깅
                    return dto;
                })
                .collect(Collectors.toList());

        // 로그 종료
        log.info(this.getClass().getName() + ".getAllMovies End! (Returned {} movies)", movieDTOs.size());
        return movieDTOs;
    }

    /**
     * 특정 영화의 상세 정보를 조회합니다.
     * 주어진 movieId를 사용하여 데이터베이스에서 영화 정보를 찾아 DTO로 변환하여 반환합니다.
     *
     * @param movieId 조회할 영화의 고유 ID (String)
     * @return 조회된 영화의 {@link MovieDTO} 객체. 해당 ID의 영화가 없을 경우 null을 반환합니다.
     */
    @Override
    public MovieDTO getMovieDetail(String movieId) {
        // 로그 시작: 조회할 영화 ID 포함
        log.info(this.getClass().getName() + ".getMovieDetail Start! movieId : {}", movieId);

        // MovieRepository를 통해 ID로 MovieEntity를 조회합니다. Optional로 반환됩니다.
        Optional<MovieEntity> movieOptional;
        try {
            movieOptional = movieRepository.findById(movieId);
        } catch (Exception e) {
            // 데이터 조회 중 예외 발생 시 에러 로그 기록
            log.error("getMovieDetail 데이터 조회 중 오류 발생 (ID: {}): {}", movieId, e.getMessage(), e);
            log.info(this.getClass().getName() + ".getMovieDetail End! (Error occurred)");
            return null; // 오류 발생 시 null 반환
        }

        MovieDTO dto = null; // 반환될 DTO 초기화

        // Optional 객체가 값을 포함하고 있는지 확인합니다.
        if (movieOptional.isPresent()) {
            // 값이 존재하면 Entity를 가져와 DTO로 변환합니다.
            MovieEntity entity = movieOptional.get();
            dto = new MovieDTO(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getLocation(),
                    entity.getPosterUrl(),
                    entity.getAddr(),
                    entity.getX(),
                    entity.getY()
            );
            log.debug("영화 상세 정보 조회 성공: {}", dto.getTitle()); // 디버그 레벨에서 성공 로깅
        } else {
            // 해당 ID의 영화가 없을 경우 경고 로그를 기록합니다.
            log.warn("ID '{}'에 해당하는 영화를 찾을 수 없습니다.", movieId);
        }

        // 로그 종료
        log.info(this.getClass().getName() + ".getMovieDetail End! (Movie found: {})", (dto != null));
        return dto; // 조회된 DTO 또는 null 반환
    }
}