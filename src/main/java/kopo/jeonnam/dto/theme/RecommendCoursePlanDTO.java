package kopo.jeonnam.dto.theme;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RecommendCoursePlanDTO {
    private String planInfoId;
    private String planName;
    private String planArea;
    private String planAddr;
    private String planPhone;
    private String planHomepage;
    private String planParking;
    private String planContents;
    private double planLatitude;
    private double planLongitude;
    private List<String> imageUrls;
}
