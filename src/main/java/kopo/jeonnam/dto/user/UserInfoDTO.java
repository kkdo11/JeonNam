package kopo.jeonnam.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserInfoDTO(
        String userId, // MongoDB ObjectId를 String으로 표현
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&+=]).*$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수 입력값입니다.")
        String name,

        @NotBlank(message = "생년월일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{8}$", message = "생년월일은 8자리 숫자(YYYYMMDD) 형식이어야 합니다.")
        String birthDate,

        // ✨ **여기에 휴대폰 번호 필드를 추가합니다!**
        @NotBlank(message = "휴대폰 번호는 필수 입력값입니다.")
        @Pattern(regexp = "^(010|011|016|017|018|019)\\d{3,4}\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (하이픈 제외 숫자만)")
        String phoneNum,

        @NotBlank(message = "성별은 필수 입력값입니다.")
        String sex,

        @NotBlank(message = "국가는 필수 입력값입니다.")
        String country,

        String regDt, // 등록일시
        String chgDt, // 변경일시
        String exist_yn // 이메일 중복 확인 등에 사용
) {
        // Record는 @Builder와 함께 사용할 때, toBuilder()를 직접 구현해야 할 수 있습니다.
        // 여기서는 간단한 DTO 사용을 가정합니다.
}