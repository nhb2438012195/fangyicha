package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单操作日志实体类
 */
@Data
@TableName("order_log")
public class OrderLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 操作人ID */
    private Long actorId;

    /** 操作人角色 */
    private String actorRole;

    /** 操作类型 */
    private String action;

    /** 操作前状态 */
    private String fromStatus;

    /** 操作后状态 */
    private String toStatus;

    /** 操作详情 */
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
