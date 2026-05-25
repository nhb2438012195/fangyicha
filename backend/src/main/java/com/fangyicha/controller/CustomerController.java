package com.fangyicha.controller;

import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.dto.RecommendationDTO;
import com.fangyicha.entity.Customer;
import com.fangyicha.service.ActivityLogService;
import com.fangyicha.service.CustomerService;
import com.fangyicha.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 客户控制器
 * 处理客户资料管理和仪表盘
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@Tag(name = "客户管理", description = "客户资料查询和修改")
public class CustomerController {

    private final CustomerService customerService;
    private final ActivityLogService activityLogService;
    private final RecommendationService recommendationService;

    public CustomerController(CustomerService customerService,
                              ActivityLogService activityLogService,
                              RecommendationService recommendationService) {
        this.customerService = customerService;
        this.activityLogService = activityLogService;
        this.recommendationService = recommendationService;
    }

    /**
     * 获取客户个人信息
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "个人信息", description = "获取当前登录客户的个人信息")
    public Result<Customer> getProfile(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            return Result.notFound("客户不存在");
        }
        customer.setPassword(null);
        return Result.success(customer);
    }

    /**
     * 更新客户个人信息
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "更新资料", description = "更新当前客户的个人资料")
    public Result<Void> updateProfile(Authentication authentication, @RequestBody Customer customer) {
        Long customerId = (Long) authentication.getPrincipal();
        boolean success = customerService.updateProfile(customerId, customer);
        if (success) {
            activityLogService.log(customerId, Constants.ROLE_CUSTOMER, "UPDATE", "Customer",
                    customerId, "更新个人资料");
            return Result.success();
        }
        return Result.badRequest("更新失败");
    }

    /**
     * 获取客户仪表盘数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "仪表盘数据", description = "获取客户的仪表盘统计数据")
    public Result<Map<String, Object>> getDashboard(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        Map<String, Object> stats = customerService.getDashboardStats(customerId);
        return Result.success(stats);
    }

    /**
     * 获取个性化房产推荐
     */
    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "房产推荐", description = "根据客户偏好区域和预算推荐房产，最多6条")
    public Result<List<RecommendationDTO>> getRecommendations(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        List<RecommendationDTO> recommendations = recommendationService.getRecommendations(customerId);
        return Result.success(recommendations);
    }
}
