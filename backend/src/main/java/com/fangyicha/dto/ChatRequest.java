package com.fangyicha.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI聊天请求
 */
@Data
public class ChatRequest {
    private Long sessionId;
    private String message;
}
