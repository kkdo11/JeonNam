package kopo.jeonnam.service.impl.favorite;

import java.util.Optional;
import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import kopo.jeonnam.repository.mongo.favorite.FavoriteRepository;
import kopo.jeonnam.service.favorite.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {

    private final FavoriteRepository favoriteRepository;

    public static FavoriteEntity fromDTO(FavoriteDTO dto) {
        return FavoriteEntity.builder()
                .userId(dto.userId())
                .type(dto.type())
                .name(dto.name())
                .location(dto.location())
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
        boolean exists = favoriteRepository.existsByUserIdAndTypeAndNameAndLocation(
                dto.userId(), dto.type(), dto.name(), dto.location());

        if (exists) {
            // 중복 시 기존 엔티티 반환 (또는 null 반환 가능)
            return favoriteRepository.findByUserIdAndTypeAndNameAndLocation(
                    dto.userId(), dto.type(), dto.name(), dto.location()
            ).orElse(null);
        }

        FavoriteEntity entity = fromDTO(dto);
        return favoriteRepository.save(entity);
    }

    public boolean existsByUserIdAndTypeAndNameAndLocation(String userId, String type, String name, String location) {
        return favoriteRepository.existsByUserIdAndTypeAndNameAndLocation(userId, type, name, location);
    }


    public boolean deleteByTypeAndNameAndLocation(String type, String name, String location, String userId) {
        Optional<FavoriteEntity> optionalFavorite = favoriteRepository
                .findByTypeAndNameAndLocationAndUserId(type, name, location, userId);

        if (optionalFavorite.isPresent()) {
            favoriteRepository.delete(optionalFavorite.get());
            return true;
        }
        return false;
    }





}