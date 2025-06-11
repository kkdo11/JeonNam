package kopo.jeonnam.service.impl.favorite;

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

    @Override
    public String saveFavorite(FavoriteDTO dto) {
        boolean exists = favoriteRepository
                .findByUserIdAndNameAndType(dto.userId(), dto.name(), dto.type())
                .isPresent();

        if (exists) {
            return "Already exists";
        }

        FavoriteEntity entity = FavoriteEntity.builder()
                .userId(dto.userId())
                .type(dto.type())
                .name(dto.name())
                .location(dto.location())
                .x(dto.x())
                .y(dto.y())
                .planPhone(dto.planPhone())
                .planHomepage(dto.planHomepage())
                .planParking(dto.planParking())
                .planContents(dto.planContents())
                .build();

        favoriteRepository.save(entity);
        return "Saved";
    }
}