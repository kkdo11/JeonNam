package kopo.jeonnam.service.favorite;

import kopo.jeonnam.dto.favorite.FavoriteDTO;
import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;

public interface IFavoriteService {
    FavoriteEntity saveFavorite(FavoriteDTO dto);
}
