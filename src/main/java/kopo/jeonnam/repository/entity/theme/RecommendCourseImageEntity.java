package kopo.jeonnam.repository.entity.theme;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "recommend_course_image") // MongoDB 컬렉션 이름 지정함
public class RecommendCourseImageEntity {

    @Id // 이 필드가 MongoDB의 _id가 됩니다. 이미지 파일 URL이 고유하다면 ID로 사용 가능
    private String _id; // 이미지 파일 URL을 ID로 사용

    @Field("course_info_id")
    private String courseInfoId; // 이 이미지가 속한 코스의 courseInfoId

    @Field("course_file_url")
    private String courseFileUrl; // 실제 이미지 URL

    @Field("course_file_nm")
    private String courseFileNm; // 이미지 파일 이름

    @Field("course_file_path")
    private String courseFilePath; // 이미지 파일 경로 (API 응답에 따라)

    @Builder
    public RecommendCourseImageEntity(String _id, String courseInfoId, String courseFileUrl,
                                      String courseFileNm, String courseFilePath) {
        this._id = _id; // ID로 사용될 이미지 URL
        this.courseInfoId = courseInfoId;
        this.courseFileUrl = courseFileUrl;
        this.courseFileNm = courseFileNm;
        this.courseFilePath = courseFilePath;
    }
}