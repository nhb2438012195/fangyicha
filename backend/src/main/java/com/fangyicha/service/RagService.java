package com.fangyicha.service;

import java.util.List;

/**
 * RAG检索服务接口
 */
public interface RagService {

    /**
     * 混合检索（BM25 + 向量）
     *
     * @param query 用户查询
     * @return 最多5条结果，按相关性降序
     */
    List<RagResult> search(String query);

    /**
     * 重新构建索引（启动时调用）
     */
    void rebuildIndex();

    /**
     * 为上传文档添加内容到索引
     *
     * @param content 文档文本内容
     * @param source  来源: 'upload' + 文件名
     * @param docId   文档ID标识
     */
    void addDocument(String content, String source, String docId);

    /**
     * 从索引中删除文档
     *
     * @param docId 文档ID标识
     */
    void removeDocument(String docId);

    /**
     * RAG检索结果
     */
    class RagResult {
        private String id;
        private String content;
        private String source;
        private float score;

        public RagResult() {}

        public RagResult(String id, String content, String source, float score) {
            this.id = id;
            this.content = content;
            this.source = source;
            this.score = score;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
    }

    /**
     * 分块结果
     */
    class ChunkResult {
        private String content;
        private int chunkIndex;

        public ChunkResult(String content, int chunkIndex) {
            this.content = content;
            this.chunkIndex = chunkIndex;
        }

        public String getContent() { return content; }
        public int getChunkIndex() { return chunkIndex; }
    }
}
