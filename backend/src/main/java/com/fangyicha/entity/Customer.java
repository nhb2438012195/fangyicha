package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户实体类
 */
@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 登录密码（BCrypt加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 购房意向（逗号分隔） */
    private String intention;

    /** 偏好区域 */
    private String preferredLocations;

    /** 最低预算 */
    private BigDecimal budgetMin;

    /** 最高预算 */
    private BigDecimal budgetMax;

    /** 购房紧迫度 */
    private String urgency;

    /** 状态：1=启用 0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
