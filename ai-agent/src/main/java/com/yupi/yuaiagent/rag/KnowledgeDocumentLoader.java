package com.yupi.yuaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@Component
public class KnowledgeDocumentLoader {

    private static final String METADATA_FILE = "metadata.properties";

    private final Path storageDir;
    private final KnowledgeDocumentSplitter knowledgeDocumentSplitter;

    @Autowired
    public KnowledgeDocumentLoader(
            @Value("${office.knowledge.storage-dir:${user.dir}/tmp/knowledge}") String storageDir,
            KnowledgeDocumentSplitter knowledgeDocumentSplitter
    ) {
        this(Path.of(storageDir), knowledgeDocumentSplitter);
    }

    KnowledgeDocumentLoader(Path storageDir, KnowledgeDocumentSplitter knowledgeDocumentSplitter) {
        this.storageDir = storageDir;
        this.knowledgeDocumentSplitter = knowledgeDocumentSplitter;
    }

    public List<Document> loadUploadedDocuments() throws IOException {
        return loadStoredFiles().stream()
                .flatMap(file -> knowledgeDocumentSplitter.split(file.fileInfo(), file.content()).stream())
                .toList();
    }

    public List<StoredKnowledgeFile> loadStoredFiles() {
        if (!Files.exists(storageDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::loadStoredFileQuietly)
                    .filter(file -> file != null)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private StoredKnowledgeFile loadStoredFileQuietly(Path fileDir) {
        try {
            Properties metadata = readMetadata(fileDir.resolve(METADATA_FILE));
            String id = metadata.getProperty("id");
            String filename = metadata.getProperty("originalFilename");
            if (!StringUtils.hasText(id) || !StringUtils.hasText(filename)) {
                return null;
            }
            Path filePath = fileDir.resolve(filename);
            if (!Files.exists(filePath)) {
                return null;
            }
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(content)) {
                return null;
            }
            return new StoredKnowledgeFile(
                    new KnowledgeFileInfo(
                            id,
                            filename,
                            Long.parseLong(metadata.getProperty("size", "0")),
                            metadata.getProperty("contentType"),
                            metadata.getProperty("status", "indexed"),
                            Long.parseLong(metadata.getProperty("uploadedAt", "0"))
                    ),
                    filePath,
                    content
            );
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private Properties readMetadata(Path metadataPath) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    public record StoredKnowledgeFile(
            KnowledgeFileInfo fileInfo,
            Path path,
            String content
    ) {
    }
}
