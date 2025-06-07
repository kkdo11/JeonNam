package kopo.jeonnam.config;

import kopo.jeonnam.dto.csv.ProductDTO;
import kopo.jeonnam.service.csv.IProductService;
import kopo.jeonnam.util.CsvParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final IProductService productService;

    @Override
    public void run(String... args) throws Exception {
        log.info("▶ CSV 데이터 로딩 시작");

        if (productService.existsAny()) {
            log.info("▶ 이미 농축산물 데이터가 존재하여 저장을 건너뜁니다.");
            return;  // 이미 데이터 있으니 저장 안 함
        }

        // resources 폴더 내 CSV 파일 읽기
        ClassPathResource resource = new ClassPathResource("data/전라남도_지리적 표시 농축산물_20230808.csv");
        try (InputStream inputStream = resource.getInputStream()) {
            List<ProductDTO> productDTOList = CsvParserUtil.parseProducts(inputStream);

            if (!productDTOList.isEmpty()) {
                productService.saveAll(productDTOList);
                log.info("▶ 데이터 저장 완료, 총 {}개", productDTOList.size());
            } else {
                log.warn("▶ 파싱된 데이터가 없습니다!");
            }
        } catch (Exception e) {
            log.error("❌ CSV 로딩 또는 저장 중 에러 발생", e);
        }
    }
}
