package kopo.jeonnam.service.theme;

import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;

import java.util.List;
import java.util.Optional;

public interface IRecommendCoursePlanService {
    /**
     * 추천 코스별 상세 일정(Plan) 데이터를 외부 API에서 받아와 MongoDB에 저장합니다
     *
     * @param courseKey 추천 코스의 고유 키 값
     * @return 저장된 데이터 개수
     */
    int fetchAndSaveRecommendCoursePlans(String courseKey);

    /**
     * 모든 장소별 추천 계획 데이터를 이미지 포함해서 조회
     * @return 위치 정보 및 이미지 포함된 RecommendCoursePlanDTO 리스트
     */
    List<RecommendCoursePlanDTO> getAllPlansWithImages();

    /**
     * 특정 코스 planInfoId에 해당하는 장소 하나만 조회
     */
    Optional<RecommendCoursePlanDTO> getPlanWithImagesById(String planInfoId);

    boolean existsAnyByCourseKey(String courseKey);
}
