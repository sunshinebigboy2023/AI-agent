package com.yupi.yuaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

@Component
public class KnowledgeDocumentLoader {

    private static final String METADATA_FILE = "metadata.properties";

    private final Path storageDir;

    public KnowledgeDocumentLoader(@Value("${office.knowledge.storage-dir:${user.dir}/tmp/knowledge}") String storageDir) {
        this(Path.of(storageDir));
    }

    KnowledgeDocumentLoader(Path storageDir) {
        this.storageDir = storageDir;
    }

    public List<Document> loadUploadedDocuments() throws IOException {
        if (!Files.exists(storageDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::loadDocumentQuietly)
                    .filter(document -> document != null)
                    .toList();
        }
    }

    private Document loadDocumentQuietly(Path fileDir) {
        try {
            Properties metadata = readMetadata(fileDir.resolve(METADATA_FILE));
            String id = metadata.getProperty("id");
            String filename = metadata.getProperty("originalFilename");
            if (id == null || filename == null) {
                return null;
            }
            Path filePath = fileDir.resolve(filename);
            if (!Files.exists(filePath)) {
                return null;
            }
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return new Document(
                    id,
                    content,
                    Map.of(
                            "fileId", id,
                            "filename", filename,
                            "category", "uploaded"
                    )
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
}
