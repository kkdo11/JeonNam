package kopo.jeonnam.service.theme;

import kopo.jeonnam.repository.entity.theme.RecommendCourseImageEntity;
import java.util.List;

public interface IRecommendCourseImageService {

    /**
     * 특정 courseInfoIds에 해당하는 이미지 데이터를 외부 API에서 받아와 MongoDB에 저장핰
     *
     * @param courseInfoIds 쉼표로 구분된 courseInfoId 문자열 (예: "0000013366,0000012108")
     * @return 저장된 데이터 개수
     */
    int fetchAndSaveRecommendCourseImages(String courseInfoIds);

    /**
     * 모든 이미지 엔티티를 조회합니다. (필요 시 추가)
     * @return 모든 RecommendCourseImageEntity 리스트
     */
    List<RecommendCourseImageEntity> getAllRecommendCourseImages();
}