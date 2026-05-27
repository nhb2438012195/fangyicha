package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@TableName("knowledge_documents")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long developerId;

    private String filename;

    private String storedFilename;

    private Long fileSize;

    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadedTime;

    private LocalDateTime indexedTime;
}
