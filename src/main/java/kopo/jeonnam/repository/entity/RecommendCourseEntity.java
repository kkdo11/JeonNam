package kopo.jeonnam.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import lombok.Getter; // Getter 자동 생성
import lombok.Setter; // Setter 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동 생성
import lombok.AllArgsConstructor; // 모든 필드를 포함하는 생성자 자동 생성
import lombok.ToString; // toString() 메서드 자동 생성

/**
 * recommend_course 컬렉션에 저장될 추천 코스 정보를 정의하는 MongoDB 엔티티 클래스
 */
@Getter // 모든 필드의 Getter 메서드를 자동으로 생성
@Setter // 모든 필드의 Setter 메서드를 자동으로 생성
@NoArgsConstructor // 인자 없는 기본 생성자를 자동으로 생성
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 자동으로 생성
@ToString // toString() 메서드를 자동으로 생성
@Document(collection = "recommend_course")
public class RecommendCourseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String courseKey;
    private String courseInfoIds;
    private String courseCategory;
    private String courseName;
    private String coursePeriod;
    private String coursePersonType;
    private String coursePersonCount;
    private String courseContents;
    private String courseArea;
    private String planCourseId;
    private String planName;
    private String planArea;
    private String planAddr;
    private String spotNm;
    private String spotAddr;
}