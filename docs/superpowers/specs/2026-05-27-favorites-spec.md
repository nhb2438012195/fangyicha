# 收藏功能 — 规格说明书

> 日期：2026-05-27 | 版本：v1.0 | 类型：完整规格说明书（可直接用于开发实现）
> 关联需求：[收藏功能需求记录](2026-05-27-favorites-requirements.md) | [AI 购房助手](2026-05-26-ai-assistant-requirements.md)

---

## 目录

1. [概述](#1-概述)
2. [数据库设计](#2-数据库设计)
3. [后端架构](#3-后端架构)
4. [API 接口规范](#4-api-接口规范)
5. [前端组件设计](#5-前端组件设计)
6. [交互流程](#6-交互流程)
7. [错误处理](#7-错误处理)
8. [AI 助手集成](#8-ai-助手集成)
9. [边界情况与约束](#9-边界情况与约束)
10. [测试策略](#10-测试策略)
11. [实现注意事项](#11-实现注意事项)

---

## 1. 概述

收藏功能允许购房客户（CUSTOMER 角色）在浏览楼盘时，将感兴趣的房产添加到个人收藏列表，并在工作台 Dashboard 中集中查看。

### 1.1 核心概念

| 概念 | 说明 |
|------|------|
| 收藏（Favorite） | 客户与楼盘之间的关联关系，表示客户对该楼盘感兴趣 |
| 收藏切换（Toggle） | 同一操作执行收藏或取消收藏，根据当前状态决定 |
| 收藏状态 | 每个客户对每个楼盘有且仅有两种状态：已收藏 / 未收藏 |

### 1.2 架构定位

```
┌─────────────────────────────────────────────────┐
│                  前端 (Vue 3)                    │
│  ┌──────────────┐  ┌──────────────┐              │
│  │ PropertyDetail│  │  Dashboard   │              │
│  │  (收藏按钮)   │  │ (收藏列表)   │              │
│  └──────┬───────┘  └──────┬───────┘              │
│         │                  │                      │
│         ▼                  ▼                      │
│  ┌──────────────────────────────────────────┐    │
│  │          favoritesApi (api 层)            │    │
│  └──────────────────┬───────────────────────┘    │
└─────────────────────┼────────────────────────────┘
                      │ HTTP + JWT
┌─────────────────────┼────────────────────────────┐
│  ┌──────────────────▼───────────────────────┐    │
│  │        FavoriteController (/api/favorites)│    │
│  └──────────────────┬───────────────────────┘    │
│                     │                             │
│  ┌──────────────────▼───────────────────────┐    │
│  │           FavoriteService                │    │
│  │  - toggleFavorite()                      │ 后端 (Spring Boot) │
│  │  - getFavorites()                        │    │
│  │  - isFavorited()                         │    │
│  │  - getFavoriteStatus()                   │    │
│  └──────────────────┬───────────────────────┘    │
│                     │                             │
│  ┌──────────────────▼───────────────────────┐    │
│  │           FavoriteMapper                 │    │
│  │           (MyBatis-Plus)                 │    │
│  └──────────────────┬───────────────────────┘    │
│                     │                             │
│              ┌──────▼──────┐                     │
│              │   MySQL     │                     │
│              │  favorites  │                     │
│              └─────────────┘                     │
└──────────────────────────────────────────────────┘

AI 助手通过 Function Calling 调用同一套 /api/favorites 接口，
不经过 AI 模块单独实现收藏逻辑。
```

### 1.3 与 AI 助手的关系

AI 助手模块不直接操作数据库，也不复制收藏逻辑。AI 侧通过 Function Calling 机制，向后端 `/api/favorites` 接口发起 HTTP 请求，与前端页面共用同一套 API 和同一份数据。

AI 助手侧只需要：
1. 在 System Prompt 中注册两个 function：`add_favorite`、`get_favorites`
2. Function 实现中调用 `favoritesApi`（与前端 API 相同）

具体集成细节见[第 8 节](#8-ai-助手集成)。

---

## 2. 数据库设计

### 2.1 表结构 DDL

```sql
-- ========================================
-- 收藏表
-- 客户对楼盘的收藏关系
-- 应用层保证 customer_id + property_id 唯一
-- ========================================
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    property_id BIGINT NOT NULL COMMENT '楼盘ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    -- 唯一约束：同一用户不能重复收藏同一楼盘
    UNIQUE KEY uk_customer_property (customer_id, property_id),

    -- 查询索引：按用户查询收藏列表（按时间倒序）
    INDEX idx_customer_created (customer_id, created_time DESC),

    -- 查询索引：按楼盘查询被哪些用户收藏（辅助统计，暂未使用）
    INDEX idx_property (property_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';
```

### 2.2 设计决策

| 决策项 | 选择 | 备选方案 | 理由 |
|--------|------|----------|------|
| 是否冗余楼盘信息 | 不冗余 | 在 favorite 表存 property_name 等字段 | 楼盘信息可能更新，冗余后需要联动更新；查询频率不高，join 一次可接受 |
| 唯一约束类型 | 联合唯一索引 | 应用层先查后插 | 数据库级唯一约束是最可靠的保证，防止并发插入重复 |
| 索引策略 | idx_customer_created (覆盖查询排序) | 仅在 customer_id 上建索引 | 加上排序字段可避免 filesort，提升列表查询性能 |
| 是否有 updated_time | 无 | 建议增加 | 收藏关系只有创建（created_time），没有更新操作，无需 updated_time |
| 是否有 deleted 软删除 | 无 | 建议增加 | 收藏操作的取消就是物理删除，不需要软删除 |
| 主键策略 | 自增 BIGINT | 雪花ID | 收藏表数据量大时自增 ID 更高效，且不需要跨库分表 |
| 字符集 | utf8mb4 | utf8 | 兼容 emoji 和生僻字，与项目中其他表保持一致 |

### 2.3 与其他表的关系

```
favorite.customer_id ──→ customer.id    (逻辑外键，无物理约束)
favorite.property_id  ──→ property.id   (逻辑外键，无物理约束)
```

本项目遵循无外键约束的设计原则，数据完整性由应用层保证。

---

## 3. 后端架构

### 3.1 包结构

```
backend/src/main/java/com/fangyicha/
├── common/
│   └── Result.java                        # 统一响应（已有，无需修改）
├── config/
│   ├── GlobalExceptionHandler.java        # 全局异常处理（已有，新增 DuplicateFavoriteException）
│   └── SecurityConfig.java                # 安全配置（已有，需确认 /api/favorites 路径）
├── entity/
│   └── Favorite.java                      # 收藏实体类（新建）
├── dto/
│   └── FavoriteDTO.java                   # 收藏列表展示 DTO（新建）
├── mapper/
│   └── FavoriteMapper.java                # MyBatis-Plus Mapper（新建）
├── service/
│   ├── FavoriteService.java               # 收藏服务接口（新建）
│   └── impl/
│       └── FavoriteServiceImpl.java       # 收藏服务实现（新建）
├── controller/
│   └── FavoriteController.java            # 收藏控制器（新建）
└── exception/
    └── DuplicateFavoriteException.java    # 重复收藏异常（新建，可选）
```

### 3.2 实体类

```java
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
```

### 3.3 DTO

```java
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
```

### 3.4 Mapper

```java
package com.fangyicha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏数据访问层
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询用户的收藏列表（含楼盘信息）
     * 使用 LEFT JOIN 关联 property 表获取楼盘详情
     * 按收藏时间倒序排列
     */
    @Select("""
        SELECT
            f.id AS favoriteId,
            f.created_time AS createdTime,
            p.id AS propertyId,
            p.property_name AS propertyName,
            p.location,
            p.price_per_sqm AS pricePerSqm,
            p.total_price AS totalPrice,
            p.area_sqm AS areaSqm,
            p.floor_plan_type AS floorPlanType,
            p.decoration,
            SUBSTRING_INDEX(p.image_urls, ',', 1) AS imageUrl,
            d.company_name AS developerName
        FROM favorite f
        LEFT JOIN property p ON f.property_id = p.id
        LEFT JOIN developer d ON p.developer_id = d.id
        WHERE f.customer_id = #{customerId}
        ORDER BY f.created_time DESC
    """)
    List<FavoriteDTO> selectFavoritesWithProperty(@Param("customerId") Long customerId);
}
```

### 3.5 服务层

**接口：**
```java
package com.fangyicha.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务接口
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 切换收藏状态
     * 如果已收藏则取消收藏，如果未收藏则添加收藏
     *
     * @param customerId 当前登录客户ID
     * @param propertyId 目标楼盘ID
     * @return true=收藏成功, false=取消收藏成功
     * @throws IllegalArgumentException 楼盘不存在时抛出
     */
    boolean toggleFavorite(Long customerId, Long propertyId);

    /**
     * 获取用户的收藏列表（带楼盘信息）
     */
    List<FavoriteDTO> getFavorites(Long customerId);

    /**
     * 判断某楼盘是否已被当前用户收藏
     */
    boolean isFavorited(Long customerId, Long propertyId);

    /**
     * 批量查询多个楼盘的收藏状态
     * 用于列表页批量展示收藏状态
     *
     * @param customerId 客户ID
     * @param propertyIds 楼盘ID列表
     * @return Map<propertyId, isFavorited>
     */
    Map<Long, Boolean> getFavoriteStatusBatch(Long customerId, List<Long> propertyIds);
}
```

**实现：**
```java
package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.entity.Favorite;
import com.fangyicha.entity.Property;
import com.fangyicha.mapper.FavoriteMapper;
import com.fangyicha.mapper.PropertyMapper;
import com.fangyicha.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final PropertyMapper propertyMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, PropertyMapper propertyMapper) {
        this.favoriteMapper = favoriteMapper;
        this.propertyMapper = propertyMapper;
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long customerId, Long propertyId) {
        // 校验楼盘存在
        Property property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new IllegalArgumentException("楼盘不存在");
        }

        // 查询当前收藏状态
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .eq(Favorite::getPropertyId, propertyId);
        Favorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            // 已收藏 → 取消收藏
            favoriteMapper.deleteById(existing.getId());
            log.info("取消收藏: customerId={}, propertyId={}", customerId, propertyId);
            return false;
        } else {
            // 未收藏 → 添加收藏
            Favorite favorite = new Favorite();
            favorite.setCustomerId(customerId);
            favorite.setPropertyId(propertyId);
            try {
                favoriteMapper.insert(favorite);
            } catch (DuplicateKeyException e) {
                // 并发情况下的重复插入保护
                log.warn("重复收藏尝试: customerId={}, propertyId={}", customerId, propertyId);
                throw new DuplicateKeyException("已收藏该楼盘");
            }
            log.info("添加收藏: customerId={}, propertyId={}", customerId, propertyId);
            return true;
        }
    }

    @Override
    public List<FavoriteDTO> getFavorites(Long customerId) {
        return favoriteMapper.selectFavoritesWithProperty(customerId);
    }

    @Override
    public boolean isFavorited(Long customerId, Long propertyId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .eq(Favorite::getPropertyId, propertyId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Map<Long, Boolean> getFavoriteStatusBatch(Long customerId, List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getCustomerId, customerId)
               .in(Favorite::getPropertyId, propertyIds);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        Map<Long, Boolean> result = propertyIds.stream().collect(
            Collectors.toMap(id -> id, id -> false)
        );
        for (Favorite fav : favorites) {
            result.put(fav.getPropertyId(), true);
        }
        return result;
    }
}
```

### 3.6 控制器

```java
package com.fangyicha.controller;

import com.fangyicha.common.Result;
import com.fangyicha.dto.FavoriteDTO;
import com.fangyicha.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 * 处理客户收藏的增删查操作
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏管理", description = "客户收藏楼盘，支持添加、取消、列表查询")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 获取当前用户的收藏列表
     * 返回带楼盘信息的完整列表，按收藏时间倒序
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "收藏列表", description = "获取当前登录客户的收藏楼盘列表，按收藏时间倒序")
    public Result<List<FavoriteDTO>> getFavorites(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        List<FavoriteDTO> favorites = favoriteService.getFavorites(customerId);
        return Result.success(favorites);
    }

    /**
     * 添加收藏
     * 如果已收藏则返回错误提示，不做幂等处理
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "添加收藏", description = "收藏指定楼盘，如果已收藏则返回错误")
    public Result<Void> addFavorite(Authentication authentication, @RequestBody Map<String, Long> body) {
        Long propertyId = body.get("propertyId");
        if (propertyId == null) {
            return Result.badRequest("propertyId 不能为空");
        }
        Long customerId = (Long) authentication.getPrincipal();
        try {
            favoriteService.toggleFavorite(customerId, propertyId);
            return Result.success("收藏成功", null);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (DuplicateKeyException e) {
            return Result.badRequest("已收藏该楼盘");
        }
    }

    /**
     * 取消收藏
     * 如果未收藏则返回错误提示
     */
    @DeleteMapping("/{propertyId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "取消收藏", description = "取消对指定楼盘的收藏")
    public Result<Void> removeFavorite(Authentication authentication, @PathVariable Long propertyId) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean wasAdded = favoriteService.toggleFavorite(customerId, propertyId);
        if (wasAdded) {
            // toggleFavorite 返回 true 表示添加收藏，false 表示取消收藏
            // 这里 wasAdded=false 说明原来已收藏，已成功取消
            return Result.success("已取消收藏", null);
        } else {
            // wasAdded=true 说明原来未收藏，执行后变成了收藏，与 DELETE 语义不符
            // 这种情况发生在调用 toggleFavorite 时原来没有收藏记录
            return Result.notFound("未收藏该楼盘");
        }
    }

    /**
     * 查询某楼盘的收藏状态
     */
    @GetMapping("/status/{propertyId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "收藏状态", description = "查询指定楼盘是否已被当前用户收藏")
    public Result<Map<String, Boolean>> getFavoriteStatus(
            Authentication authentication, @PathVariable Long propertyId) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean favorited = favoriteService.isFavorited(customerId, propertyId);
        return Result.success(Map.of("favorited", favorited));
    }

    /**
     * 批量查询收藏状态（列表页使用）
     */
    @PostMapping("/status/batch")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "批量收藏状态", description = "批量查询多个楼盘是否已被当前用户收藏")
    public Result<Map<Long, Boolean>> getFavoriteStatusBatch(
            Authentication authentication, @RequestBody Map<String, List<Long>> body) {
        List<Long> propertyIds = body.get("propertyIds");
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Result.success(Map.of());
        }
        Long customerId = (Long) authentication.getPrincipal();
        Map<Long, Boolean> status = favoriteService.getFavoriteStatusBatch(customerId, propertyIds);
        return Result.success(status);
    }
}
```

### 3.7 安全配置更新

需要在 `SecurityConfig.java` 中确认 `/api/favorites/**` 路径的权限配置。由于 Controller 上使用了 `@PreAuthorize("hasRole('CUSTOMER')")`，且 `SecurityConfig` 中已经配置了 `.anyRequest().authenticated()`，API 路径 `/api/favorites` 将自动被 JWT 过滤器拦截，要求认证。无需单独在 `SecurityConfig` 中添加 permit 规则。

### 3.8 全局异常处理更新

在现有 `GlobalExceptionHandler.java` 中新增对 `DuplicateKeyException` 的处理：

```java
/**
 * 重复键异常（如重复收藏）
 */
@ExceptionHandler(DuplicateKeyException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
    log.warn("数据重复: {}", e.getMessage());
    return Result.error(409, e.getMessage());
}
```

---

## 4. API 接口规范

### 4.1 接口总览

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/favorites` | 获取收藏列表 | CUSTOMER |
| POST | `/api/favorites` | 添加收藏 | CUSTOMER |
| DELETE | `/api/favorites/{propertyId}` | 取消收藏 | CUSTOMER |
| GET | `/api/favorites/status/{propertyId}` | 查询单个收藏状态 | CUSTOMER |
| POST | `/api/favorites/status/batch` | 批量查询收藏状态 | CUSTOMER |

### 4.2 GET /api/favorites

**请求：** 无参数（从 JWT 中获取用户身份）

**响应示例（成功）：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "favoriteId": 1,
      "createdTime": "2026-05-27T10:30:00",
      "propertyId": 101,
      "propertyName": "阳光花园",
      "location": "北京市朝阳区建国路88号",
      "pricePerSqm": 85000.00,
      "totalPrice": 6800000.00,
      "areaSqm": 80.00,
      "floorPlanType": "两室一厅",
      "decoration": "精装",
      "imageUrl": "https://picsum.photos/seed/阳光花园/400/300",
      "developerName": "万科地产"
    }
  ]
}
```

**响应示例（空列表）：**
```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

### 4.3 POST /api/favorites

**请求体：**
```json
{
  "propertyId": 101
}
```

**响应成功（首次收藏）：**
```json
{
  "code": 200,
  "message": "收藏成功",
  "data": null
}
```

**响应失败（已收藏）：**
```json
{
  "code": 409,
  "message": "已收藏该楼盘",
  "data": null
}
```

**响应失败（楼盘不存在）：**
```json
{
  "code": 404,
  "message": "楼盘不存在",
  "data": null
}
```

### 4.4 DELETE /api/favorites/{propertyId}

**路径参数：** `propertyId` — 楼盘 ID

**响应成功：**
```json
{
  "code": 200,
  "message": "已取消收藏",
  "data": null
}
```

**响应失败（未收藏）：**
```json
{
  "code": 404,
  "message": "未收藏该楼盘",
  "data": null
}
```

### 4.5 GET /api/favorites/status/{propertyId}

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "favorited": true
  }
}
```

### 4.6 POST /api/favorites/status/batch

**请求体：**
```json
{
  "propertyIds": [101, 102, 103]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "101": true,
    "102": false,
    "103": true
  }
}
```

### 4.7 错误码汇总

| 状态码 | code | message | 场景 |
|--------|------|---------|------|
| 200 | 200 | 收藏成功/已取消收藏 | 正常操作 |
| 400 | 400 | propertyId 不能为空 | 请求参数缺失 |
| 404 | 404 | 楼盘不存在 | 传入的 propertyId 无效 |
| 404 | 404 | 未收藏该楼盘 | 要取消的收藏不存在 |
| 409 | 409 | 已收藏该楼盘 | 重复收藏（数据库唯一约束命中） |
| 401 | 401 | 未认证 | 未提供有效 JWT |

---

## 5. 前端组件设计

### 5.1 组件结构

```
frontend/src/
├── api/
│   └── favorite.ts                    # 收藏相关 API 封装（新建）
├── types/
│   └── index.ts                       # 新增 FavoriteItem 类型
└── views/
    └── customer/
        ├── components/
        │   ├── FavoriteButton.vue     # 收藏按钮组件（新建）
        │   └── FavoriteCard.vue       # 收藏卡片组件（新建）
        ├── PropertyDetailView.vue     # 房产详情页（新增收藏按钮）
        └── DashboardView.vue          # 工作台（新增收藏列表区域）
```

### 5.2 API 封装

```typescript
// frontend/src/api/favorite.ts
import api from './index'
import type { ApiResult, FavoriteItem } from '../types'

/** 收藏相关 API */
export const favoriteApi = {
  /** 获取收藏列表 */
  getList(): Promise<ApiResult<FavoriteItem[]>> {
    return api.get('/favorites').then(res => res.data)
  },

  /** 添加收藏 */
  add(propertyId: number): Promise<ApiResult<void>> {
    return api.post('/favorites', { propertyId }).then(res => res.data)
  },

  /** 取消收藏 */
  remove(propertyId: number): Promise<ApiResult<void>> {
    return api.delete(`/favorites/${propertyId}`).then(res => res.data)
  },

  /** 查询单个收藏状态 */
  getStatus(propertyId: number): Promise<ApiResult<{ favorited: boolean }>> {
    return api.get(`/favorites/status/${propertyId}`).then(res => res.data)
  },

  /** 批量查询收藏状态 */
  getStatusBatch(propertyIds: number[]): Promise<ApiResult<Record<number, boolean>>> {
    return api.post('/favorites/status/batch', { propertyIds }).then(res => res.data)
  }
}
```

### 5.3 类型定义

```typescript
// 在 frontend/src/types/index.ts 中新增

/** 收藏列表项 */
export interface FavoriteItem {
  favoriteId: number
  createdTime: string
  propertyId: number
  propertyName: string
  location: string
  pricePerSqm: number
  totalPrice: number
  areaSqm: number
  floorPlanType: string
  decoration: string
  imageUrl: string
  developerName: string
}
```

### 5.4 FavoriteButton 组件

收藏按钮组件，展示在房产详情页。

```vue
<!-- frontend/src/views/customer/components/FavoriteButton.vue -->
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { favoriteApi } from '../../../api/favorite'

const props = defineProps<{
  propertyId: number
  /** 初始收藏状态（由父组件传入，可选择直接使用或自行查询） */
  initialFavorited?: boolean
}>()

const emit = defineEmits<{
  change: [favorited: boolean]
}>()

const favorited = ref(props.initialFavorited ?? false)
const loading = ref(false)

async function toggleFavorite() {
  loading.value = true
  try {
    if (favorited.value) {
      await favoriteApi.remove(props.propertyId)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(props.propertyId)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
    emit('change', favorited.value)
  } catch (error: any) {
    // 409 已收藏 → 刷新状态
    if (error?.response?.status === 409) {
      favorited.value = true
    }
    // 其他错误由 api interceptor 统一处理
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (props.initialFavorited === undefined) {
    try {
      const res = await favoriteApi.getStatus(props.propertyId)
      favorited.value = res.data.favorited
    } catch {
      // 静默失败，默认未收藏
    }
  }
})
</script>

<template>
  <el-button
    :type="favorited ? 'warning' : 'default'"
    :icon="favorited ? StarFilled : Star"
    :loading="loading"
    :class="{ 'is-favorited': favorited }"
    @click="toggleFavorite"
  >
    {{ favorited ? '已收藏' : '收藏' }}
  </el-button>
</template>

<style scoped>
.is-favorited {
  --el-button-bg-color: #fff7e6;
  --el-button-border-color: #f5a623;
  --el-button-text-color: #f5a623;
}
</style>
```

**设计要点：**

- 按钮使用 Element Plus 的 `el-button`，收藏态用 filled star 图标 + 警告色，未收藏态用 outline star 图标
- 组件内部自行管理收藏状态（初次加载时调用 `/api/favorites/status/{id}` 查询）
- 支持 `initialFavorited` prop，父组件可直接传入已知状态避免额外请求
- 收藏/取消收藏后 emit `change` 事件，父组件可监听并联动刷新
- 点击后显示 loading 状态，防止重复操作
- 使用 `api interceptor` 统一处理错误，但 409 状态码需要特殊处理以同步状态

### 5.5 FavoriteCard 组件

收藏卡片组件，展示在 Dashboard 收藏列表和 AI 助手对话中。

```vue
<!-- frontend/src/views/customer/components/FavoriteCard.vue -->
<script setup lang="ts">
import type { FavoriteItem } from '../../../types'

defineProps<{
  item: FavoriteItem
}>()

const emit = defineEmits<{
  click: [propertyId: number]
}>()

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}
</script>

<template>
  <el-card
    shadow="never"
    class="favorite-card"
    @click="emit('click', item.propertyId)"
  >
    <div class="card-image">
      <img
        :src="item.imageUrl || `https://picsum.photos/seed/${encodeURIComponent(item.propertyName)}/400/300`"
        :alt="item.propertyName"
        loading="lazy"
      />
      <div class="card-overlay">
        <span class="overlay-text">查看详情</span>
      </div>
    </div>
    <div class="card-body">
      <div class="card-title">{{ item.propertyName }}</div>
      <div class="card-location">{{ item.location }}</div>
      <div class="card-meta">
        <span class="meta-type">{{ item.floorPlanType }}</span>
        <span class="meta-area">{{ item.areaSqm }}㎡</span>
        <span class="meta-decoration">{{ item.decoration }}</span>
      </div>
      <div class="card-price-row">
        <span class="card-price">{{ formatPrice(item.totalPrice) }}</span>
        <span class="card-unit-price">{{ formatPrice(item.pricePerSqm) }}/㎡</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.favorite-card {
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  border: 1px solid #e8ddd0;
}

.favorite-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-image {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.favorite-card:hover .card-image img {
  transform: scale(1.05);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.favorite-card:hover .card-overlay {
  opacity: 1;
}

.overlay-text {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 16px;
  border: 1px solid rgba(255,255,255,0.6);
  border-radius: 4px;
}

.card-body {
  padding: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-location {
  font-size: 12px;
  color: #8a7a6a;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.card-meta span {
  background: #f5f0eb;
  padding: 2px 8px;
  border-radius: 4px;
}

.card-price-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #e74c3c;
}

.card-unit-price {
  font-size: 12px;
  color: #999;
}
</style>
```

**与 PropertyRecommendCard 的关系：**

`FavoriteCard` 的视觉样式与 `PropertyRecommendCard` 保持一致（相同配色、圆角、悬停效果、价格格式），两者的区别在于：
- `FavoriteCard` 的 `imageUrl` 字段已由后端预处理为单张图片（`SUBSTRING_INDEX` 取第一张）
- `FavoriteCard` 不需要 `reason` 标签行（减少展示冗余）
- `FavoriteCard` 不需要 `developerName` 字段（列表环境已隐含）

### 5.6 PropertyDetailView 修改

在房产详情页的页面标题区域或购买按钮旁添加收藏按钮：

```vue
<!-- PropertyDetailView.vue 中新增内容（在 <template> 适当位置） -->
<div class="detail-header" v-if="property">
  <div class="title-row">
    <h1 class="property-title">{{ property.propertyName }}</h1>
    <FavoriteButton
      :property-id="property.id"
      @change="onFavoriteChange"
    />
  </div>
  <!-- 现有内容不变 -->
</div>

<script setup lang="ts">
// 在 script 中新增
import FavoriteButton from './components/FavoriteButton.vue'

function onFavoriteChange(favorited: boolean) {
  // 可在此处触发其他联动，例如刷新推荐等
}
</script>
```

### 5.7 DashboardView 收藏列表区域

在 `DashboardView.vue` 的 `<template>` 中，于推荐区域之后、快捷操作之前新增收藏列表区域：

```vue
<!-- 收藏楼盘列表 -->
<div class="favorites-section">
  <div class="section-header">
    <h3 class="section-title">我的收藏</h3>
    <el-button
      text
      size="small"
      :icon="Refresh"
      :loading="favLoading"
      @click="fetchFavorites"
    >
      刷新
    </el-button>
  </div>

  <el-skeleton :loading="favLoading" animated>
    <template #template>
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
        <el-skeleton-item variant="rect" style="height: 280px;" v-for="i in 3" :key="i" />
      </div>
    </template>
  </el-skeleton>

  <!-- 收藏卡片网格 -->
  <div v-if="!favLoading && favorites.length > 0" class="favorites-grid">
    <FavoriteCard
      v-for="item in favorites"
      :key="item.propertyId"
      :item="item"
      @click="goToProperty"
    />
  </div>

  <!-- 收藏空状态 -->
  <div v-if="!favLoading && favorites.length === 0" class="empty-fav">
    <el-empty description="暂无收藏的楼盘，去浏览楼盘吧" :image-size="60">
      <template #image>
        <svg viewBox="0 0 100 100" width="60" height="60" fill="none">
          <path d="M50 20 L50 80 M20 50 L80 50" stroke="#e8ddd0" stroke-width="2" stroke-linecap="round"/>
          <circle cx="50" cy="50" r="40" stroke="#e8ddd0" stroke-width="2" />
        </svg>
      </template>
      <el-button type="primary" size="small" @click="router.push('/customer/properties')">
        浏览楼盘
      </el-button>
    </el-empty>
  </div>
</div>
```

**对应的 script 逻辑：**
```typescript
import FavoriteCard from './components/FavoriteCard.vue'
import { favoriteApi } from '../../api/favorite'
import type { FavoriteItem } from '../../types'

// 新增状态变量
const favorites = ref<FavoriteItem[]>([])
const favLoading = ref(false)

async function fetchFavorites() {
  favLoading.value = true
  try {
    const res = await favoriteApi.getList()
    favorites.value = res.data || []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    favorites.value = []
  } finally {
    favLoading.value = false
  }
}

// 在 onMounted 中调用
onMounted(() => {
  fetchDashboard()
  fetchRecommendations(false)
  fetchFavorites()
})
```

**CSS 样式（与 recommend-grid 保持一致）：**
```css
.favorites-section {
  margin-bottom: 32px;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.empty-fav {
  padding: 40px 0;
  background: #fdf8f3;
  border-radius: 10px;
}

@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
```

---

## 6. 交互流程

### 6.1 收藏/取消收藏流程

```
用户点击收藏按钮
    │
    ├── JWT 未登录
    │   └── 按钮不显示（无需处理）
    │
    └── JWT 已登录
        │
        ├── 按钮状态：未收藏（空心星标）
        │   ├── 点击 → POST /api/favorites → 200
        │   │   ├── 按钮变实心星标 + "已收藏"
        │   │   └── Toast: "收藏成功"
        │   └── 点击 → POST /api/favorites → 409
        │       └── 按钮自动恢复为实心星标（与服务器同步）
        │
        └── 按钮状态：已收藏（实心星标）
            ├── 点击 → DELETE /api/favorites/{id} → 200
            │   ├── 按钮变空心星标 + "收藏"
            │   └── Toast: "已取消收藏"
            └── 点击 → DELETE /api/favorites/{id} → 404
                └── 按钮自动恢复为空心星标（与服务器同步）
```

### 6.2 Dashboard 收藏列表加载流程

```
进入 Dashboard 页面
    │
    ├── 加载中 → 骨架屏
    │
    ├── 加载成功 → 列表 > 0 → 展示卡片网格
    │
    ├── 加载成功 → 列表 = 0 → 展示空状态
    │   └── "暂无收藏的楼盘，去浏览楼盘吧"
    │       └── 点击按钮 → 跳转 /customer/properties
    │
    ├── 加载失败 → 展示错误提示 + 重试按钮
    │
    └── 用户手动点击"刷新"按钮 → 重新调用 API
```

### 6.3 收藏数量上限

当前设计中，收藏数量无上限。如果未来需要限制，可以在 `FavoriteService.toggleFavorite()` 中增加计数检查逻辑。

### 6.4 跨页面状态同步

当用户在房产详情页收藏/取消收藏后，返回 Dashboard 页面时，收藏列表需要反映最新状态。

两种方案（二选一，推荐方案 A）：

| 方案 | 实现 | 优点 | 缺点 |
|------|------|------|------|
| **A. 每次进入 Dashboard 重新请求** | `onMounted` 中调用 `fetchFavorites()` | 实现简单，保证数据最新 | 增加一次 API 请求 |
| B. 全局状态管理（Pinia） | 使用 Pinia store 管理收藏状态 | 跨页面实时同步 | 引入额外复杂度，小功能无需状态管理 |

**选择方案 A**，理由：
- 收藏数据变更频率低（用户只在详情页操作）
- Dashboard 页面切换时会重新挂载 `onMounted`，自动刷新
- 避免引入额外的全局状态管理

---

## 7. 错误处理

### 7.1 后端错误处理矩阵

| 错误场景 | 检测时机 | 异常类型 | HTTP 状态码 | 响应 message | 处理策略 |
|----------|----------|----------|-------------|-------------|----------|
| 未提供 JWT | 过滤器 | 无 | 401 | 未认证 | 前端跳转登录页 |
| JWT 过期 | 过滤器 | ExpiredJwtException | 401 | 登录已过期，请重新登录 | 前端清除 token 并跳转登录 |
| 角色非 CUSTOMER | 控制器 | AccessDeniedException | 403 | 权限不足 | 前端弹提示 |
| POST 缺少 propertyId | Controller | 无 | 400 | propertyId 不能为空 | 前端显示错误信息 |
| 楼盘 ID 无效 | Service | IllegalArgumentException | 404 | 楼盘不存在 | 前端显示"楼盘已下架或不存在" |
| 重复收藏（唯一约束） | Mapper/SQL | DuplicateKeyException | 409 | 已收藏该楼盘 | 前端同步状态为"已收藏" |
| DELETE 未收藏记录 | Service | 无 | 404 | 未收藏该楼盘 | 前端同步状态为"未收藏" |
| 系统内部错误 | 任意 | Exception | 500 | 系统繁忙，请稍后重试 | 前端显示通用错误提示 |

### 7.2 前端错误处理策略

| 错误码 | 前端处理 |
|--------|----------|
| 400 | 在按钮区域显示红色提示文字（如"数据有误，请重试"） |
| 401/403 | 由 `api interceptor` 统一处理，收藏按钮不会在未登录时展示 |
| 404 | 收藏按钮：不做 toast，也不报错，认为楼盘已不存在，按钮置灰不可点击 |
| 404 (取消收藏) | 同步本地状态为"未收藏"，不做 toast |
| 409 | 同步本地状态为"已收藏"，不做 toast（服务器状态为准） |
| 500 | 显示 Toast "操作失败，请稍后重试"，保持原有状态不变 |
| 网络异常 | 显示 Toast "网络连接失败"，保持原有状态不变 |

**原则：** 前端始终以服务器响应状态为准，不尝试"乐观更新"。因为收藏操作的并发冲突概率低，但数据一致性要求高。

---

## 8. AI 助手集成

### 8.1 Function Calling 定义

AI 助手的 LLM Function Calling 注册两个 function：

```json
{
  "type": "function",
  "function": {
    "name": "add_favorite",
    "description": "收藏指定的楼盘，用户说"帮我收藏"时调用",
    "parameters": {
      "type": "object",
      "properties": {
        "propertyId": {
          "type": "integer",
          "description": "楼盘ID，从对话上下文或推荐结果中获取"
        }
      },
      "required": ["propertyId"]
    }
  }
}
```

```json
{
  "type": "function",
  "function": {
    "name": "get_favorites",
    "description": "查看用户的收藏楼盘列表，用户说"看看我的收藏"时调用，支持自然语言条件筛选",
    "parameters": {
      "type": "object",
      "properties": {
        "keyword": {
          "type": "string",
          "description": "筛选关键字（可选），如区域名、楼盘名等，从用户对话中提取"
        },
        "maxPrice": {
          "type": "number",
          "description": "价格上限（可选），单位为万元，用于按预算筛选"
        }
      }
    }
  }
}
```

### 8.2 Function 实现

AI 助手模块在后端实现这两个 function，调用的是同一套 `FavoriteService`，而非直接操作数据库：

```java
// AI 助手模块中的 Function 实现（伪代码，在 AI 助手的 service 中实现）

@Component
public class FavoriteFunctions {

    private final FavoriteService favoriteService;

    public FavoriteFunctions(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 收藏楼盘 Function
     * 直接在 Java 层调用 FavoriteService，不走 HTTP
     */
    public String addFavorite(Long customerId, Integer propertyId) {
        try {
            favoriteService.toggleFavorite(customerId, propertyId.longValue());
            return "收藏成功";
        } catch (IllegalArgumentException e) {
            return "收藏失败：" + e.getMessage();
        } catch (DuplicateKeyException e) {
            return "已收藏该楼盘";
        }
    }

    /**
     * 查询收藏列表 Function
     * 支持关键字和价格上限条件筛选（在 Java 层过滤，不涉及 SQL 动态条件）
     */
    public String getFavorites(Long customerId, String keyword, Double maxPrice) {
        List<FavoriteDTO> favorites = favoriteService.getFavorites(customerId);
        // 筛选逻辑
        Stream<FavoriteDTO> stream = favorites.stream();
        if (keyword != null && !keyword.isBlank()) {
            stream = stream.filter(f ->
                f.getPropertyName().contains(keyword) ||
                f.getLocation().contains(keyword)
            );
        }
        if (maxPrice != null) {
            stream = stream.filter(f ->
                f.getTotalPrice().compareTo(BigDecimal.valueOf(maxPrice * 10000)) <= 0
            );
        }
        List<FavoriteDTO> result = stream.limit(5).collect(Collectors.toList());
        // 格式化为 JSON 或卡片数据返回给 AI 模块
        return JSON.toJSONString(result);
    }
}
```

### 8.3 AI 对话映射

| 用户说 | 触发的 function | 后端操作 |
|--------|----------------|----------|
| "帮我收藏这个楼盘" | add_favorite | 调用 FavoriteService.toggleFavorite() |
| "收藏阳光花园" | add_favorite | 从上下文提取 propertyId |
| "看看我的收藏" | get_favorites | 调用 FavoriteService.getFavorites() |
| "朝阳区的收藏有哪些" | get_favorites (keyword="朝阳") | 调用 FavoriteService.getFavorites() + Java 层过滤 |
| "300万以内的收藏" | get_favorites (maxPrice=300) | 调用 FavoriteService.getFavorites() + Java 层过滤 |

### 8.4 数据一致性保障

AI 侧的收藏操作与前端页面操作共享同一张 `favorite` 表，因此：

- AI 添加的收藏，前端 Dashboard 能立即看到（下次请求时）
- 前端的收藏/取消，AI 侧也能实时感知
- 无需额外同步逻辑

---

## 9. 边界情况与约束

### 9.1 并发操作

| 场景 | 问题 | 解决方案 |
|------|------|----------|
| 用户快速连点收藏按钮 | 两次 POST 请求几乎同时到达，第一次插入了记录，第二次违反唯一约束 | 数据库 `UNIQUE KEY` 兜底，捕获 `DuplicateKeyException` 返回 409 |
| 用户同时在前端和 AI 助手中收藏同一楼盘 | 同上 | 同上 |
| 用户 A 先收藏，然后楼盘被开发商删除 | 收藏记录还在，但 property_id 已失效 | `toggleFavorite` 中先校验 `propertyMapper.selectById(propertyId)`，不存在则抛出 `IllegalArgumentException` |

### 9.2 权限约束

| 场景 | 约束 | 处理 |
|------|------|------|
| 未登录用户看到收藏按钮 | 不显示 | 登录检查在前端完成 |
| 开发商账户调用收藏 API | `@PreAuthorize("hasRole('CUSTOMER')")` 拦截 | 返回 403 |
| 客户 A 查看客户 B 的收藏 | JWT 中的 customerId 只取自 `authentication.getPrincipal()` | 返回 A 自己的收藏，无法越权 |
| 被删除的楼盘在收藏列表中 | left join 后 property 字段为 null | Mapper 的 `LEFT JOIN` 保证收藏记录仍在，但楼盘信息为空。前端展示时判断 propertyId 是否为 null |

### 9.3 数据一致性

| 场景 | 处理方式 |
|------|----------|
| 楼盘被删除后，收藏记录如何处理 | 保留收藏记录（LEFT JOIN 后 property 字段为 null），前端展示时隐藏或标记为"已下架" |
| 楼盘被删除后，收藏按钮如何处理 | 详情页已不可访问（404），按钮不存在 |
| 客户账号被删除 | 收藏记录保留，但前端不可见（无法登录） |

### 9.4 数据展示约束

| 场景 | 处理 |
|------|------|
| 收藏列表为空 | 展示空状态引导，见 [6.2 节 Dashboard 收藏列表加载流程](#62-dashboard-收藏列表加载流程) |
| 收藏列表超过一屏 | 目前不做分页，一次性返回全部。预计单个用户最多数百条收藏，性能可接受 |
| 收藏卡片的图片不存在 | 使用 `picsum.photos` 兜底图（与 `PropertyRecommendCard` 行为一致） |
| 收藏的楼盘已被删除 | 卡片中 propertyId 为 null，不做渲染 |

---

## 10. 测试策略

### 10.1 单元测试

**后端单元测试（JUnit 5 + Mockito）：**

| 测试类 | 测试方法 | 测试用例 |
|--------|----------|----------|
| FavoriteServiceImplTest | toggleFavorite_shouldAdd_whenNotFavorited | 未收藏时调用，验证 insert 被调用 |
| FavoriteServiceImplTest | toggleFavorite_shouldRemove_whenFavorited | 已收藏时调用，验证 delete 被调用 |
| FavoriteServiceImplTest | toggleFavorite_shouldThrow_whenPropertyNotFound | 不存在的 propertyId，验证抛出 IllegalArgumentException |
| FavoriteServiceImplTest | getFavorites_shouldReturnList | 用户有收藏时，验证返回列表 |
| FavoriteServiceImplTest | getFavorites_shouldReturnEmptyList | 用户无收藏时，验证返回空列表 |
| FavoriteServiceImplTest | isFavorited_shouldReturnTrue_whenFavorited | 已收藏，验证返回 true |
| FavoriteServiceImplTest | isFavorited_shouldReturnFalse_whenNotFavorited | 未收藏，验证返回 false |
| FavoriteServiceImplTest | getFavoriteStatusBatch_shouldReturnCorrectMap | 批量查询，验证返回正确状态映射 |

**后端集成测试（SpringBootTest + H2 内嵌数据库）：**

| 测试类 | 测试用例 |
|--------|----------|
| FavoriteControllerTest | POST /api/favorites 正常收藏 |
| FavoriteControllerTest | POST /api/favorites 重复收藏返回 409 |
| FavoriteControllerTest | POST /api/favorites 楼盘不存在返回 404 |
| FavoriteControllerTest | DELETE /api/favorites/{id} 取消收藏 |
| FavoriteControllerTest | DELETE /api/favorites/{id} 未收藏返回 404 |
| FavoriteControllerTest | GET /api/favorites 返回收藏列表 |
| FavoriteControllerTest | GET /api/favorites 无收藏返回空列表 |
| FavoriteControllerTest | GET /api/favorites/status/{id} 已收藏 |
| FavoriteControllerTest | GET /api/favorites/status/{id} 未收藏 |
| FavoriteControllerTest | POST /api/favorites/status/batch 批量查询 |

### 10.2 前端测试

**组件测试（Vitest + Vue Test Utils）：**

| 组件 | 测试用例 |
|------|----------|
| FavoriteButton | 未收藏状态渲染空心星标 |
| FavoriteButton | 已收藏状态渲染实心星标 |
| FavoriteButton | 点击未收藏按钮调用 add API |
| FavoriteButton | 点击已收藏按钮调用 remove API |
| FavoriteButton | API 失败时保持原有状态 |
| FavoriteButton | 未登录时不渲染 |
| FavoriteButton | loading 状态禁用点击 |
| FavoriteCard | 正常渲染所有字段 |
| FavoriteCard | 点击触发 navigate 到详情页 |
| FavoriteCard | 图片加载失败显示兜底图 |

**E2E 测试（Playwright / Cypress）：**

| 场景 | 步骤 |
|------|------|
| 用户收藏楼盘 | 登录 → 打开详情页 → 点击收藏按钮 → 验证按钮变为"已收藏" |
| 用户取消收藏 | 登录 → 打开已收藏的详情页 → 点击"已收藏"按钮 → 验证按钮变为"收藏" |
| 查看收藏列表 | 登录 → 进入 Dashboard → 看到收藏列表区域 |
| 收藏列表空状态 | 登录 → 确保无收藏 → 进入 Dashboard → 看到空状态 |
| 从收藏列表跳转详情 | 登录 → 有收藏 → 进入 Dashboard → 点击收藏卡片 → 跳转到详情页 |
| 重复收藏提示 | 登录 → 多次快速点击收藏按钮 → 状态最终与服务器同步 |

### 10.3 测试数据准备

测试用收藏数据通过 `DataInitializer` 或测试专用的 SQL 脚本初始化：

```sql
-- 测试数据样例
INSERT INTO favorite (customer_id, property_id, created_time) VALUES
(1, 1, '2026-05-20 10:00:00'),
(1, 2, '2026-05-21 14:30:00'),
(1, 3, '2026-05-22 09:15:00');
```

建议在集成测试中使用 `@Sql` 注解加载测试数据，或在测试类中用 `@BeforeEach` 通过 MyBatis-Plus 的 `save` 方法构造测试数据。

---

## 11. 实现注意事项

### 11.1 开发顺序

建议按以下顺序实现，每一步完成后验证再进入下一步：

| 步骤 | 内容 | 预计工时 |
|------|------|----------|
| 1 | MySQL 建表（favorite DDL） | 0.5h |
| 2 | 后端 Entity + Mapper 层 | 0.5h |
| 3 | 后端 Service 层（含单元测试） | 1.5h |
| 4 | 后端 Controller 层（含集成测试） | 1h |
| 5 | 前端 API 封装 + 类型定义 | 0.5h |
| 6 | FavoriteButton 组件 + PropertyDetailView 集成 | 1h |
| 7 | FavoriteCard 组件 + Dashboard 收藏列表 | 1h |
| 8 | 全局异常处理更新 + 安全配置确认 | 0.5h |
| 9 | E2E 测试 | 1h |
| 10 | AI 助手 Function Calling 集成 | 1h |
| | **总计** | **~8.5h** |

### 11.2 与推荐功能的联动

收藏行为可以作为推荐系统的正向信号。但在本功能中，收藏不直接影响推荐的排序逻辑。未来可通过以下方式联动：
- 在 `ActivityLogService` 中记录收藏事件（`action=CREATE, entityType=Favorite`）
- 推荐算法可以将收藏过的同区域/同价格区间楼盘作为"更多推荐"的因子

### 11.3 后续可扩展

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 收藏数统计 | 在 PropertyDetailDTO 中增加 `favoriteCount` 字段 | 低 |
| 收藏分组 | 支持自定义收藏夹分组（如"朝阳区""300万以下"） | 低 |
| 收藏上限 | 限制单个用户最多收藏数量 | 低（监控后再决定） |
| 收藏排序 | 支持按价格/区域重新排序 | 低 |
| 收藏提醒 | 收藏的楼盘降价时发送通知 | 低 |

### 11.4 已知约束

- **不分页：** 收藏列表目前一次性返回全部数据。预计单用户收藏数在百级左右，性能可接受。如果未来出现万级收藏用户，需增加分页。
- **不缓存：** 收藏列表不做前端缓存，每次进入 Dashboard 重新请求。Dashboard 页面预期访问频率低（与推荐不同），缓存收益不大。
- **不乐观更新：** 前端在收藏操作时不提前切换 UI 状态，等待服务器响应后再更新。这是为确保数据一致性，虽然会牺牲一点点响应速度感知，但避免了状态不同步的问题。

### 11.5 Anti-pattern 避免清单

| 反模式 | 说明 | 正确做法 |
|--------|------|----------|
| 收藏按钮用 icon 组件而非 el-button | 不可访问（无键盘焦点、无 loading 态） | 使用 `el-button` + 自定义图标 |
| 收藏状态用 localStorage 持久化 | 无法与服务器同步 | 每次打开页面重新查询 |
| 取消收藏使用 POST 请求 | 不符合 RESTful 语义 | 使用 DELETE 方法 |
| 添加收藏用 GET 请求（以 query param 传 propertyId） | 有副作用的操作应当使用 POST | 使用 POST 方法 |
| 在 AI 助手模块中重新建表存储收藏 | 数据不一致 | 共用 favorite 表和 API |
| 乐观更新 + 失败回滚 | 收藏场景不需要"瞬时响应" | 先请求后更新 |

---

## 附录

### A. 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/main/resources/db/schema.sql` | 修改 | 新增 favorite 表 DDL |
| `backend/src/main/java/com/fangyicha/entity/Favorite.java` | 新增 | 收藏实体类 |
| `backend/src/main/java/com/fangyicha/dto/FavoriteDTO.java` | 新增 | 收藏列表 DTO |
| `backend/src/main/java/com/fangyicha/mapper/FavoriteMapper.java` | 新增 | MyBatis-Plus Mapper |
| `backend/src/main/java/com/fangyicha/service/FavoriteService.java` | 新增 | 服务接口 |
| `backend/src/main/java/com/fangyicha/service/impl/FavoriteServiceImpl.java` | 新增 | 服务实现 |
| `backend/src/main/java/com/fangyicha/controller/FavoriteController.java` | 新增 | API 控制器 |
| `backend/src/main/java/com/fangyicha/config/GlobalExceptionHandler.java` | 修改 | 新增 DuplicateKeyException 处理 |
| `frontend/src/types/index.ts` | 修改 | 新增 FavoriteItem 类型 |
| `frontend/src/api/favorite.ts` | 新增 | 收藏 API 封装 |
| `frontend/src/views/customer/components/FavoriteButton.vue` | 新增 | 收藏按钮组件 |
| `frontend/src/views/customer/components/FavoriteCard.vue` | 新增 | 收藏卡片组件 |
| `frontend/src/views/customer/PropertyDetailView.vue` | 修改 | 集成收藏按钮 |
| `frontend/src/views/customer/DashboardView.vue` | 修改 | 新增收藏列表区域 |

### B. 相关文档

- [收藏功能需求记录](../2026-05-27-favorites-requirements.md) — 高层需求定义
- [AI 购房助手需求记录](../2026-05-26-ai-assistant-requirements.md) — AI 助手 Function Calling 集成参考
