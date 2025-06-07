package kopo.jeonnam.repository.mongo.csv;

import kopo.jeonnam.model.MediaSpot;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 🗃 MediaSpot MongoDB CRUD 인터페이스
 */
@Repository
public interface MediaSpotRepository extends MongoRepository<MediaSpot, ObjectId> {

    // 법정동명 기준 전라남도 촬영지 검색
    List<MediaSpot> findBySpotLegalDongContaining(String spotLegalDong);

    // 촬영지 이름으로 검색 (예: 허준)
    List<MediaSpot> findBySpotNmContainingIgnoreCase(String spotNm);
}
