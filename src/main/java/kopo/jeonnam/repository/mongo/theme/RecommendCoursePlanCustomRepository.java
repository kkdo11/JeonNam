package kopo.jeonnam.repository.mongo.theme;

import kopo.jeonnam.dto.theme.RecommendCoursePlanDTO;

import java.util.List;

public interface RecommendCoursePlanCustomRepository {
    // AFTER ✅
    List<RecommendCoursePlanDTO> findNearbyPlansWithStringCoordinates(
            double latMin, double latMax, double lngMin, double lngMax
    );
}