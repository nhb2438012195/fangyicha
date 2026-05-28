package com.fangyicha.service.impl;

import com.fangyicha.service.RagService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RagServiceImplTest {

    private Directory directory;
    private IndexWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        writer = new IndexWriter(directory, config);
    }

    @Test
    void testChunkText() {
        RagServiceImpl service = createService();
        String text = "A".repeat(310);
        List<RagService.ChunkResult> chunks = service.chunkText(text);

        assertFalse(chunks.isEmpty());
        assertEquals(2, chunks.size());
        assertEquals(300, chunks.get(0).getContent().length());
        assertEquals(0, chunks.get(0).getChunkIndex());
    }

    @Test
    void testChunkTextShort() {
        RagServiceImpl service = createService();
        List<RagService.ChunkResult> chunks = service.chunkText("short text");
        assertEquals(1, chunks.size());
        assertEquals("short text", chunks.get(0).getContent());
    }

    @Test
    void testChunkTextEmpty() {
        RagServiceImpl service = createService();
        assertTrue(service.chunkText("").isEmpty());
        assertTrue(service.chunkText(null).isEmpty());
    }

    @Test
    void testCosineSimilarityIdentical() throws Exception {
        RagServiceImpl service = createService();
        Method method = RagServiceImpl.class.getDeclaredMethod("cosineSimilarity", float[].class, float[].class);
        method.setAccessible(true);

        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {1.0f, 0.0f, 0.0f};
        assertEquals(1.0f, (float) method.invoke(service, a, b), 0.001f);
    }

    @Test
    void testCosineSimilarityOrthogonal() throws Exception {
        RagServiceImpl service = createService();
        Method method = RagServiceImpl.class.getDeclaredMethod("cosineSimilarity", float[].class, float[].class);
        method.setAccessible(true);

        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        assertEquals(0.0f, (float) method.invoke(service, a, b), 0.001f);
    }

    @Test
    void testCosineSimilarityNullOrMismatchedInput() throws Exception {
        RagServiceImpl service = createService();
        Method method = RagServiceImpl.class.getDeclaredMethod("cosineSimilarity", float[].class, float[].class);
        method.setAccessible(true);

        assertEquals(0.0f, (float) method.invoke(service, null, new float[]{1.0f}));
        assertEquals(0.0f, (float) method.invoke(service, new float[]{1.0f, 2.0f}, new float[]{1.0f}));
    }

    @Test
    void testExtractEmbeddingFromDoc() throws Exception {
        RagServiceImpl service = createService();
        Method method = RagServiceImpl.class.getDeclaredMethod(
            "extractEmbeddingFromDoc", org.apache.lucene.document.Document.class, int.class, IndexSearcher.class);
        method.setAccessible(true);

        float[] embedding = {0.5f, 0.3f, 0.1f};
        Document doc = new Document();
        doc.add(new StringField("id", "test_1", Field.Store.YES));
        doc.add(new KnnFloatVectorField("embedding", embedding, VectorSimilarityFunction.COSINE));

        writer.addDocument(doc);
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Document storedDoc = searcher.doc(0);
            float[] result = (float[]) method.invoke(service, storedDoc, 0, searcher);
            assertNotNull(result);
            assertEquals(3, result.length);
            assertEquals(0.5f, result[0], 0.001f);
            assertEquals(0.3f, result[1], 0.001f);
            assertEquals(0.1f, result[2], 0.001f);
        }
    }

    @Test
    void testExtractEmbeddingFromDocWithoutVector() throws Exception {
        RagServiceImpl service = createService();
        Method method = RagServiceImpl.class.getDeclaredMethod(
            "extractEmbeddingFromDoc", org.apache.lucene.document.Document.class, int.class, IndexSearcher.class);
        method.setAccessible(true);

        Document doc = new Document();
        doc.add(new StringField("id", "test_2", Field.Store.YES));
        doc.add(new TextField("content", "no vector", Field.Store.YES));

        writer.addDocument(doc);
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Document storedDoc = searcher.doc(0);
            float[] result = (float[]) method.invoke(service, storedDoc, 0, searcher);
            assertNull(result);
        }
    }

    private RagServiceImpl createService() {
        RagServiceImpl service = new RagServiceImpl();
        try {
            java.lang.reflect.Field apiKeyField = RagServiceImpl.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(service, "");
            java.lang.reflect.Field baseUrlField = RagServiceImpl.class.getDeclaredField("baseUrl");
            baseUrlField.setAccessible(true);
            baseUrlField.set(service, "https://api.deepseek.com/v1");
        } catch (Exception ignore) {}
        return service;
    }
}
