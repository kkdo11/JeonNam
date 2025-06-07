package kopo.jeonnam.service.csv;

import kopo.jeonnam.dto.csv.ProductDTO;
import kopo.jeonnam.model.Product;

import java.util.List;

/**
 * 💡 ProductService 인터페이스
 * 비즈니스 로직 정의 (CRUD 등)
 */
public interface IProductService {
    List<ProductDTO> getAll();        // 전체 목록 조회
    List<ProductDTO> findByAreaAndKeyword(String area, String keyword); //검색
    void saveAll(List<ProductDTO> dtoList); // 여러 개 저장
    boolean existsAny();  // DB에 저장된 데이터가 있는지 확인
}
