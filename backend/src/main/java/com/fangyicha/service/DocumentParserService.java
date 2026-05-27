package com.fangyicha.service;

import java.io.InputStream;

/**
 * 文档解析服务接口
 */
public interface DocumentParserService {

    /**
     * 解析文档为纯文本
     *
     * @param input    文档输入流
     * @param filename 文件名（用于识别扩展名）
     * @return 解析后的纯文本
     */
    String parseDocument(InputStream input, String filename);
}
