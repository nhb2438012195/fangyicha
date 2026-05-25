package com.fangyicha.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.dto.PropertyDetailDTO;
import com.fangyicha.dto.PropertyQueryRequest;
import com.fangyicha.entity.Developer;
import com.fangyicha.entity.PriceHistory;
import com.fangyicha.entity.Property;
import com.fangyicha.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 房产控制器
 * 处理房产CRUD、多条件查询、统计分析
 */
@Slf4j
@RestController
@RequestMapping("/api/properties")
@Tag(name = "房产管理", description = "房产信息CRUD、多条件查询、空置率统计分析")
public class PropertyController {

    private final PropertyService propertyService;
    private final ActivityLogService activityLogService;
    private final DeveloperService developerService;
    private final PriceHistoryService priceHistoryService;

    public PropertyController(PropertyService propertyService,
                              ActivityLogService activityLogService,
                              DeveloperService developerService,
                              PriceHistoryService priceHistoryService) {
        this.propertyService = propertyService;
        this.activityLogService = activityLogService;
        this.developerService = developerService;
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * 多条件分页查询房产（公开）
     * 支持关键字、位置、楼层、户型、价格、空置率等多种条件组合查询
     */
    @GetMapping
    @Operation(summary = "房产列表", description = "多条件分页查询房产，支持模糊匹配、范围筛选、排序")
    public Result<Page<Property>> listProperties(PropertyQueryRequest request) {
        Page<Property> result = propertyService.queryProperties(request);
        return Result.success(result, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取房产详情（公开）
     */
    @GetMapping("/{id}")
    @Operation(summary = "房产详情", description = "根据ID获取房产详细信息，包含开发商名称")
    public Result<PropertyDetailDTO> getProperty(@PathVariable Long id) {
        Property property = propertyService.getById(id);
        if (property == null) {
            return Result.notFound("房产不存在");
        }
        PropertyDetailDTO dto = new PropertyDetailDTO();
        org.springframework.beans.BeanUtils.copyProperties(property, dto);
        // 填充开发商名称
        Developer developer = developerService.getById(property.getDeveloperId());
        if (developer != null) {
            dto.setDeveloperName(developer.getCompanyName());
        }
        return Result.success(dto);
    }

    /**
     * 获取我（开发商）的房产列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "我的房产", description = "开发商查看自己名下的房产列表")
    public Result<Page<Property>> getMyProperties(Authentication authentication, PropertyQueryRequest request) {
        Long developerId = (Long) authentication.getPrincipal();
        Page<Property> result = propertyService.getMyProperties(developerId, request);
        return Result.success(result, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 创建房产（开发商）
     */
    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "创建房产", description = "开发商创建新的房产信息")
    public Result<Property> createProperty(Authentication authentication, @Valid @RequestBody Property property) {
        Long developerId = (Long) authentication.getPrincipal();
        Property saved = propertyService.createProperty(developerId, property);
        activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "CREATE", "Property",
                saved.getId(), "创建房产: " + saved.getPropertyName());
        return Result.success(saved);
    }

    /**
     * 更新房产信息（开发商只能更新自己的）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "更新房产", description = "开发商更新自己的房产信息")
    public Result<Void> updateProperty(Authentication authentication,
                                       @PathVariable Long id,
                                       @RequestBody Property property) {
        Long developerId = (Long) authentication.getPrincipal();
        boolean success = propertyService.updateProperty(id, developerId, property);
        if (success) {
            activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "UPDATE", "Property",
                    id, "更新房产信息: " + property.getPropertyName());
            return Result.success();
        }
        return Result.badRequest("更新失败，房产不存在或无权操作");
    }

    /**
     * 删除房产（开发商只能删除自己的）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "删除房产", description = "开发商删除自己的房产信息")
    public Result<Void> deleteProperty(Authentication authentication, @PathVariable Long id) {
        Long developerId = (Long) authentication.getPrincipal();
        boolean success = propertyService.deleteProperty(id, developerId);
        if (success) {
            activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "DELETE", "Property",
                    id, "删除房产");
            return Result.success();
        }
        return Result.badRequest("删除失败，房产不存在或无权操作");
    }

    // ========== 价格历史接口 ==========

    /**
     * 获取房产价格历史（近24个月）
     */
    @GetMapping("/{id}/price-history")
    @Operation(summary = "价格历史", description = "获取指定房产近24个月的价格走势数据")
    public Result<List<PriceHistory>> getPriceHistory(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "24") int months) {
        Property property = propertyService.getById(id);
        if (property == null) {
            return Result.notFound("房产不存在");
        }
        List<PriceHistory> history = priceHistoryService.getPriceHistory(id, months);
        return Result.success(history);
    }

    // ========== 统计分析接口 ==========

    /**
     * 按位置统计空置率（柱状图）
     */
    @GetMapping("/statistics/vacancy-by-location")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "空置率-位置统计", description = "按地理位置统计平均空置率（柱状图数据）")
    public Result<List<Map<String, Object>>> getVacancyByLocation(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> data = propertyService.getVacancyByLocation(developerId);
        return Result.success(data);
    }

    /**
     * 按户型统计空置率（分组柱状图）
     */
    @GetMapping("/statistics/vacancy-by-type")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "空置率-户型统计", description = "按户型类型统计平均空置率（分组柱状图数据）")
    public Result<List<Map<String, Object>>> getVacancyByType(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> data = propertyService.getVacancyByType(developerId);
        return Result.success(data);
    }

    /**
     * 楼层与空置率散点图
     */
    @GetMapping("/statistics/vacancy-by-floor")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "楼层-空置率散点", description = "楼层数与空置率的关系散点图数据")
    public Result<List<Map<String, Object>>> getVacancyByFloor(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> data = propertyService.getVacancyByFloor(developerId);
        return Result.success(data);
    }
}
