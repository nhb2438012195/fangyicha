package com.fangyicha.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI消息DTO
 */
@Data
public class AiMessageDTO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String messageType;
    private Object metadata;
    private LocalDateTime createdTime;
}
