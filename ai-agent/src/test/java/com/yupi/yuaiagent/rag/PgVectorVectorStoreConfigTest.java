package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@Disabled("Integration test requires PGVector and embedding service configuration.")
@SpringBootTest
class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("Office reimbursement process and approval policy", Map.of("meta1", "meta1")),
                new Document("Weekly report writing guide"),
                new Document("Meeting minutes action item template", Map.of("meta2", "meta2"))
        );
        pgVectorVectorStore.add(documents);
        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder().query("How to write meeting minutes").topK(3).build()
        );
        Assertions.assertNotNull(results);
    }
}
