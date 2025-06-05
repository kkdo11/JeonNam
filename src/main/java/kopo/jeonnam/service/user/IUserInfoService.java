package kopo.jeonnam.service.user;

import kopo.jeonnam.dto.user.MsgDTO;
import kopo.jeonnam.dto.user.UserInfoDTO;

/**
 * 사용자 정보 관련 서비스 인터페이스
 */
public interface IUserInfoService {
    
    /**
     * 회원가입 처리
     * @param userInfoDTO 사용자 정보
     * @return 처리 결과 (1: 성공, 2: 이메일 중복, 0: 실패)
     * @throws Exception 예외 발생 시
     */
    int insertUserInfo(UserInfoDTO userInfoDTO) throws Exception;
    
    /**
     * 이메일 중복 확인
     * @param userInfoDTO 이메일 정보
     * @return 중복 여부 (exist_yn: Y/N)
     * @throws Exception 예외 발생 시
     */
    UserInfoDTO getEmailExists(UserInfoDTO userInfoDTO) throws Exception;
    
    /**
     * 이름과 이메일로 사용자 찾기
     * @param userInfoDTO 이름, 이메일 정보
     * @return 사용자 정보
     * @throws Exception 예외 발생 시
     */
    UserInfoDTO findUserByNameAndEmail(UserInfoDTO userInfoDTO) throws Exception;
    
    /**
     * 비밀번호 재설정
     * @param email 이메일
     * @param name 이름
     * @return 처리 결과
     * @throws Exception 예외 발생 시
     */
    MsgDTO resetUserPassword(String email, String name) throws Exception;
    
    /**
     * 이메일로 사용자 찾기
     * @param email 이메일
     * @return 사용자 정보
     * @throws Exception 예외 발생 시
     */
    UserInfoDTO findByEmail(String email) throws Exception;
    
    /**
     * 비밀번호 변경
     * @param email 이메일
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @return 변경 성공 여부
     */
    boolean changePassword(String email, String currentPassword, String newPassword);

    UserInfoDTO findEmailByNameAndBirthDate(String name, String birthDate) throws Exception;


    /**
     * 사용자 로그인 처리
     * @param userInfoDTO (email, password)
     * @return 1: 성공, 0: 실패
     * @throws Exception
     */
    int userLogin(UserInfoDTO userInfoDTO) throws Exception;
}
