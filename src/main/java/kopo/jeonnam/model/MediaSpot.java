package kopo.jeonnam.model;

import kopo.jeonnam.dto.csv.MediaSpotDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 🎬 MediaSpot 모델 - MongoDB 'media_spot' 컬렉션과 매핑
 * @Value + @Builder 조합으로 불변 객체 & 빌더 패턴 적용
 * 전라남도 드라마/영화 촬영지 데이터를 담는 모델이에요~
 */
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "media_spot")
public class MediaSpot {

    @Id
    @Builder.Default
    private final ObjectId id = new ObjectId();  // 고유 ID, 기본 생성

    private final String spotNm;         // 촬영지 이름 (POI_NM)
    private final String spotArea;       // 시군구명 (SIGNGU_NM)
    private final String spotLegalDong;  // 법정동명
    private final String spotRi;         // 리명
    private final String spotBunji;      // 번지번호
    private final String spotRoadAddr;   // 도로명주소명
    private final String spotLon;        // 경도 (LC_LO)
    private final String spotLat;        // 위도 (LC_LA)

    /**
     * 🛠 DTO → Entity 변환 메서드
     */
    public static MediaSpot fromDTO(MediaSpotDTO dto) {
        return MediaSpot.builder()
                .id(dto.spotId() != null ? new ObjectId(dto.spotId()) : new ObjectId())
                .spotNm(dto.spotNm())
                .spotArea(dto.spotArea())
                .spotLegalDong(dto.spotLegalDong())
                .spotRi(dto.spotRi())
                .spotBunji(dto.spotBunji())
                .spotRoadAddr(dto.spotRoadAddr())
                .spotLon(dto.spotLon())
                .spotLat(dto.spotLat())
                .build();
    }

    /**
     * 🎯 Entity → DTO 변환 메서드
     */
    public MediaSpotDTO toDTO() {
        return MediaSpotDTO.builder()
                .spotId(this.id.toHexString())
                .spotNm(this.spotNm)
                .spotArea(this.spotArea)
                .spotLegalDong(this.spotLegalDong)
                .spotRi(this.spotRi)
                .spotBunji(this.spotBunji)
                .spotRoadAddr(this.spotRoadAddr)
                .spotLon(this.spotLon)
                .spotLat(this.spotLat)
                .build();
    }
}
