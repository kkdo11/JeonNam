package kopo.jeonnam.service.csv;

import kopo.jeonnam.dto.csv.MediaSpotMapDTO;
import kopo.jeonnam.model.MediaSpot;

import java.io.InputStream;
import java.util.List;

/**
 * 🎬 MediaSpot 서비스 인터페이스 - 비즈니스 로직 정의
 */
public interface IMediaSpotService {

    /**
     * CSV InputStream 받아 파싱 후 전라남도 데이터만 DB 저장
     */
    void loadMediaSpotsFromCsv(InputStream csvInputStream) throws Exception;

    /**
     * 전라남도 모든 촬영지 조회
     */
    List<MediaSpot> getAllJeonnamSpots();

    /**
     * 촬영지 이름(spotNm)으로 검색
     */
    List<MediaSpot> searchBySpotNm(String keyword);

    boolean existsAny();

    /**
     * 지도 표시에 사용할 DTO 리스트 반환
     */
    List<MediaSpotMapDTO> getAllMapReadySpots();
}
