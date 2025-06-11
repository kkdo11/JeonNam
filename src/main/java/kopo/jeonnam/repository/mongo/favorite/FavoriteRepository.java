package kopo.jeonnam.repository.mongo.favorite;

import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FavoriteRepository extends MongoRepository<FavoriteEntity, String> {
    Optional<FavoriteEntity> findByUserIdAndNameAndType(String userId, String name, String type);
}