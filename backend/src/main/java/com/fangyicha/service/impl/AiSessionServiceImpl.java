package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.dto.AiSessionDTO;
import com.fangyicha.entity.AiMessage;
import com.fangyicha.entity.AiSession;
import com.fangyicha.mapper.AiMessageMapper;
import com.fangyicha.mapper.AiSessionMapper;
import com.fangyicha.service.AiSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI会话服务实现
 */
@Service
public class AiSessionServiceImpl extends ServiceImpl<AiSessionMapper, AiSession> implements AiSessionService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;

    public AiSessionServiceImpl(AiSessionMapper sessionMapper, AiMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public AiSession createSession(Long customerId, String firstMessage) {
        AiSession session = new AiSession();
        session.setCustomerId(customerId);
        session.setTitle(generateTitle(firstMessage));
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public List<AiSessionDTO> getUserSessions(Long customerId) {
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getCustomerId, customerId);
        wrapper.orderByDesc(AiSession::getUpdatedTime);
        wrapper.last("LIMIT 50");

        List<AiSession> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().map(s -> {
            AiSessionDTO dto = new AiSessionDTO();
            dto.setId(s.getId());
            dto.setTitle(s.getTitle());
            dto.setCreatedTime(s.getCreatedTime());
            dto.setUpdatedTime(s.getUpdatedTime());
            // Count messages
            LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(AiMessage::getSessionId, s.getId());
            dto.setMessageCount(Math.toIntExact(messageMapper.selectCount(msgWrapper)));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean deleteSession(Long sessionId, Long customerId) {
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getId, sessionId)
               .eq(AiSession::getCustomerId, customerId);
        AiSession session = sessionMapper.selectOne(wrapper);
        if (session == null) {
            return false;
        }
        // Cascade delete messages
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(AiMessage::getSessionId, sessionId);
        messageMapper.delete(msgWrapper);
        // Delete session
        sessionMapper.deleteById(sessionId);
        return true;
    }

    @Override
    public String generateTitle(String message) {
        if (message == null || message.isEmpty()) {
            return "新对话";
        }
        String clean = message.replaceAll("\\s+", " ").trim();
        if (clean.length() <= 30) {
            return clean;
        }
        return clean.substring(0, 27) + "...";
    }
}
