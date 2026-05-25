package com.fangyicha.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.entity.Developer;
import com.fangyicha.service.ActivityLogService;
import com.fangyicha.service.DeveloperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 开发商控制器
 * 处理开发商信息查询、公司资料管理、仪表盘
 */
@Slf4j
@RestController
@RequestMapping("/api/developers")
@Tag(name = "开发商管理", description = "开发商信息查询、公司资料管理")
public class DeveloperController {

    private final DeveloperService developerService;
    private final ActivityLogService activityLogService;

    public DeveloperController(DeveloperService developerService,
                               ActivityLogService activityLogService) {
        this.developerService = developerService;
        this.activityLogService = activityLogService;
    }

    /**
     * 分页查询开发商列表（公开）
     */
    @GetMapping
    @Operation(summary = "开发商列表", description = "分页查询所有开发商列表，支持按公司名称搜索")
    public Result<Page<Developer>> listDevelopers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Developer> result = developerService.getDeveloperPage(keyword, page, pageSize);
        return Result.success(result, result.getTotal(), Long.valueOf(page), Long.valueOf(pageSize));
    }

    /**
     * 获取开发商详情（公开）
     */
    @GetMapping("/{id}")
    @Operation(summary = "开发商详情", description = "根据ID获取开发商详细信息")
    public Result<Developer> getDeveloper(@PathVariable Long id) {
        Developer developer = developerService.getById(id);
        if (developer == null) {
            return Result.notFound("开发商不存在");
        }
        // 不返回密码
        developer.setPassword(null);
        return Result.success(developer);
    }

    /**
     * 更新开发商公司信息（仅开发商自己）
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "更新公司信息", description = "开发商更新自己的公司资料")
    public Result<Void> updateProfile(Authentication authentication, @RequestBody Developer developer) {
        Long developerId = (Long) authentication.getPrincipal();
        boolean success = developerService.updateProfile(developerId, developer);
        if (success) {
            activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "UPDATE", "Developer",
                    developerId, "更新公司信息");
            return Result.success();
        }
        return Result.badRequest("更新失败");
    }

    /**
     * 获取开发商仪表盘数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "仪表盘数据", description = "获取开发商的仪表盘统计数据")
    public Result<Map<String, Object>> getDashboard(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = developerService.getDashboardStats(developerId);
        return Result.success(stats);
    }
}
