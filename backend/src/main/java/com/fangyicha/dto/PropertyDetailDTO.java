package com.fangyicha.dto;

import com.fangyicha.entity.Property;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 房产详情 DTO
 * 在 Property 基础之上增加开发商名称
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PropertyDetailDTO extends Property {

    /** 开发商公司名称 */
    private String developerName;

    /** 当前登录客户是否已收藏 */
    private Boolean favorited;
}
