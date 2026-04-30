package com.yupi.yuaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class KnowledgeFileService {

    private static final String METADATA_FILE = "metadata.properties";
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".md", ".txt");

    private final Path storageDir;
    private final VectorStore vectorStore;

    public KnowledgeFileService(
            @Value("${office.knowledge.storage-dir:${user.dir}/tmp/knowledge}") String storageDir,
            VectorStore vectorStore
    ) {
        this(Path.of(storageDir), vectorStore);
    }

    KnowledgeFileService(Path storageDir, VectorStore vectorStore) {
        this.storageDir = storageDir;
        this.vectorStore = vectorStore;
    }

    public KnowledgeFileInfo upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        validateSupportedFile(originalFilename);

        Files.createDirectories(storageDir);
        String fileId = UUID.randomUUID().toString();
        Path fileDir = storageDir.resolve(fileId);
        Files.createDirectories(fileDir);

        Path savedFile = fileDir.resolve(originalFilename);
        Files.copy(file.getInputStream(), savedFile, StandardCopyOption.REPLACE_EXISTING);

        String content = Files.readString(savedFile, StandardCharsets.UTF_8);
        KnowledgeFileInfo info = new KnowledgeFileInfo(
                fileId,
                originalFilename,
                file.getSize(),
                file.getContentType(),
                "indexed",
                Instant.now().toEpochMilli()
        );
        saveMetadata(fileDir, info);
        indexFile(info, content);
        return info;
    }

    public List<KnowledgeFileInfo> listFiles() throws IOException {
        if (!Files.exists(storageDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::readMetadataQuietly)
                    .filter(info -> info != null)
                    .sorted(Comparator.comparingLong(KnowledgeFileInfo::uploadedAt).reversed())
                    .toList();
        }
    }

    public void delete(String fileId) throws IOException {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId is required.");
        }
        vectorStore.delete(List.of(fileId));
        FileSystemUtils.deleteRecursively(storageDir.resolve(fileId));
    }

    private void indexFile(KnowledgeFileInfo info, String content) {
        Document document = new Document(
                info.id(),
                content,
                Map.of(
                        "fileId", info.id(),
                        "filename", info.originalFilename(),
                        "category", "uploaded"
                )
        );
        vectorStore.add(List.of(document));
    }

    private void saveMetadata(Path fileDir, KnowledgeFileInfo info) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", info.id());
        properties.setProperty("originalFilename", info.originalFilename());
        properties.setProperty("size", String.valueOf(info.size()));
        properties.setProperty("contentType", info.contentType() == null ? "" : info.contentType());
        properties.setProperty("status", info.status());
        properties.setProperty("uploadedAt", String.valueOf(info.uploadedAt()));
        try (var writer = Files.newBufferedWriter(fileDir.resolve(METADATA_FILE), StandardCharsets.UTF_8)) {
            properties.store(writer, "Knowledge file metadata");
        }
    }

    private KnowledgeFileInfo readMetadataQuietly(Path fileDir) {
        Path metadataPath = fileDir.resolve(METADATA_FILE);
        if (!Files.exists(metadataPath)) {
            return null;
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return new KnowledgeFileInfo(
                    properties.getProperty("id"),
                    properties.getProperty("originalFilename"),
                    Long.parseLong(properties.getProperty("size", "0")),
                    properties.getProperty("contentType"),
                    properties.getProperty("status", "indexed"),
                    Long.parseLong(properties.getProperty("uploadedAt", "0"))
            );
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required.");
        }
        return Path.of(filename).getFileName().toString();
    }

    private void validateSupportedFile(String filename) {
        String lowerName = filename.toLowerCase();
        boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!supported) {
            throw new IllegalArgumentException("Only .md and .txt files are supported.");
        }
    }
}
