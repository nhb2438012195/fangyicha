package com.fangyicha.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI会话列表DTO
 */
@Data
public class AiSessionDTO {
    private Long id;
    private String title;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Integer messageCount;
}
