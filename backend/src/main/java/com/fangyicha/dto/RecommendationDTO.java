package com.fangyicha.dto;

import com.fangyicha.entity.Property;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房产推荐 DTO
 * 包含推荐理由
 */
@Data
public class RecommendationDTO {

    /** 房产ID */
    private Long propertyId;

    /** 楼盘名称 */
    private String propertyName;

    /** 位置 */
    private String location;

    /** 单价 */
    private BigDecimal pricePerSqm;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 面积 */
    private BigDecimal areaSqm;

    /** 户型 */
    private String floorPlanType;

    /** 装修情况 */
    private String decoration;

    /** 图片URL */
    private String imageUrls;

    /** 开发商名称 */
    private String developerName;

    /** 推荐理由 */
    private String reason;
}
