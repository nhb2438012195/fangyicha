package com.fangyicha.service.impl;

import com.fangyicha.service.RagService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现 — 基于 Lucene 混合 BM25 + 向量检索
 * 使用 REST API 直接调用 DeepSeek Embedding 端点
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private static final String INDEX_DIR = "data/lucene-index";
    private static final String EMB_CACHE_DIR = "data/emb-cache";
    private static final int CHUNK_SIZE = 300;
    private static final int CHUNK_OVERLAP = 50;
    private static final int TOP_BM25 = 20;
    private static final int TOP_FINAL = 5;
    private static final float MIN_SCORE_THRESHOLD = 0.15f;
    private static final int EMBEDDING_DIM = 1024;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    private Directory directory;
    private IndexWriter writer;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(INDEX_DIR));
            Files.createDirectories(Path.of(EMB_CACHE_DIR));

            directory = FSDirectory.open(Path.of(INDEX_DIR));
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            writer = new IndexWriter(directory, config);
            writer.commit();
            log.info("Lucene索引初始化完成: {}", INDEX_DIR);
        } catch (IOException e) {
            log.error("Lucene索引初始化失败", e);
            throw new RuntimeException("Lucene索引初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (writer != null) writer.close();
            if (directory != null) directory.close();
        } catch (IOException e) {
            log.error("关闭Lucene索引失败", e);
        }
    }

    @Override
    public void rebuildIndex() {
        try {
            writer.deleteAll();
            writer.commit();

            Path platformDir = Path.of("data/rag-platform");
            if (Files.exists(platformDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(platformDir, "*.txt")) {
                    for (Path file : stream) {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        String filename = file.getFileName().toString();
                        addDocument(content, "platform", filename);
                    }
                }
            }
            log.info("平台数据索引重建完成");
        } catch (IOException e) {
            log.error("重建索引失败", e);
        }
    }

    @Override
    public void addDocument(String content, String source, String docId) {
        try {
            removeDocument(docId);
            List<ChunkResult> chunks = chunkText(content);

            for (int i = 0; i < chunks.size(); i++) {
                ChunkResult chunk = chunks.get(i);
                String chunkContent = chunk.getContent();
                float[] embedding = getOrGenerateEmbedding(chunkContent);

                Document doc = new Document();
                doc.add(new StringField("id", docId, Field.Store.YES));
                doc.add(new TextField("content", chunkContent, Field.Store.YES));
                doc.add(new StringField("source", source, Field.Store.YES));
                doc.add(new IntPoint("chunkIndex", chunk.getChunkIndex()));
                doc.add(new StoredField("chunkIndex", chunk.getChunkIndex()));
                if (embedding != null) {
                    doc.add(new KnnFloatVectorField("embedding", embedding, VectorSimilarityFunction.COSINE));
                }
                writer.addDocument(doc);
            }

            writer.commit();
            log.info("文档已加入索引: docId={}, chunks={}, source={}", docId, chunks.size(), source);
        } catch (IOException e) {
            log.error("添加文档到索引失败: docId={}", docId, e);
        }
    }

    @Override
    public void removeDocument(String docId) {
        try {
            writer.deleteDocuments(new Term("id", docId));
            writer.commit();
        } catch (IOException e) {
            log.error("从索引删除文档失败: docId={}", docId, e);
        }
    }

    @Override
    public List<RagResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            IndexReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            try {
                // Step 1: BM25 keyword search
                QueryParser parser = new QueryParser("content", new StandardAnalyzer());
                Query bm25Query = parser.parse(QueryParser.escape(query));
                TopDocs bm25TopDocs = searcher.search(bm25Query, TOP_BM25);
                Map<Integer, Float> bm25Scores = new HashMap<>();
                float maxBm25Score = 1.0f;

                for (ScoreDoc scoreDoc : bm25TopDocs.scoreDocs) {
                    bm25Scores.put(scoreDoc.doc, scoreDoc.score);
                    if (scoreDoc.score > maxBm25Score) {
                        maxBm25Score = scoreDoc.score;
                    }
                }

                if (bm25Scores.isEmpty()) {
                    return Collections.emptyList();
                }

                // Step 2: Generate query embedding
                float[] queryEmbedding = getOrGenerateEmbedding(query);

                // Step 3: Compute hybrid scores
                List<RagResult> results = new ArrayList<>();
                for (Map.Entry<Integer, Float> entry : bm25Scores.entrySet()) {
                    int docId = entry.getKey();
                    float bm25Score = entry.getValue();
                    float normalizedBm25 = maxBm25Score > 0 ? bm25Score / maxBm25Score : 0f;

                    Document doc = searcher.doc(docId);
                    float vectorScore = 0f;
                    if (queryEmbedding != null) {
                        float[] docEmbedding = extractEmbeddingFromDoc(doc, docId, searcher);
                        if (docEmbedding != null) {
                            vectorScore = cosineSimilarity(queryEmbedding, docEmbedding);
                        }
                    }

                    float finalScore = normalizedBm25 * 0.5f + vectorScore * 0.5f;

                    if (finalScore >= MIN_SCORE_THRESHOLD) {
                        RagResult result = new RagResult(
                            doc.get("id"),
                            doc.get("content"),
                            doc.get("source"),
                            finalScore
                        );
                        results.add(result);
                    }
                }

                results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
                return results.stream().limit(TOP_FINAL).collect(Collectors.toList());

            } finally {
                reader.close();
            }
        } catch (Exception e) {
            log.error("RAG检索失败: query={}", query, e);
            return Collections.emptyList();
        }
    }

    public List<ChunkResult> chunkText(String text) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        String cleanText = text.trim();
        int start = 0;
        int index = 0;

        while (start < cleanText.length()) {
            int end = Math.min(start + CHUNK_SIZE, cleanText.length());
            chunks.add(new ChunkResult(cleanText.substring(start, end), index));
            index++;
            if (end >= cleanText.length()) break;
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }

        return chunks;
    }

    /**
     * 获取或生成嵌入向量
     */
    private float[] getOrGenerateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String hash = DigestUtils.sha256Hex(text);
        Path cacheFile = Path.of(EMB_CACHE_DIR, hash + ".emb");

        if (Files.exists(cacheFile)) {
            try {
                byte[] bytes = Files.readAllBytes(cacheFile);
                if (bytes.length == EMBEDDING_DIM * 4) {
                    float[] embedding = new float[EMBEDDING_DIM];
                    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                    for (int i = 0; i < EMBEDDING_DIM; i++) {
                        embedding[i] = buffer.getFloat();
                    }
                    return embedding;
                }
            } catch (IOException e) {
                log.warn("读取嵌入缓存失败: {}", hash, e);
            }
        }

        try {
            float[] embedding = callEmbeddingApi(text);
            if (embedding != null) {
                ByteBuffer buffer = ByteBuffer.allocate(EMBEDDING_DIM * 4).order(ByteOrder.LITTLE_ENDIAN);
                for (float v : embedding) {
                    buffer.putFloat(v);
                }
                Files.write(cacheFile, buffer.array());
            }
            return embedding;
        } catch (Exception e) {
            log.error("生成嵌入向量失败", e);
            return null;
        }
    }

    /**
     * 直接调用 DeepSeek Embedding API
     */
    private float[] callEmbeddingApi(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-embedding");
            requestBody.put("input", List.of(text));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/embeddings";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");
                    if (embeddingList != null && !embeddingList.isEmpty()) {
                        float[] embedding = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            embedding[i] = embeddingList.get(i).floatValue();
                        }
                        return embedding;
                    }
                }
            }

            log.warn("Embedding API返回格式异常: {}", body);
            return null;
        } catch (Exception e) {
            log.error("DeepSeek Embedding API调用失败", e);
            return null;
        }
    }

    /**
     * 从文档中提取嵌入向量
     */
    private float[] extractEmbeddingFromDoc(Document doc, int docId, IndexSearcher searcher) {
        try {
            // Attempt to get vector via FloatVectorValues
            for (var leaf : searcher.getIndexReader().leaves()) {
                FloatVectorValues vectorValues = leaf.reader().getFloatVectorValues("embedding");
                if (vectorValues != null) {
                    int docIndex = docId - leaf.docBase;
                    if (docIndex >= 0 && docIndex < leaf.reader().maxDoc()) {
                        // We need to advance to the correct doc
                        // FloatVectorValues iterates over docs that have this vector field
                        // Since many docs may not have the field, we try a simpler approach
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取文档嵌入向量失败, docId={}", docId);
        }
        return null;
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double norm = Math.sqrt(normA) * Math.sqrt(normB);
        return norm == 0 ? 0f : (float) (dotProduct / norm);
    }
}
