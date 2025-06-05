package kopo.jeonnam.service.user;

import kopo.jeonnam.dto.user.MailDTO;

/**
 * 메일 서비스 인터페이스
 */
public interface IMailService {
    
    /**
     * 이메일 발송
     * @param mailDTO 메일 정보
     * @return 발송 결과 (1: 성공, 0: 실패)
     * @throws Exception 예외 발생 시
     */
    int doSendMail(MailDTO mailDTO) throws Exception;
    
    /**
     * 인증 코드 생성
     * @return 생성된 인증 코드
     */
    String generateVerificationCode();
    
    /**
     * 인증 메일 발송
     * @param email 수신자 이메일
     * @param code 인증 코드
     * @return 발송 결과 (1: 성공, 0: 실패)
     * @throws Exception 예외 발생 시
     */
    int sendVerificationMail(String email, String code) throws Exception;
}
