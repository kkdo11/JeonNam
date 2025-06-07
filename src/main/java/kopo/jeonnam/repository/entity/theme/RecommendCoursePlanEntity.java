package kopo.jeonnam.repository.entity.theme;

import org.springframework.data.annotation.Id; // MongoDB의 _id 필드에 매핑
import org.springframework.data.mongodb.core.mapping.Document; // MongoDB 컬렉션 매핑 어노테이션

import java.io.Serializable; // 네트워크 전송 등을 위해 직렬화 가능하도록 마커 인터페이스 구현

import lombok.Getter; // 모든 필드의 Getter 메서드를 자동으로 생성
import lombok.Setter; // 모든 필드의 Setter 메서드를 자동으로 생성
import lombok.NoArgsConstructor; // 인자 없는 기본 생성자를 자동으로 생성
import lombok.AllArgsConstructor; // 모든 필드를 포함하는 생성자(AllArgsConstructor)를 자동으로 생성
import lombok.ToString; // toString() 메서드를 자동으로 생성

/**
 * recommend_course_plan 컬렉션에 저장될 추천 코스 계획 정보를 정의하는 MongoDB 엔티티 클래스
 * 이 엔티티는 특정 추천 코스(planCourseId)에 대한 상세 계획 정보를 담으며,
 * 각 계획 항목은 고유한 planInfoId를 가집니다
 */
@Getter // 모든 필드에 대한 Getter 메서드를 자동으로 생성합니다.
@Setter // 모든 필드에 대한 Setter 메서드를 자동으로 생성합니다.
@NoArgsConstructor // 인자 없는 기본 생성자를 자동으로 생성합니다. (Spring Data MongoDB에서 필요)
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자를 자동으로 생성합니다.
@ToString // 객체의 필드 값을 문자열로 반환하는 toString() 메서드를 자동으로 생성합니다.
@Document(collection = "recommend_course_plan") // 이 클래스가 MongoDB의 "recommend_course_plan" 컬렉션에 매핑됨을 나타냅니다.
public class RecommendCoursePlanEntity implements Serializable {

    private static final long serialVersionUID = 1L; // 직렬화 버전 ID

    @Id // 이 필드가 MongoDB 문서의 _id 필드에 매핑됨을 나타냅니다.
    private String planInfoId; // 계획 정보의 고유 ID (예: "0000000455_01") - 기본 키 역할

    private String planCourseId; // 해당 계획이 속한 추천 코스의 ID
    private String planDay; // 계획 일자 (예: "1" for 첫째 날)
    private String planTime; // 계획 시간
    private String planName; // 계획 장소 이름
    private String planArea; // 계획 장소 지역
    private String planAddr; // 계획 장소 주소
    private String planAddrDetail; // 계획 장소 상세 주소
    private String planLatitude; // 계획 장소 위도
    private String planLongitude; // 계획 장소 경도
    private String planPhone; // 계획 장소 전화번호
    private String planFax; // 계획 장소 팩스번호
    private String planHomepage; // 계획 장소 홈페이지 URL
    private String planParking; // 계획 장소 주차 정보
    private String planContents; // 계획 장소 상세 내용
}