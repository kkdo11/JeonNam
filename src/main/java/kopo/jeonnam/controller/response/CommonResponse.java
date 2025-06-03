package kopo.jeonnam.controller.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonResponse<T> {

    // HTTP 상태 코드
    private HttpStatus httpStatus;

    // 메시지 (예: SUCCESS / ERROR)
    private String message;

    // 응답 데이터
    private T data;

    /**
     * 공통 응답 객체 생성 메서드
     */
    public static <T> CommonResponse<T> of(HttpStatus httpStatus, String message, T data) {
        return CommonResponse.<T>builder()
                .httpStatus(httpStatus)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 유효성 검사 실패 시 에러 메시지 리스트 반환
     */
    public static ResponseEntity<CommonResponse<?>> getErrors(BindingResult bindingResult){
        return ResponseEntity.badRequest()
                .body(CommonResponse.of(
                        HttpStatus.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST.series().name(),
                        bindingResult.getAllErrors()
                ));
    }

//    /**
//     * SweetAlert용 유효성 검사 단일 메시지 반환
//     */
//    public static ResponseEntity<CommonResponse<SweetAlertMsgDTO>> getFirstErrorAlert(BindingResult bindingResult){
//        String errorMsg = bindingResult.getFieldError() != null
//                ? bindingResult.getFieldError().getDefaultMessage()
//                : "유효성 검사에 실패하였습니다.";
//
//        SweetAlertMsgDTO alert = new SweetAlertMsgDTO(0, "error", "요청 실패", errorMsg);
//        return ResponseEntity.badRequest()
//                .body(CommonResponse.of(
//                        HttpStatus.BAD_REQUEST,
//                        "fail",
//                        alert
//                ));
//    }

}
