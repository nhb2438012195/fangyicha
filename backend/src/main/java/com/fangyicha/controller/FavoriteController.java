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

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "收藏列表", description = "获取当前登录客户的收藏楼盘列表，按收藏时间倒序")
    public Result<List<FavoriteDTO>> getFavorites(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        List<FavoriteDTO> favorites = favoriteService.getFavorites(customerId);
        return Result.success(favorites);
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "添加收藏", description = "收藏指定楼盘")
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

    @DeleteMapping("/{propertyId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "取消收藏", description = "取消对指定楼盘的收藏")
    public Result<Void> removeFavorite(Authentication authentication, @PathVariable Long propertyId) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean wasAdded = favoriteService.toggleFavorite(customerId, propertyId);
        if (!wasAdded) {
            return Result.success("已取消收藏", null);
        } else {
            return Result.notFound("未收藏该楼盘");
        }
    }

    @GetMapping("/status/{propertyId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "收藏状态", description = "查询指定楼盘是否已被当前用户收藏")
    public Result<Map<String, Boolean>> getFavoriteStatus(
            Authentication authentication, @PathVariable Long propertyId) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean favorited = favoriteService.isFavorited(customerId, propertyId);
        return Result.success(Map.of("favorited", favorited));
    }

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
