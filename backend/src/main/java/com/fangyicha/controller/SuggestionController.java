package com.fangyicha.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangyicha.common.Constants;
import com.fangyicha.common.Result;
import com.fangyicha.entity.Suggestion;
import com.fangyicha.service.ActivityLogService;
import com.fangyicha.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 建议/购房意向控制器
 * 处理客户提交建议、开发商回复建议
 */
@Slf4j
@RestController
@RequestMapping("/api/suggestions")
@Tag(name = "建议管理", description = "客户提交购房意向，开发商回复建议")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final ActivityLogService activityLogService;

    public SuggestionController(SuggestionService suggestionService,
                                ActivityLogService activityLogService) {
        this.suggestionService = suggestionService;
        this.activityLogService = activityLogService;
    }

    /**
     * 客户提交建议
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "提交建议", description = "客户向开发商提交购房意向")
    public Result<Void> submitSuggestion(Authentication authentication, @RequestBody Suggestion suggestion) {
        Long customerId = (Long) authentication.getPrincipal();
        if (suggestion.getDeveloperId() == null) {
            return Result.badRequest("请选择开发商");
        }
        boolean success = suggestionService.submitSuggestion(customerId, suggestion);
        if (success) {
            activityLogService.log(customerId, Constants.ROLE_CUSTOMER, "CREATE", "Suggestion",
                    suggestion.getId(), "提交购房建议");
            return Result.success();
        }
        return Result.error(500, "提交失败");
    }

    /**
     * 客户查看自己的建议列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "我的建议", description = "客户查看自己提交的所有建议")
    public Result<Page<Suggestion>> getMySuggestions(Authentication authentication,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        Long customerId = (Long) authentication.getPrincipal();
        Page<Suggestion> result = suggestionService.getMySuggestions(customerId, page, pageSize);
        return Result.success(result, result.getTotal(), Long.valueOf(page), Long.valueOf(pageSize));
    }

    /**
     * 开发商查看收到的建议列表
     */
    @GetMapping("/received")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "收到的建议", description = "开发商查看客户提交给他们的建议")
    public Result<Page<Suggestion>> getReceivedSuggestions(Authentication authentication,
                                                           @RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Long developerId = (Long) authentication.getPrincipal();
        Page<Suggestion> result = suggestionService.getReceivedSuggestions(developerId, page, pageSize);
        return Result.success(result, result.getTotal(), Long.valueOf(page), Long.valueOf(pageSize));
    }

    /**
     * 开发商回复建议
     */
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "回复建议", description = "开发商回复客户提交的建议")
    public Result<Void> replySuggestion(Authentication authentication,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        Long developerId = (Long) authentication.getPrincipal();
        String replyContent = body.get("replyContent");
        if (replyContent == null || replyContent.trim().isEmpty()) {
            return Result.badRequest("回复内容不能为空");
        }
        boolean success = suggestionService.replySuggestion(id, developerId, replyContent);
        if (success) {
            activityLogService.log(developerId, Constants.ROLE_DEVELOPER, "UPDATE", "Suggestion",
                    id, "回复客户建议");
            return Result.success();
        }
        return Result.badRequest("回复失败，建议不存在或无权操作");
    }
}
