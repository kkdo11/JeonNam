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

import java.util.Optional;
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

        // Step 1: 이메일 중복 확인
        Optional<UserInfo> existingUserOptional = userRepository.findByEmail(userInfoDTO.email());
        if (existingUserOptional.isPresent()) {
            logger.warn("❗ 중복 이메일 존재 - 이메일: {}", userInfoDTO.email());
            return 2;
        }

        try {
            // Step 2: 비밀번호 암호화
            String encryptedPassword = EncryptUtil.encHashSHA256(userInfoDTO.password());
            logger.debug("🔐 비밀번호 암호화 성공");

            // Step 3: UserInfo 객체 생성 및 저장
            // DTO에서 모델로 변환 시 모든 필드를 포함하여 빌드
            UserInfo userInfo = UserInfo.builder()
                    .email(userInfoDTO.email()) // DTO는 레코드이므로 .email()로 접근
                    .password(encryptedPassword) // 암호화된 비밀번호로 교체
                    .name(userInfoDTO.name())
                    .birthDate(userInfoDTO.birthDate())
                    .phoneNum(userInfoDTO.phoneNum()) // ✨ **userInfoDTO에서 phoneNum 가져와서 추가!**
                    .sex(userInfoDTO.sex())
                    .country(userInfoDTO.country())
                    .build();

            userRepository.save(userInfo);
            logger.info("✅ 사용자 저장 완료 - userId: {}", userInfo.getUserId()); // UserInfo는 getUserId()

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

        // 1. 이메일로 사용자 정보 조회
        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        if (userOptional.isEmpty()) {
            logger.warn("❗ 존재하지 않는 이메일 - 이메일: {}", userInfoDTO.email());
            return 0; // 이메일 없음
        }

        UserInfo user = userOptional.get();

        // 2. 입력된 비밀번호 암호화 후 저장된 비밀번호와 비교
        String encryptedInputPassword = EncryptUtil.encHashSHA256(userInfoDTO.password());

        // ✨ UserInfo 모델(Lombok @Value)은 getPassword()를 사용합니다.
        if (user.getPassword().equals(encryptedInputPassword)) {
            logger.info("✅ 로그인 성공 - 이메일: {}", userInfoDTO.email());
            return 1; // 로그인 성공
        } else {
            logger.warn("❌ 비밀번호 불일치 - 이메일: {}", userInfoDTO.email());
            return 0; // 비밀번호 불일치
        }
    }

    /**
     * 이름, 생년월일, 휴대폰 번호로 이메일 찾기 구현
     */
    @Override
    public UserInfoDTO findEmailByNameAndBirthDateAndPhoneNum(String name, String birthDate, String phoneNum) throws Exception {
        logger.info("🔍 [findEmailByNameAndBirthDateAndPhoneNum] 요청 - 이름: {}, 생년월일: {}, 휴대폰 번호: {}", name, birthDate, phoneNum);

        Optional<UserInfo> userOptional = userRepository.findByNameAndBirthDateAndPhoneNum(name, birthDate, phoneNum);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 이메일 조회 성공 - 이메일: {}", user.getEmail()); // ✨ UserInfo는 getEmail() 사용
            return UserInfoDTO.builder()
                    .email(user.getEmail()) // ✨ UserInfo는 getEmail() 사용
                    .build();
        }

        logger.warn("❗ 사용자 정보 일치하지 않음 - 이름: {}, 생년월일: {}, 휴대폰 번호: {}", name, birthDate, phoneNum);
        return null;
    }




    /**
     * 이메일 존재 여부 확인
     */
    @Override
    public UserInfoDTO getEmailExists(UserInfoDTO userInfoDTO) throws Exception {
        logger.info("🔍 [getEmailExists] 이메일 중복 확인 요청 - 이메일: {}", userInfoDTO.email());

        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        String result = (userOptional.isPresent()) ? "Y" : "N";
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

        Optional<UserInfo> userOptional = userRepository.findByEmail(userInfoDTO.email());

        // ✨ UserInfo 모델(Lombok @Value)은 getName()을 사용합니다.
        if (userOptional.isPresent() && userOptional.get().getName().equals(userInfoDTO.name())) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 정보 일치 - userId: {}", user.getUserId()); // UserInfo는 getUserId()
            return UserInfoDTO.builder()
                    .userId(user.getUserId().toHexString()) // UserInfo는 getUserId()
                    .email(user.getEmail()) // UserInfo는 getEmail()
                    .name(user.getName()) // UserInfo는 getName()
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

        Optional<UserInfo> userOptional = userRepository.findByEmail(email);
        logger.debug("🔍 사용자 조회 결과: {}", userOptional.isPresent() ? userOptional.get().toString() : "null");

        // ✨ UserInfo 모델(Lombok @Value)은 getName()을 사용합니다.
        if (userOptional.isEmpty() || !userOptional.get().getName().equals(name)) {
            logger.warn("❗ 사용자 존재하지 않거나 이름 불일치 - 이메일: {}, 이름: {}", email, name);
            return MsgDTO.builder()
                    .result(0)
                    .msg("일치하는 사용자 정보가 없습니다.")
                    .build();
        }

        UserInfo user = userOptional.get();

        String tempPassword = generateTempPassword();
        logger.debug("🔑 생성된 임시 비밀번호: {}", tempPassword);

        try {
            mailService.doSendMail(MailDTO.builder()
                    .toMail(email)
                    .title("전남 영화/드라마 여행 임시 비밀번호 발급")
                    .contents("임시 비밀번호: " + tempPassword + "\n로그인 후 반드시 비밀번호를 변경해주세요.")
                    .build());
            logger.info("📧 임시 비밀번호 이메일 발송 시도 성공 - 이메일: {}", email);

            String encryptedPassword = EncryptUtil.encHashSHA256(tempPassword);

            // UserInfo는 불변 객체이므로 Builder 패턴 사용
            // ✨ UserInfo 모델(Lombok @Value)은 getXXX()를 사용합니다.
            UserInfo updatedUser = UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(encryptedPassword) // 암호화된 새 비밀번호로 교체
                    .name(user.getName())
                    .birthDate(user.getBirthDate())
                    .phoneNum(user.getPhoneNum()) // ✨ **phoneNum도 포함하여 업데이트!**
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

        Optional<UserInfo> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            logger.info("✅ 사용자 조회 성공 - userId: {}", user.getUserId()); // ✨ UserInfo는 getUserId()
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
            Optional<UserInfo> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                logger.warn("❗ 사용자 없음 - 이메일: {}", email);
                return false;
            }
            UserInfo user = userOptional.get();

            String encryptedCurrentPassword = EncryptUtil.encHashSHA256(currentPassword);
            // ✨ UserInfo 모델(Lombok @Value)은 getPassword()를 사용합니다.
            if (!user.getPassword().equals(encryptedCurrentPassword)) {
                logger.warn("❌ 현재 비밀번호 불일치 - 이메일: {}", email);
                return false;
            }

            String encryptedNewPassword = EncryptUtil.encHashSHA256(newPassword);

            // 사용자 정보 업데이트 (불변 객체이므로 Builder 패턴 사용)
            // ✨ UserInfo 모델(Lombok @Value)은 getXXX()를 사용합니다.
            UserInfo updatedUser = UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .password(encryptedNewPassword)
                    .name(user.getName())
                    .birthDate(user.getBirthDate())
                    .phoneNum(user.getPhoneNum()) // ✨ **phoneNum도 포함하여 업데이트!**
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