// 파일: PlaceInfoDTO.java
package kopo.jeonnam.dto.gpt;

public record PlaceInfoDTO(
        String name,
        String addr  // 이 필드가 반드시 있어야 함
) {}
