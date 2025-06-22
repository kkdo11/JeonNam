package kopo.jeonnam.service.impl.favorite;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.dto.favorite.TourDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import kopo.jeonnam.repository.mongo.favorite.FavoriteRepository;
import kopo.jeonnam.service.favorite.IFavoriteService;
import kopo.jeonnam.util.NetworkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ObjectMapper objectMapper;

    @Value("${KakaoRestApiKey}")
    private String kakaoRestApiKey;

    private String extractSiGunGu(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) return "";
        String[] parts = fullAddress.trim().split("\\s+");
        for (String part : parts) {
            if (part.endsWith("시") || part.endsWith("군") || part.endsWith("구")) {
                return part;
            }
        }
        return "";
    }

    public static FavoriteEntity fromDTO(FavoriteDTO dto) {
        return FavoriteEntity.builder()
                .userId(dto.userId())
                .type(dto.type())
                .name(dto.name())
                .location(dto.location())
                .addr(dto.addr())
                .posterUrl(dto.posterUrl())
                .x(dto.x())
                .y(dto.y())
                .planPhone(dto.planPhone())
                .planHomepage(dto.planHomepage())
                .planParking(dto.planParking())
                .planContents(dto.planContents())
                .build();
    }

    public FavoriteEntity saveFavorite(FavoriteDTO dto) {
        // 중복 체크
        boolean exists = favoriteRepository.existsByUserIdAndTypeAndNameAndAddr(
                dto.userId(), dto.type(), dto.name(), dto.addr());

        if (exists) {
            // 중복 시 기존 엔티티 반환 (또는 null 반환 가능)
            return favoriteRepository.findByUserIdAndTypeAndNameAndAddr(
                    dto.userId(), dto.type(), dto.name(), dto.location()
            ).orElse(null);
        }

        // Parse 시/군/구 from full address
        // ✅ 미디어가 아닐 때만 addr에서 시/군/구 추출
        String simplifiedLocation = "media".equals(dto.type())
                ? dto.location()  // 그대로 사용
                : extractSiGunGu(dto.addr());  // addr에서 시/군/구 추출

// Create a new DTO with simplified location
        FavoriteDTO updatedDto = new FavoriteDTO(
                dto.userId(),
                dto.type(),
                dto.name(),
                simplifiedLocation,         // ✅ parsed 시/군/구
                dto.addr(),
                dto.posterUrl(),
                dto.x(),
                dto.y(),
                dto.planPhone(),
                dto.planHomepage(),
                dto.planParking(),
                dto.planContents()
        );

        FavoriteEntity entity = fromDTO(updatedDto);

        return favoriteRepository.save(entity);
    }

    @Override
    public FavoriteDTO toDTO(FavoriteEntity entity) {
        return new FavoriteDTO(
                entity.getUserId(),
                entity.getType(),
                entity.getName(),
                entity.getLocation(),
                entity.getAddr(),
                entity.getPosterUrl(),
                entity.getX(),
                entity.getY(),
                entity.getPlanPhone(),
                entity.getPlanHomepage(),
                entity.getPlanParking(),
                entity.getPlanContents()
        );
    }

    @Override
    public List<FavoriteDTO> getFavoritesByUserId(String userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<FavoriteDTO> findNearbyFavorites(String email, double latMin, double latMax, double lngMin, double lngMax) {
        List<FavoriteEntity> entities = favoriteRepository.findNearby(email, latMin, latMax, lngMin, lngMax);
        return entities.stream()
                .map(this::toDTO) // 또는 직접 DTO 생성
                .collect(Collectors.toList());
    }

    // ✅ Kakao 로컬 API를 이용한 관광지 검색
    @Override
    public List<TourDTO> searchTourByKeyword(String keyword) {
        log.info("🔍 관광지 검색 요청: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("⚠️ 검색 키워드가 null 또는 빈 문자열입니다.");
            return Collections.emptyList(); // 또는 예외 throw
        }

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + encodedKeyword;

            String response = NetworkUtil.get(url, Map.of(
                    "Authorization", "KakaoAK " + kakaoRestApiKey
            ));

            JsonNode root = objectMapper.readTree(response);
            JsonNode documents = root.get("documents");

            List<TourDTO> results = new ArrayList<>();

            for (JsonNode doc : documents) {
                TourDTO dto = new TourDTO();
                dto.setTName(keyword);
                dto.setName(doc.path("place_name").asText(""));
                dto.setAddress(doc.path("road_address_name").asText(""));
                dto.setPhone(doc.path("phone").asText(""));
                dto.setUrl(doc.path("place_url").asText(""));
                dto.setX(doc.path("x").asDouble(0));
                dto.setY(doc.path("y").asDouble(0));
                results.add(dto);
            }

            log.info("✅ 검색 결과 수: {}", results.size());
            return results;

        } catch (Exception e) {
            log.error("❌ Kakao 검색 API 오류: {}", e.getMessage(), e);
            throw new RuntimeException("Kakao 검색 실패", e);
        }
    }



    public boolean existsByUserIdAndTypeAndNameAndAddr(String userId, String type, String name, String addr) {
        return favoriteRepository.existsByUserIdAndTypeAndNameAndAddr(userId, type, name, addr);
    }


    public boolean deleteByTypeAndNameAndAddr(String type, String name, String addr, String userId) {

        log.info("🔍 Trying to delete: type={}, name={}, addr={}, userId={}", type, name, addr, userId);
        favoriteRepository.findByUserId(userId).forEach(f -> {
            log.info("📦 Stored entity: name={}, addr={}, type={}", f.getName(), f.getAddr(), f.getType());
        });
        Optional<FavoriteEntity> optionalFavorite = favoriteRepository
                .findByTypeAndNameAndAddrAndUserId(type, name, addr, userId);

        if (optionalFavorite.isPresent()) {
            favoriteRepository.delete(optionalFavorite.get());
            return true;
        }
        return false;
    }







}