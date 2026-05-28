package com.fangyicha.controller;

import com.fangyicha.dto.AiMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the Gson->Jackson metadata serialization fix (N1).
 * Verifies AiMessageDTO with Jackson JsonNode metadata serializes correctly.
 */
class AiControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testMessageDToWithoutMetadata() {
        AiMessageDTO dto = new AiMessageDTO();
        dto.setId(1L);
        dto.setSessionId(10L);
        dto.setRole("user");
        dto.setContent("hello");
        dto.setMessageType("text");
        dto.setMetadata(null);

        assertEquals("user", dto.getRole());
        assertEquals("hello", dto.getContent());
        assertNull(dto.getMetadata());
    }

    @Test
    void testMessageDToWithRecommendationMetadata() throws Exception {
        AiMessageDTO dto = new AiMessageDTO();
        dto.setId(1L);
        dto.setSessionId(10L);
        dto.setRole("assistant");
        dto.setContent("推荐结果");
        dto.setMessageType("recommendation");

        String metadataJson = "{\"cards\":[{\"propertyName\":\"测试楼盘\"}],\"total\":1}";
        dto.setMetadata(mapper.readTree(metadataJson));

        assertNotNull(dto.getMetadata());

        String serialized = mapper.writeValueAsString(dto);
        assertTrue(serialized.contains("cards"));
        assertTrue(serialized.contains("测试楼盘"));
    }

    @Test
    void testMessageDToWithOrderMetadata() throws Exception {
        AiMessageDTO dto = new AiMessageDTO();
        dto.setId(2L);
        dto.setSessionId(10L);
        dto.setRole("assistant");
        dto.setContent("订单确认");
        dto.setMessageType("order_summary");

        String metadataJson = "{\"orderNo\":\"ORD-001\",\"propertyName\":\"测试楼盘\"}";
        dto.setMetadata(mapper.readTree(metadataJson));

        String serialized = mapper.writeValueAsString(dto);
        assertTrue(serialized.contains("ORD-001"));
    }
}
