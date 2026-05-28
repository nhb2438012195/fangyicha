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
public class ViewFavoritesTool {

    private final FunctionCallService functionCallService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ViewFavoritesTool(FunctionCallService functionCallService) {
        this.functionCallService = functionCallService;
    }

    @Bean
    public ToolCallback viewFavoritesToolCallback() {
        return FunctionToolCallback.builder("view_favorites", (String input, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Map<String, Object> args = new HashMap<>();
                if (input != null && !input.isEmpty() && !"{}".equals(input)) {
                    args = objectMapper.readValue(input, new TypeReference<Map<String, Object>>() {});
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
                return "{\"error\": \"查看收藏失败\"}";
            }
        })
        .description("查看用户的收藏楼盘列表。用户说「看看我的收藏」或「我的收藏有什么」时调用")
        .inputType(String.class)
        .build();
    }
}
