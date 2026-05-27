package com.yupi.yuaiagent.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class KnowledgeFileService {

    private static final String METADATA_FILE = "metadata.properties";
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".md", ".txt");
    private static final long DEFAULT_MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

    private final Path storageDir;
    private final long maxUploadBytes;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeClientIdSupport knowledgeClientIdSupport;

    @Autowired
    public KnowledgeFileService(
            @Value("${office.knowledge.storage-dir:${user.dir}/tmp/knowledge}") String storageDir,
            @Value("${office.knowledge.max-upload-bytes:10485760}") long maxUploadBytes,
            KnowledgeIndexService knowledgeIndexService,
            KnowledgeClientIdSupport knowledgeClientIdSupport
    ) {
        this(Path.of(storageDir), knowledgeIndexService, knowledgeClientIdSupport, maxUploadBytes);
    }

    KnowledgeFileService(Path storageDir, KnowledgeIndexService knowledgeIndexService, KnowledgeClientIdSupport knowledgeClientIdSupport) {
        this(storageDir, knowledgeIndexService, knowledgeClientIdSupport, DEFAULT_MAX_UPLOAD_BYTES);
    }

    KnowledgeFileService(Path storageDir, KnowledgeIndexService knowledgeIndexService, KnowledgeClientIdSupport knowledgeClientIdSupport, long maxUploadBytes) {
        this.storageDir = storageDir;
        this.knowledgeIndexService = knowledgeIndexService;
        this.knowledgeClientIdSupport = knowledgeClientIdSupport;
        this.maxUploadBytes = maxUploadBytes;
    }

    public KnowledgeFileInfo upload(String clientId, MultipartFile file) throws IOException {
        clientId = knowledgeClientIdSupport.requireValidClientId(clientId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("Uploaded file exceeds the " + maxUploadBytes + " byte limit.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        validateSupportedFile(originalFilename);

        Files.createDirectories(storageDir);
        String fileId = UUID.randomUUID().toString();
        Path clientDir = storageDir.resolve(clientId);
        Files.createDirectories(clientDir);
        Path fileDir = clientDir.resolve(fileId);
        Files.createDirectories(fileDir);

        Path savedFile = fileDir.resolve(originalFilename);
        Files.copy(file.getInputStream(), savedFile, StandardCopyOption.REPLACE_EXISTING);

        String content = Files.readString(savedFile, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(content)) {
            FileSystemUtils.deleteRecursively(fileDir);
            throw new IllegalArgumentException("Uploaded file is empty after decoding.");
        }
        KnowledgeFileInfo info = new KnowledgeFileInfo(
                fileId,
                clientId,
                originalFilename,
                file.getSize(),
                file.getContentType(),
                "indexed",
                Instant.now().toEpochMilli()
        );
        try {
            saveMetadata(fileDir, info);
            knowledgeIndexService.indexFile(clientId, info, content);
            return info;
        } catch (Exception e) {
            FileSystemUtils.deleteRecursively(fileDir);
            throw new IOException("Failed to index uploaded knowledge file: " + e.getMessage(), e);
        }
    }

    public List<KnowledgeFileInfo> listFiles(String clientId) throws IOException {
        clientId = knowledgeClientIdSupport.requireValidClientId(clientId);
        Path clientDir = storageDir.resolve(clientId);
        if (!Files.exists(clientDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(clientDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::readMetadataQuietly)
                    .filter(info -> info != null)
                    .sorted(Comparator.comparingLong(KnowledgeFileInfo::uploadedAt).reversed())
                    .toList();
        }
    }

    public void delete(String clientId, String fileId) throws IOException {
        clientId = knowledgeClientIdSupport.requireValidClientId(clientId);
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId is required.");
        }
        try {
            UUID.fromString(fileId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("fileId is invalid.");
        }
        Path fileDir = storageDir.resolve(clientId).resolve(fileId);
        if (!Files.exists(fileDir)) {
            throw new IllegalArgumentException("Knowledge file does not exist for current client.");
        }
        knowledgeIndexService.deleteFileIndex(clientId, fileId);
        FileSystemUtils.deleteRecursively(fileDir);
    }

    private void saveMetadata(Path fileDir, KnowledgeFileInfo info) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", info.id());
        properties.setProperty("clientId", info.clientId());
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
                    properties.getProperty("clientId"),
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
