package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.repository.entity.RecommendCourseImageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendCourseImageRepository extends MongoRepository<RecommendCourseImageEntity, String> {
    // courseInfoId 리스트에 해당하는 모든 이미지 엔티티를 찾습니다.
    List<RecommendCourseImageEntity> findByCourseInfoIdIn(List<String> courseInfoIds);
}