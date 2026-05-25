package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价格历史实体类
 * 记录每个房产的月度价格变动
 */
@Data
@TableName("price_history")
public class PriceHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 房产ID */
    private Long propertyId;

    /** 记录日期（月粒度） */
    private LocalDate recordDate;

    /** 当月单价（元/平米） */
    private BigDecimal pricePerSqm;

    /** 当月总价 */
    private BigDecimal totalPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
