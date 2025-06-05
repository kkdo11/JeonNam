package kopo.jeonnam.dto.user;

import lombok.Builder;

/**
 * 이메일 전송을 위한 DTO
 */
@Builder
public record MailDTO(
    String toMail,
    String title,
    String contents
) {}
