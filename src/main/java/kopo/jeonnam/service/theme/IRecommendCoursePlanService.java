package kopo.jeonnam.service.theme;

public interface IRecommendCoursePlanService {
    /**
     * 추천 코스별 상세 일정(Plan) 데이터를 외부 API에서 받아와 MongoDB에 저장합니다
     *
     * @param courseKey 추천 코스의 고유 키 값
     * @return 저장된 데이터 개수
     */
    int fetchAndSaveRecommendCoursePlans(String courseKey);
}
