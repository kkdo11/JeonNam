package kopo.jeonnam.service.impl.movie;

import kopo.jeonnam.dto.movie.MovieDTO;
import kopo.jeonnam.dto.movie.MovieSearchRequest;
import kopo.jeonnam.repository.entity.movie.MovieEntity;
import kopo.jeonnam.repository.mongo.movie.MovieRepository;
import kopo.jeonnam.service.movie.IMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
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
    private final MongoTemplate mongoTemplate;

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
            movieEntities = movieRepository.findAll();
            log.debug("조회된 MovieEntity 개수: {}", movieEntities.size());
        } catch (Exception e) {
            log.error("getAllMovies 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            log.info(this.getClass().getName() + ".getAllMovies End! (Error occurred, returning empty list)");
            return Collections.emptyList();
        }

        List<MovieDTO> movieDTOs = movieEntities.stream()
                .map(entity -> {
                    MovieDTO dto = new MovieDTO(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getLocation(),
                            entity.getPosterUrl(),
                            entity.getAddr(),
                            entity.getX(),
                            entity.getY()
                    );
                    log.trace("Entity 변환: {} -> {}", entity.getId(), dto.getTitle());
                    return dto;
                })
                .collect(Collectors.toList());

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
        log.info(this.getClass().getName() + ".getMovieDetail Start! movieId : {}", movieId);

        Optional<MovieEntity> movieOptional;
        try {
            movieOptional = movieRepository.findById(movieId);
        } catch (Exception e) {
            log.error("getMovieDetail 데이터 조회 중 오류 발생 (ID: {}): {}", movieId, e.getMessage(), e);
            log.info(this.getClass().getName() + ".getMovieDetail End! (Error occurred)");
            return null;
        }

        MovieDTO dto = null;
        if (movieOptional.isPresent()) {
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
            log.debug("영화 상세 정보 조회 성공: {}", dto.getTitle());
        } else {
            log.warn("ID '{}'에 해당하는 영화를 찾을 수 없습니다.", movieId);
        }

        log.info(this.getClass().getName() + ".getMovieDetail End! (Movie found: {})", (dto != null));
        return dto;
    }

    /**
     * 정규표현식 메타문자를 이스케이프하는 유틸리티 메서드.
     * 사용자가 입력한 문자열에 특수문자가 포함되어 있을 경우, 정규표현식에서 리터럴로 인식되도록 처리합니다.
     */
    private String escapeRegex(String text) {
        return Pattern.quote(text);
    }

    /**
     * 검색어를 정규화(Normalize)하여 검색에 방해가 되는 특수문자를 제거하고 공백을 정리하는 유틸리티 메서드.
     * 예를 들어 "(구) 장흥교도소" -> "구 장흥교도소" 또는 "장흥교도소"로 변환할 수 있습니다.
     */
    private String normalizeSearchText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replaceAll("[()\\-/_.,]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }


    @Override
    public Page<MovieDTO> searchMovies(MovieSearchRequest searchRequest, Pageable pageable) {
        log.info(this.getClass().getName() + ".searchMovies Start! searchRequest: {}, pageable: {}", searchRequest, pageable);

        Page<MovieDTO> movieDTOSPage;

        try {
            // 1. 공통 검색 Criteria 구성
            List<Criteria> searchCriteriaList = new ArrayList<>(); // <-- 여기서 선언됨

            // 검색어를 정규화하여 사용
            String normalizedKeyword = normalizeSearchText(searchRequest.getKeyword());
            String normalizedTitle = normalizeSearchText(searchRequest.getTitle());
            String normalizedLocation = normalizeSearchText(searchRequest.getLocation());
            String normalizedAddr = normalizeSearchText(searchRequest.getAddr());

            // 1. 통합 검색 키워드 (keyword) 처리
            if (StringUtils.hasText(normalizedKeyword)) {
                String keywordRegex = ".*" + escapeRegex(normalizedKeyword) + ".*";
                // ⭐️ searchCriteriaList 사용
                searchCriteriaList.add(new Criteria().orOperator(
                        Criteria.where("title").regex(keywordRegex, "i"),
                        Criteria.where("location").regex(keywordRegex, "i"),
                        Criteria.where("Addr").regex(keywordRegex, "i")
                ));
            } else {
                // keyword가 없고 개별 필드 검색 조건이 있는 경우 (AND 조건으로 결합)
                if (StringUtils.hasText(normalizedTitle)) {
                    // ⭐️ searchCriteriaList 사용
                    searchCriteriaList.add(Criteria.where("title").regex(".*" + escapeRegex(normalizedTitle) + ".*", "i"));
                }
                if (StringUtils.hasText(normalizedLocation)) {
                    // ⭐️ searchCriteriaList 사용
                    searchCriteriaList.add(Criteria.where("location").regex(".*" + escapeRegex(normalizedLocation) + ".*", "i"));
                }
                if (StringUtils.hasText(normalizedAddr)) {
                    // ⭐️ searchCriteriaList 사용
                    searchCriteriaList.add(Criteria.where("Addr").regex(".*" + escapeRegex(normalizedAddr) + ".*", "i"));
                }
            }

            // 2. 검색 조건이 있다면 Criteria.andOperator로 최종 CriteriaDefinition 생성
            Criteria finalSearchCriteria = null;
            if (!searchCriteriaList.isEmpty()) {
                finalSearchCriteria = new Criteria().andOperator(searchCriteriaList.toArray(new Criteria[0]));
            }

            // 3. 전체 개수를 세기 위한 Query 객체 생성 및 Criteria 적용
            Query totalCountQuery;
            if (finalSearchCriteria != null) {
                totalCountQuery = Query.query(finalSearchCriteria);
            } else {
                totalCountQuery = new Query();
            }
            long total = mongoTemplate.count(totalCountQuery, MovieEntity.class);
            log.debug("서비스: 필터링 조건에 맞는 총 영화 개수 = {}", total);

            // 4. 실제 페이지 데이터를 가져오기 위한 Query 객체 생성 및 Criteria 적용
            Query pagedDataQuery;
            if (finalSearchCriteria != null) {
                pagedDataQuery = Query.query(finalSearchCriteria);
            } else {
                pagedDataQuery = new Query();
            }

            // 정렬 조건 추가
            Sort sort = Sort.by(Sort.Direction.DESC, "_id");
            if (StringUtils.hasText(searchRequest.getSortBy())) {
                Sort.Direction direction = Sort.Direction.ASC;
                if (StringUtils.hasText(searchRequest.getSortDirection()) &&
                        searchRequest.getSortDirection().equalsIgnoreCase("desc")) {
                    direction = Sort.Direction.DESC;
                }
                sort = Sort.by(direction, searchRequest.getSortBy());
            }
            pagedDataQuery.with(sort);

            // 페이징 적용
            pagedDataQuery.with(pageable);
            log.debug("서비스: 페이징 적용 (page={}, size={})", pageable.getPageNumber(), pageable.getPageSize());

            // 5. 실제 페이지 데이터 조회
            List<MovieEntity> movies = mongoTemplate.find(pagedDataQuery, MovieEntity.class);
            log.debug("서비스: 현재 페이지 조회된 데이터 개수 = {}", movies.size());

            // 6. PageImpl 객체 생성
            movieDTOSPage = new PageImpl<>(
                    movies.stream()
                            .map(entity -> new MovieDTO(
                                    entity.getId(),
                                    entity.getTitle(),
                                    entity.getLocation(),
                                    entity.getPosterUrl(),
                                    entity.getAddr(),
                                    entity.getX(),
                                    entity.getY()
                            ))
                            .collect(Collectors.toList()),
                    pageable,
                    total
            );

        } catch (Exception e) {
            log.error("searchMovies 데이터 조회 중 오류 발생: searchRequest={}, error={}", searchRequest, e.getMessage(), e);
            log.info(this.getClass().getName() + ".searchMovies End! (Error occurred, returning empty page)");
            return Page.empty(pageable);
        }

        log.info(this.getClass().getName() + ".searchMovies End! TotalElements={}, TotalPages={}, CurrentPageSize={}",
                movieDTOSPage.getTotalElements(), movieDTOSPage.getTotalPages(), movieDTOSPage.getContent().size());
        return movieDTOSPage;
    }
}