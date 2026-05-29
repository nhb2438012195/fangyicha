package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.ai.tool.ToolResultHolder;
import com.fangyicha.dto.ChatResponse;
import com.fangyicha.entity.AiMessage;
import com.fangyicha.entity.AiSession;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.AiMessageMapper;
import com.fangyicha.mapper.AiSessionMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.AiChatService;
import com.fangyicha.service.AiSessionService;
import com.fangyicha.service.CustomerService;
import com.fangyicha.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI聊天服务实现 — 核心对话逻辑
 * 使用 Spring AI ChatClient + DeepSeek 原生 Tool Calling 实现函数调用
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @org.springframework.beans.factory.annotation.Value("${ai.system-prompt:}")
    private String customSystemPrompt;

    private String getSystemPrompt() {
        if (customSystemPrompt != null && !customSystemPrompt.isEmpty()) {
            return customSystemPrompt;
        }
        return "你是一个亲切友好的购房助手，名字叫房易小助手。你是房易查平台的AI助手，仅帮助用户查询房产信息、推荐楼盘、管理收藏和创建订单。用口语化的中文回答，温暖亲切，像朋友帮你参谋购房。回复要简短精炼，两三句话内说清重点，不要长篇大论。回答基于提供的知识库内容，如果知识库中没有相关信息，请告知用户无法回答。\n\n重要限制：你只回答与购房、房产相关的问题。如果用户询问与购房无关的内容（如天气、新闻、编程、数学题、闲聊等），请礼貌地告知你只能回答购房相关问题。";
    }

    private static final int MAX_CONVERSATION_ROUNDS = 10;
    private static final int RAG_CONTEXT_LIMIT = 2000;
    private static final int MAX_MESSAGES_PER_SESSION = 500;

    private static final Pattern PROPERTY_ID_PATTERN = Pattern.compile("property_(\\d+)\\.txt");

    private final ChatClient chatClient;
    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiSessionService sessionService;
    private final RagService ragService;
    private final PropertyMapper propertyMapper;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiChatServiceImpl(DeepSeekChatModel chatModel,
                              AiSessionMapper sessionMapper,
                              AiMessageMapper messageMapper,
                              AiSessionService sessionService,
                              RagService ragService,
                              PropertyMapper propertyMapper,
                              CustomerService customerService,
                              List<ToolCallback> toolCallbacks) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.sessionService = sessionService;
        this.ragService = ragService;
        this.propertyMapper = propertyMapper;
        this.customerService = customerService;

        this.chatClient = ChatClient.builder(chatModel)
            .defaultToolCallbacks(toolCallbacks)
            .build();
    }

    @Override
    @Transactional
    public ChatResponse chat(Long sessionId, String message, Long customerId) {
        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse(sessionId, new ChatResponse.ReplyContent("text", "请输入问题", null));
        }

        message = message.trim();

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

        LambdaQueryWrapper<AiMessage> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(AiMessage::getSessionId, sessionId);
        if (messageMapper.selectCount(countWrapper) >= MAX_MESSAGES_PER_SESSION) {
            return new ChatResponse(sessionId,
                new ChatResponse.ReplyContent("text", "对话已达到500条上限，请创建新对话继续咨询", null));
        }

        Long finalSessionId = sessionId;

        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setMessageType("text");
        messageMapper.insert(userMsg);

        List<AiMessage> history = loadConversationHistory(sessionId);
        List<RagService.RagResult> ragResults = ragService.search(message);

        try {
            ChatResponse result = executeChat(finalSessionId, history, message, customerId, ragResults);

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

    private ChatResponse executeChat(Long sessionId, List<AiMessage> history,
                                      String userMessage, Long customerId,
                                      List<RagService.RagResult> ragResults) {
        ToolResultHolder.clear();
        com.fangyicha.ai.tool.SessionIdHolder.set(sessionId);
        try {
            String userContext = buildUserContext(customerId);
            String systemPrompt = buildSystemPrompt(ragResults, userContext);

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

            org.springframework.ai.chat.model.ChatResponse response =
                chatClient.prompt().messages(messages.toArray(new Message[0])).call().chatResponse();

            String replyText = response.getResult().getOutput().getText();
            Map<String, Object> cardData = ToolResultHolder.get();

            if (cardData != null && !cardData.isEmpty()) {
                return new ChatResponse(sessionId,
                    new ChatResponse.ReplyContent(determineMessageType(cardData), replyText, cardData));
            }

            Map<String, Object> recCards = null;
            if (isHousingRelated(userMessage)) {
                recCards = buildRecommendationCards(ragResults);
            }
            if (recCards != null) {
                return new ChatResponse(sessionId,
                    new ChatResponse.ReplyContent("recommendation", replyText, recCards));
            }

            return new ChatResponse(sessionId,
                new ChatResponse.ReplyContent("text", replyText, null));

        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            throw e;
        } finally {
            ToolResultHolder.clear();
            com.fangyicha.ai.tool.SessionIdHolder.clear();
        }
    }

    private String determineMessageType(Map<String, Object> cardData) {
        String type = (String) cardData.get("_type");
        if ("favorites".equals(type)) return "favorites";
        if (cardData.containsKey("orderNo")) return "order_summary";
        if (cardData.containsKey("cards")) return "recommendation";
        return "text";
    }

    private String buildUserContext(Long customerId) {
        if (customerId == null) return "";
        try {
            com.fangyicha.entity.Customer customer = customerService.getById(customerId);
            if (customer != null) {
                return "当前用户已登录，用户名为" + customer.getRealName()
                    + "，手机号为" + (customer.getPhone() != null ? customer.getPhone() : "未设置")
                    + "。请勿询问用户的登录信息、用户名或手机号，系统已自动识别用户身份。下单时直接使用以上信息，无需用户提供。";
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: customerId={}", customerId, e);
        }
        return "当前用户已登录，请勿询问用户的登录信息、用户名或手机号，系统已自动识别用户身份。";
    }

    private String buildSystemPrompt(List<RagService.RagResult> ragResults, String extraContext) {
        StringBuilder sb = new StringBuilder(getSystemPrompt());

        if (extraContext != null && !extraContext.isEmpty()) {
            sb.append("\n\n").append(extraContext);
        }

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

    private boolean isHousingRelated(String message) {
        String[] keywords = {"房", "楼", "买", "卖", "租", "价格", "户型", "面积", "装修",
            "小区", "收藏", "订单", "下单", "订购", "购买", "首付", "贷款", "物业", "绿化",
            "学区", "地铁", "交通", "周边", "配套", "优惠", "折扣", "看房", "购房", "买房",
            "推荐", "株洲", "广州", "成都", "长沙", "深圳", "北京", "上海", "杭州", "武汉",
            "三室", "两厅", "一室", "四室", "五室", "复式", "平层", "公寓", "别墅",
            "商铺", "写字楼", "住宅", "地段", "位置", "开发商", "预算", "房价"};
        String msg = message.toLowerCase();
        for (String kw : keywords) {
            if (msg.contains(kw)) return true;
        }
        return false;
    }

    private Map<String, Object> buildRecommendationCards(List<RagService.RagResult> ragResults) {
        if (ragResults == null || ragResults.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (RagService.RagResult result : ragResults) {
            Matcher matcher = PROPERTY_ID_PATTERN.matcher(result.getId());
            if (!matcher.find()) continue;

            Long propertyId = Long.parseLong(matcher.group(1));
            if (seenIds.contains(propertyId)) continue;
            seenIds.add(propertyId);

            Property property = propertyMapper.selectById(propertyId);
            if (property == null) continue;

            Map<String, Object> card = new HashMap<>();
            card.put("propertyId", property.getId());
            card.put("propertyName", property.getPropertyName());
            card.put("location", property.getLocation());
            card.put("floorPlanType", property.getFloorPlanType());
            card.put("areaSqm", property.getAreaSqm());
            card.put("totalPrice", property.getTotalPrice());
            card.put("pricePerSqm", property.getPricePerSqm());
            card.put("imageUrl", property.getImageUrls() != null ? property.getImageUrls().split(",")[0].trim() : null);
            String reason = result.getContent();
            if (reason != null && reason.length() > 60) {
                reason = reason.substring(0, 60) + "...";
            }
            card.put("reason", reason);
            card.put("developerName", null);

            cards.add(card);
        }

        if (cards.isEmpty()) return null;

        Map<String, Object> data = new HashMap<>();
        data.put("cards", cards);
        data.put("total", cards.size());
        return data;
    }

    private List<AiMessage> loadConversationHistory(Long sessionId) {
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getSessionId, sessionId);
        wrapper.orderByDesc(AiMessage::getCreatedTime);
        wrapper.last("LIMIT " + (MAX_CONVERSATION_ROUNDS * 2));
        List<AiMessage> messages = messageMapper.selectList(wrapper);
        Collections.reverse(messages);
        return messages;
    }

    private void saveAssistantMessage(Long sessionId, ChatResponse.ReplyContent reply) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(reply.getText());
        msg.setMessageType(reply.getType());
        if (reply.getData() != null) {
            try {
                msg.setMetadata(objectMapper.writeValueAsString(reply.getData()));
            } catch (Exception e) {
                log.warn("序列化metadata失败", e);
            }
        }
        messageMapper.insert(msg);
    }
}
