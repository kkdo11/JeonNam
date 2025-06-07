package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.repository.entity.RecommendCourseEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendCourseRepository extends MongoRepository<RecommendCourseEntity, String> {
    // 필요시 커스텀 쿼리 메서드 추가 가능
}

