package com.fangyicha.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.dto.PropertyDetailDTO;
import com.fangyicha.dto.RecommendationDTO;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.OrderMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ApiTools {

    private final PropertyMapper propertyMapper;
    private final OrderMapper orderMapper;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiTools(PropertyMapper propertyMapper, OrderMapper orderMapper, CustomerService customerService) {
        this.propertyMapper = propertyMapper;
        this.orderMapper = orderMapper;
        this.customerService = customerService;
    }

    @Bean
    public ToolCallback searchPropertiesCallback() {
        return FunctionToolCallback.builder("search_properties", (Map<String, Object> args, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Property::getStatus, "在售");

                if (args.containsKey("keyword") && args.get("keyword") != null) {
                    wrapper.and(w -> w.like(Property::getPropertyName, (String) args.get("keyword"))
                        .or().like(Property::getLocation, (String) args.get("keyword")));
                }
                if (args.containsKey("location") && args.get("location") != null) {
                    wrapper.like(Property::getLocation, (String) args.get("location"));
                }
                if (args.containsKey("floorPlanType") && args.get("floorPlanType") != null) {
                    wrapper.eq(Property::getFloorPlanType, (String) args.get("floorPlanType"));
                }
                if (args.containsKey("decoration") && args.get("decoration") != null) {
                    wrapper.eq(Property::getDecoration, (String) args.get("decoration"));
                }
                wrapper.last("LIMIT 10");

                List<Property> properties = propertyMapper.selectList(wrapper);
                List<Map<String, Object>> list = new ArrayList<>();
                for (Property p : properties) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", p.getId());
                    item.put("propertyName", p.getPropertyName());
                    item.put("location", p.getLocation());
                    item.put("floorPlanType", p.getFloorPlanType());
                    item.put("areaSqm", p.getAreaSqm());
                    item.put("totalPrice", p.getTotalPrice());
                    item.put("pricePerSqm", p.getPricePerSqm());
                    item.put("decoration", p.getDecoration());
                    list.add(item);
                }
                return objectMapper.writeValueAsString(Map.of("total", list.size(), "properties", list));
            } catch (Exception e) {
                return "{\"error\":\"搜索失败\"}";
            }
        })
        .description("搜索在售楼盘。可按关键词、区域、户型、装修筛选")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{\"keyword\":{\"type\":\"string\",\"description\":\"搜索关键词\"},\"location\":{\"type\":\"string\",\"description\":\"区域如天元区\"},\"floorPlanType\":{\"type\":\"string\",\"description\":\"户型如三室两厅\"},\"decoration\":{\"type\":\"string\",\"description\":\"装修如精装\"}}}")
        .build();
    }

    @Bean
    public ToolCallback getPropertyDetailCallback() {
        return FunctionToolCallback.builder("get_property_detail", (Map<String, Object> args, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                String propertyName = (String) args.get("propertyName");
                Property property = null;
                if (propertyName != null) {
                    LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
                    wrapper.like(Property::getPropertyName, propertyName).last("LIMIT 1");
                    property = propertyMapper.selectOne(wrapper);
                }
                if (property == null) {
                    return "{\"error\":\"未找到该楼盘\"}";
                }
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("id", property.getId());
                detail.put("propertyName", property.getPropertyName());
                detail.put("location", property.getLocation());
                detail.put("floorPlanType", property.getFloorPlanType());
                detail.put("areaSqm", property.getAreaSqm());
                detail.put("totalPrice", property.getTotalPrice());
                detail.put("pricePerSqm", property.getPricePerSqm());
                detail.put("decoration", property.getDecoration());
                detail.put("status", property.getStatus());
                detail.put("description", property.getDescription());
                return objectMapper.writeValueAsString(detail);
            } catch (Exception e) {
                return "{\"error\":\"查询失败\"}";
            }
        })
        .description("查询楼盘的详细信息。输入楼盘名称获取完整信息")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{\"propertyName\":{\"type\":\"string\",\"description\":\"楼盘名称\"}},\"required\":[\"propertyName\"]}")
        .build();
    }

    @Bean
    public ToolCallback listOrdersCallback() {
        return FunctionToolCallback.builder("list_orders", (Map<String, Object> args, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Order::getCustomerId, customerId).orderByDesc(Order::getCreatedTime).last("LIMIT 10");
                List<Order> orders = orderMapper.selectList(wrapper);

                List<Map<String, Object>> list = new ArrayList<>();
                for (Order o : orders) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", o.getId());
                    item.put("orderNo", o.getOrderNo());
                    item.put("propertyName", o.getPropertyName());
                    item.put("totalPrice", o.getTotalPrice());
                    item.put("status", o.getStatus());
                    item.put("createdTime", o.getCreatedTime() != null ? o.getCreatedTime().toString() : null);
                    list.add(item);
                }
                return objectMapper.writeValueAsString(Map.of("total", list.size(), "orders", list));
            } catch (Exception e) {
                return "{\"error\":\"查询失败\"}";
            }
        })
        .description("查询当前用户的订单列表")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
        .build();
    }

    @Bean
    public ToolCallback getUserProfileCallback() {
        return FunctionToolCallback.builder("get_user_profile", (Map<String, Object> args, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                var customer = customerService.getById(customerId);
                if (customer == null) return "{\"error\":\"未找到用户\"}";
                Map<String, Object> profile = new LinkedHashMap<>();
                profile.put("realName", customer.getRealName());
                profile.put("phone", customer.getPhone());
                profile.put("preferredLocations", customer.getPreferredLocations());
                profile.put("intention", customer.getIntention());
                return objectMapper.writeValueAsString(profile);
            } catch (Exception e) {
                return "{\"error\":\"查询失败\"}";
            }
        })
        .description("获取当前用户的个人资料、偏好设置等信息")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
        .build();
    }

    @Bean
    public ToolCallback getRecommendationsCallback() {
        return FunctionToolCallback.builder("get_recommendations", (Map<String, Object> args, org.springframework.ai.chat.model.ToolContext ctx) -> {
            try {
                Long customerId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                var customer = customerService.getById(customerId);

                LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Property::getStatus, "在售");

                if (customer != null && customer.getPreferredLocations() != null) {
                    String[] locations = customer.getPreferredLocations().split(",");
                    wrapper.and(w -> {
                        for (int i = 0; i < locations.length; i++) {
                            if (i == 0) w.like(Property::getLocation, locations[i].trim());
                            else w.or().like(Property::getLocation, locations[i].trim());
                        }
                    });
                }
                wrapper.last("LIMIT 6");
                List<Property> properties = propertyMapper.selectList(wrapper);

                List<Map<String, Object>> list = new ArrayList<>();
                for (Property p : properties) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("propertyName", p.getPropertyName());
                    item.put("location", p.getLocation());
                    item.put("floorPlanType", p.getFloorPlanType());
                    item.put("totalPrice", p.getTotalPrice());
                    list.add(item);
                }
                return objectMapper.writeValueAsString(Map.of("total", list.size(), "recommendations", list));
            } catch (Exception e) {
                return "{\"error\":\"查询失败\"}";
            }
        })
        .description("根据用户偏好获取个性化楼盘推荐")
        .inputType(Map.class)
        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
        .build();
    }
}
