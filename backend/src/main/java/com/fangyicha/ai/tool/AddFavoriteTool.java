package com.fangyicha.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangyicha.service.FunctionCallService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AddFavoriteTool {

    private final FunctionCallService functionCallService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AddFavoriteTool(FunctionCallService functionCallService) {
        this.functionCallService = functionCallService;
    }

    @Bean
    public ToolCallback addFavoriteToolCallback() {
        return FunctionToolCallback.builder("add_favorite", (Map<String, Object> arguments, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = arguments != null ? arguments : Map.of();
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                FunctionCallService.FunctionResult result = functionCallService.execute("add_favorite", args, customerId);
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("_type", "add_favorite");
                data.put("message", result.getMessage());
                if (result.getData() != null) data.putAll(result.getData());
                ToolResultHolder.set(data);
                return objectMapper.writeValueAsString(Map.of("success", result.isSuccess(), "message", result.getMessage()));
            } catch (Exception e) {
                return "{\"error\":\"执行收藏操作失败\"}";
            }
        })
        .description("收藏一个楼盘到收藏夹。调用时propertyName参数必填，值为楼盘名称")
        .inputType(java.util.Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{\"propertyName\":{\"type\":\"string\",\"description\":\"楼盘名称\"}},\"required\":[\"propertyName\"]}")
        .build();
    }
}
