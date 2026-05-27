package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI消息实体
 */
@Data
@TableName("ai_message")
public class AiMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    @TableField("message_type")
    private String messageType;

    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
