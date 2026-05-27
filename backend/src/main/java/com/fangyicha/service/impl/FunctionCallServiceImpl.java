package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.entity.*;
import com.fangyicha.mapper.*;
import com.fangyicha.service.FunctionCallService;
import com.fangyicha.service.FavoriteService;
import com.fangyicha.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Function Calling 服务实现
 */
@Slf4j
@Service
public class FunctionCallServiceImpl implements FunctionCallService {

    private final FavoriteService favoriteService;
    private final OrderService orderService;
    private final PropertyMapper propertyMapper;
    private final CustomerMapper customerMapper;
    private final PendingOrderMapper pendingOrderMapper;
    private final FavoriteMapper favoriteMapper;
    private final RagServiceImpl ragService;

    public FunctionCallServiceImpl(FavoriteService favoriteService,
                                    OrderService orderService,
                                    PropertyMapper propertyMapper,
                                    CustomerMapper customerMapper,
                                    PendingOrderMapper pendingOrderMapper,
                                    FavoriteMapper favoriteMapper,
                                    RagServiceImpl ragService) {
        this.favoriteService = favoriteService;
        this.orderService = orderService;
        this.propertyMapper = propertyMapper;
        this.customerMapper = customerMapper;
        this.pendingOrderMapper = pendingOrderMapper;
        this.favoriteMapper = favoriteMapper;
        this.ragService = ragService;
    }

    @Override
    public FunctionResult execute(String functionName, Map<String, Object> arguments, Long customerId) {
        log.info("执行函数调用: function={}, args={}, customerId={}", functionName, arguments, customerId);
        try {
            return switch (functionName) {
                case "add_favorite" -> handleAddFavorite(arguments, customerId);
                case "view_favorites" -> handleViewFavorites(arguments, customerId);
                case "create_order_preview" -> handleCreateOrderPreview(arguments, customerId);
                case "confirm_order" -> handleConfirmOrder(arguments, customerId);
                default -> new FunctionResult(false, "未知函数: " + functionName, null);
            };
        } catch (Exception e) {
            log.error("函数调用执行异常: function={}", functionName, e);
            return new FunctionResult(false, "操作执行失败: " + e.getMessage(), null);
        }
    }

    /**
     * 添加收藏
     */
    private FunctionResult handleAddFavorite(Map<String, Object> arguments, Long customerId) {
        Object propertyIdObj = arguments.get("propertyId");
        Long propertyId = null;
        if (propertyIdObj instanceof Number) {
            propertyId = ((Number) propertyIdObj).longValue();
        } else if (propertyIdObj instanceof String) {
            propertyId = Long.parseLong((String) propertyIdObj);
        }

        // Resolve property by name if propertyId is null
        if (propertyId == null) {
            // Try "propertyName" or "name" param
            String name = (String) arguments.getOrDefault("propertyName",
                           arguments.getOrDefault("name", ""));
            if (!name.isEmpty()) {
                Property property = resolvePropertyByName(name);
                if (property != null) {
                    propertyId = property.getId();
                }
            }
        }

        if (propertyId == null) {
            return new FunctionResult(false, "请指定要收藏的楼盘", null);
        }

        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            return new FunctionResult(false, "楼盘不存在", null);
        }

        boolean wasAdded = favoriteService.toggleFavorite(customerId, propertyId);
        String message = wasAdded ? "已收藏 " + property.getPropertyName() + "！可以在「我的收藏」中查看"
                                   : "已取消收藏 " + property.getPropertyName();

        Map<String, Object> data = new HashMap<>();
        data.put("propertyId", propertyId);
        data.put("propertyName", property.getPropertyName());
        data.put("action", wasAdded ? "added" : "removed");

