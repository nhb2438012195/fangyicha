package com.fangyicha.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 房产多条件查询请求DTO
 */
@Data
public class PropertyQueryRequest {

    /** 当前页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 排序字段 */
    private String sortBy;

    /** 排序方向：asc/desc */
    private String sortOrder;

    /** 关键字（模糊匹配楼盘名称、位置） */
    private String keyword;

    /** 位置（模糊匹配） */
    private String location;

    /** 最低楼层 */
    private Integer floorMin;

    /** 最高楼层 */
    private Integer floorMax;

    /** 户型（多选用逗号分隔） */
    private String floorPlanType;

    /** 最低单价 */
    private BigDecimal priceMin;

    /** 最高单价 */
    private BigDecimal priceMax;

    /** 最低总价 */
    private BigDecimal totalPriceMin;

    /** 最高总价 */
    private BigDecimal totalPriceMax;

    /** 最低空置率 */
    private BigDecimal vacancyRateMin;

    /** 最高空置率 */
    private BigDecimal vacancyRateMax;

    /** 房产状态 */
    private String status;

    /** 装修情况 */
    private String decoration;

    /** 开发商ID（用于开发商查看自己的房产） */
    private Long developerId;
}
