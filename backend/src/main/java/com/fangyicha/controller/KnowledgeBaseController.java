package com.fangyicha.controller;

import com.fangyicha.common.Result;
import com.fangyicha.entity.KnowledgeDocument;
import com.fangyicha.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器（开发商）
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/knowledge-base")
@Tag(name = "知识库管理", description = "开发商知识库文档管理")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "上传文档", description = "上传PDF/Word文档到知识库暂存区")
    public Result<KnowledgeDocument> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        try {
            KnowledgeDocument doc = knowledgeBaseService.uploadDocument(file, developerId);
            return Result.success("上传成功", doc);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文档上传接口异常", e);
            return Result.serverError("上传失败");
        }
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/files")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "文档列表", description = "获取当前开发商的所有上传文档")
    public Result<List<KnowledgeDocument>> getDocuments(Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        List<KnowledgeDocument> docs = knowledgeBaseService.getDocuments(developerId);
        return Result.success(docs);
    }

    /**
     * 纳入知识库（触发索引）
     */
    @PostMapping("/{fileId}/index")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "纳入知识库", description = "将上传的文档解析并索引到知识库中")
    public Result<KnowledgeDocument> indexDocument(@PathVariable Long fileId, Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        try {
            KnowledgeDocument doc = knowledgeBaseService.indexDocument(fileId, developerId);
            if ("error".equals(doc.getStatus())) {
                return Result.badRequest("索引失败: " + doc.getErrorMessage());
            }
            return Result.success("文档已纳入知识库", doc);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("文档索引接口异常", e);
            return Result.serverError("索引失败");
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "删除文档", description = "从知识库中删除文档（含存储文件和索引）")
    public Result<Void> deleteDocument(@PathVariable Long fileId, Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        try {
            boolean deleted = knowledgeBaseService.deleteDocument(fileId, developerId);
            if (deleted) {
                return Result.success("文档已删除", null);
            }
            return Result.notFound("文档不存在");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("文档删除接口异常", e);
            return Result.serverError("删除失败");
        }
    }

    /**
     * 文档预览
     */
    @GetMapping("/{fileId}/preview")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "文档预览", description = "获取文档解析后的文本内容（前500字）")
    public Result<Map<String, String>> previewDocument(@PathVariable Long fileId, Authentication authentication) {
        Long developerId = (Long) authentication.getPrincipal();
        try {
            String preview = knowledgeBaseService.getDocumentPreview(fileId, developerId);
            return Result.success(Map.of("content", preview));
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("文档预览接口异常", e);
            return Result.serverError("预览失败");
        }
    }
}
