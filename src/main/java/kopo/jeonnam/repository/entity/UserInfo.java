package kopo.jeonnam.repository.entity;

import kopo.jeonnam.dto.user.UserInfoDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB 'userInfo' 컬렉션에 매핑되는 도메인 모델.
 * 사용자 정보를 불변 객체로 표현합니다.
 * Lombok의 @Value 어노테이션을 사용하여 불변 클래스를 자동 생성하며,
 * @Builder를 통해 유연한 객체 생성을 지원합니다.
 */
@Value // 모든 필드를 private final로 만들고, Getter, equals, hashCode, toString, AllArgsConstructor를 자동 생성 (불변 객체에 최적화)
@Builder // Builder 패턴을 통해 유연한 객체 생성 지원
@RequiredArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 필드 생성자를 private으로 만들어 Builder를 강제
@Document(collection = "userInfo")
public class UserInfo {

    @Id
    @Builder.Default // Builder 사용 시 userId가 명시되지 않으면 자동으로 ObjectId를 생성하도록 설정
    private final ObjectId userId = new ObjectId(); // @Id 필드에 초기값 할당

    private final String email;
    private final String password;
    private final String name;
    private final String birthDate;

    private final String phoneNum; // ✨ **여기에 휴대폰 번호 필드 추가!**

    private final String sex;
    private final String country;


    /**
     * UserInfoDTO 객체를 UserInfo 모델 객체로 변환하는 정적 팩토리 메서드.
     * 새로운 UserInfo 객체를 생성할 때 사용됩니다.
     *
     * @param dto 변환할 UserInfoDTO 객체
     * @return UserInfo 모델 객체
     */
    public static UserInfo fromDTO(UserInfoDTO dto) {
        return UserInfo.builder()
                // ✨ 레코드의 필드 접근은 'dto.필드명()' 방식입니다!
                .userId(dto.userId() != null ? new ObjectId(dto.userId()) : new ObjectId())
                .email(dto.email())
                .password(dto.password())
                .name(dto.name())
                .birthDate(dto.birthDate())
                .phoneNum(dto.phoneNum()) // ✨ **dto에서 phoneNum 가져와서 추가!**
                .sex(dto.sex())
                .country(dto.country())
                .build();
    }

    /**
     * UserInfo 모델 객체를 UserInfoDTO 객체로 변환합니다.
     *
     * @return UserInfoDTO 객체
     */
    public UserInfoDTO toDTO() {
        return UserInfoDTO.builder()
                .userId(this.userId != null ? this.userId.toHexString() : null)
                .email(this.email)
                .password(this.password) // 비밀번호는 DTO에서 필요에 따라 제거될 수 있음
                .name(this.name)
                .birthDate(this.birthDate)
                .phoneNum(this.phoneNum) // ✨ **모델의 phoneNum을 DTO로 변환 시 추가!**
                .sex(this.sex)
                .country(this.country)
                .exist_yn("Y") // 이 모델이 존재하므로 "Y"로 설정
                .build();
    }
}