package kopo.jeonnam.repository.mongo.user;

import kopo.jeonnam.model.UserInfo;
import org.bson.types.ObjectId; // ObjectId import 추가
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Optional import 추가

/**
 * MongoDB 'userInfo' 컬렉션에 대한 데이터 액세스 계층.
 * Spring Data MongoDB의 MongoRepository를 상속받아 기본적인 CRUD 기능을 제공하며,
 * 도메인 특화된 쿼리 메서드를 정의합니다.
 * UserInfo 모델의 ID가 ObjectId이므로 MongoRepository의 두 번째 제네릭 타입을 ObjectId로 지정합니다.
 */
@Repository
public interface UserRepository extends MongoRepository<UserInfo, ObjectId> { // ObjectId로 변경!

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

    /**
     * userId 기준 내림차순 정렬 후 첫 번째 사용자 조회 (가장 최근에 등록된 사용자).
     * @return 가장 최근에 등록된 사용자 정보 (Optional로 래핑)
     */
    @Query(value = "{}", sort = "{ userId : -1 }") // MongoDB 기본 ID 필드명은 _id 이므로 userId 대신 _id 사용 권장
    Optional<UserInfo> findTopByOrderByUserIdDesc(); // 메서드명은 유지하거나 findTopByOrderBy_idDesc()로 변경 고려 가능
    // 다만 UserInfo 모델에 @Id private final ObjectId userId; 가 있다면
    // Spring Data가 userId를 _id에 매핑하므로 이 메서드명도 동작은 할 수 있음.
    // 하지만 _id를 명시하는게 더 안전하고 일반적임.

}