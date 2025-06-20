package kopo.jeonnam.repository.mongo.movie;

import kopo.jeonnam.repository.entity.movie.MovieEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends MongoRepository<MovieEntity, String> {
    List<MovieEntity> findAll();

    // ID로 특정 영화를 조회 (Optional을 사용하여 null 처리 용이)
    Optional<MovieEntity> findById(String id);

    // 1. 제목 또는 주소(Addr)로 검색
    // `IgnoreCase`를 붙이면 대소문자를 구분하지 않습니다.
    Page<MovieEntity> findByTitleContainingIgnoreCaseOrAddrContainingIgnoreCase(String titleKeyword, String addrKeyword, Pageable pageable);

    // 2. 제목, 장소(location), 주소(Addr) 중 하나라도 포함하는 경우 검색
    Page<MovieEntity> findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCaseOrAddrContainingIgnoreCase(
            String titleKeyword, String locationKeyword, String addrKeyword, Pageable pageable);

    // 3. 특정 필드로만 검색 (예: 장소로만 검색)
    Page<MovieEntity> findByLocationContainingIgnoreCase(String locationKeyword, Pageable pageable);

    // (추가) 제목과 주소를 동시에 만족하는 검색 (AND 조건)
    Page<MovieEntity> findByTitleContainingIgnoreCaseAndAddrContainingIgnoreCase(String titleKeyword, String addrKeyword, Pageable pageable);


}
