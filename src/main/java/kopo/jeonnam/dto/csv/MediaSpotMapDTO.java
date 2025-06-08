package kopo.jeonnam.dto.csv;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaSpotMapDTO {
    private String spotNm;     // 촬영지 이름
    private String address;    // 결합된 주소
    private double lat;        // 위도
    private double lon;        // 경도
    private String posterUrl;  // 포스터 이미지 URL
}