        return new FunctionResult(true, message, data);
    }

    /**
     * 查看收藏
     */
    private FunctionResult handleViewFavorites(Map<String, Object> arguments, Long customerId) {
        String filter = (String) arguments.getOrDefault("filter", "");

        List<com.fangyicha.dto.FavoriteDTO> favorites = favoriteService.getFavorites(customerId);

        if (favorites == null || favorites.isEmpty()) {
            return new FunctionResult(true, "你还没有收藏任何楼盘哦，试试搜索你感兴趣的楼盘吧！", null);
        }

        // Client-side filtering for simple conditions
        if (filter != null && !filter.isEmpty()) {
            final String filterLower = filter.toLowerCase();
            favorites = favorites.stream()
                .filter(f -> {
                    String name = f.getPropertyName() != null ? f.getPropertyName().toLowerCase() : "";
                    String loc = f.getLocation() != null ? f.getLocation().toLowerCase() : "";
                    return name.contains(filterLower) || loc.contains(filterLower);
                })
                .limit(5)
                .collect(Collectors.toList());
        } else {
            // Default: latest 5
            favorites = favorites.stream().limit(5).collect(Collectors.toList());
        }

        List<Map<String, Object>> cardList = new ArrayList<>();
        for (com.fangyicha.dto.FavoriteDTO f : favorites) {
            Map<String, Object> card = new HashMap<>();
            card.put("type", "favorite");
            card.put("propertyId", f.getPropertyId());
            card.put("propertyName", f.getPropertyName());
            card.put("location", f.getLocation());
            card.put("pricePerSqm", f.getPricePerSqm());
            card.put("totalPrice", f.getTotalPrice());
            card.put("areaSqm", f.getAreaSqm());
            card.put("floorPlanType", f.getFloorPlanType());
            card.put("decoration", f.getDecoration());
            card.put("imageUrl", f.getImageUrl());
            card.put("developerName", f.getDeveloperName());
            cardList.add(card);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("cards", cardList);
        data.put("total", favorites.size());
        data.put("message", "以下是你的收藏楼盘（共" + favorites.size() + "条）");

        return new FunctionResult(true, "以下是你的收藏楼盘", data);
    }

    /**
     * 创建订单预览
     */
    @Transactional
    public FunctionResult handleCreateOrderPreview(Map<String, Object> arguments, Long customerId) {
        Long propertyId = resolvePropertyId(arguments);
        if (propertyId == null) {
            return new FunctionResult(false, "请指定要购买的楼盘", null);
        }

        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            return new FunctionResult(false, "楼盘不存在", null);
        }
        // 只允许在售楼盘创建订单
        if (!"在售".equals(property.getStatus())) {
            return new FunctionResult(false, "该楼盘当前不在在售状态，暂时无法购买", null);
        }

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            return new FunctionResult(false, "客户信息不存在", null);
        }

        // Get customer name and phone from arguments or profile
        String customerName = (String) arguments.getOrDefault("customerName", customer.getRealName());
        String customerPhone = (String) arguments.getOrDefault("customerPhone", customer.getPhone());

        if (customerName == null || customerName.isEmpty()) {
            return new FunctionResult(false, "请提供客户姓名", null);
        }
        if (customerPhone == null || customerPhone.isEmpty()) {
            return new FunctionResult(false, "请提供客户电话", null);
        }

        // Resolve sessionId from arguments (added internally)
        Object sessionIdObj = arguments.get("_sessionId");
        Long sessionId = sessionIdObj instanceof Number ? ((Number) sessionIdObj).longValue() : null;

        // Check for existing pending order in this session
        if (sessionId != null) {
            LambdaQueryWrapper<PendingOrder> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(PendingOrder::getSessionId, sessionId)
                       .eq(PendingOrder::getStatus, "pending");
            PendingOrder existing = pendingOrderMapper.selectOne(checkWrapper);
            if (existing != null) {
                // Update existing pending order
                existing.setPropertyId(propertyId);
                existing.setCustomerName(customerName);
                existing.setCustomerPhone(customerPhone);
                existing.setCreatedTime(LocalDateTime.now());
                pendingOrderMapper.updateById(existing);

                Map<String, Object> data = buildOrderPreviewData(existing.getId(), property, customerName, customerPhone);
                return new FunctionResult(true, "订单预览已更新", data);
            }
        }

        // Create new pending order
        PendingOrder pendingOrder = new PendingOrder();
        pendingOrder.setSessionId(sessionId);
        pendingOrder.setCustomerId(customerId);
        pendingOrder.setPropertyId(propertyId);
        pendingOrder.setCustomerName(customerName);
        pendingOrder.setCustomerPhone(customerPhone);
        pendingOrder.setStatus("pending");
        pendingOrderMapper.insert(pendingOrder);

        Map<String, Object> data = buildOrderPreviewData(pendingOrder.getId(), property, customerName, customerPhone);
        return new FunctionResult(true, "订单预览已创建", data);
    }

    /**
     * 确认订单
     */
    @Transactional
    public FunctionResult handleConfirmOrder(Map<String, Object> arguments, Long customerId) {
        Object sessionIdObj = arguments.get("_sessionId");
        Long sessionId = sessionIdObj instanceof Number ? ((Number) sessionIdObj).longValue() : null;

        if (sessionId == null) {
            return new FunctionResult(false, "会话ID缺失，无法确认订单", null);
        }

        // Find pending order for this session
        LambdaQueryWrapper<PendingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PendingOrder::getSessionId, sessionId)
               .eq(PendingOrder::getStatus, "pending");
        PendingOrder pendingOrder = pendingOrderMapper.selectOne(wrapper);

        if (pendingOrder == null) {
            return new FunctionResult(false, "没有待确认的订单，请先创建订单预览", null);
        }

        // Create actual order via OrderService
        Order order = orderService.createOrder(customerId, pendingOrder.getPropertyId());

        // Update pending order status
        pendingOrder.setStatus("confirmed");
        pendingOrder.setConfirmedTime(LocalDateTime.now());
        pendingOrderMapper.updateById(pendingOrder);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("propertyId", order.getPropertyId());
        data.put("propertyName", order.getPropertyName());
        data.put("location", order.getPropertyLocation());
        data.put("floorPlanType", order.getFloorPlanType());
        data.put("areaSqm", order.getAreaSqm());
        data.put("totalPrice", order.getTotalPrice());
        data.put("pricePerSqm", order.getPricePerSqm());
        data.put("customerName", order.getCustomerName());
        data.put("customerPhone", order.getCustomerPhone());
        data.put("status", order.getStatus());

        return new FunctionResult(true, "订单已确认！订单号: " + order.getOrderNo(), data);
    }

    // ===================== 辅助方法 =====================

    /**
     * 根据参数解析楼盘ID
     */
    private Long resolvePropertyId(Map<String, Object> arguments) {
        Object propertyIdObj = arguments.get("propertyId");
        if (propertyIdObj instanceof Number) {
            return ((Number) propertyIdObj).longValue();
        } else if (propertyIdObj instanceof String) {
            try {
                return Long.parseLong((String) propertyIdObj);
            } catch (NumberFormatException ignored) {}
        }

        String name = (String) arguments.getOrDefault("propertyName",
                       arguments.getOrDefault("name", ""));
        if (!name.isEmpty()) {
            Property property = resolvePropertyByName(name);
            if (property != null) {
                return property.getId();
            }
        }
        return null;
    }

    /**
     * 通过名称解析楼盘（三步查找）
     * 1. SQL LIKE 匹配
     * 2. RAG 语义搜索
     * 3. 返回第一个匹配
     */
    private Property resolvePropertyByName(String name) {
        // Step 1: SQL LIKE match
        LambdaQueryWrapper<Property> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Property::getPropertyName, name)
               .last("LIMIT 1");
        Property property = propertyMapper.selectOne(wrapper);
        if (property != null) {
            return property;
        }

        // Step 2: RAG semantic search
        List<com.fangyicha.service.RagService.RagResult> results = ragService.search(name);
        for (com.fangyicha.service.RagService.RagResult result : results) {
            String id = result.getId();
            if (id != null && id.startsWith("property_")) {
                try {
                    Long propId = Long.parseLong(id.replace("property_", "").replace(".txt", ""));
                    Property p = propertyMapper.selectById(propId);
                    if (p != null) return p;
                } catch (NumberFormatException ignored) {}
            }
        }

        return null;
    }

    /**
     * 构建订单预览数据
     */
    private Map<String, Object> buildOrderPreviewData(Long previewId, Property property,
                                                       String customerName, String customerPhone) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "order_preview");
        data.put("previewId", previewId);
        data.put("propertyId", property.getId());
        data.put("propertyName", property.getPropertyName());
        data.put("location", property.getLocation());
        data.put("floorPlanType", property.getFloorPlanType());
        data.put("areaSqm", property.getAreaSqm());
        data.put("pricePerSqm", property.getPricePerSqm());
        data.put("totalPrice", property.getTotalPrice());
        data.put("customerName", customerName);
        data.put("customerPhone", customerPhone);
        return data;
    }
}
