package com.fangyicha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long sessionId;
    private ReplyContent reply;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyContent {
        private String type;
        private String text;
        private Map<String, Object> data;
    }
}
