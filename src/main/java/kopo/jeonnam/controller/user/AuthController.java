package kopo.jeonnam.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.jeonnam.dto.user.UserInfoDTO;
import kopo.jeonnam.service.user.IUserInfoService;
import kopo.jeonnam.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자 인증(로그인, 로그아웃) 관련 요청을 처리하는 컨트롤러.
 * Spring MVC의 Controller 역할을 수행하며, Lombok의 @Slf4j를 통해 로깅 기능을,
 * @RequiredArgsConstructor를 통해 final 필드에 대한 생성자 주입을 자동으로 처리합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/auth") // 인증 관련 요청은 "/auth" 경로로 시작
public class AuthController {

    private final IUserInfoService userInfoService; // 사용자 정보 비즈니스 로직을 처리하는 서비스 인터페이스


    /**
     * ------------------------ 로그인 상태 확인 API ------------------------
     */

    /**
     * 현재 사용자의 로그인 상태를 확인하는 API.
     * 로그인되어 있으면 200 OK 응답을, 로그인되어 있지 않으면 401 Unauthorized 응답을 반환합니다.
     * 이 API는 클라이언트(JavaScript)에서 사용자의 로그인 여부를 비동기적으로 확인하는 데 사용됩니다.
     *
     * @param session HTTP 세션 객체. 세션에 사용자 이메일 정보가 있는지 확인합니다.
     * @return 로그인 상태에 따른 ResponseEntity (200 OK 또는 401 Unauthorized)
     */
    @GetMapping("/check-login")
    @ResponseBody // 이 어노테이션이 있어야 메서드 반환값이 HTTP 응답 본문으로 직접 전송됩니다.
    public ResponseEntity<Void> checkLoginStatus(HttpSession session) {
        log.info("[UserController] checkLoginStatus start.");
        if (session.getAttribute("email") != null) {
            log.info("[UserController] User is logged in.");
            return ResponseEntity.ok().build(); // 200 OK
        } else {
            log.warn("[UserController] User is not logged in. Returning 401 Unauthorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
    }

    /**
     * ------------------------ 로그인 처리 ------------------------
     */

    /**
     * 로그인 처리 API (POST 요청)
     * 사용자가 제출한 이메일과 비밀번호를 받아 로그인을 처리합니다.
     * 성공 시 세션에 사용자 이메일을 저장하고 로그인 성공 페이지로 리다이렉트하며,
     * 실패 시 로그인 페이지로 다시 리다이렉트하고 오류 메시지를 전달합니다.
     *
     * @param request HTTP 요청 객체. 이메일과 비밀번호 파라미터를 포함합니다.
     * @param session 세션 객체. 로그인 성공 시 사용자 이메일 정보를 저장합니다.
     * @param redirectAttributes 리다이렉트 시 Flash 속성을 통해 메시지를 전달하는 데 사용됩니다.
     * @return 로그인 성공 시 로그인 성공 처리 페이지 URL, 실패 시 로그인 페이지 URL로 리다이렉트
     * @throws Exception 서비스 계층에서 발생할 수 있는 예외를 상위로 던집니다.
     */
    @PostMapping("/login")
    public String doLogin(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        log.info("[AuthController] doLogin start.");

        String email = CmmUtil.nvl(request.getParameter("email"));
        String password = CmmUtil.nvl(request.getParameter("password"));

        // 필수 정보 누락 체크
        if (email.isEmpty() || password.isEmpty()) {
            log.warn("[AuthController] doLogin failed - Email or password missing. Email: {}", email);
            redirectAttributes.addFlashAttribute("errorMsg", "이메일과 비밀번호를 모두 입력해주세요.");
            return "redirect:/auth/login";
        }

        UserInfoDTO loginDTO = UserInfoDTO.builder().email(email).password(password).build();
        int res = userInfoService.userLogin(loginDTO);

        if (res == 1) { // 로그인 성공

            UserInfoDTO userInfo = userInfoService.findByEmail(email); // 사용자 정보 조회
            session.setAttribute("email", userInfo.email());
            session.setAttribute("name", userInfo.name()); // DB에서 가져온 이름 저장

//            session.setAttribute("email", email); // 세션에 이메일 저장

            log.info("[AuthController] doLogin success. User Email: {}", email);
            return "redirect:/auth/loginsuccess";
        } else { // 로그인 실패
            log.warn("[AuthController] doLogin failed - Email: {}, reason: password mismatch or unregistered account.", email);
            redirectAttributes.addFlashAttribute("errorMsg", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/user/login";
        }
    }

    /**
     * 로그인 성공 후 처리 페이지
     * 로그인 성공 시 메인 페이지 또는 대시보드 페이지로 이동 전에 추가적인 처리를 할 수 있습니다.
     * 현재는 단순히 `user/index` 뷰를 반환합니다.
     *
     * @param session 세션 객체. 로그인된 사용자의 이메일 정보를 확인합니다.
     * @return 로그인 성공 시 보여줄 뷰 페이지 경로
     */
    @GetMapping("/loginsuccess")
    public String loginSuccess(HttpSession session) {
        String email = CmmUtil.nvl((String) session.getAttribute("email"));
        if (email.isEmpty()) {
            log.warn("[AuthController] loginSuccess - No login info (email) in session. Unauthorized access.");
            return "redirect:/auth/login"; // 로그인 정보 없으면 로그인 페이지로 리다이렉트
        }
        log.info("[AuthController] loginSuccess page accessed by user: {}", email);
        return "user/index"; // 예: 로그인 후 메인 페이지 또는 대시보드 페이지
    }

    /**
     * ------------------------ 로그아웃 처리 ------------------------
     */

    /**
     * 로그아웃 처리 API
     * 현재 사용자의 세션을 무효화하여 로그아웃을 처리합니다.
     *
     * @param session HTTP 세션 객체. 현재 세션을 무효화합니다.
     * @param redirectAttributes 리다이렉트 시 메시지 전달용
     * @return 로그아웃 후 리다이렉트할 페이지 (예: 로그인 페이지 또는 메인 페이지)
     */
    @GetMapping("/logout")
    public String doLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = CmmUtil.nvl((String) session.getAttribute("email"));
        if (!email.isEmpty()) { // 세션에 이메일이 있는 경우에만 로그아웃 처리 로깅
            session.invalidate(); // 세션 무효화
            log.info("[AuthController] doLogout success for user: {}", email);
            redirectAttributes.addFlashAttribute("successMsg", "성공적으로 로그아웃되었습니다.");
        } else {
            log.warn("[AuthController] doLogout - Logout request from unauthenticated user.");
        }
        return "/user/login"; // 로그아웃 후 로그인 페이지로 리다이렉트
    }
}