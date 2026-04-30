package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OfficeVectorStoreConfig {

    @Resource
    private OfficeDocumentLoader officeDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private KnowledgeDocumentLoader knowledgeDocumentLoader;

    @Bean
    VectorStore officeVectorStore(EmbeddingModel dashscopeEmbeddingModel) throws Exception {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        List<Document> documentList = officeDocumentLoader.loadMarkdowns();
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
        simpleVectorStore.add(enrichedDocuments);
        simpleVectorStore.add(knowledgeDocumentLoader.loadUploadedDocuments());
        return simpleVectorStore;
    }
}
