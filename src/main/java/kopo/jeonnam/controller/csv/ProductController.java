package kopo.jeonnam.controller.csv;

import kopo.jeonnam.dto.csv.ProductDTO;
import kopo.jeonnam.service.csv.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.util.List;

/**
 * 🌿 ProductController
 * - JSON API 및 Thymeleaf View 제공
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    /**
     * 🖥️ View 페이지 반환 (Thymeleaf)
     */
    @GetMapping("/products/page")
    public String viewPage() {
        return "csv/product"; // templates/products.html
    }

    /**
     * 📡 제품 리스트 API (JSON)
     * - 전체 조회 또는 지역 + 검색어 필터링
     */
    @ResponseBody
    @GetMapping("/products")
    public List<ProductDTO> getProducts(
            @RequestParam(required = false) String area,
            @RequestParam(required = false, name = "search") String keyword
    ) {
        log.info("📥 GET /products 요청 - area: '{}', keyword: '{}'", area, keyword);
        return productService.findByAreaAndKeyword(area, keyword);
    }

    /**
     * 🧪 데이터 있는지 여부 체크 (선택)
     */
    @ResponseBody
    @GetMapping("/products/exists")
    public boolean hasAnyData() {
        return productService.existsAny();
    }
}
