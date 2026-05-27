package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KnowledgeIndexService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private KnowledgeDocumentSplitter knowledgeDocumentSplitter;

    @Resource
    private KnowledgeDocumentLoader knowledgeDocumentLoader;

    @Resource
    private KnowledgeIndexMetadataStore knowledgeIndexMetadataStore;

    @Value("${office.knowledge.rebuild-on-startup:true}")
    private boolean rebuildOnStartup;

    public KnowledgeIndexService() {
    }

    KnowledgeIndexService(
            VectorStore vectorStore,
            KnowledgeDocumentSplitter knowledgeDocumentSplitter,
            KnowledgeDocumentLoader knowledgeDocumentLoader,
            KnowledgeIndexMetadataStore knowledgeIndexMetadataStore
    ) {
        this.vectorStore = vectorStore;
        this.knowledgeDocumentSplitter = knowledgeDocumentSplitter;
        this.knowledgeDocumentLoader = knowledgeDocumentLoader;
        this.knowledgeIndexMetadataStore = knowledgeIndexMetadataStore;
    }

    public List<Document> indexFile(KnowledgeFileInfo fileInfo, String content) {
        List<Document> documents = knowledgeDocumentSplitter.split(fileInfo, content);
        if (documents.isEmpty()) {
            log.warn("No chunks generated for knowledge file {}", fileInfo.originalFilename());
            knowledgeIndexMetadataStore.saveChunkIds(fileInfo.id(), List.of());
            return List.of();
        }
        List<String> existingChunkIds = knowledgeIndexMetadataStore.getChunkIds(fileInfo.id());
        if (!existingChunkIds.isEmpty()) {
            vectorStore.delete(existingChunkIds);
        }
        vectorStore.add(documents);
        knowledgeIndexMetadataStore.saveChunkIds(
                fileInfo.id(),
                documents.stream().map(Document::getId).toList()
        );
        return documents;
    }

    public void deleteFileIndex(String fileId) {
        List<String> chunkIds = knowledgeIndexMetadataStore.getChunkIds(fileId);
        if (!chunkIds.isEmpty()) {
            vectorStore.delete(chunkIds);
        }
        knowledgeIndexMetadataStore.remove(fileId);
    }

    public List<String> listIndexedChunks(String fileId) {
        return knowledgeIndexMetadataStore.getChunkIds(fileId);
    }

    public synchronized int rebuildAll() {
        int rebuiltCount = 0;
        for (KnowledgeDocumentLoader.StoredKnowledgeFile storedFile : knowledgeDocumentLoader.loadStoredFiles()) {
            try {
                List<Document> documents = indexFile(storedFile.fileInfo(), storedFile.content());
                if (!documents.isEmpty()) {
                    rebuiltCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to rebuild knowledge index for file {}: {}",
                        storedFile.fileInfo().originalFilename(), e.getMessage());
            }
        }
        return rebuiltCount;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildOnStartupIfNecessary() {
        if (!rebuildOnStartup || !(vectorStore instanceof SimpleVectorStore)) {
            return;
        }
        int rebuiltCount = rebuildAll();
        log.info("Knowledge index rebuild on startup finished, rebuilt {} file(s)", rebuiltCount);
    }
}
