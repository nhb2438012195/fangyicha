package com.fangyicha.service;

import com.fangyicha.dto.ChatResponse;

/**
 * AI聊天服务接口
 */
public interface AiChatService {

    /**
     * 处理用户聊天消息
     *
     * @param sessionId  会话ID（null表示新会话）
     * @param message    用户消息
     * @param customerId 客户ID
     * @return 聊天响应（包含回复文本和结构化数据）
     */
    ChatResponse chat(Long sessionId, String message, Long customerId);
}
