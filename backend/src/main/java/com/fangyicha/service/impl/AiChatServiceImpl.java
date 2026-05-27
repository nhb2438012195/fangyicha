package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.dto.ChatResponse;
import com.fangyicha.entity.AiMessage;
import com.fangyicha.entity.AiSession;
import com.fangyicha.mapper.AiMessageMapper;
import com.fangyicha.mapper.AiSessionMapper;
import com.fangyicha.service.AiChatService;
import com.fangyicha.service.AiSessionService;
import com.fangyicha.service.FunctionCallService;
import com.fangyicha.service.RagService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI聊天服务实现 — 核心对话逻辑
 * 使用文本模式匹配进行函数调用触发
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String SYSTEM_PROMPT = "你是一个亲切友好的购房助手，名字叫房易小助手。你是房易查平台的AI助手，帮助用户查询房产信息、推荐楼盘、管理收藏和创建订单。用口语化的中文回答，温暖亲切，像朋友帮你参谋购房。回答基于提供的知识库内容，如果知识库中没有相关信息，请告知用户无法回答。";

    private static final int MAX_CONVERSATION_ROUNDS = 3;
    private static final int MAX_FUNCTION_ITERATIONS = 5;
    private static final int RAG_CONTEXT_LIMIT = 2000;
    private static final int MAX_MESSAGES_PER_SESSION = 500;

    private final OpenAiChatModel chatModel;
    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiSessionService sessionService;
    private final RagService ragService;
    private final FunctionCallService functionCallService;
    private final Gson gson = new Gson();

    public AiChatServiceImpl(OpenAiChatModel chatModel,
                              AiSessionMapper sessionMapper,
                              AiMessageMapper messageMapper,
                              AiSessionService sessionService,
                              RagService ragService,
                              FunctionCallService functionCallService) {
        this.chatModel = chatModel;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.sessionService = sessionService;
        this.ragService = ragService;
        this.functionCallService = functionCallService;
    }

    @Override
    @Transactional
    public ChatResponse chat(Long sessionId, String message, Long customerId) {
        if (message == null || message.trim().isEmpty()) {
            ChatResponse.ReplyContent reply = new ChatResponse.ReplyContent("text", "请输入问题", null);
            return new ChatResponse(sessionId, reply);
        }

        message = message.trim();

        // Find or create session
        AiSession session;
        if (sessionId == null) {
            session = sessionService.createSession(customerId, message);
            sessionId = session.getId();
        } else {
            session = sessionMapper.selectById(sessionId);
            if (session == null || !session.getCustomerId().equals(customerId)) {
                session = sessionService.createSession(customerId, message);
                sessionId = session.getId();
            }
        }

        // Check message limit
        LambdaQueryWrapper<AiMessage> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(AiMessage::getSessionId, sessionId);
        if (messageMapper.selectCount(countWrapper) >= MAX_MESSAGES_PER_SESSION) {
            ChatResponse.ReplyContent reply = new ChatResponse.ReplyContent("text", "对话已达到500条上限，请创建新对话继续咨询", null);
            return new ChatResponse(sessionId, reply);
        }

        Long finalSessionId = sessionId;

        // Save user message
        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setMessageType("text");
        messageMapper.insert(userMsg);

        // Load conversation history
        List<AiMessage> history = loadConversationHistory(sessionId);

        // RAG search
        List<RagService.RagResult> ragResults = ragService.search(message);

        try {
            ChatResponse result = executeChat(finalSessionId, history, message, customerId);

            if (result != null && result.getReply() != null) {
                saveAssistantMessage(finalSessionId, result.getReply());
            }

            session.setUpdatedTime(LocalDateTime.now());
            sessionMapper.updateById(session);

            result.setSessionId(finalSessionId);
            return result;

        } catch (Exception e) {
            log.error("AI聊天调用失败: sessionId={}", sessionId, e);

            ChatResponse.ReplyContent errorReply = new ChatResponse.ReplyContent("text", "AI 助手暂时不可用，请稍后重试", null);
            saveAssistantMessage(finalSessionId, errorReply);

            return new ChatResponse(finalSessionId, errorReply);
        }
    }

    /**
     * 执行聊天API调用（两阶段：先调用DeepSeek，再处理函数调用）
     */
    private ChatResponse executeChat(Long sessionId, List<AiMessage> history,
                                      String userMessage, Long customerId) {
        // First, check if the user message contains function-calling intent
        // This avoids relying on DeepSeek's native function calling which may have API compatibility issues
        Map<String, Object> functionCardData = new HashMap<>();
        boolean functionHandled = detectAndExecuteFunction(userMessage, customerId, sessionId, functionCardData);

        if (functionHandled) {
            // If function was handled, generate a conversational reply via DeepSeek
            String systemPrompt = buildSystemPrompt(null);
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            // Add history
            for (AiMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
            messages.add(new UserMessage(userMessage));

            try {
                Prompt prompt = new Prompt(messages);
                org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
                String replyText = response.getResult().getOutput().getText();

                String messageType = "text";
                if (!functionCardData.isEmpty()) {
                    if (functionCardData.containsKey("cards")) {
                        messageType = "recommendation";
                    } else if (functionCardData.containsKey("orderNo")) {
                        messageType = "order_summary";
                    }
                }

                return new ChatResponse(sessionId,
                    new ChatResponse.ReplyContent(messageType, replyText, functionCardData.isEmpty() ? null : functionCardData));
            } catch (Exception e) {
                log.error("生成回复失败", e);
                String msg = functionCardData.containsKey("message") ?
                    (String) functionCardData.get("message") : "操作已完成";
                return new ChatResponse(sessionId,
                    new ChatResponse.ReplyContent("text", msg, null));
            }
        }

        // Regular chat without function calling
        String systemPrompt = buildSystemPrompt(ragService.search(userMessage));
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for (AiMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(userMessage));

        try {
            Prompt prompt = new Prompt(messages);
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            String replyText = response.getResult().getOutput().getText();
            return new ChatResponse(sessionId, new ChatResponse.ReplyContent("text", replyText, null));
        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            throw e;
        }
    }

    /**
     * 检测用户意图并执行对应的函数调用
     */
    private boolean detectAndExecuteFunction(String message, Long customerId,
                                              Long sessionId, Map<String, Object> cardData) {
        String msg = message.toLowerCase();

        // Check for add_favorite intent
        if (msg.contains("收藏") && (msg.contains("帮我") || msg.contains("帮我看看") || msg.contains("收藏这个"))) {
            String propertyName = extractPropertyName(message, "收藏");
            if (propertyName != null) {
                Map<String, Object> args = new HashMap<>();
                args.put("propertyName", propertyName);
                FunctionCallService.FunctionResult result = functionCallService.execute("add_favorite", args, customerId);
                cardData.put("message", result.getMessage());
                return true;
            }
        }

        // Check for view_favorites intent
        if (msg.contains("收藏") && (msg.contains("看看") || msg.contains("查看") || msg.contains("我的收藏"))) {
            Map<String, Object> args = new HashMap<>();
            FunctionCallService.FunctionResult result = functionCallService.execute("view_favorites", args, customerId);
            cardData.put("message", result.getMessage());
            if (result.isSuccess() && result.getData() != null) {
                cardData.put("cards", result.getData().get("cards"));
                cardData.put("total", result.getData().get("total"));
            }
            return true;
        }

        // Check for create_order_preview intent
        if ((msg.contains("买") || msg.contains("下单") || msg.contains("订购") || msg.contains("购买"))) {
            // Extract property info from message
            String propertyName = extractPropertyName(message, "买");
            if (propertyName != null) {
                Map<String, Object> args = new HashMap<>();
                args.put("propertyName", propertyName);
                args.put("_sessionId", sessionId);
                FunctionCallService.FunctionResult result = functionCallService.execute("create_order_preview", args, customerId);
                cardData.put("message", result.getMessage());
                if (result.isSuccess() && result.getData() != null) {
                    cardData.putAll(result.getData());
                }
                return true;
            }
        }

        // Check for confirm_order intent
        if (msg.contains("确认") || msg.contains("确认订单") || msg.contains("提交") || msg.contains("是的")) {
            Map<String, Object> args = new HashMap<>();
            args.put("_sessionId", sessionId);
            FunctionCallService.FunctionResult result = functionCallService.execute("confirm_order", args, customerId);
            cardData.put("message", result.getMessage());
            if (result.isSuccess() && result.getData() != null) {
                cardData.put("orderNo", result.getData().get("orderNo"));
                cardData.put("orderId", result.getData().get("orderId"));
            }
            return true;
        }

        return false;
    }

    /**
     * 从消息中提取楼盘名称
     */
    private String extractPropertyName(String message, String context) {
        // Simple extraction: remove common words and return what remains
        String[] stopWords = {"帮我", "收藏", "买", "看看", "查看", "下单", "购买", "订购", "一下", " ", "的", "了", "吧", "啊", "呢", "吗"};
        String clean = message;
        for (String word : stopWords) {
            clean = clean.replace(word, "");
        }
        clean = clean.trim();
        return clean.isEmpty() ? null : clean;
    }

    /**
     * 构建系统提示（含RAG上下文）
     */
    private String buildSystemPrompt(List<RagService.RagResult> ragResults) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        if (ragResults != null && !ragResults.isEmpty()) {
            sb.append("\n\n以下是与用户问题相关的知识库内容，请基于这些内容回答。如果知识库内容不足以回答，请告知用户。\n");
            int totalChars = 0;
            for (RagService.RagResult result : ragResults) {
                String prefix = "[来源: " + result.getId() + "] ";
                String entry = prefix + result.getContent() + "\n";
                if (totalChars + entry.length() > RAG_CONTEXT_LIMIT) break;
                sb.append(entry);
                totalChars += entry.length();
            }
        }

        return sb.toString();
    }

    /**
     * 加载会话历史（最近3轮）
     */
    private List<AiMessage> loadConversationHistory(Long sessionId) {
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getSessionId, sessionId);
        wrapper.orderByDesc(AiMessage::getCreatedTime);
        wrapper.last("LIMIT " + (MAX_CONVERSATION_ROUNDS * 2));
        List<AiMessage> messages = messageMapper.selectList(wrapper);
        Collections.reverse(messages);
        return messages;
    }

    /**
     * 保存助手消息
     */
    private void saveAssistantMessage(Long sessionId, ChatResponse.ReplyContent reply) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(reply.getText());
        msg.setMessageType(reply.getType());
        if (reply.getData() != null) {
            msg.setMetadata(gson.toJson(reply.getData()));
        }
        messageMapper.insert(msg);
    }
}
