package kopo.jeonnam.service.impl.csv;

import kopo.jeonnam.dto.csv.ProductDTO;
import kopo.jeonnam.model.Product;
import kopo.jeonnam.repository.mongo.csv.ProductRepository;
import kopo.jeonnam.service.csv.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎯 ProductService 구현체
 * - ProductRepository를 통해 MongoDB 연동
 * - DTO ↔ Entity 변환 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductDTO> getAll() {
        log.info("📦 전체 product 리스트 조회 요청");
        return productRepository.findAll().stream()
                .map(Product::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> findByAreaAndKeyword(String area, String keyword) {
        log.info("🔍 지역 필터: '{}', 키워드 검색: '{}'", area, keyword);

        // 모두 공백이면 전체 조회
        if ((area == null || area.isBlank()) && (keyword == null || keyword.isBlank())) {
            return getAll();
        }

        List<Product> filtered = productRepository.findByProAreaContainingIgnoreCaseAndProNameContainingIgnoreCase(
                area == null ? "" : area,
                keyword == null ? "" : keyword
        );

        return filtered.stream()
                .map(Product::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(List<ProductDTO> dtoList) {
        log.info("📝 총 {}개의 product 저장 요청", dtoList.size());
        List<Product> entities = dtoList.stream()
                .map(Product::fromDTO)
                .collect(Collectors.toList());
        productRepository.saveAll(entities);
        log.info("✅ 저장 완료!");
    }

    @Override
    public boolean existsAny() {
        return productRepository.count() > 0;
    }


}
