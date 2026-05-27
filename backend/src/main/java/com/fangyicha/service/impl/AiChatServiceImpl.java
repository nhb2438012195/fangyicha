package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private static final String SYSTEM_PROMPT = "你是一个亲切友好的购房助手，名字叫房易小助手。你是房易查平台的AI助手，仅帮助用户查询房产信息、推荐楼盘、管理收藏和创建订单。用口语化的中文回答，温暖亲切，像朋友帮你参谋购房。回复要简短精炼，两三句话内说清重点，不要长篇大论。回答基于提供的知识库内容，如果知识库中没有相关信息，请告知用户无法回答。\n\n重要限制：你只回答与购房、房产相关的问题。如果用户询问与购房无关的内容（如天气、新闻、编程、数学题、闲聊等），请礼貌地告知你只能回答购房相关问题。";

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
    private final PropertyMapper propertyMapper;
    private final CustomerService customerService;
    private final Gson gson = new Gson();
    private static final Pattern PROPERTY_ID_PATTERN = Pattern.compile("property_(\\d+)\\.txt");

    public AiChatServiceImpl(OpenAiChatModel chatModel,
                              AiSessionMapper sessionMapper,
                              AiMessageMapper messageMapper,
                              AiSessionService sessionService,
                              RagService ragService,
                              FunctionCallService functionCallService,
                              PropertyMapper propertyMapper,
                              CustomerService customerService) {
        this.chatModel = chatModel;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.sessionService = sessionService;
        this.ragService = ragService;
        this.functionCallService = functionCallService;
        this.propertyMapper = propertyMapper;
        this.customerService = customerService;
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
            String userContext = buildUserContext(customerId);

            // Build function result context so AI knows what actually happened (not just conversation history)
            String functionContext = buildFunctionContext(functionCardData);
            String extraContext = userContext;
            if (functionContext != null) {
                extraContext = userContext + "\n\n" + functionContext;
            }

            String systemPrompt = buildSystemPrompt(null, extraContext);
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

                // Determine message type based on function type marker
                String messageType = "text";
                if (!functionCardData.isEmpty()) {
                    String type = (String) functionCardData.get("_type");
                    if ("favorites".equals(type)) {
                        messageType = "favorites";
                    } else if (functionCardData.containsKey("cards")) {
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
        List<RagService.RagResult> ragResults = ragService.search(userMessage);
        String systemPrompt = buildSystemPrompt(ragResults);
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

            // Post-process: extract property recommendation cards from RAG results
            Map<String, Object> cardData = null;
            if (isHousingRelated(userMessage)) {
                cardData = buildRecommendationCards(ragResults);
            }
            if (cardData != null) {
                return new ChatResponse(sessionId,
                    new ChatResponse.ReplyContent("recommendation", replyText, cardData));
            }

            return new ChatResponse(sessionId, new ChatResponse.ReplyContent("text", replyText, null));
        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            throw e;
        }
    }

    /**
     * 检测用户意图并执行对应的函数调用
     * 先检测查看收藏，再检测添加收藏，避免"帮我看看我的收藏"误匹配
     */
    private boolean detectAndExecuteFunction(String message, Long customerId,
                                              Long sessionId, Map<String, Object> cardData) {
        String msg = message.toLowerCase();

        // Check for view_favorites intent FIRST (more specific)
        if (msg.contains("收藏") &&
            (msg.contains("看看") || msg.contains("查看") || msg.contains("我的收藏"))) {
            Map<String, Object> args = new HashMap<>();
            FunctionCallService.FunctionResult result = functionCallService.execute("view_favorites", args, customerId);
            cardData.put("message", result.getMessage());
            cardData.put("_type", "favorites");
            if (result.isSuccess() && result.getData() != null) {
                cardData.put("cards", result.getData().get("cards"));
                cardData.put("total", result.getData().get("total"));
            }
            return true;
        }

        // Check for add_favorite intent (broader patterns to avoid silent failures)
        if (msg.contains("收藏") &&
            (msg.contains("帮我") || msg.contains("收藏这个") || msg.contains("收藏了")
             || msg.contains("收藏一下") || msg.contains("加入收藏") || msg.contains("收藏一个")
             || msg.contains("收藏该") || msg.startsWith("收藏"))) {
            String propertyName = extractPropertyName(message);
            if (propertyName != null) {
                Map<String, Object> args = new HashMap<>();
                args.put("propertyName", propertyName);
                FunctionCallService.FunctionResult result = functionCallService.execute("add_favorite", args, customerId);
                cardData.put("_type", "add_favorite");
                cardData.put("message", result.getMessage());
                return true;
            } else {
                // User said "收藏" but didn't specify which property
                cardData.put("_type", "add_favorite");
                cardData.put("message", "请告诉我具体要收藏哪个楼盘，比如「收藏招商·湘江府」");
                return true;
            }
        }

        // Check for confirm_order intent (before create_order_preview to avoid "确认下单" mis-match)
        if (msg.contains("确认") || msg.contains("确认订单") || msg.contains("提交") || msg.contains("是的")) {
            Map<String, Object> args = new HashMap<>();
            args.put("_sessionId", sessionId);
            FunctionCallService.FunctionResult result = functionCallService.execute("confirm_order", args, customerId);
            cardData.put("_type", "confirm_order");
            cardData.put("message", result.getMessage());
            if (result.isSuccess() && result.getData() != null) {
                cardData.putAll(result.getData());
            }
            return true;
        }

        // Check for create_order_preview intent (after confirm_order to avoid "确认下单" mis-match)
        if (msg.contains("买") || msg.contains("下单") || msg.contains("订购") || msg.contains("购买") || msg.contains("订单")) {
            String propertyName = extractPropertyName(message);
            if (propertyName != null) {
                Map<String, Object> args = new HashMap<>();
                args.put("propertyName", propertyName);
                args.put("_sessionId", sessionId);
                FunctionCallService.FunctionResult result = functionCallService.execute("create_order_preview", args, customerId);
                cardData.put("_type", "create_order_preview");
                cardData.put("message", result.getMessage());
                if (result.isSuccess() && result.getData() != null) {
                    cardData.putAll(result.getData());
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 从消息中提取楼盘名称
     */
    private String extractPropertyName(String message) {
        String[] stopWords = {"帮我", "收藏", "买", "看看", "查看", "下单", "购买", "订购", "一下", " ", "的", "了", "吧", "啊", "呢", "吗", "这个", "那个", "一个"};
        String clean = message;
        for (String word : stopWords) {
            clean = clean.replace(word, "");
        }
        clean = clean.trim();
        // Return null if result is too short or generic (not a real property name)
        if (clean.isEmpty() || clean.length() <= 1 ||
            clean.equals("楼盘") || clean.equals("房子") || clean.equals("房产")) {
            return null;
        }
        return clean;
    }

    /**
     * 构建用户上下文信息（登录状态、姓名等）
     */
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

    /**
     * 构建系统提示（含RAG上下文）
     */
    private String buildSystemPrompt(List<RagService.RagResult> ragResults, String extraContext) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

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

    /**
     * 构建系统提示（无额外上下文）
     */
    private String buildSystemPrompt(List<RagService.RagResult> ragResults) {
        return buildSystemPrompt(ragResults, null);
    }

    /**
     * 构建函数执行结果上下文，注入AI以便生成真实回复而非凭历史记录编造
     */
    private String buildFunctionContext(Map<String, Object> cardData) {
        if (cardData == null || cardData.isEmpty()) return null;

        StringBuilder ctx = new StringBuilder("系统操作反馈（请以此为依据回复，不要自行脑补）：\n");

        String type = (String) cardData.get("_type");
        String message = (String) cardData.get("message");

        if ("favorites".equals(type)) {
            Object cards = cardData.get("cards");
            if (cards instanceof List && !((List<?>) cards).isEmpty()) {
                List<?> cardList = (List<?>) cards;
                ctx.append("用户查看了收藏夹，共有 ").append(cardList.size()).append(" 个收藏楼盘。");
                ctx.append(" 实际收藏列表如下：\n");
                for (int i = 0; i < cardList.size(); i++) {
                    Object card = cardList.get(i);
                    if (card instanceof Map) {
                        String name = (String) ((Map<?, ?>) card).get("propertyName");
                        String loc = (String) ((Map<?, ?>) card).get("location");
                        ctx.append((i + 1) + ". ").append(name != null ? name : "未知")
                           .append(" - ").append(loc != null ? loc : "未知").append("\n");
                    }
                }
                ctx.append("请根据以上实际数据回答用户。不要编造任何不在列表中的楼盘。");
            } else {
                ctx.append("用户查看了收藏夹，但收藏夹为空（没有任何收藏记录）。请如实告知用户还没有收藏任何楼盘。");
            }
        } else if ("add_favorite".equals(type)) {
            if (message != null) {
                ctx.append(message);
            } else {
                ctx.append("收藏操作已完成。");
            }
        } else if ("create_order_preview".equals(type)) {
            if (message != null) {
                ctx.append(message).append("。");
            }
            String propertyName = (String) cardData.get("propertyName");
            if (propertyName != null) {
                String customerName = (String) cardData.get("customerName");
                String customerPhone = (String) cardData.get("customerPhone");
                ctx.append(" 楼盘：").append(propertyName);
                ctx.append(" 系统已自动使用用户资料中的姓名和手机号生成订单预览，无需再向用户询问。请直接告知用户订单信息并请用户确认。");
            } else {
                ctx.append(" 创建订单预览失败，请如实告知用户失败原因。");
            }
        } else if ("confirm_order".equals(type)) {
            if (message != null) {
                ctx.append(message).append("。");
            }
            String orderNo = (String) cardData.get("orderNo");
            if (orderNo != null) {
                ctx.append(" 订单号：").append(orderNo).append("。");
                ctx.append(" 订单已确认成功，请告知用户。");
            } else {
                ctx.append(" 订单确认失败，请如实告知用户失败原因。");
            }
        } else {
            return null;
        }

        return ctx.toString();
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
     * 粗略判断用户问题是否与购房相关，避免在无关问题上展示推荐卡片
     */
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

    /**
     * 从RAG结果中提取楼盘信息并构建推荐卡片数据
     */
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
            // Use RAG content snippet as recommendation reason
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
