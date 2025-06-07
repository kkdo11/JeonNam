package kopo.jeonnam.dto.csv;

import lombok.Builder;

/**
 * 🎞️ MediaSpotDTO - 프론트와 통신할 때 사용하는 데이터 전송 객체 (record + builder)
 */
@Builder
public record MediaSpotDTO(
        String spotId,         // MongoDB ObjectId (16진수 문자열)
        String spotNm,         // 촬영지 이름 (POI_NM에서 '촬영지' 제거한 값)
        String spotArea,       // 시군구명 (SIGNGU_NM)
        String spotLegalDong,  // 법정동명
        String spotRi,         // 리명
        String spotBunji,      // 번지번호
        String spotRoadAddr,   // 도로명 주소
        String spotLon,        // 경도 (LC_LO)
        String spotLat         // 위도 (LC_LA)
) {}
