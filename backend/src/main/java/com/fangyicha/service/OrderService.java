package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.entity.Order;
import com.fangyicha.entity.OrderLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    Order createOrder(Long customerId, Long propertyId);

    /**
     * 支付订单
     */
    Order payOrder(Long orderId, Long customerId);

    /**
     * 取消订单
     */
    Order cancelOrder(Long orderId, Long customerId, String reason);

    /**
     * 确认完成（开发商）
     */
    Order completeOrder(Long orderId, Long developerId);

    /**
     * 查询我的订单（客户）
     */
    Page<Order> getMyOrders(Long customerId, String status, Integer page, Integer pageSize);

    /**
     * 查询收到的订单（开发商）
     */
    Page<Order> getReceivedOrders(Long developerId, String status, Integer page, Integer pageSize);

    /**
     * 获取订单详情（含验证权限）
     */
    Order getOrderDetail(Long orderId, Long userId, String role);

    /**
     * 获取订单日志
     */
    List<OrderLog> getOrderLogs(Long orderId);

    /**
     * 获取客户订单统计
     */
    Map<String, Object> getCustomerOrderStats(Long customerId);

    /**
     * 获取开发商订单统计
     */
    Map<String, Object> getDeveloperOrderStats(Long developerId);
}
