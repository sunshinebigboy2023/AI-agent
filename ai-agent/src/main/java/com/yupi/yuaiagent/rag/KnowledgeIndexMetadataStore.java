package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class KnowledgeIndexMetadataStore {

    private static final String METADATA_FILENAME = "index-metadata.properties";

    private final Path metadataPath;

    public KnowledgeIndexMetadataStore(@Value("${office.knowledge.storage-dir:${user.dir}/tmp/knowledge}") String storageDir) {
        this.metadataPath = Path.of(storageDir).resolve(METADATA_FILENAME);
    }

    public synchronized List<String> getChunkIds(String fileId) {
        return new ArrayList<>(loadMappings().getOrDefault(fileId, List.of()));
    }

    public synchronized List<String> getChunkIds(String clientId, String fileId) {
        Map<String, List<String>> mappings = loadMappings();
        String key = buildKey(clientId, fileId);
        if (mappings.containsKey(key)) {
            return new ArrayList<>(mappings.get(key));
        }
        return new ArrayList<>(mappings.getOrDefault(fileId, List.of()));
    }

    public synchronized void saveChunkIds(String fileId, List<String> chunkIds) {
        Map<String, List<String>> mappings = loadMappings();
        mappings.put(fileId, List.copyOf(chunkIds));
        persist(mappings);
    }

    public synchronized void saveChunkIds(String clientId, String fileId, List<String> chunkIds) {
        Map<String, List<String>> mappings = loadMappings();
        mappings.put(buildKey(clientId, fileId), List.copyOf(chunkIds));
        mappings.remove(fileId);
        persist(mappings);
    }

    public synchronized void remove(String fileId) {
        Map<String, List<String>> mappings = loadMappings();
        mappings.remove(fileId);
        persist(mappings);
    }

    public synchronized void remove(String clientId, String fileId) {
        Map<String, List<String>> mappings = loadMappings();
        mappings.remove(buildKey(clientId, fileId));
        mappings.remove(fileId);
        persist(mappings);
    }

    public synchronized Map<String, List<String>> loadAll() {
        return loadMappings();
    }

    private Map<String, List<String>> loadMappings() {
        Properties properties = new Properties();
        if (Files.exists(metadataPath)) {
            try (Reader reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                log.warn("Failed to read knowledge index metadata from {}, skipping corrupted metadata: {}",
                        metadataPath, e.getMessage());
                return new LinkedHashMap<>();
            }
        }
        Map<String, List<String>> mappings = new LinkedHashMap<>();
        for (String fileId : properties.stringPropertyNames()) {
            String rawValue = properties.getProperty(fileId, "");
            List<String> ids = rawValue.isBlank()
                    ? List.of()
                    : List.of(rawValue.split(","));
            mappings.put(fileId, ids.stream().map(String::trim).filter(value -> !value.isEmpty()).toList());
        }
        return mappings;
    }

    private void persist(Map<String, List<String>> mappings) {
        try {
            Files.createDirectories(metadataPath.getParent());
            Properties properties = new Properties();
            for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
                properties.setProperty(entry.getKey(), String.join(",", entry.getValue()));
            }
            try (Writer writer = Files.newBufferedWriter(metadataPath, StandardCharsets.UTF_8)) {
                properties.store(writer, "Knowledge chunk index metadata");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist knowledge index metadata", e);
        }
    }

    private String buildKey(String clientId, String fileId) {
        return clientId + ":" + fileId;
    }
}
