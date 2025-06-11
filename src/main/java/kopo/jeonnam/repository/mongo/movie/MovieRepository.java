package kopo.jeonnam.repository.mongo.movie;

import kopo.jeonnam.repository.entity.movie.MovieEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends MongoRepository<MovieEntity, String> {
    List<MovieEntity> findAll();

    // ID로 특정 영화를 조회 (Optional을 사용하여 null 처리 용이)
    Optional<MovieEntity> findById(String id);
}
