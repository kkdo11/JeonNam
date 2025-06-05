package kopo.jeonnam.service.impl.user;

import kopo.jeonnam.dto.user.MailDTO;
import kopo.jeonnam.service.user.IMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class MailService implements IMailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.sender.address}")
    private String senderEmailAddress; // 발신자 이메일 주소를 필드로 주입

    /**
     * 일반 메일 발송 메서드
     * @param mailDTO - 메일 수신자, 제목, 내용 포함 DTO
     * @return 1(성공), 0(실패)
     * @throws Exception
     */
    @Override
    public int doSendMail(MailDTO mailDTO) throws Exception {
        try {
            logger.info("doSendMail 시작 - 수신자: {}, 제목: {}", mailDTO.toMail(), mailDTO.title());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailDTO.toMail());
            message.setSubject(mailDTO.title());
            message.setText(mailDTO.contents());

            // 하드코딩 대신 설정에서 읽어온 발신자 주소 사용
            message.setFrom(senderEmailAddress);

            mailSender.send(message);

            logger.info("메일 발송 성공");
            return 1;
        } catch (Exception e) {
            logger.error("메일 발송 실패 - 수신자: {}, 제목: {}", mailDTO.toMail(), mailDTO.title(), e);
            return 0;
        }
    }


    /**
     * 6자리 숫자 인증 코드 생성 메서드
     * @return 인증 코드 문자열
     */
    @Override
    public String generateVerificationCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }

        logger.debug("생성된 인증 코드: {}", sb.toString());
        return sb.toString();
    }

    /**
     * 이메일 인증 코드 발송 메서드
     * @param email - 인증 코드 수신 이메일 주소
     * @param code - 인증 코드 문자열
     * @return 1(성공), 0(실패)
     * @throws Exception
     */
    @Override
    public int sendVerificationMail(String email, String code) throws Exception {
        try {
            logger.info("sendVerificationMail 시작 - 수신자: {}, 인증 코드: {}", email, code);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("전남 영화/드라마 여행 이메일 인증");
            message.setText("인증 코드: " + code + "\n이 코드를 입력하여 이메일 인증을 완료해주세요.");

            logger.info("인증 코드: " + code + "\n이 코드를 입력하여 이메일 인증을 완료해주세요.");
            // ⚠️ 여기를 수정하세요: 하드코딩 대신 주입받은 senderEmailAddress 사용
            message.setFrom(senderEmailAddress);

            mailSender.send(message);

            logger.info("인증 메일 발송 성공");
            return 1;
        } catch (Exception e) {
            // 오류 로그에 수신자와 코드 정보를 추가하여 디버깅을 돕습니다.
            logger.error("인증 메일 발송 실패 - 수신자: {}, 인증 코드: {}", email, code, e);
            return 0;
        }
    }

}