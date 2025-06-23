    package kopo.jeonnam.controller.user;

    import jakarta.servlet.http.HttpSession;
    import jakarta.validation.Valid;
    import kopo.jeonnam.dto.user.MsgDTO;
    import kopo.jeonnam.dto.user.PasswordChangeRequest;
    import kopo.jeonnam.dto.user.UserInfoDTO;
    import kopo.jeonnam.dto.user.UserRegisterRequest;
    import kopo.jeonnam.service.user.IMailService;
    import kopo.jeonnam.service.user.IUserInfoService;
    import kopo.jeonnam.util.CmmUtil;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Controller;
    import org.springframework.validation.BindingResult;
    import org.springframework.web.bind.annotation.*;

    import java.util.Optional; // Optional import 추가 (혹시 없으면)

    /**
     * 사용자 계정 정보(회원가입, 이메일 인증, 비밀번호 변경 등) 관련 요청을 처리하는 컨트롤러.
     * Spring MVC의 Controller 역할을 수행하며, Lombok의 @Slf4j를 통해 로깅 기능을,
     * @RequiredArgsConstructor를 통해 final 필드에 대한 생성자 주입을 자동으로 처리합니다.
     */
    @Slf4j
    @RequiredArgsConstructor
    @Controller
    @RequestMapping("/account") // 계정 관련 요청은 "/account" 경로로 시작
    public class AccountController {

        private final IUserInfoService userInfoService; // 사용자 정보 비즈니스 로직을 처리하는 서비스 인터페이스
        private final IMailService mailService; // 메일 관련 비즈니스 로직을 처리하는 서비스 인터페이스

        /**
         * ------------------------ 회원가입 ------------------------
         */

        /**
         * 회원가입 처리 API
         * 클라이언트로부터 전달된 사용자 정보를 받아 회원가입을 처리합니다.
         * 이메일 중복 여부를 확인하고, 유효성 검사를 거쳐 회원 정보를 저장합니다.
         *
         * @param requestDto 클라이언트로부터 전송된 사용자 정보 및 인증 코드를 담은 DTO 객체.
         * @Valid 어노테이션으로 유효성 검사 수행.
         * @param bindingResult @Valid 검사 결과 오류 정보를 담는 객체.
         * @param session HTTP 세션 객체.
         * @return 가입 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         * @throws Exception 서비스 계층에서 발생할 수 있는 예외
         */
        @ResponseBody
        @PostMapping("/insertUserInfo")
        public MsgDTO insertUserInfo(@RequestBody @Valid UserRegisterRequest requestDto,
                                     BindingResult bindingResult,
                                     HttpSession session) throws Exception {
            // 회원가입 요청 시작 로그 (INFO 레벨 유지)
            log.info("[AccountController] insertUserInfo start. Request Email: {}", CmmUtil.nvl(requestDto.email()));

            // 1. DTO 바인딩 및 컨트롤러 단 유효성 검사
            if (requestDto == null) {
                log.error("[AccountController] insertUserInfo requestDto is null. Potential JSON parsing failure.");
                return MsgDTO.builder().result(0).msg("서버 요청 처리 중 오류가 발생했습니다. (데이터 누락)").build();
            }

            if (bindingResult.hasErrors()) {
                String defaultErrorMessage = "입력값이 올바르지 않습니다.";
                String errorMessage = bindingResult.getFieldError() != null ?
                        bindingResult.getFieldError().getDefaultMessage() : defaultErrorMessage;
                log.warn("[AccountController] insertUserInfo controller validation failed: {}", errorMessage);
                return MsgDTO.builder().result(0).msg(errorMessage).build();
            }

            // 2. 이메일 인증 코드 유효성 검사 (세션에 저장된 코드와 비교)
            String sessionEmail = (String) session.getAttribute("emailToVerify");
            String sessionCode = (String) session.getAttribute("emailVerificationCode");

            // 세션 정보 누락 또는 만료
            if (CmmUtil.nvl(sessionEmail).isEmpty() || CmmUtil.nvl(sessionCode).isEmpty()) {
                log.warn("[AccountController] insertUserInfo failed - Email verification session expired or not found. Request Email: {}", requestDto.email());
                return MsgDTO.builder().result(0).msg("이메일 인증 세션이 만료되었거나 존재하지 않습니다. 인증 메일을 다시 요청해주세요.").build();
            }

            // 요청 이메일과 세션 이메일 불일치
            if (!requestDto.email().equals(sessionEmail)) {
                log.warn("[AccountController] insertUserInfo failed - Request email and session email mismatch. Request: '{}', Session: '{}'", requestDto.email(), sessionEmail);
                return MsgDTO.builder().result(0).msg("이메일 주소가 변경되었거나 인증된 이메일과 다릅니다. 다시 인증해주세요.").build();
            }

            // 인증 코드 불일치
            if (!requestDto.verificationCode().equals(sessionCode)) {
                log.warn("[AccountController] insertUserInfo failed - Verification code mismatch. Input Code: '{}', Session Code: '{}'", requestDto.verificationCode(), sessionCode);
                return MsgDTO.builder().result(0).msg("인증 코드가 일치하지 않습니다. 다시 확인해주세요.").build();
            }

            // 3. 서비스 계층으로 DTO 전달 (UserRegisterRequest -> UserInfoDTO 변환)
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .email(requestDto.email())
                    .password(requestDto.password())
                    .name(requestDto.name())
                    .birthDate(requestDto.birthDate())
                    .phoneNum(requestDto.phoneNum()) // ✨ **회원가입 요청 시 phoneNum도 DTO에 담아서 서비스로 전달!**
                    .sex(requestDto.sex())
                    .country(requestDto.country())
                    .build();

            int res = 0;
            String msg = "";
            try {
                res = userInfoService.insertUserInfo(userInfoDTO);

                if (res == 1) {
                    msg = "회원가입이 성공적으로 완료되었습니다.";
                    // 회원가입 성공 시에만 이메일 인증 세션 정보 삭제
                    session.removeAttribute("emailToVerify");
                    session.removeAttribute("emailVerificationCode");
                    log.info("[AccountController] insertUserInfo success. Email: {}", requestDto.email());
                } else if (res == 2) {
                    msg = "이미 가입된 이메일 주소입니다. 다른 이메일을 사용해주세요.";
                    log.warn("[AccountController] insertUserInfo failed - Email already exists. Email: {}", requestDto.email());
                } else {
                    msg = "회원가입 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의하세요.";
                    log.error("[AccountController] insertUserInfo failed - Unknown reason. Email: {}", requestDto.email());
                }
            } catch (Exception e) {
                log.error("[AccountController] insertUserInfo exception: {}", e.getMessage(), e);
                msg = "회원가입 처리 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                res = 0;
            }

            log.info("[AccountController] insertUserInfo end.");
            return MsgDTO.builder().result(res).msg(msg).build();
        }

        /**
         * ------------------------ 이메일 중복 확인 ------------------------
         */

        /**
         * 이메일 중복 확인 API
         * 회원가입 시 사용자가 입력한 이메일이 이미 데이터베이스에 존재하는지 확인합니다.
         *
         * @param emailToCheck 중복 확인을 요청할 이메일 주소 (POST 요청의 "email" 파라미터)
         * @return UserInfoDTO 객체. `exist_yn` 필드를 통해 중복 여부("Y" 또는 "N")를 반환합니다.
         */
        @ResponseBody
        @PostMapping("/getEmailExists")
        public UserInfoDTO getEmailExists(@RequestParam("email") String emailToCheck) {
            log.info("[AccountController] getEmailExists start. Checking Email: {}", emailToCheck);

            if (CmmUtil.nvl(emailToCheck).isEmpty()) {
                log.warn("[AccountController] getEmailExists - Email is missing in request.");
                return UserInfoDTO.builder().exist_yn("Y").build(); // 이메일 없으면 중복으로 간주 (안전)
            }

            try {
                UserInfoDTO queryDTO = UserInfoDTO.builder().email(emailToCheck).build();
                UserInfoDTO rDTO = userInfoService.getEmailExists(queryDTO);

                log.info("[AccountController] getEmailExists end. Email: {}, Exists: {}", emailToCheck, rDTO.exist_yn());
                return rDTO;

            } catch (Exception e) {
                log.error("[AccountController] getEmailExists exception: {}", e.getMessage(), e);
                return UserInfoDTO.builder().exist_yn("Y").build(); // 서버 오류 시 '중복'으로 처리 (안전 우선)
            }
        }

        /**
         * ------------------------ 이메일 인증 ------------------------
         */

        /**
         * 이메일 인증 메일 전송 API
         * 사용자에게 인증 코드가 포함된 이메일을 전송하고, 해당 코드를 세션에 저장합니다.
         *
         * @param emailToVerify 인증 메일을 전송할 이메일 주소 (POST 요청의 "email" 파라미터)
         * @param session HTTP 세션 객체. 생성된 인증 코드를 임시로 저장합니다.
         * @return 전송 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         * @throws Exception 서비스 계층에서 발생할 수 있는 예외
         */
        @ResponseBody
        @PostMapping("/sendVerificationEmail")
        public MsgDTO sendVerificationEmail(@RequestParam("email") String emailToVerify, HttpSession session) throws Exception {
            log.info("[AccountController] sendVerificationEmail start. Target Email: {}", emailToVerify);

            if (CmmUtil.nvl(emailToVerify).isEmpty()) {
                log.warn("[AccountController] sendVerificationEmail - Email is missing in request.");
                return MsgDTO.builder().result(0).msg("인증 메일을 전송할 이메일 주소를 입력해주세요.").build();
            }

            try {
                String verificationCode = mailService.generateVerificationCode();

                int sendResult = mailService.sendVerificationMail(emailToVerify, verificationCode);

                if (sendResult == 1) {
                    session.setAttribute("emailToVerify", emailToVerify);
                    session.setAttribute("emailVerificationCode", verificationCode);
                    session.setMaxInactiveInterval(5 * 60); // 세션 만료 시간 설정 (5분)
                    log.info("[AccountController] sendVerificationEmail success. Email: {}", emailToVerify);
                    return MsgDTO.builder().result(1).msg("인증 메일이 성공적으로 전송되었습니다. 메일함을 확인해주세요.").build();
                } else {
                    log.warn("[AccountController] sendVerificationEmail failed. Target Email: {}", emailToVerify);
                    return MsgDTO.builder().result(0).msg("인증 메일 전송에 실패했습니다. 이메일 주소를 다시 확인하거나 잠시 후 다시 시도해주세요.").build();
                }
            } catch (Exception e) {
                log.error("[AccountController] sendVerificationEmail exception: {}", e.getMessage(), e);
                return MsgDTO.builder().result(0).msg("서버 오류로 인해 인증 메일 전송에 실패했습니다. 관리자에게 문의해주세요.").build();
            } finally {
                log.info("[AccountController] sendVerificationEmail end.");
            }
        }

        /**
         * 이메일 인증 코드 검증 API
         * 사용자가 입력한 인증 코드를 세션에 저장된 코드와 비교하여 이메일 인증을 완료합니다.
         *
         * @param emailFromRequest 요청에서 추출한 이메일 (POST 요청의 "email" 파라미터)
         * @param inputCode 사용자가 입력한 인증 코드 (POST 요청의 "code" 파라미터)
         * @param session HTTP 세션 객체. 이전에 저장된 인증 코드를 가져옵니다.
         * @return 인증 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         */
        @ResponseBody
        @PostMapping("/verifyEmailCode")
        public MsgDTO verifyEmailCode(@RequestParam("email") String emailFromRequest,
                                      @RequestParam("code") String inputCode,
                                      HttpSession session) {
            log.info("[AccountController] verifyEmailCode start. Request Email: {}", emailFromRequest);

            String cleanedEmail = CmmUtil.nvl(emailFromRequest);
            String cleanedInputCode = CmmUtil.nvl(inputCode);

            if (cleanedEmail.isEmpty() || cleanedInputCode.isEmpty()) {
                log.warn("[AccountController] verifyEmailCode - Email or code is missing. Email: {}, Code Length: {}", emailFromRequest, inputCode.length());
                return MsgDTO.builder().result(0).msg("이메일과 인증 코드를 모두 입력해주세요.").build();
            }

            String sessionEmail = (String) session.getAttribute("emailToVerify");
            String sessionCode = (String) session.getAttribute("emailVerificationCode");

            if (CmmUtil.nvl(sessionEmail).isEmpty() || CmmUtil.nvl(sessionCode).isEmpty()) {
                log.warn("[AccountController] verifyEmailCode failed - Email verification session expired or not found. Request Email: {}", cleanedEmail);
                return MsgDTO.builder().result(0).msg("인증 세션이 만료되었거나 존재하지 않습니다. 인증 메일을 다시 요청해주세요.").build();
            }

            if (cleanedEmail.equals(sessionEmail) && cleanedInputCode.equals(sessionCode)) {
                log.info("[AccountController] verifyEmailCode success. Email: {}", cleanedEmail);
                return MsgDTO.builder().result(1).msg("이메일 인증에 성공하였습니다.").build();
            } else {
                log.warn("[AccountController] verifyEmailCode failed - Email or code mismatch. Request Email: {}, Session Email: {}, Input Code: {}, Session Code length: {}", cleanedEmail, sessionEmail, cleanedInputCode, sessionCode.length());
                return MsgDTO.builder().result(0).msg("인증 코드가 일치하지 않습니다. 다시 확인해주세요.").build();
            }
        }


        /**
         * 아이디(이메일) 찾기 API
         * 사용자의 이름, 생년월일, 휴대폰 번호를 통해 가입된 이메일 주소를 찾아 반환합니다.
         *
         * @param nameFromRequest 사용자의 이름 (POST 요청의 "name" 파라미터)
         * @param birthDateFromRequest 사용자의 생년월일 (POST 요청의 "birthDate" 파라미터)
         * @param phoneNumFromRequest 사용자의 휴대폰 번호 (POST 요청의 "phoneNum" 파라미터) ✨ **새롭게 추가된 파라미터**
         * @return 이메일 찾기 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         */
        @ResponseBody
        @PostMapping("/findEmail")
        public MsgDTO findEmail(@RequestParam("name") String nameFromRequest,
                                @RequestParam("birthDate") String birthDateFromRequest,
                                @RequestParam("phoneNum") String phoneNumFromRequest) { // ✨ **phoneNum 파라미터 추가**
            log.info("[AccountController] findEmail start. Request Name: {}, BirthDate: {}, PhoneNum: {}", nameFromRequest, birthDateFromRequest, phoneNumFromRequest);

            String cleanedName = CmmUtil.nvl(nameFromRequest);
            String cleanedBirthDate = CmmUtil.nvl(birthDateFromRequest);
            String cleanedPhoneNum = CmmUtil.nvl(phoneNumFromRequest); // ✨ **phoneNum 정제**

            if (cleanedName.isEmpty() || cleanedBirthDate.isEmpty() || cleanedPhoneNum.isEmpty()) { // ✨ **phoneNum 유효성 검사 추가**
                log.warn("[AccountController] findEmail - Name, birthDate, or phoneNum is missing.");
                return MsgDTO.builder().result(0).msg("이름, 생년월일, 휴대폰 번호를 모두 입력해주세요.").build();
            }

            try {
                // ✨ **수정: findEmailByNameAndBirthDateAndPhoneNum 메서드 호출**
                UserInfoDTO rDTO = userInfoService.findEmailByNameAndBirthDateAndPhoneNum(cleanedName, cleanedBirthDate, cleanedPhoneNum);

                if (rDTO != null && !CmmUtil.nvl(rDTO.email()).isEmpty()) {
                    log.info("[AccountController] findEmail success. Found Email: {}", rDTO.email());
                    return MsgDTO.builder().result(1).msg("회원님의 이메일은 [" + rDTO.email() + "]입니다.").build();
                } else {
                    log.warn("[AccountController] findEmail failed - No account found for name: {}, birthDate: {}, phoneNum: {}", cleanedName, cleanedBirthDate, cleanedPhoneNum);
                    return MsgDTO.builder().result(0).msg("입력하신 정보와 일치하는 이메일이 없습니다. 다시 확인해주세요.").build();
                }
            } catch (Exception e) {
                log.error("[AccountController] findEmail exception: {}", e.getMessage(), e);
                return MsgDTO.builder().result(0).msg("서버 오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요.").build();
            } finally {
                log.info("[AccountController] findEmail end.");
            }
        }


        /**
         * 비밀번호 재설정 (임시 비밀번호 발급) API
         * 클라이언트로부터 이메일과 이름을 받아 임시 비밀번호를 발급하고 해당 이메일로 전송합니다.
         *
         * @param passwordChangeRequest 비밀번호 재설정을 위한 이메일과 이름을 담은 DTO (JSON 요청 본문)
         * @return 재설정 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         * @throws Exception 서비스 계층에서 발생할 수 있는 예외
         */
        @ResponseBody
        @PostMapping("/resetPassword")
        public MsgDTO resetPassword(@RequestBody PasswordChangeRequest passwordChangeRequest) throws Exception {
            log.info("[AccountController] resetPassword start. Request Email: {}", CmmUtil.nvl(passwordChangeRequest.getEmail()));

            String email = CmmUtil.nvl(passwordChangeRequest.getEmail());
            String name = CmmUtil.nvl(passwordChangeRequest.getName());

            if (email.isEmpty() || name.isEmpty()) {
                log.warn("[AccountController] resetPassword - Email or name is missing. Email: {}, Name: {}", email, name);
                return MsgDTO.builder().result(0).msg("이메일과 이름을 모두 입력해주세요.").build();
            }

            try {
                MsgDTO resultMsg = userInfoService.resetUserPassword(email, name);

                if (resultMsg.result() == 1) {
                    log.info("[AccountController] resetPassword success. Email: {}", email);
                } else {
                    log.warn("[AccountController] resetPassword failed. Email: {}, Reason: {}", email, resultMsg.msg());
                }
                return resultMsg;
            } catch (Exception e) {
                log.error("[AccountController] resetPassword exception: {}", e.getMessage(), e);
                return MsgDTO.builder().result(0).msg("서버 오류가 발생하여 비밀번호 재설정에 실패했습니다. 관리자에게 문의하세요.").build();
            } finally {
                log.info("[AccountController] resetPassword end.");
            }
        }


        /**
         * 비밀번호 변경 처리 API
         * 로그인된 사용자가 기존 비밀번호를 확인하고 새로운 비밀번호로 변경합니다.
         * 세션에서 로그인 이메일을 가져와 현재 사용자를 식별합니다.
         *
         * @param passwordChangeRequest 현재 비밀번호와 새 비밀번호를 담은 DTO (JSON 요청 본문)
         * @param session HTTP 세션 객체. 현재 로그인된 사용자의 이메일 정보를 확인합니다.
         * @return 변경 결과 메시지와 상태 코드를 담은 MsgDTO 객체
         * @throws Exception 서비스 계층에서 발생할 수 있는 예외
         */
        @PostMapping("/changePassword")
        @ResponseBody
        public MsgDTO changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest, HttpSession session) throws Exception {
            String userEmail = CmmUtil.nvl((String) session.getAttribute("email")); // 세션에서 로그인된 사용자 이메일 가져오기
            log.info("[AccountController] changePassword start. Logged-in User Email: {}", userEmail);

            if (userEmail.isEmpty()) {
                log.warn("[AccountController] changePassword - No login info in session. Unauthorized access attempt.");
                return MsgDTO.builder().result(0).msg("로그인이 필요합니다. 다시 로그인 후 시도해주세요.").build();
            }

            String currentPassword = CmmUtil.nvl(passwordChangeRequest.getCurrentPassword());
            String newPassword = CmmUtil.nvl(passwordChangeRequest.getNewPassword());

            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                log.warn("[AccountController] changePassword - Missing current or new password. Email: {}", userEmail);
                return MsgDTO.builder().result(0).msg("현재 비밀번호와 새 비밀번호를 모두 입력해주세요.").build();
            }

            // 새 비밀번호가 현재 비밀번호와 동일한지 확인
            if (currentPassword.equals(newPassword)) {
                log.warn("[AccountController] changePassword - New password is same as current password. Email: {}", userEmail);
                return MsgDTO.builder().result(0).msg("새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다.").build();
            }

            try {
                boolean success = userInfoService.changePassword(userEmail, currentPassword, newPassword);

                if (success) {
                    log.info("[AccountController] changePassword success. Email: {}", userEmail);
                    return MsgDTO.builder().result(1).msg("비밀번호가 성공적으로 변경되었습니다.").build();
                } else {
                    log.warn("[AccountController] changePassword failed - Current password mismatch or other reason. Email: {}", userEmail);
                    return MsgDTO.builder().result(0).msg("현재 비밀번호가 일치하지 않거나 비밀번호 변경에 실패했습니다. 다시 확인해주세요.").build();
                }
            } catch (Exception e) {
                log.error("[AccountController] changePassword exception: {}", e.getMessage(), e);
                return MsgDTO.builder().result(0).msg("서버 오류가 발생하여 비밀번호 변경에 실패했습니다. 관리자에게 문의하세요.").build();
            } finally {
                log.info("[AccountController] changePassword end.");
            }
        }
    }