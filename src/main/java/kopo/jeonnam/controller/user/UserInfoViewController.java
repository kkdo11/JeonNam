package kopo.jeonnam.controller.user;

import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.user.UserInfoDTO;
import kopo.jeonnam.service.user.IUserInfoService;
import kopo.jeonnam.util.CmmUtil; // CmmUtil 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사용자 인터페이스(UI) 페이지 이동을 관리하는 컨트롤러.
 * 로그인, 회원가입, 아이디/비밀번호 찾기, 마이페이지 등 사용자와 상호작용하는 화면을 제공합니다.
 * 비즈니스 로직보다는 뷰 반환에 초점을 맞춥니다.
 */
@Slf4j // Lombok을 사용하여 로거 자동 생성
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 함
@Controller // Spring MVC 컨트롤러임을 명시
@RequestMapping("/user") // 이 컨트롤러의 모든 메서드는 "/user" 경로로 시작하는 요청을 처리
public class UserInfoViewController {

    // IUserInfoService는 사용자 정보를 조회하는 데 사용될 수 있으므로 주입
    private final IUserInfoService userInfoService;

    /**
     * ------------------------ 페이지 이동 ------------------------
     */

    /**
     * 회원가입 페이지 요청을 처리합니다.
     * 단순히 `userRegForm.html` 뷰를 반환합니다.
     *
     * @return 회원가입 폼 뷰 경로
     */
    @GetMapping("/userRegForm")
    public String userRegForm() {
        log.info("[UserInfoViewController] userRegForm page requested.");
        return "user/userRegForm"; // src/main/resources/templates/user/userRegForm.html 경로의 뷰를 반환
    }

    /**
     * 로그인 페이지 요청을 처리합니다.
     * 로그인 실패 메시지나 비밀번호 변경 성공 메시지 등 URL 파라미터를 통해 전달된 정보를 뷰에 추가합니다.
     *
     * @param error 로그인 오류 여부 (URL 파라미터 `error=true`일 경우)
     * @param passwordChanged 비밀번호 변경 성공 여부 (URL 파라미터 `passwordChanged=true`일 경우)
     * @param model 뷰로 데이터를 전달하는 데 사용되는 Spring Model 객체
     * @return 로그인 페이지 뷰 경로
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "passwordChanged", required = false) String passwordChanged,
            Model model) {
        log.info("[UserInfoViewController] loginPage requested. error param: {}, passwordChanged param: {}",
                CmmUtil.nvl(error), CmmUtil.nvl(passwordChanged)); // CmmUtil.nvl 적용

        if ("true".equals(error)) { // 문자열 비교는 "상수".equals(변수) 형식으로 하는 것이 NPE 방지에 더 안전함
            model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해주세요.");
            log.info("[UserInfoViewController] Added error message for login page.");
        }
        if ("true".equals(passwordChanged)) {
            model.addAttribute("successMsg", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
            log.info("[UserInfoViewController] Added success message for password change on login page.");
        }
        return "user/login"; // src/main/resources/templates/user/login.html 경로의 뷰를 반환
    }

    /**
     * 이메일 찾기 페이지 요청을 처리합니다.
     *
     * @return 이메일 찾기 폼 뷰 경로
     */
    @GetMapping("/findEmail")
    public String findEmailPage() {
        log.info("[UserInfoViewController] findEmailPage requested.");
        return "user/findEmail"; // src/main/resources/templates/user/findEmail.html 경로의 뷰를 반환
    }

    /**
     * 비밀번호 찾기 페이지 요청을 처리합니다.
     * (이 기능은 일반적으로 "비밀번호 재설정"으로 불리기도 합니다.)
     *
     * @return 비밀번호 찾기 폼 뷰 경로
     */
    @GetMapping("/findPWD")
    public String findPWDPage() {
        log.info("[UserInfoViewController] findPWDPage requested.");
        return "user/findPWD"; // src/main/resources/templates/user/findPWD.html 경로의 뷰를 반환
    }

    /**
     * 마이페이지 요청을 처리합니다.
     * 사용자 세션을 확인하여 로그인 여부를 검사하고, 로그인된 경우 사용자 정보를 조회하여 뷰에 전달합니다.
     * 로그인되지 않은 경우 로그인 페이지로 리다이렉트합니다.
     *
     * @param session 현재 사용자의 HTTP 세션
     * @param model 뷰로 데이터를 전달하는 데 사용되는 Spring Model 객체
     * @return 마이페이지 뷰 경로 또는 로그인 페이지로의 리다이렉트 경로
     * @throws Exception 사용자 정보 조회 중 발생할 수 있는 예외
     */
    @GetMapping("/myPage")
    public String myPage(HttpSession session, Model model) throws Exception {
        String email = CmmUtil.nvl((String) session.getAttribute("email")); // CmmUtil.nvl 적용

        log.info("[UserInfoViewController] myPage requested. Session Email: {}", email.isEmpty() ? "None (Not logged in)" : email);

        // 이메일이 세션에 없으면 (로그인되지 않은 상태)
        if (email.isEmpty()) {
            log.warn("[UserInfoViewController] myPage access denied. Redirecting to login page due to no session email.");
            return "redirect:/auth/login"; // 로그인 페이지로 리다이렉트 (Auth 컨트롤러 매핑 사용)
        }

        try {
            // 이메일을 사용하여 사용자 상세 정보 조회
            UserInfoDTO userInfo = userInfoService.findByEmail(email);

            // 조회된 사용자 정보가 없거나 이메일이 일치하지 않을 경우
            if (userInfo == null || CmmUtil.nvl(userInfo.email()).isEmpty()) {
                log.error("[UserInfoViewController] myPage - User info not found for session email: {}. Invalidating session.", email);
                session.invalidate(); // 비정상적인 세션이므로 무효화
                model.addAttribute("errorMsg", "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
                return "redirect:/auth/login"; // 로그인 페이지로 리다이렉트 (Auth 컨트롤러 매핑 사용)
            }

            log.info("[UserInfoViewController] myPage - User info retrieved for email: {}", userInfo.email());
            model.addAttribute("userInfo", userInfo); // 조회된 사용자 정보를 'userInfo'라는 이름으로 모델에 추가

            return "user/myPage"; // src/main/resources/templates/user/myPage.html 경로의 뷰를 반환
        } catch (Exception e) {
            log.error("[UserInfoViewController] myPage exception while fetching user info: {}", e.getMessage(), e);
            model.addAttribute("errorMsg", "사용자 정보를 불러오는 중 오류가 발생했습니다. 다시 시도해주세요.");
            return "redirect:/auth/login"; // 로그인 페이지로 리다이렉트 (Auth 컨트롤러 매핑 사용)
        }
    }
}