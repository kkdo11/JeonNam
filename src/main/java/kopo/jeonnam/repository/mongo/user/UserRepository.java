package kopo.jeonnam.repository.mongo.user;

import kopo.jeonnam.repository.entity.UserInfo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB 'userInfo' 컬렉션에 대한 데이터 액세스 계층.
 * Spring Data MongoDB의 MongoRepository를 상속받아 기본적인 CRUD 기능을 제공하며,
 * 도메인 특화된 쿼리 메서드를 정의합니다.
 * UserInfo 모델의 ID가 ObjectId이므로 MongoRepository의 두 번째 제네릭 타입을 ObjectId로 지정합니다.
 */
@Repository
public interface UserRepository extends MongoRepository<UserInfo, ObjectId> {

    /**
     * 이메일로 사용자 정보를 조회합니다.
     * 조회 결과가 없을 수 있으므로 Optional<UserInfo>를 반환합니다.
     * @param email 사용자 이메일
     * @return 일치하는 사용자 정보 (Optional로 래핑)
     */
    Optional<UserInfo> findByEmail(String email);

    /**
     * 이름으로 사용자 정보를 조회합니다.
     * @param name 사용자 이름
     * @return 일치하는 사용자 정보 (Optional로 래핑)
     */
    Optional<UserInfo> findByName(String name);

    /**
     * 이름과 이메일로 사용자 정보를 조회합니다.
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @return 일치하는 사용자 정보 (Optional로 래핑)
     */
    Optional<UserInfo> findByNameAndEmail(String name, String email);

    /**
     * 이름과 생년월일로 사용자 정보를 조회합니다.
     * @param name 사용자 이름
     * @param birthDate 사용자 생년월일
     * @return 일치하는 사용자 정보 (Optional로 래핑)
     */
    Optional<UserInfo> findByNameAndBirthDate(String name, String birthDate);

    // ✨ **여기에 이름, 생년월일, 휴대폰 번호로 사용자 정보를 조회하는 메서드 추가!**
    /**
     * 이름, 생년월일, 휴대폰 번호로 사용자 정보를 조회합니다.
     * @param name 사용자 이름
     * @param birthDate 사용자 생년월일
     * @param phoneNum 사용자 휴대폰 번호
     * @return 일치하는 사용자 정보 (Optional로 래핑)
     */
    Optional<UserInfo> findByNameAndBirthDateAndPhoneNum(String name, String birthDate, String phoneNum);


    /**
     * userId 기준 내림차순 정렬 후 첫 번째 사용자 조회 (가장 최근에 등록된 사용자).
     * @return 가장 최근에 등록된 사용자 정보 (Optional로 래핑)
     */
    @Query(value = "{}", sort = "{ userId : -1 }")
    Optional<UserInfo> findTopByOrderByUserIdDesc();

}