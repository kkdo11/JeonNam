package kopo.jeonnam.service.favorite;

import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.dto.favorite.TourDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;

import java.util.List;

public interface IFavoriteService {
    FavoriteEntity saveFavorite(FavoriteDTO dto);

    FavoriteDTO toDTO(FavoriteEntity entity);

    List<FavoriteDTO> getFavoritesByUserId(String userId);

    List<FavoriteDTO> findNearbyFavorites(String email, double latMin, double latMax, double lngMin, double lngMax);

    // ✅ 관광지 키워드 검색 (카카오 로컬 API 사용)
    List<TourDTO> searchTourByKeyword(String keyword);
}
