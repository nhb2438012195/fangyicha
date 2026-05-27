package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI待确认订单实体
 */
@Data
@TableName("pending_order")
public class PendingOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long customerId;

    private Long propertyId;

    private String customerName;

    private String customerPhone;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime confirmedTime;
}
