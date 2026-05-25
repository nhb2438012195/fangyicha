package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表记录实体类
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long userId;

    /** 角色 */
    private String userRole;

    /** 报表类型 */
    private String reportType;

    /** 查询参数（JSON） */
    private String queryParams;

    /** 结果摘要（JSON） */
    private String resultSummary;

    /** PDF文件路径 */
    private String filePath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
