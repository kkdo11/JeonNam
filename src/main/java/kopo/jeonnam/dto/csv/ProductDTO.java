package kopo.jeonnam.dto.csv;

import lombok.Builder;

/**
 * 📦 ProductDTO - 프론트와 통신할 때 사용하는 데이터 전송 객체
 */
@Builder
public record ProductDTO(
        String proId,       // product id
        String proRegNo,    // product 등록번호
        String proName,     // product 이름
        String proRegDate,  // 등록일자
        String proArea,     // 지역 (region)
        String proPlanQty,  // 생산 계획량
        String proCompany,  // 업체명
        String proBaseDate,  // 기준일자

        String proFeature,
        String proBenefit,
        String imageUrl
) {}
