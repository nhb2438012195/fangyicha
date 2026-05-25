package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.common.Constants;
import com.fangyicha.entity.*;
import com.fangyicha.mapper.*;
import com.fangyicha.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final PropertyMapper propertyMapper;
    private final CustomerMapper customerMapper;
    private final DeveloperMapper developerMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderLogMapper orderLogMapper,
                            PropertyMapper propertyMapper,
                            CustomerMapper customerMapper,
                            DeveloperMapper developerMapper) {
        this.orderMapper = orderMapper;
        this.orderLogMapper = orderLogMapper;
        this.propertyMapper = propertyMapper;
        this.customerMapper = customerMapper;
        this.developerMapper = developerMapper;
    }

    @Override
    @Transactional
    public Order createOrder(Long customerId, Long propertyId) {
        // 校验房产
        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new RuntimeException("房产不存在");
        }
        if (!Constants.PROPERTY_STATUS_ON_SALE.equals(property.getStatus())) {
            throw new RuntimeException("该房产不在在售状态，无法购买");
        }

        // 校验客户
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 查询开发商
        Developer developer = developerMapper.selectById(property.getDeveloperId());

        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setDeveloperId(property.getDeveloperId());
        order.setPropertyId(propertyId);
        order.setPropertyName(property.getPropertyName());
        order.setPropertyLocation(property.getLocation());
        order.setFloorPlanType(property.getFloorPlanType());
        order.setAreaSqm(property.getAreaSqm());
        order.setTotalPrice(property.getTotalPrice());
        order.setPricePerSqm(property.getPricePerSqm());
        order.setStatus(Constants.ORDER_PENDING_PAYMENT);
        order.setCustomerName(customer.getRealName());
        order.setCustomerPhone(customer.getPhone());
        order.setDeveloperName(developer != null ? developer.getCompanyName() : "");

        orderMapper.insert(order);
        log.info("订单创建成功: orderNo={}, customerId={}, propertyId={}", orderNo, customerId, propertyId);

        // 记录订单日志
        logOrderAction(order.getId(), customerId, Constants.ROLE_CUSTOMER, "创建订单",
                null, Constants.ORDER_PENDING_PAYMENT, "客户下单购买房产");

        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Long orderId, Long customerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许支付");
        }

        String fromStatus = order.getStatus();
        order.setStatus(Constants.ORDER_PAID);
        order.setPaidTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单支付成功: orderNo={}", order.getOrderNo());

        // 记录日志
        logOrderAction(orderId, customerId, Constants.ROLE_CUSTOMER, "支付订单",
                fromStatus, Constants.ORDER_PAID, "客户完成支付");

        return order;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, Long customerId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许取消");
        }

        String fromStatus = order.getStatus();
        order.setStatus(Constants.ORDER_CANCELLED);
        order.setCancelledTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        log.info("订单取消: orderNo={}, reason={}", order.getOrderNo(), reason);

        // 记录日志
        logOrderAction(orderId, customerId, Constants.ROLE_CUSTOMER, "取消订单",
                fromStatus, Constants.ORDER_CANCELLED, reason != null ? reason : "客户主动取消");

        return order;
    }

    @Override
    @Transactional
    public Order completeOrder(Long orderId, Long developerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getDeveloperId().equals(developerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_PAID.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许确认完成");
        }

        String fromStatus = order.getStatus();
        order.setStatus(Constants.ORDER_COMPLETED);
        order.setCompletedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单完成: orderNo={}", order.getOrderNo());

        // 记录日志
        logOrderAction(orderId, developerId, Constants.ROLE_DEVELOPER, "确认完成",
                fromStatus, Constants.ORDER_COMPLETED, "开发商确认订单完成");

        return order;
    }

    @Override
    public Page<Order> getMyOrders(Long customerId, String status, Integer page, Integer pageSize) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getCustomerId, customerId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Order> getReceivedOrders(Long developerId, String status, Integer page, Integer pageSize) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getDeveloperId, developerId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Order getOrderDetail(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 权限校验：客户只能看自己的，开发商只能看自己的
        if (Constants.ROLE_CUSTOMER.equals(role) && !order.getCustomerId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        if (Constants.ROLE_DEVELOPER.equals(role) && !order.getDeveloperId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        return order;
    }

    @Override
    public List<OrderLog> getOrderLogs(Long orderId) {
        LambdaQueryWrapper<OrderLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderLog::getOrderId, orderId);
        wrapper.orderByAsc(OrderLog::getCreatedTime);
        return orderLogMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getCustomerOrderStats(Long customerId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Order> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Order::getCustomerId, customerId);
        stats.put("orderCount", orderMapper.selectCount(allWrapper));

        LambdaQueryWrapper<Order> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Order::getCustomerId, customerId)
                .eq(Order::getStatus, Constants.ORDER_PENDING_PAYMENT);
        stats.put("pendingOrderCount", orderMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<Order> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(Order::getCustomerId, customerId)
                .eq(Order::getStatus, Constants.ORDER_PAID);
        stats.put("paidOrderCount", orderMapper.selectCount(paidWrapper));

        LambdaQueryWrapper<Order> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Order::getCustomerId, customerId)
                .eq(Order::getStatus, Constants.ORDER_COMPLETED);
        stats.put("completedOrderCount", orderMapper.selectCount(completedWrapper));

        return stats;
    }

    @Override
    public Map<String, Object> getDeveloperOrderStats(Long developerId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Order> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Order::getDeveloperId, developerId);
        stats.put("orderCount", orderMapper.selectCount(allWrapper));

        LambdaQueryWrapper<Order> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Order::getDeveloperId, developerId)
                .eq(Order::getStatus, Constants.ORDER_PENDING_PAYMENT);
        stats.put("pendingOrderCount", orderMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<Order> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(Order::getDeveloperId, developerId)
                .eq(Order::getStatus, Constants.ORDER_PAID);
        stats.put("paidOrderCount", orderMapper.selectCount(paidWrapper));

        LambdaQueryWrapper<Order> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Order::getDeveloperId, developerId)
                .eq(Order::getStatus, Constants.ORDER_COMPLETED);
        stats.put("completedOrderCount", orderMapper.selectCount(completedWrapper));

        return stats;
    }

    // ===================== 私有方法 =====================

    /**
     * 生成订单号：FC + yyyyMMdd + 6位序列
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "FC" + datePart;

        // 查询当日最大序列号
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Order::getOrderNo, prefix);
        wrapper.orderByDesc(Order::getOrderNo);
        wrapper.last("LIMIT 1");

        List<Order> lastOrders = orderMapper.selectList(wrapper);
        int seq = 1;
        if (lastOrders != null && !lastOrders.isEmpty()) {
            String lastNo = lastOrders.get(0).getOrderNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%06d", seq);
    }

    /**
     * 记录订单操作日志
     */
    private void logOrderAction(Long orderId, Long actorId, String actorRole,
                                 String action, String fromStatus, String toStatus, String detail) {
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setActorId(actorId);
        logEntry.setActorRole(actorRole);
        logEntry.setAction(action);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setDetail(detail);
        orderLogMapper.insert(logEntry);
    }
}
