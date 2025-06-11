package kopo.jeonnam.repository.mongo.favorite;

import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends MongoRepository<FavoriteEntity, String> {
    List<FavoriteEntity> findByUserId(String userId);
    List<FavoriteEntity> findByUserIdAndType(String userId, String type);
    boolean existsByUserIdAndTypeAndNameAndLocation(String userId, String type, String name, String location);
    Optional<FavoriteEntity> findByUserIdAndTypeAndNameAndLocation(String userId, String type, String name, String location);
    Optional<FavoriteEntity> findByTypeAndNameAndLocationAndUserId(String type, String name, String location, String userId);

    @Query("{ 'userId': ?0, 'y': { $gte: ?1, $lte: ?2 }, 'x': { $gte: ?3, $lte: ?4 } }")
    List<FavoriteEntity> findNearby(String userId, double latMin, double latMax, double lngMin, double lngMax);
}