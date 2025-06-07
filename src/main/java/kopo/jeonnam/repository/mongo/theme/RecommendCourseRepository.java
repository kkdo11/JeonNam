package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 추천 여행 코스 정보를 MongoDB에서 관리하는 Repository 인터페이스입니다.
 * 기본 CRUD 외에 필요시 커스텀 쿼리 메서드를 추가할 수 있습니다.
 *
 * 협업 및 디버깅을 위해, 커스텀 메서드 작성 시 JavaDoc 주석을 남기고,
 * 쿼리 동작이 복잡할 경우 구현체에서 로그를 남기는 것을 권장합니다.
 */
@Repository
public interface RecommendCourseRepository extends MongoRepository<RecommendCourseEntity, String> {
    // 필요시 커스텀 쿼리 메서드 추가 가능
}
