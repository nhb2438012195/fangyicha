package com.fangyicha.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangyicha.service.FunctionCallService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CreateOrderPreviewTool {

    private final FunctionCallService functionCallService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateOrderPreviewTool(FunctionCallService functionCallService) {
        this.functionCallService = functionCallService;
    }

    @Bean
    public ToolCallback createOrderPreviewToolCallback() {
        return FunctionToolCallback.builder("create_order_preview", (Map<String, Object> arguments, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = arguments != null ? new HashMap<>(arguments) : new HashMap<>();
                Long sessionId = SessionIdHolder.get();
                if (sessionId != null) args.put("_sessionId", sessionId);
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                FunctionCallService.FunctionResult result = functionCallService.execute("create_order_preview", args, customerId);
                Map<String, Object> data = new HashMap<>();
                data.put("_type", "create_order_preview");
                data.put("message", result.getMessage());
                if (result.getData() != null) data.putAll(result.getData());
                ToolResultHolder.set(data);
                return objectMapper.writeValueAsString(result.getData() != null ? result.getData() : Map.of("message", result.getMessage()));
            } catch (Exception e) {
                return "{\"error\":\"创建订单预览失败\"}";
            }
        })
        .description("创建购房订单预览。调用时propertyName参数必填。仅创建预览，需用户确认后才正式创建订单")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{\"propertyName\":{\"type\":\"string\",\"description\":\"楼盘名称\"}},\"required\":[\"propertyName\"]}")
        .build();
    }
}
