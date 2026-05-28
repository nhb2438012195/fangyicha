package com.fangyicha.service.impl;

import com.fangyicha.entity.AiSession;
import com.fangyicha.entity.Customer;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.AiMessageMapper;
import com.fangyicha.mapper.AiSessionMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock private DeepSeekChatModel deepSeekChatModel;
    @Mock private AiSessionMapper sessionMapper;
    @Mock private AiMessageMapper messageMapper;
    @Mock private AiSessionService sessionService;
    @Mock private RagService ragService;
    @Mock private PropertyMapper propertyMapper;
    @Mock private CustomerService customerService;
    @Mock private ChatClient chatClient;

    private AiChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiChatServiceImpl(deepSeekChatModel, sessionMapper, messageMapper,
            sessionService, ragService, propertyMapper, customerService, List.of());
        ReflectionTestUtils.setField(service, "chatClient", chatClient);
    }

    @Test
    void testChatWithEmptyMessage() {
        var response = service.chat(null, "", 100L);
        assertEquals("请输入问题", response.getReply().getText());
    }

    @Test
    void testChatWithNullMessage() {
        var response = service.chat(null, null, 100L);
        assertEquals("请输入问题", response.getReply().getText());
    }

    @Test
    void testChatNewSession() throws Exception {
        AiSession session = new AiSession();
        session.setId(1L);
        session.setCustomerId(100L);

        when(sessionService.createSession(100L, "你好")).thenReturn(session);
        when(messageMapper.selectCount(any())).thenReturn(0L);
        when(ragService.search("你好")).thenReturn(Collections.emptyList());
        when(customerService.getById(100L)).thenReturn(null);

        var callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        var requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        var chatResponse = mock(org.springframework.ai.chat.model.ChatResponse.class);
        var generation = mock(org.springframework.ai.chat.model.Generation.class);
        var output = mock(org.springframework.ai.chat.messages.AssistantMessage.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(any(org.springframework.ai.chat.messages.Message[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn("你好！有什么可以帮你的？");

        var response = service.chat(null, "你好", 100L);

        assertNotNull(response);
        assertEquals("你好！有什么可以帮你的？", response.getReply().getText());
        verify(sessionService).createSession(100L, "你好");
    }

    @Test
    void testChatMessageLimitReached() {
        when(messageMapper.selectCount(any())).thenReturn(500L);

        AiSession session = new AiSession();
        session.setId(1L);
        session.setCustomerId(100L);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        var response = service.chat(1L, "新消息", 100L);

        assertTrue(response.getReply().getText().contains("500条上限"));
    }

    @Test
    void testIsHousingRelated() throws Exception {
        Method method = AiChatServiceImpl.class.getDeclaredMethod("isHousingRelated", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(service, "推荐几个株洲的楼盘"));
        assertTrue((boolean) method.invoke(service, "三室两厅有什么选择"));
        assertTrue((boolean) method.invoke(service, "帮我收藏这个楼盘"));
        assertFalse((boolean) method.invoke(service, "今天天气怎么样"));
        assertFalse((boolean) method.invoke(service, "帮我写一段代码"));
    }

    @Test
    void testBuildUserContextWithCustomer() throws Exception {
        Method method = AiChatServiceImpl.class.getDeclaredMethod("buildUserContext", Long.class);
        method.setAccessible(true);

        Customer customer = new Customer();
        customer.setRealName("张三");
        customer.setPhone("13800138000");
        when(customerService.getById(100L)).thenReturn(customer);

        String result = (String) method.invoke(service, 100L);
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("13800138000"));
    }

    @Test
    void testBuildUserContextNoCustomer() throws Exception {
        Method method = AiChatServiceImpl.class.getDeclaredMethod("buildUserContext", Long.class);
        method.setAccessible(true);

        when(customerService.getById(100L)).thenReturn(null);
        String result = (String) method.invoke(service, 100L);
        assertTrue(result.contains("当前用户已登录"));
    }

    @Test
    void testBuildRecommendationCards() throws Exception {
        Method method = AiChatServiceImpl.class.getDeclaredMethod("buildRecommendationCards", List.class);
        method.setAccessible(true);

        RagService.RagResult ragResult = new RagService.RagResult(
            "property_42.txt", "三室两厅 精装修", "platform", 0.85f);
        Property property = new Property();
        property.setId(42L);
        property.setPropertyName("测试楼盘");
        property.setLocation("株洲天元区");
        property.setFloorPlanType("三室两厅");
        property.setAreaSqm(new BigDecimal("120"));
        property.setTotalPrice(new BigDecimal("86400000"));
        property.setPricePerSqm(new BigDecimal("7200"));

        when(propertyMapper.selectById(42L)).thenReturn(property);

        @SuppressWarnings("unchecked")
        Map<String, Object> cards = (Map<String, Object>) method.invoke(service, List.of(ragResult));

        assertNotNull(cards);
        assertEquals(1, cards.get("total"));
    }

    @Test
    void testBuildRecommendationCardsEmpty() throws Exception {
        Method method = findMethod("buildRecommendationCards");
        method.setAccessible(true);

        assertNull(method.invoke(service, (Object) null));
        assertNull(method.invoke(service, Collections.emptyList()));
    }

    private Method findMethod(String name) {
        for (Method m : AiChatServiceImpl.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    @Test
    void testDetermineMessageType() throws Exception {
        Method method = AiChatServiceImpl.class.getDeclaredMethod("determineMessageType", Map.class);
        method.setAccessible(true);

        assertEquals("favorites", method.invoke(service, Map.of("_type", "favorites")));
        assertEquals("order_summary", method.invoke(service, Map.of("orderNo", "ORD-001")));
        assertEquals("recommendation", method.invoke(service, Map.of("cards", List.of())));
        assertEquals("text", method.invoke(service, Map.of()));
    }
}
