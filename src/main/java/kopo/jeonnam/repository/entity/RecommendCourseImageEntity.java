
package kopo.jeonnam.repository.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "recommendCourseImage") // MongoDB 컬렉션 이름
public class RecommendCourseImageEntity {

    @Id // MongoDB의 _id 필드에 매핑
    // courseFileUrl은 이미지 URL이므로 고유성을 가질 가능성이 높고, 이미지 식별에 유용합니다.
    // 만약 URL이 변경될 가능성이 있다면, courseFileNm + courseInfoId 조합을 고려해야 합니다.
    @Field("courseFileUrl")
    private String courseFileUrl; // 이미지 URL을 ID로 사용

    @Field("courseInfoId") // 어떤 코스에 속하는 이미지인지 연결 (API 응답에는 없지만, 호출 시 파라미터로 주입해야 함)
    private String courseInfoId;

    @Field("courseFileNm") // 이미지 파일명
    private String courseFileNm;

    // API 응답에는 없지만, 나중에 추가 정보가 필요할 경우를 대비하거나
    // 기본값을 제공하기 위해 필드 유지 가능. 현재 XML에서는 이 정보 없음.
    // private String imageCaption;
    // private int imageWidth;
    // private int imageHeight;
}