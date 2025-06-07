package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.repository.entity.RecommendCoursePlanEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendCoursePlanRepository extends MongoRepository<RecommendCoursePlanEntity, String> {
}

