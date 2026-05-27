package com.fangyicha.service;

import com.fangyicha.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档（仅保存到存储区，不索引）
     *
     * @param file        上传文件
     * @param developerId 开发商ID
     * @return 知识库文档记录
     */
    KnowledgeDocument uploadDocument(MultipartFile file, Long developerId);

    /**
     * 获取开发商的文档列表
     */
    List<KnowledgeDocument> getDocuments(Long developerId);

    /**
     * 纳入知识库（触发完整索引流程：解析 -> 分块 -> 嵌入 -> 索引）
     */
    KnowledgeDocument indexDocument(Long fileId, Long developerId);

    /**
     * 删除文档（从存储和索引中移除）
     */
    boolean deleteDocument(Long fileId, Long developerId);

    /**
     * 获取文档的已解析内容
     */
    String getDocumentContent(Long fileId, Long developerId);

    /**
     * 获取文档预览文本（前500字）
     */
    String getDocumentPreview(Long fileId, Long developerId);
}
