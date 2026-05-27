package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.dto.AiSessionDTO;
import com.fangyicha.entity.AiSession;

import java.util.List;

/**
 * AI会话服务接口
 */
public interface AiSessionService extends IService<AiSession> {

    /**
     * 创建新会话（自动根据首条消息生成标题）
     */
    AiSession createSession(Long customerId, String firstMessage);

    /**
     * 获取用户的会话列表（按updated_time倒序，最多50条）
     */
    List<AiSessionDTO> getUserSessions(Long customerId);

    /**
     * 删除会话（级联删除消息）
     */
    boolean deleteSession(Long sessionId, Long customerId);

    /**
     * 获取会话的标题（自动生成：取首条消息前30字）
     */
    String generateTitle(String message);
}
