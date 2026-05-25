package com.fangyicha.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.OrderLog;
import com.fangyicha.service.ActivityLogService;
import com.fangyicha.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * 处理客户购房订单的创建、支付、取消和开发商的确认完成
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "购房订单的创建、支付、取消和确认完成")
public class OrderController {

    private final OrderService orderService;
    private final ActivityLogService activityLogService;

    public OrderController(OrderService orderService, ActivityLogService activityLogService) {
        this.orderService = orderService;
        this.activityLogService = activityLogService;
    }

    /**
     * 创建订单（客户）
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "创建订单", description = "客户下单购买房产")
    public Result<Order> createOrder(Authentication authentication, @RequestBody Map<String, Long> body) {
        Long customerId = (Long) authentication.getPrincipal();
        Long propertyId = body.get("propertyId");
        if (propertyId == null) {
            return Result.badRequest("房产ID不能为空");
        }
        try {
            Order order = orderService.createOrder(customerId, propertyId);
            activityLogService.log(customerId, Constants.ROLE_CUSTOMER, "CREATE",
                    Constants.ENTITY_ORDER, order.getId(), "创建订单: " + order.getOrderNo());
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 获取我的订单列表（客户）
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "我的订单", description = "客户查看自己的订单列表")
    public Result<Page<Order>> getMyOrders(Authentication authentication,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Long customerId = (Long) authentication.getPrincipal();
        Page<Order> result = orderService.getMyOrders(customerId, status, page, pageSize);
        return Result.success(result, result.getTotal(), Long.valueOf(page), Long.valueOf(pageSize));
    }

    /**
     * 获取收到的订单列表（开发商）
     */
    @GetMapping("/received")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "收到的订单", description = "开发商查看收到的客户订单")
    public Result<Page<Order>> getReceivedOrders(Authentication authentication,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        Long developerId = (Long) authentication.getPrincipal();
        Page<Order> result = orderService.getReceivedOrders(developerId, status, page, pageSize);
        return Result.success(result, result.getTotal(), Long.valueOf(page), Long.valueOf(pageSize));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "查看订单详细信息（客户或开发商均可查看自己的）")
    public Result<Order> getOrderDetail(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("");
        try {
            Order order = orderService.getOrderDetail(id, userId, role);
            return Result.success(order);
        } catch (RuntimeException e) {
            if ("订单不存在".equals(e.getMessage())) {
                return Result.notFound(e.getMessage());
            }
            return Result.forbidden(e.getMessage());
        }
    }

    /**
     * 支付订单（客户）
     */
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "支付订单", description = "客户对订单进行支付")
    public Result<Order> payOrder(Authentication authentication, @PathVariable Long id) {
        Long customerId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.payOrder(id, customerId);
            activityLogService.log(customerId, Constants.ROLE_CUSTOMER, "PAY",
                    Constants.ENTITY_ORDER, order.getId(), "支付订单: " + order.getOrderNo());
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 取消订单（客户）
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "取消订单", description = "客户取消待支付的订单")
    public Result<Order> cancelOrder(Authentication authentication,
                                     @PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        Long customerId = (Long) authentication.getPrincipal();
        String reason = body != null ? body.get("reason") : null;
        try {
            Order order = orderService.cancelOrder(id, customerId, reason);
            activityLogService.log(customerId, Constants.ROLE_CUSTOMER, "CANCEL",
                    Constants.ENTITY_ORDER, order.getId(), "取消订单: " + order.getOrderNo());
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 确认完成订单（开发商）
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "确认完成", description = "开发商确认订单已完成")
    public Result<Order> completeOrder(Authentication authentication, @PathVariable Long id) {
        Long developerId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.completeOrder(id, developerId);
            activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "COMPLETE",
                    Constants.ENTITY_ORDER, order.getId(), "确认完成: " + order.getOrderNo());
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 获取订单日志
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "订单日志", description = "查看订单操作日志")
    public Result<List<OrderLog>> getOrderLogs(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("");
        // 先验证权限
        try {
            orderService.getOrderDetail(id, userId, role);
        } catch (RuntimeException e) {
            return Result.forbidden(e.getMessage());
        }
        List<OrderLog> logs = orderService.getOrderLogs(id);
        return Result.success(logs);
    }

    /**
     * 获取订单统计（客户用）
     */
    @GetMapping("/stats/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "我的订单统计", description = "客户订单统计数据")
    public Result<Map<String, Object>> getMyOrderStats(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = orderService.getCustomerOrderStats(customerId);
        return Result.success(stats);
    }

    /**
     * 获取订单统计（开发商用）
     */
    @GetMapping("/stats/received")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "收到的订单统计", description = "开发商订单统计数据")
    public Result<Map<String, Object>> getReceivedOrderStats(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = orderService.getDeveloperOrderStats(developerId);
        return Result.success(stats);
    }
}
