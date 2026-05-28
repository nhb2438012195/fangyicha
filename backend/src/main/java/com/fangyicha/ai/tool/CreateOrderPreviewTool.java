package com.fangyicha.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
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
        return FunctionToolCallback.builder("create_order_preview", (String input, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = objectMapper.readValue(input, new TypeReference<Map<String, Object>>() {});
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                FunctionCallService.FunctionResult result = functionCallService.execute("create_order_preview", args, customerId);
                Map<String, Object> data = new HashMap<>();
                data.put("_type", "create_order_preview");
                data.put("message", result.getMessage());
                if (result.getData() != null) data.putAll(result.getData());
                ToolResultHolder.set(data);
                return objectMapper.writeValueAsString(result.getData() != null ? result.getData() : Map.of("message", result.getMessage()));
            } catch (Exception e) {
                return "{\"error\": \"创建订单预览失败\"}";
            }
        })
        .description("创建购房订单预览。用户说「买XX楼盘」或「帮我下单XX」时调用，仅创建预览不实际下单，需要用户确认后才正式创建")
        .inputType(String.class)
        .build();
    }
}
