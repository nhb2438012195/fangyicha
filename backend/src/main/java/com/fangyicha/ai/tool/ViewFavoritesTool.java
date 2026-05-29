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
public class ViewFavoritesTool {

    private final FunctionCallService functionCallService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ViewFavoritesTool(FunctionCallService functionCallService) {
        this.functionCallService = functionCallService;
    }

    @Bean
    public ToolCallback viewFavoritesToolCallback() {
        return FunctionToolCallback.builder("view_favorites", (Map<String, Object> arguments, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = new HashMap<>();
                if (arguments != null && !arguments.isEmpty()) {
                    args.putAll(arguments);
                }
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                FunctionCallService.FunctionResult result = functionCallService.execute("view_favorites", args, customerId);
                Map<String, Object> data = new HashMap<>();
                data.put("_type", "favorites");
                data.put("message", result.getMessage());
                if (result.getData() != null) data.putAll(result.getData());
                ToolResultHolder.set(data);
                return objectMapper.writeValueAsString(result.getData() != null ? result.getData() : Map.of("message", result.getMessage()));
            } catch (Exception e) {
                return "{\"error\":\"查看收藏失败\"}";
            }
        })
        .description("查看用户的收藏楼盘列表。用户说「看看我的收藏」时调用")
        .inputType(java.util.Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{\"filter\":{\"type\":\"string\",\"description\":\"可选的筛选关键词\"}}}")
        .build();
    }
}
