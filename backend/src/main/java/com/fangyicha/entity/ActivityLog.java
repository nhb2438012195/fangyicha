package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动日志实体类
 */
@Data
@TableName("activity_log")
public class ActivityLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long actorId;

    /** 操作人角色 */
    private String actorRole;

    /** 操作类型 CREATE/UPDATE/DELETE */
    private String action;

    /** 实体类型 */
    private String entityType;

    /** 实体ID */
    private Long entityId;

    /** 操作详情 */
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
