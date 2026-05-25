package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 建议/购房意向实体类
 */
@Data
@TableName("suggestion")
public class Suggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 开发商ID */
    private Long developerId;

    /** 偏好户型 */
    private String preferredType;

    /** 最低预算 */
    private BigDecimal priceMin;

    /** 最高预算 */
    private BigDecimal priceMax;

    /** 备注说明 */
    private String notes;

    /** 状态：待回复/已回复/已关闭 */
    private String status;

    /** 回复内容 */
    private String replyContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 以下字段用于联表查询，非数据库字段 */
    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String developerName;
}
