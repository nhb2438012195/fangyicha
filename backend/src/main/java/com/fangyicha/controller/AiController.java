package com.fangyicha.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangyicha.common.Result;
import com.fangyicha.dto.AiMessageDTO;
import com.fangyicha.dto.AiSessionDTO;
import com.fangyicha.dto.ChatRequest;
import com.fangyicha.dto.ChatResponse;
import com.fangyicha.entity.AiMessage;
import com.fangyicha.entity.AiSession;
import com.fangyicha.mapper.AiMessageMapper;
import com.fangyicha.mapper.AiSessionMapper;
import com.fangyicha.service.AiChatService;
import com.fangyicha.service.AiSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI助手控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI助手", description = "房易AI助手对话管理")
public class AiController {

    private final AiChatService aiChatService;
    private final AiSessionService aiSessionService;
    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public AiController(AiChatService aiChatService,
                         AiSessionService aiSessionService,
                         AiSessionMapper sessionMapper,
                         AiMessageMapper messageMapper,
                         ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.aiSessionService = aiSessionService;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送聊天消息
     */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "发送消息", description = "发送消息给AI助手，返回回复内容")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request, Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.badRequest("请输入问题");
        }

        try {
            ChatResponse response = aiChatService.chat(request.getSessionId(), request.getMessage(), customerId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天接口异常", e);
            return Result.serverError("AI 助手暂时不可用，请稍后重试");
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "创建会话", description = "创建新的AI对话会话")
    public Result<AiSession> createSession(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        AiSession session = aiSessionService.createSession(customerId, "新对话");
        return Result.success(session);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "会话列表", description = "获取当前用户的AI对话列表（按更新时间倒序，最多50条）")
    public Result<List<AiSessionDTO>> getSessions(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        List<AiSessionDTO> sessions = aiSessionService.getUserSessions(customerId);
        return Result.success(sessions);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "删除会话", description = "删除指定AI会话及其所有消息")
    public Result<Void> deleteSession(@PathVariable Long id, Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean deleted = aiSessionService.deleteSession(id, customerId);
        if (deleted) {
            return Result.success("会话已删除", null);
        }
        return Result.notFound("会话不存在");
    }

    /**
     * 获取会话消息列表（分页）
     */
    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "会话消息", description = "获取指定会话的消息列表（分页，每页50条）")
    public Result<List<AiMessageDTO>> getMessages(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();

        // Verify session belongs to user
        AiSession session = sessionMapper.selectById(id);
        if (session == null || !session.getCustomerId().equals(customerId)) {
            return Result.notFound("会话不存在");
        }

        Page<AiMessage> pageParam = new Page<>(page, 50);
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getSessionId, id);
        wrapper.orderByAsc(AiMessage::getCreatedTime);

        Page<AiMessage> messagePage = messageMapper.selectPage(pageParam, wrapper);

        List<AiMessageDTO> dtos = messagePage.getRecords().stream().map(msg -> {
            AiMessageDTO dto = new AiMessageDTO();
            dto.setId(msg.getId());
            dto.setSessionId(msg.getSessionId());
            dto.setRole(msg.getRole());
            dto.setContent(msg.getContent());
            dto.setMessageType(msg.getMessageType());
            dto.setCreatedTime(msg.getCreatedTime());
            // Parse metadata JSON
            if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
                try {
                    dto.setMetadata(objectMapper.readTree(msg.getMetadata()));
                } catch (Exception e) {
                    dto.setMetadata(msg.getMetadata());
                }
            }
            return dto;
        }).collect(Collectors.toList());

        return Result.success(dtos, messagePage.getTotal(), messagePage.getCurrent(), messagePage.getSize());
    }
}
