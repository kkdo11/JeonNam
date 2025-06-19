package kopo.jeonnam.model;

import kopo.jeonnam.dto.csv.ProductDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 🌱 Product 모델 - MongoDB의 'product' 컬렉션과 매핑됨
 * @Value + @Builder 사용하여 불변 객체로 정의
 */
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "product")
public class Product {

    @Id
    @Builder.Default
    private final ObjectId proId = new ObjectId();  // 기본값 넣어야 함

    private final String proRegNo;
    private final String proName;
    private final String proRegDate;
    private final String proArea;
    private final String proPlanQty;
    private final String proCompany;
    private final String proBaseDate;

    private String proFeature;
    private String proBenefit;
    private String imageUrl;

    /**
     * DTO → Entity 변환
     */
    public static Product fromDTO(ProductDTO dto) {
        return Product.builder()
                .proId(dto.proId() != null ? new ObjectId(dto.proId()) : new ObjectId())
                .proRegNo(dto.proRegNo())
                .proName(dto.proName())
                .proRegDate(dto.proRegDate())
                .proArea(dto.proArea())
                .proPlanQty(dto.proPlanQty())
                .proCompany(dto.proCompany())
                .proBaseDate(dto.proBaseDate())
                .proFeature(dto.proFeature())
                .proBenefit(dto.proBenefit())
                .imageUrl(dto.imageUrl())
                .build();
    }

    /**
     * Entity → DTO 변환
     */
    public ProductDTO toDTO() {
        return ProductDTO.builder()
                .proId(this.proId.toHexString())
                .proRegNo(this.proRegNo)
                .proName(this.proName)
                .proRegDate(this.proRegDate)
                .proArea(this.proArea)
                .proPlanQty(this.proPlanQty)
                .proCompany(this.proCompany)
                .proBaseDate(this.proBaseDate)
                .proFeature(this.proFeature)
                .proBenefit(this.proBenefit)
                .imageUrl(this.imageUrl)
                .build();
    }
}
