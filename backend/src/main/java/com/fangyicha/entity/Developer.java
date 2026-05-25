package com.fangyicha.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开发商实体类
 */
@Data
@TableName("developer")
public class Developer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 公司地址 */
    private String address;

    /** 营业执照号 */
    private String businessLicense;

    /** 公司简介 */
    private String description;

    /** 登录用户名 */
    private String username;

    /** 登录密码（BCrypt加密） */
    private String password;

    /** 状态：1=启用 0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
