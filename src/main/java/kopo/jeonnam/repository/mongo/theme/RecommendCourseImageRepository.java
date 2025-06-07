package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.repository.entity.theme.RecommendCourseImageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendCourseImageRepository extends MongoRepository<RecommendCourseImageEntity, String> {
//
}