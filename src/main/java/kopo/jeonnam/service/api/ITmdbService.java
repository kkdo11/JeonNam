package kopo.jeonnam.service.api;

import java.util.Optional;

public interface ITmdbService {

    /**
     * 영화/드라마 제목으로 TMDB에서 포스터 URL 조회 (없으면 Optional.empty)
     */
    Optional<String> getPosterUrlByTitle(String title);
}
