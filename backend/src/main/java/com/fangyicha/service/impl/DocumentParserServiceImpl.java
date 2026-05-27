package com.fangyicha.service.impl;

import com.fangyicha.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析服务实现 — 基于 Apache Tika
 */
@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    /** 最大解析大小：10MB */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** 支持的文档格式 */
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".doc"};

    @Override
    public String parseDocument(InputStream input, String filename) {
        if (filename == null || filename.isEmpty()) {
            return "不支持的文件格式";
        }

        String ext = getExtension(filename).toLowerCase();
        boolean supported = false;
        for (String supportedExt : SUPPORTED_EXTENSIONS) {
            if (supportedExt.equals(ext)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return "不支持的文件格式: " + ext;
        }

        try {
            // Tika auto-detects format
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler((int) MAX_FILE_SIZE);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(input, handler, metadata, context);
            String text = handler.toString();

            if (text.trim().isEmpty()) {
                return "文档内容为空";
            }

            log.info("文档解析成功: filename={}, length={}", filename, text.length());
            return text;

        } catch (EncryptedDocumentException e) {
            log.warn("文档受密码保护: {}", filename);
            return "该文档受密码保护，无法解析";
        } catch (TikaException e) {
            log.warn("文档解析异常: filename={}, error={}", filename, e.getMessage());
            return "文档格式错误，请重新上传";
        } catch (IOException e) {
            log.error("文档读取IO异常: filename={}", filename, e);
            return "文档读取失败，请重新上传";
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("10MB")) {
                return "文档过大，请上传10MB以内的文件";
            }
            log.error("文档解析未知异常: filename={}", filename, e);
            return "文档解析失败，请重新上传";
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
}
