package kopo.jeonnam.dto.theme;

import lombok.Getter;           // Getter 메서드 자동 생성
import lombok.Setter;           // Setter 메서드 자동 생성
import lombok.NoArgsConstructor;    // 인자 없는 기본 생성자 자동 생성
import lombok.AllArgsConstructor;   // 모든 필드를 포함하는 생성자 자동 생성
import lombok.ToString;         // toString() 메서드 자동 생성

/**
 * 남도 추천 여행 코스 DTO (Data Transfer Object)
 * 코스 및 주요 지점 정보를 담는 데이터 전송 객체입니다.
 * Controller/Service/Repository 계층 간 데이터 전달에 사용됩니다.
 *
 */
@Getter             // 모든 필드에 대한 public Getter 메서드를 자동으로 생성합니다.
@Setter             // 모든 필드에 대한 public Setter 메서드를 자동으로 생성합니다.
@NoArgsConstructor  // 인자 없는 기본 생성자를 자동으로 생성합니다.
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자를 자동으로 생성합니다.
@ToString           // 객체의 필드 값을 문자열로 반환하는 toString() 메서드를 자동으로 생성합니다.
public class RecommendCourseDTO {

    // 코스 ID
    private String planCourseId;
    // 코스명
    private String planName;
    // 지역명
    private String planArea;
    // 코스 주소
    private String planAddr;
    // 주요 지점명
    private String spotNm;
    // 주요 지점 주소
    private String spotAddr;

}