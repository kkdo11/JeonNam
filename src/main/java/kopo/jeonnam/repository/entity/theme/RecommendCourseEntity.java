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
@NoArgsConstructor // Lombok의 기본 생성자 자동 생성
@Document(collection = "recommend_course") // MongoDB 컬렉션 이름 지정
public class RecommendCourseEntity {

    @Id // 이 필드가 MongoDB의 _id가 됩니다.
    private String _id; // MongoDB의 ObjectId에 매핑될 필드

    @Field("plan_course_id")
    private String planCourseId;

    @Field("plan_name")
    private String planName;

    @Field("plan_area")
    private String planArea;

    @Field("plan_addr")
    private String planAddr;

    @Field("spot_nm")
    private String spotNm;

    @Field("spot_addr")
    private String spotAddr;

    @Field("course_key")
    private String courseKey; // API 응답의 courseKey (MongoDB _id와는 별개일 수 있음)

    @Field("course_info_ids")
    private String courseInfoIds;

    @Field("course_category")
    private String courseCategory;

    @Field("course_name")
    private String courseName;

    @Field("course_period")
    private String coursePeriod;

    @Field("course_person_type")
    private String coursePersonType;

    @Field("course_person_count")
    private String coursePersonCount;

    @Field("course_contents")
    private String courseContents;

    @Field("course_area")
    private String courseArea;

    @Builder // Lombok의 빌더 패턴 자동 생성
    public RecommendCourseEntity(String _id, String planCourseId, String planName, String planArea, String planAddr,
                                 String spotNm, String spotAddr, String courseKey, String courseInfoIds,
                                 String courseCategory, String courseName, String coursePeriod,
                                 String coursePersonType, String coursePersonCount, String courseContents,
                                 String courseArea) {
        this._id = _id; // 만약 _id를 직접 주입받아 사용한다면
        this.planCourseId = planCourseId;
        this.planName = planName;
        this.planArea = planArea;
        this.planAddr = planAddr;
        this.spotNm = spotNm;
        this.spotAddr = spotAddr;
        this.courseKey = courseKey;
        this.courseInfoIds = courseInfoIds;
        this.courseCategory = courseCategory;
        this.courseName = courseName;
        this.coursePeriod = coursePeriod;
        this.coursePersonType = coursePersonType;
        this.coursePersonCount = coursePersonCount;
        this.courseContents = courseContents;
        this.courseArea = courseArea;
    }

    // 만약 _id가 자동으로 생성되고, courseKey를 엔티티의 "주요 식별자"로 사용한다면
    // _id 대신 courseKey를 getter에서 반환하는 방식으로도 사용 가능하지만,
    // 일반적으로는 @Id 필드의 getter를 사용하는 것이 명확합니다.
    // 여기서는 course.get_id() 대신 course.getCourseKey()로 수정했습니다.
    // 만약 API에서 받아오는 courseKey가 항상 고유하고 ID로 사용하고 싶다면,
    // '_id' 필드를 제거하고 'courseKey' 필드에 @Id를 붙이는 것이 더 효율적입니다.
    // 지금은 _id를 유지하고 courseKey를 사용하도록 코드를 수정했습니다.
}