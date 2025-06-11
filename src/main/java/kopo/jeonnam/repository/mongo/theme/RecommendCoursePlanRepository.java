package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;
import kopo.jeonnam.repository.entity.theme.RecommendCoursePlanEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 추천 여행 코스 상세 일정(Plan) 정보를 MongoDB에서 관리하는 Repository 인터페이스입니다.
 *
 * 협업 및 디버깅을 위해, 커스텀 쿼리 메서드 작성 시 JavaDoc 주석을 남기고,
 * 복잡한 쿼리 동작이 필요할 경우 구현체에서 로그를 남기는 것을 권장합니다.
 */
// 기존 + 커스텀 인터페이스 확장 추가
@Repository
public interface RecommendCoursePlanRepository extends
        MongoRepository<RecommendCoursePlanEntity, String>,
        RecommendCoursePlanCustomRepository {

    List<RecommendCoursePlanEntity> findAll();

    long countByPlanCourseId(String planCourseId);

    // 이건 제거하거나 주석처리해도 무방합니다. (String 타입 좌표 비교는 신뢰도 낮음)
    // List<RecommendCoursePlanDTO> findByPlanLatitudeBetweenAndPlanLongitudeBetween(...);
}

