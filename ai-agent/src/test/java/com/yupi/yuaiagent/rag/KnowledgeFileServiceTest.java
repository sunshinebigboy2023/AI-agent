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

    @Test
    void uploadListAndDeleteTextFile() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-service-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeFileService service = new KnowledgeFileService(storageDir, vectorStore);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "meeting-notes.txt",
                "text/plain",
                "Project meeting notes\n- Alice owns the launch checklist".getBytes()
        );

        KnowledgeFileInfo uploaded = service.upload(file);

        assertEquals("meeting-notes.txt", uploaded.originalFilename());
        assertEquals("indexed", uploaded.status());
        assertTrue(Files.exists(storageDir.resolve(uploaded.id()).resolve("meeting-notes.txt")));
        assertEquals(1, vectorStore.addedDocuments.size());
        assertEquals(uploaded.id(), vectorStore.addedDocuments.getFirst().getMetadata().get("fileId"));

        List<KnowledgeFileInfo> files = service.listFiles();
        assertEquals(1, files.size());
        assertEquals(uploaded.id(), files.getFirst().id());

        service.delete(uploaded.id());

        assertTrue(vectorStore.deletedIds.contains(uploaded.id()));
        assertFalse(Files.exists(storageDir.resolve(uploaded.id())));
        assertTrue(service.listFiles().isEmpty());
    }

    @Test
    void loadDocumentsFromStoredKnowledgeFiles() throws Exception {
        Path storageDir = Files.createTempDirectory("knowledge-loader-test");
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        KnowledgeFileService service = new KnowledgeFileService(storageDir, vectorStore);
        KnowledgeFileInfo uploaded = service.upload(new MockMultipartFile(
                "file",
                "policy.md",
                "text/markdown",
                "# Reimbursement\nSubmit invoices before Friday.".getBytes()
        ));

        KnowledgeDocumentLoader loader = new KnowledgeDocumentLoader(storageDir);
        List<Document> documents = loader.loadUploadedDocuments();

        assertEquals(1, documents.size());
        assertEquals(uploaded.id(), documents.getFirst().getId());
        assertEquals("policy.md", documents.getFirst().getMetadata().get("filename"));
        assertTrue(documents.getFirst().getText().contains("Submit invoices"));
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
