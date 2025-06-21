package kopo.jeonnam.repository.mongo.favorite;

import kopo.jeonnam.repository.entity.favorite.FavoriteEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends MongoRepository<FavoriteEntity, String> {
    List<FavoriteEntity> findByUserId(String userId);
    List<FavoriteEntity> findByUserIdAndType(String userId, String type);
    // ✅ 수정된 부분: location → addr
    boolean existsByUserIdAndTypeAndNameAndAddr(String userId, String type, String name, String addr);
    Optional<FavoriteEntity> findByUserIdAndTypeAndNameAndAddr(String userId, String type, String name, String addr);
    Optional<FavoriteEntity> findByTypeAndNameAndAddrAndUserId(String type, String name, String addr, String userId);

    @Query("{ 'userId': ?0, 'y': { $gte: ?1, $lte: ?2 }, 'x': { $gte: ?3, $lte: ?4 } }")
    List<FavoriteEntity> findNearby(String userId, double latMin, double latMax, double lngMin, double lngMax);
}