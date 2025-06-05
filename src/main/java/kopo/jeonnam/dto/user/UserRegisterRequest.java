package kopo.jeonnam.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// @Builder는 현재 UserInfoDTO가 Lombok의 @Builder를 사용하고 있어서 일관성을 위해 추가.
// 실제로 request DTO에서는 Builder가 필수는 아님.
public record UserRegisterRequest(
    @NotBlank(message = "이메일 주소는 필수 입력입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&+=]).{8,20}$",
             message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8~20자여야 합니다.")
    String password,

    @NotBlank(message = "이름은 필수 입력입니다.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
    String name,

    @NotBlank(message = "생년월일은 필수 입력입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)")
    String birthDate,

    @NotBlank(message = "성별은 필수 선택입니다.")
    String sex,

    @NotBlank(message = "국가는 필수 입력입니다.")
    @Size(min = 2, max = 50, message = "국가 이름은 2자 이상 50자 이하여야 합니다.")
    String country,

    // 🌟 이메일 인증 코드를 Request DTO에 포함!
    @NotBlank(message = "이메일 인증 코드는 필수 입력입니다.")
    String verificationCode
) {
    // Lombok의 @Builder를 사용하여 빌더 패턴을 구현
    @Builder
    public UserRegisterRequest {
        // 모든 필드를 초기화하는 기본 생성자 역할
    }
}