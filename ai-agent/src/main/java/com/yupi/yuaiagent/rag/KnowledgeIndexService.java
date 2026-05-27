package com.yupi.yuaiagent.rag;

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

    private final VectorStore vectorStore;
    private final KnowledgeDocumentSplitter knowledgeDocumentSplitter;
    private final KnowledgeDocumentLoader knowledgeDocumentLoader;
    private final KnowledgeIndexMetadataStore knowledgeIndexMetadataStore;
    private final boolean rebuildOnStartup;

    public KnowledgeIndexService(
            VectorStore vectorStore,
            KnowledgeDocumentSplitter knowledgeDocumentSplitter,
            KnowledgeDocumentLoader knowledgeDocumentLoader,
            KnowledgeIndexMetadataStore knowledgeIndexMetadataStore,
            @Value("${office.knowledge.rebuild-on-startup:true}") boolean rebuildOnStartup
    ) {
        this.vectorStore = vectorStore;
        this.knowledgeDocumentSplitter = knowledgeDocumentSplitter;
        this.knowledgeDocumentLoader = knowledgeDocumentLoader;
        this.knowledgeIndexMetadataStore = knowledgeIndexMetadataStore;
        this.rebuildOnStartup = rebuildOnStartup;
    }

    public List<Document> indexFile(String clientId, KnowledgeFileInfo fileInfo, String content) {
        List<Document> documents = knowledgeDocumentSplitter.split(clientId, fileInfo, content);
        if (documents.isEmpty()) {
            log.warn("No chunks generated for knowledge file {}", fileInfo.originalFilename());
            knowledgeIndexMetadataStore.saveChunkIds(clientId, fileInfo.id(), List.of());
            return List.of();
        }
        List<String> existingChunkIds = knowledgeIndexMetadataStore.getChunkIds(clientId, fileInfo.id());
        if (!existingChunkIds.isEmpty()) {
            vectorStore.delete(existingChunkIds);
        }
        vectorStore.add(documents);
        knowledgeIndexMetadataStore.saveChunkIds(
                clientId,
                fileInfo.id(),
                documents.stream().map(Document::getId).toList()
        );
        return documents;
    }

    public void deleteFileIndex(String clientId, String fileId) {
        List<String> chunkIds = knowledgeIndexMetadataStore.getChunkIds(clientId, fileId);
        if (!chunkIds.isEmpty()) {
            vectorStore.delete(chunkIds);
        }
        knowledgeIndexMetadataStore.remove(clientId, fileId);
    }

    public List<String> listIndexedChunks(String clientId, String fileId) {
        return knowledgeIndexMetadataStore.getChunkIds(clientId, fileId);
    }

    public synchronized int rebuildAll() {
        int rebuiltCount = 0;
        for (KnowledgeDocumentLoader.StoredKnowledgeFile storedFile : knowledgeDocumentLoader.loadStoredFiles()) {
            try {
                List<Document> documents = indexFile(storedFile.fileInfo().clientId(), storedFile.fileInfo(), storedFile.content());
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
