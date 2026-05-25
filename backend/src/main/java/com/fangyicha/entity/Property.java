package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房产/楼盘实体类
 */
@Data
@TableName("property")
public class Property {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属开发商ID */
    private Long developerId;

    /** 楼盘名称 */
    private String propertyName;

    /** 地理位置 */
    private String location;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 最低楼层 */
    private Integer floorMin;

    /** 最高楼层 */
    private Integer floorMax;

    /** 户型 */
    private String floorPlanType;

    /** 总户数 */
    private Integer totalUnits;

    /** 空置户数 */
    private Integer vacantUnits;

    /** 空置率（自动计算：vacant/total * 100） */
    private BigDecimal vacancyRate;

    /** 单价（元/平米） */
    private BigDecimal pricePerSqm;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 面积（平米） */
    private BigDecimal areaSqm;

    /** 装修情况：毛坯/简装/精装/豪装 */
    private String decoration;

    /** 状态：在售/已售/待开盘 */
    private String status;

    /** 楼盘描述 */
    private String description;

    /** 图片URL（逗号分隔） */
    private String imageUrls;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
