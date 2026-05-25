package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购房订单实体类
 */
@Data
@TableName("purchase_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（FC+yyyyMMdd+6位序列） */
    private String orderNo;

    /** 客户ID */
    private Long customerId;

    /** 开发商ID */
    private Long developerId;

    /** 房产ID */
    private Long propertyId;

    /** 房产名称（冗余） */
    private String propertyName;

    /** 房产位置（冗余） */
    private String propertyLocation;

    /** 房产户型（冗余） */
    private String floorPlanType;

    /** 面积（冗余） */
    private BigDecimal areaSqm;

    /** 成交总价 */
    private BigDecimal totalPrice;

    /** 单价 */
    private BigDecimal pricePerSqm;

    /** 订单状态：待支付/已支付/已取消/已完成 */
    private String status;

    /** 客户姓名（冗余） */
    private String customerName;

    /** 客户电话（冗余） */
    private String customerPhone;

    /** 开发商公司名（冗余） */
    private String developerName;

    /** 支付时间 */
    private LocalDateTime paidTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 取消时间 */
    private LocalDateTime cancelledTime;

    /** 取消原因 */
    private String cancelReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
