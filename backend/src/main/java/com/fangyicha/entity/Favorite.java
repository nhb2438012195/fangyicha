package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体类
 * 记录客户与楼盘之间的收藏关系
 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 楼盘ID */
    private Long propertyId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
