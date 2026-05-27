package com.fangyicha.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收藏列表展示 DTO
 * 包含楼盘基本信息，用于前端列表展示
 */
@Data
public class FavoriteDTO {

    /** 收藏记录ID */
    private Long favoriteId;

    /** 收藏时间 */
    private String createdTime;

    /** 楼盘ID */
    private Long propertyId;

    /** 楼盘名称 */
    private String propertyName;

    /** 地理位置 */
    private String location;

    /** 单价（元/平米） */
    private BigDecimal pricePerSqm;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 面积（平米） */
    private BigDecimal areaSqm;

    /** 户型 */
    private String floorPlanType;

    /** 装修情况 */
    private String decoration;

    /** 图片URL（取第一张） */
    private String imageUrl;

    /** 开发商名称 */
    private String developerName;
}
