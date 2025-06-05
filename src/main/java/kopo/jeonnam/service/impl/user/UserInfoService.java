package kopo.jeonnam.service.impl.user;

import kopo.jeonnam.dto.user.MailDTO;
import kopo.jeonnam.dto.user.MsgDTO;
import kopo.jeonnam.dto.user.UserInfoDTO;
import kopo.jeonnam.model.UserInfo;
import kopo.jeonnam.repository.mongo.user.UserRepository;
import kopo.jeonnam.service.user.IMailService;
import kopo.jeonnam.service.user.IUserInfoService;
import kopo.jeonnam.util.EncryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional; // Optional import 추가
import java.util.Random;

/**
 * 사용자 정보(회원가입, 로그인, 비밀번호 변경 등) 관련 비즈니스 로직을 처리하는 서비스 구현체.
 * UserInfoService는 사용자 정보 처리의 핵심 로직을 담당합니다.
 */
@Service
public class UserInfoService implements IUserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IMailService mailService;

    /**
     * 회원가입 처리 메서드
     */
    @Override
    public int insertUserInfo(UserInfoDTO userInfoDTO) throws Exception {
        logger.info("🔵 [insertUserInfo] 시작 - 이메일: {}", userInfoDTO.email());

        // Step 1: 이메일 중복 확인 (Optional 반환에 맞춰 수정)
        Optional<UserInfo> existingUserOptional = userRepository.findByEmail(userInfoDTO.email());
        if (existingUserOptional.isPresent()) { // Optional에서 UserInfo가 존재하는지 확인
            logger.warn("❗ 중복 이메일 존재 - 이메일: {}", userInfoDTO.email());
            return 2;
        }

        try {
            // Step 2: 비밀번호 암호화
            String encryptedPassword = EncryptUtil.encHashSHA256(userInfoDTO.password());
            logger.debug("🔐 비밀번호 암호화 성공");

            // Step 3: UserInfo 객체 생성 및 저장
            UserInfo userInfo = UserInfo.fromDTO(userInfoDTO); // DTO로부터 기본 UserInfo 모델 생성

            // fromDTO로 생성된 객체에 암호화된 비밀번호 반영 (불변 객체이므로 새로 빌드)
            userInfo = UserInfo.builder()
                    .userId(userInfo.getUserId())
                    .email(userInfo.getEmail())
                    .password(encryptedPassword) // 암호화된 비밀번호로 교체
                    .name(userInfo.getName())
                    .birthDate(userInfo.getBirthDate())
                    .sex(userInfo.getSex())
                    .country(userInfo.getCountry())
                    .build();

            userRepository.save(userInfo);
            logger.info("✅ 사용자 저장 완료 - userId: {}", userInfo.getUserId());

            // Step 4: 회원가입 완료 메일 발송
            mailService.doSendMail(MailDTO.builder()
                    .toMail(userInfoDTO.email())
                    .title("전남 영화/드라마 여행 회원가입 완료")
                    .contents("정상적으로 회원가입이 완료되었습니다.")
                    .build());
            logger.info("📧 회원가입 완료 메일 발송 - 이메일: {}", userInfoDTO.email());

            return 1;
        } catch (Exception e) {
            logger.error("🔥 회원가입 처리 중 예외 발생", e);
            return 0;
        }
    }

    /**
     * 사용자 로그인 처리 구현
     */
    @Override
    public int userLogin(UserInfoDTO userInfoDTO) throws Exception {
        logger.info("🔵 [userLogin] 시작 - 이메일: {}", userInfoDTO.email());

        // 1. 이메일로 사용자 정보 조회 (Optional 반환에 맞춰 수정)
        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        if (userOptional.isEmpty()) { // Optional에서 UserInfo가 없는지 확인
            logger.warn("❗ 존재하지 않는 이메일 - 이메일: {}", userInfoDTO.email());
            return 0; // 이메일 없음
        }

        UserInfo user = userOptional.get(); // Optional에서 실제 UserInfo 객체 가져오기

        // 2. 입력된 비밀번호 암호화 후 저장된 비밀번호와 비교
        String encryptedInputPassword = EncryptUtil.encHashSHA256(userInfoDTO.password());

        // Lombok @Value는 필드명과 동일한 메서드명(password())을 생성하므로 user.password()로 접근
        if (user.getPassword().equals(encryptedInputPassword)) { // UserInfo.getPassword() 대신 user.password() 사용
            logger.info("✅ 로그인 성공 - 이메일: {}", userInfoDTO.email());
            return 1; // 로그인 성공
        } else {
            logger.warn("❌ 비밀번호 불일치 - 이메일: {}", userInfoDTO.email());
            return 0; // 비밀번호 불일치
        }
    }

    /**
     * 이름과 생년월일로 이메일 찾기
     */
    @Override
    public UserInfoDTO findEmailByNameAndBirthDate(String name, String birthDate) throws Exception {
        logger.info("🔍 [findEmailByNameAndBirthDate] 요청 - 이름: {}, 생년월일: {}", name, birthDate);

        // Optional 반환에 맞춰 수정
        Optional<UserInfo> userOptional = userRepository.findByNameAndBirthDate(name, birthDate);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 이메일 조회 성공 - 이메일: {}", user.getEmail());
            return UserInfoDTO.builder()
                    .email(user.getEmail())
                    .build();
        }

        logger.warn("❗ 사용자 정보 일치하지 않음 - 이름: {}, 생년월일: {}", name, birthDate);
        return null;
    }

    /**
     * 이메일 존재 여부 확인
     */
    @Override
    public UserInfoDTO getEmailExists(UserInfoDTO userInfoDTO) throws Exception {
        logger.info("🔍 [getEmailExists] 이메일 중복 확인 요청 - 이메일: {}", userInfoDTO.email());

        // Optional 반환에 맞춰 수정
        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        String result = (userOptional.isPresent()) ? "Y" : "N"; // Optional에서 UserInfo가 존재하는지 확인
        logger.debug("📌 이메일 존재 여부: {}", result);
        return UserInfoDTO.builder()
                .exist_yn(result)
                .build();
    }

    /**
     * 이름 + 이메일로 사용자 찾기 (비밀번호 찾기 시 사용)
     */
    @Override
    public UserInfoDTO findUserByNameAndEmail(UserInfoDTO userInfoDTO) throws Exception {
        logger.info("🔍 [findUserByNameAndEmail] 요청 - 이메일: {}, 이름: {}", userInfoDTO.email(), userInfoDTO.name());

        // Optional 반환에 맞춰 수정
        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        // 이메일이 존재하고, 이름도 일치하는지 확인
        if (userOptional.isPresent() && userOptional.get().getName().equals(userInfoDTO.name())) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 정보 일치 - userId: {}", user.getUserId());
            return UserInfoDTO.builder()
                    .userId(user.getUserId().toHexString())
                    .email(user.getEmail())
                    .name(user.getName())
                    .build();
        }

        logger.warn("❗ 사용자 정보 불일치 - 이메일: {}, 이름: {}", userInfoDTO.email(), userInfoDTO.name());
        return null;
    }

    /**
     * 비밀번호 재설정 (임시 비밀번호 발급)
     */
    @Override
    public MsgDTO resetUserPassword(String email, String name) throws Exception {
        logger.info("🟡 [resetUserPassword] 호출 - 이메일: {}, 이름: {}", email, name);

        // 사용자 존재 여부 확인 (Optional 반환에 맞춰 수정)
        Optional<UserInfo> userOptional = userRepository.findByEmail(email);
        logger.debug("🔍 사용자 조회 결과: {}", userOptional.isPresent() ? userOptional.get().toString() : "null");

        // 이메일이 존재하지 않거나 이름이 일치하지 않는 경우
        if (userOptional.isEmpty() || !userOptional.get().getName().equals(name)) {
            logger.warn("❗ 사용자 존재하지 않거나 이름 불일치 - 이메일: {}, 이름: {}", email, name);
            return MsgDTO.builder()
                    .result(0)
                    .msg("일치하는 사용자 정보가 없습니다.")
                    .build();
        }

        UserInfo user = userOptional.get(); // 실제 UserInfo 객체 가져오기

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        logger.debug("🔑 생성된 임시 비밀번호: {}", tempPassword);

        // 이메일로 임시 비밀번호 발송
        try {
            mailService.doSendMail(MailDTO.builder()
                    .toMail(email)
                    .title("전남 영화/드라마 여행 임시 비밀번호 발급")
                    .contents("임시 비밀번호: " + tempPassword + "\n로그인 후 반드시 비밀번호를 변경해주세요.")
                    .build());
            logger.info("📧 임시 비밀번호 이메일 발송 시도 성공 - 이메일: {}", email); // 메일 발송 자체의 성공

            // 메일 발송 성공 후! 데이터베이스 비밀번호 업데이트
            String encryptedPassword = EncryptUtil.encHashSHA256(tempPassword);

            // UserInfo는 불변 객체이므로 Builder 패턴 사용
            UserInfo updatedUser = UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(encryptedPassword) // 암호화된 새 비밀번호로 교체
                    .name(user.getName())
                    .birthDate(user.getBirthDate())
                    .sex(user.getSex())
                    .country(user.getCountry())
                    .build();

            userRepository.save(updatedUser);
            logger.info("✅ 임시 비밀번호 저장 완료 (메일 발송 후) - userId: {}", updatedUser.getUserId());

            return MsgDTO.builder()
                    .result(1)
                    .msg("임시 비밀번호가 이메일로 발송되었습니다.")
                    .build();

        } catch (Exception e) {
            logger.error("🔥 임시 비밀번호 이메일 발송 실패 및 비밀번호 변경 취소 - 이메일: {}", email, e);
            // 메일 발송 실패 시, 비밀번호를 변경하지 않고 오류 메시지 반환
            return MsgDTO.builder()
                    .result(0)
                    .msg("임시 비밀번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.")
                    .build();
        }
    }

    /**
     * 이메일로 사용자 정보 조회
     */
    @Override
    public UserInfoDTO findByEmail(String email) throws Exception {
        logger.info("🔍 [findByEmail] 호출 - 이메일: {}", email);

        // Optional 반환에 맞춰 수정
        Optional<UserInfo> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 조회 성공 - userId: {}", user.getUserId());
            return user.toDTO();
        }

        logger.warn("❗ 사용자 존재하지 않음 - 이메일: {}", email);
        return null;
    }

    /**
     * 비밀번호 변경
     */
    @Override
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        logger.info("🟢 [changePassword] 호출 - 이메일: {}", email);
        try {
            // 사용자 조회 (Optional 반환에 맞춰 수정)
            Optional<UserInfo> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                logger.warn("❗ 사용자 없음 - 이메일: {}", email);
                return false;
            }
            UserInfo user = userOptional.get(); // 실제 UserInfo 객체 가져오기

            // 기존 비밀번호 확인
            String encryptedCurrentPassword = EncryptUtil.encHashSHA256(currentPassword);
            if (!user.getPassword().equals(encryptedCurrentPassword)) { // user.getPassword() 대신 user.password() 사용
                logger.warn("❌ 현재 비밀번호 불일치 - 이메일: {}", email);
                return false;
            }

            // 새 비밀번호 암호화
            String encryptedNewPassword = EncryptUtil.encHashSHA256(newPassword);

            // 사용자 정보 업데이트 (불변 객체이므로 Builder 패턴 사용)
            UserInfo updatedUser = UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(encryptedNewPassword) // 암호화된 새 비밀번호로 교체
                    .name(user.getName())
                    .birthDate(user.getBirthDate())
                    .sex(user.getSex())
                    .country(user.getCountry())
                    .build();

            userRepository.save(updatedUser);
            logger.info("✅ 비밀번호 변경 성공 - 이메일: {}", email);
            return true;
        } catch (Exception e) {
            logger.error("🔥 비밀번호 변경 중 예외 발생 - 이메일: {}", email, e);
            return false;
        }
    }

    /**
     * 랜덤 임시 비밀번호 생성기
     */
    private String generateTempPassword() {
        Random random = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        logger.debug("🔧 [generateTempPassword] 생성된 임시 비밀번호: {}", sb.toString());
        return sb.toString();
    }
}