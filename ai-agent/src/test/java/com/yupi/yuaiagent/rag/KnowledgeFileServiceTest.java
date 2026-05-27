package com.yupi.yuaiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeFileServiceTest {

    private static final String CLIENT_A = "client_A123";
    private static final String CLIENT_B = "client_B456";

    @Test
    void uploadListAndDeleteShouldHandleAllChunks() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-service-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(80, 10);
        KnowledgeIndexMetadataStore metadataStore = new KnowledgeIndexMetadataStore(storageDir.toString());
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir, splitter);
        KnowledgeIndexService indexService = new KnowledgeIndexService(vectorStore, splitter, loader, metadataStore, true);
        KnowledgeFileService service = new KnowledgeFileService(storageDir, indexService, new KnowledgeClientIdSupport());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "meeting-notes.txt",
                "text/plain",
                "Project meeting notes\n".repeat(20).getBytes()
        );

        KnowledgeFileInfo uploaded = service.upload(CLIENT_A, file);

        assertEquals("meeting-notes.txt", uploaded.originalFilename());
        assertTrue(Files.exists(storageDir.resolve(CLIENT_A).resolve(uploaded.id()).resolve("meeting-notes.txt")));
        assertTrue(vectorStore.addedDocuments.size() > 1);

        List<KnowledgeFileInfo> files = service.listFiles(CLIENT_A);
        assertEquals(1, files.size());

        service.delete(CLIENT_A, uploaded.id());

        assertEquals(vectorStore.addedDocuments.size(), vectorStore.deletedIds.size());
        assertFalse(Files.exists(storageDir.resolve(CLIENT_A).resolve(uploaded.id())));
        assertTrue(service.listFiles(CLIENT_A).isEmpty());
    }

    @Test
    void rebuildAllShouldReuseChunkMetadataWithoutCrashing() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-rebuild-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(80, 10);
        KnowledgeIndexMetadataStore metadataStore = new KnowledgeIndexMetadataStore(storageDir.toString());
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir, splitter);
        KnowledgeIndexService indexService = new KnowledgeIndexService(vectorStore, splitter, loader, metadataStore, true);
        KnowledgeFileService service = new KnowledgeFileService(storageDir, indexService, new KnowledgeClientIdSupport());

        service.upload(CLIENT_A, new MockMultipartFile(
                "file",
                "policy.md",
                "text/markdown",
                ("# 报销制度\n" + "说明内容".repeat(30)).getBytes()
        ));

        int firstRebuild = indexService.rebuildAll();
        int secondRebuild = indexService.rebuildAll();

        assertEquals(1, firstRebuild);
        assertEquals(1, secondRebuild);
        assertFalse(metadataStore.loadAll().isEmpty());
        assertFalse(vectorStore.deletedIds.isEmpty());
    }

    @Test
    void loadDocumentsFromStoredKnowledgeFiles() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-loader-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(1000, 150);
        KnowledgeIndexMetadataStore metadataStore = new KnowledgeIndexMetadataStore(storageDir.toString());
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir, splitter);
        KnowledgeIndexService indexService = new KnowledgeIndexService(vectorStore, splitter, loader, metadataStore, true);
        KnowledgeFileService service = new KnowledgeFileService(storageDir, indexService, new KnowledgeClientIdSupport());
        KnowledgeFileInfo uploaded = service.upload(CLIENT_A, new MockMultipartFile(
                "file",
                "policy.md",
                "text/markdown",
                "# Reimbursement\nSubmit invoices before Friday.".getBytes()
        ));

        List<Document> documents = loader.loadUploadedDocuments();

        assertEquals(1, documents.size());
        assertEquals(CLIENT_A + "_" + uploaded.id() + "_chunk_0", documents.getFirst().getId());
        assertEquals(CLIENT_A, documents.getFirst().getMetadata().get("clientId"));
        assertEquals("policy.md", documents.getFirst().getMetadata().get("filename"));
        assertTrue(documents.getFirst().getText().contains("Submit invoices"));
    }

    @Test
    void listAndDeleteShouldBeIsolatedByClientId() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-client-isolation-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(120, 20);
        KnowledgeIndexMetadataStore metadataStore = new KnowledgeIndexMetadataStore(storageDir.toString());
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir, splitter);
        KnowledgeIndexService indexService = new KnowledgeIndexService(vectorStore, splitter, loader, metadataStore, true);
        KnowledgeFileService service = new KnowledgeFileService(storageDir, indexService, new KnowledgeClientIdSupport());

        KnowledgeFileInfo clientAFile = service.upload(CLIENT_A, new MockMultipartFile(
                "file",
                "client-a.txt",
                "text/plain",
                "client a knowledge".getBytes()
        ));
        service.upload(CLIENT_B, new MockMultipartFile(
                "file",
                "client-b.txt",
                "text/plain",
                "client b knowledge".getBytes()
        ));

        assertEquals(1, service.listFiles(CLIENT_A).size());
        assertEquals(1, service.listFiles(CLIENT_B).size());

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.delete(CLIENT_B, clientAFile.id())
        );
        assertTrue(exception.getMessage().contains("does not exist"));
        assertEquals(1, service.listFiles(CLIENT_A).size());
    }

    @Test
    void invalidClientIdShouldBeRejected() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-invalid-client-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(120, 20);
        KnowledgeIndexMetadataStore metadataStore = new KnowledgeIndexMetadataStore(storageDir.toString());
        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir, splitter);
        KnowledgeIndexService indexService = new KnowledgeIndexService(vectorStore, splitter, loader, metadataStore, true);
        KnowledgeFileService service = new KnowledgeFileService(storageDir, indexService, new KnowledgeClientIdSupport());

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.listFiles("../../root")
        );

        assertTrue(exception.getMessage().contains("X-Client-Id"));
    }

    static class RecordingVectorStore implements VectorStore {
        private final List<Document> addedDocuments = new ArrayList<>();
        private final List<String> deletedIds = new ArrayList<>();

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public void add(List<Document> documents) {
            addedDocuments.addAll(documents);
        }

        @Override
        public void delete(List<String> idList) {
            deletedIds.addAll(idList);
        }

        @Override
        public void delete(Filter.Expression expression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }

        @Override
        public <T> Optional<T> getNativeClient() {
            return Optional.empty();
        }
    }
}
