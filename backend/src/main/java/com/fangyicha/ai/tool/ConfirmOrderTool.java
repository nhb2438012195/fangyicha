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
public class ConfirmOrderTool {

    private final FunctionCallService functionCallService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfirmOrderTool(FunctionCallService functionCallService) {
        this.functionCallService = functionCallService;
    }

    @Bean
    public ToolCallback confirmOrderToolCallback() {
        return FunctionToolCallback.builder("confirm_order", (Map<String, Object> arguments, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = new HashMap<>();
                Long sessionId = SessionIdHolder.get();
                if (sessionId != null) args.put("_sessionId", sessionId);
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                FunctionCallService.FunctionResult result = functionCallService.execute("confirm_order", args, customerId);
                Map<String, Object> data = new HashMap<>();
                data.put("_type", "confirm_order");
                data.put("message", result.getMessage());
                if (result.getData() != null) data.putAll(result.getData());
                ToolResultHolder.set(data);
                return objectMapper.writeValueAsString(result.getData() != null ? result.getData() : Map.of("message", result.getMessage()));
            } catch (Exception e) {
                return "{\"error\":\"确认订单失败\"}";
            }
        })
        .description("确认购房订单。用户说「确认」「是的」「提交」时调用")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
        .build();
    }
}
