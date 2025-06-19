package kopo.jeonnam.dto.favorite;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourDTO {

    @JsonProperty("tName")
    private String tName;       // 검색 키워드 (입력용)

    private String name;        // 장소 이름
    private String address;     // 전체 주소
    private String phone;       // 전화번호
    private String url;         // 홈페이지 URL
    private double x;           // 경도 (longitude)
    private double y;           // 위도 (latitude)
}