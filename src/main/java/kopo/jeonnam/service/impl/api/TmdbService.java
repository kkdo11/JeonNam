package kopo.jeonnam.service.impl.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.jeonnam.service.api.ITmdbService;
import kopo.jeonnam.util.NetworkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbService implements ITmdbService {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private static final String TMDB_SEARCH_MOVIE_URL = "https://api.themoviedb.org/3/search/movie";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<String> getPosterUrlByTitle(String title) {
        try {
            // 영화 → TV 순으로 시도
            Optional<String> moviePoster = searchTmdbPoster(title, "movie");
            if (moviePoster.isPresent()) return moviePoster;

            Optional<String> tvPoster = searchTmdbPoster(title, "tv");
            if (tvPoster.isPresent()) return tvPoster;

        } catch (Exception e) {
            log.warn("TMDB API 호출 실패: {}", e.getMessage());
        }

        return Optional.empty();
    }

    // 내부 메서드: type은 "movie" 또는 "tv"
    private Optional<String> searchTmdbPoster(String title, String type) {
        try {
            String query = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String apiUrl = "https://api.themoviedb.org/3/search/" + type
                    + "?api_key=" + tmdbApiKey
                    + "&query=" + query
                    + "&language=ko-KR"; // 한글 결과 우선

            String response = NetworkUtil.get(apiUrl);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            if (results.isArray() && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                String posterPath = firstResult.path("poster_path").asText(null);

                if (posterPath != null && !posterPath.isEmpty()) {
                    return Optional.of("https://image.tmdb.org/t/p/w500" + posterPath);
                }
            }
        } catch (Exception e) {
            log.warn("TMDB {} 검색 중 오류: {}", type.toUpperCase(), e.getMessage());
        }
        return Optional.empty();
    }
}