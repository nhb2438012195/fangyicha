package com.fangyicha.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fangyicha.entity.KnowledgeDocument;
import com.fangyicha.mapper.KnowledgeDocumentMapper;
import com.fangyicha.service.DocumentParserService;
import com.fangyicha.service.KnowledgeBaseService;
import com.fangyicha.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 知识库管理服务实现
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String UPLOAD_DIR = new java.io.File("data/rag-uploads").getAbsolutePath();
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentParserService documentParserService;
    private final RagService ragService;

    public KnowledgeBaseServiceImpl(KnowledgeDocumentMapper documentMapper,
                                     DocumentParserService documentParserService,
                                     RagService ragService) {
        this.documentMapper = documentMapper;
        this.documentParserService = documentParserService;
        this.ragService = ragService;
    }

    @Override
    public KnowledgeDocument uploadDocument(MultipartFile file, Long developerId) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过10MB限制");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String ext = getExtension(filename).toLowerCase();
        if (!ext.equals(".pdf") && !ext.equals(".docx") && !ext.equals(".doc")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF、DOCX、DOC");
        }

        try {
            Files.createDirectories(Path.of(UPLOAD_DIR));

            // Generate stored filename
            String storedFilename = UUID.randomUUID().toString() + ext;
            Path targetPath = Path.of(UPLOAD_DIR, storedFilename);

            // Save file
            file.transferTo(targetPath.toFile());

            // Create DB record
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setDeveloperId(developerId);
            doc.setFilename(filename);
            doc.setStoredFilename(storedFilename);
            doc.setFileSize(file.getSize());
            doc.setStatus("uploaded");
            documentMapper.insert(doc);

            log.info("文档上传成功: id={}, filename={}, size={}", doc.getId(), filename, file.getSize());
            return doc;

        } catch (IOException e) {
            log.error("文档上传IO异常", e);
            throw new RuntimeException("文档上传失败", e);
        }
    }

    @Override
    public List<KnowledgeDocument> getDocuments(Long developerId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getDeveloperId, developerId);
        wrapper.orderByDesc(KnowledgeDocument::getUploadedTime);
        return documentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public KnowledgeDocument indexDocument(Long fileId, Long developerId) {
        KnowledgeDocument doc = documentMapper.selectById(fileId);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!doc.getDeveloperId().equals(developerId)) {
            throw new IllegalArgumentException("无权操作此文档");
        }

        try {
            // Read file from storage
            Path filePath = Path.of(UPLOAD_DIR, doc.getStoredFilename());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("存储文件不存在: " + doc.getStoredFilename());
            }

            byte[] fileBytes = Files.readAllBytes(filePath);

            // Parse with Tika
            String parsedText;
            try (InputStream input = new ByteArrayInputStream(fileBytes)) {
                parsedText = documentParserService.parseDocument(input, doc.getFilename());
            }

            // Check for error messages from parser
            if (parsedText.startsWith("该文档受密码保护") ||
                parsedText.startsWith("文档格式错误") ||
                parsedText.startsWith("文档过大") ||
                parsedText.startsWith("文档读取失败") ||
                parsedText.startsWith("不支持的文件格式")) {
                doc.setStatus("error");
                doc.setErrorMessage(parsedText);
                documentMapper.updateById(doc);
                return doc;
            }

            // Add to Lucene index with upload source
            String docId = "upload_" + doc.getId();
            ragService.addDocument(parsedText, "upload", docId);

            // Update status
            doc.setStatus("indexed");
            doc.setIndexedTime(LocalDateTime.now());
            doc.setErrorMessage(null);
            documentMapper.updateById(doc);

            log.info("文档索引成功: id={}, filename={}", fileId, doc.getFilename());

        } catch (Exception e) {
            log.error("文档索引失败: id={}", fileId, e);
            doc.setStatus("error");
            doc.setErrorMessage("索引失败: " + e.getMessage());
            documentMapper.updateById(doc);
        }

        return doc;
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long fileId, Long developerId) {
        KnowledgeDocument doc = documentMapper.selectById(fileId);
        if (doc == null) {
            return false;
        }
        if (!doc.getDeveloperId().equals(developerId)) {
            throw new IllegalArgumentException("无权操作此文档");
        }

        // Remove from Lucene index
        String docId = "upload_" + doc.getId();
        ragService.removeDocument(docId);

        // Delete storage file
        try {
            Path filePath = Path.of(UPLOAD_DIR, doc.getStoredFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除存储文件失败: {}", doc.getStoredFilename(), e);
        }

        // Delete DB record
        documentMapper.deleteById(fileId);
        log.info("文档删除成功: id={}", fileId);
        return true;
    }

    @Override
    public String getDocumentContent(Long fileId, Long developerId) {
        KnowledgeDocument doc = documentMapper.selectById(fileId);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!doc.getDeveloperId().equals(developerId)) {
            throw new IllegalArgumentException("无权操作此文档");
        }

        if (!"indexed".equals(doc.getStatus())) {
            return "文档尚未解析";
        }

        try {
            Path filePath = Path.of(UPLOAD_DIR, doc.getStoredFilename());
            if (!Files.exists(filePath)) {
                return "存储文件不存在";
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            try (InputStream input = new ByteArrayInputStream(fileBytes)) {
                return documentParserService.parseDocument(input, doc.getFilename());
            }
        } catch (IOException e) {
            log.error("读取文档内容失败: id={}", fileId, e);
            return "读取文档失败";
        }
    }

    @Override
    public String getDocumentPreview(Long fileId, Long developerId) {
        String content = getDocumentContent(fileId, developerId);
        if (content == null || content.startsWith("文档") || content.startsWith("该文档") || content.startsWith("存储")) {
            return content;
        }
        if (content.length() > 500) {
            return content.substring(0, 500) + "...";
        }
        return content;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
}
