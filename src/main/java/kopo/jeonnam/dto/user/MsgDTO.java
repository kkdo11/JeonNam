package kopo.jeonnam.dto.user;

import lombok.Builder;

/**
 * 메시지 응답을 위한 DTO
 */
@Builder
public record MsgDTO(
    int result,
    String msg
) {}
