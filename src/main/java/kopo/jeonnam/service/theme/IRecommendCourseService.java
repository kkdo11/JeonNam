package kopo.jeonnam.service.theme;

import kopo.jeonnam.repository.entity.theme.RecommendCourseEntity;
import kopo.jeonnam.repository.entity.theme.RecommendCourseImageEntity;

import java.util.List;
import java.util.Optional;

public interface IRecommendCourseService {
    /**
     * 남도 추천 여행 코스 데이터를 외부 API에서 받아와 MongoDB에 저장
     * @return 저장된 데이터 개수
     */
    int fetchAndSaveRecommendCourses();

    /**
     * 특정 추천 코스 (RecommendCourseEntity)의 상세 정보를 조회합니다.
     * @param courseId 조회할 추천 코스의 _id (courseKey에 해당)
     * @return 조회된 RecommendCourseEntity (Optional로 감싸져 있음)
     */
    Optional<RecommendCourseEntity> getRecommendCourseDetail(String courseId);

    /**
     * 쉼표로 구분된 courseInfoId 문자열을 받아 해당 ID들에 연결된 이미지 데이터를 조회합니다
     * @param courseInfoIds 쉼표로 구분된 courseInfoId 문자열
     * @return 조회된 RecommendCourseImageEntity 리스트
     */
    List<RecommendCourseImageEntity> getImagesByCourseInfoIds(String courseInfoIds);

    /**
     * 모든 추천 코스 엔티티를 조회합니다.
     * @return 모든 RecommendCourseEntity 리스트
     */
    List<RecommendCourseEntity> getAllRecommendCourses(); // 추가된 메서드
}