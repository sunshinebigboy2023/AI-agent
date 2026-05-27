package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KnowledgeDocumentSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    public KnowledgeDocumentSplitter(
            @Value("${office.knowledge.chunk-size:1000}") int chunkSize,
            @Value("${office.knowledge.chunk-overlap:150}") int chunkOverlap
    ) {
        this.chunkSize = Math.max(50, chunkSize);
        this.chunkOverlap = Math.max(0, Math.min(chunkOverlap, this.chunkSize - 1));
    }

    public List<Document> split(KnowledgeFileInfo fileInfo, String content) {
        String normalized = normalizeContent(content);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<String> blocks = isMarkdown(fileInfo.originalFilename())
                ? splitMarkdownBlocks(normalized)
                : splitTextBlocks(normalized);
        List<String> chunks = buildChunks(blocks);
        if (chunks.isEmpty()) {
            chunks = List.of(normalized);
        }
        long now = Instant.now().toEpochMilli();
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("fileId", fileInfo.id());
            metadata.put("filename", fileInfo.originalFilename());
            metadata.put("originalFilename", fileInfo.originalFilename());
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            metadata.put("source", "knowledge-file");
            metadata.put("category", "uploaded");
            metadata.put("contentType", fileInfo.contentType() == null ? "text/plain" : fileInfo.contentType());
            metadata.put("createTime", now);
            metadata.put("updateTime", now);
            documents.add(new Document(
                    fileInfo.id() + "_chunk_" + i,
                    chunks.get(i),
                    metadata
            ));
        }
        return documents;
    }

    private List<String> splitMarkdownBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\n", -1)) {
            if (line.matches("^#{1,6}\\s+.*")) {
                flushBlock(blocks, current);
                current.append(line.stripTrailing()).append("\n");
                continue;
            }
            if (line.isBlank()) {
                flushBlock(blocks, current);
                continue;
            }
            current.append(line.stripTrailing()).append("\n");
        }
        flushBlock(blocks, current);
        return blocks;
    }

    private List<String> splitTextBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        for (String block : content.split("\\n\\s*\\n+")) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private List<String> buildChunks(List<String> blocks) {
        List<String> baseChunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (block.length() > chunkSize) {
                flushCurrentChunk(baseChunks, current);
                baseChunks.addAll(splitLongBlock(block));
                continue;
            }
            String candidate = current.isEmpty() ? block : current + "\n\n" + block;
            if (candidate.length() <= chunkSize) {
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(block);
            } else {
                flushCurrentChunk(baseChunks, current);
                current.append(block);
            }
        }
        flushCurrentChunk(baseChunks, current);
        return applyOverlap(baseChunks);
    }

    private List<String> splitLongBlock(String block) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < block.length()) {
            int end = Math.min(block.length(), start + chunkSize);
            if (end < block.length()) {
                int boundary = findBoundary(block, start, end);
                if (boundary > start + 100) {
                    end = boundary;
                }
            }
            chunks.add(block.substring(start, end));
            if (end >= block.length()) {
                break;
            }
            start = Math.max(end - chunkOverlap, start + 1);
        }
        return chunks;
    }

    private List<String> applyOverlap(List<String> chunks) {
        if (chunkOverlap <= 0 || chunks.size() < 2) {
            return chunks;
        }
        List<String> overlapped = new ArrayList<>(chunks.size());
        overlapped.add(chunks.getFirst());
        for (int i = 1; i < chunks.size(); i++) {
            String previous = chunks.get(i - 1);
            String current = chunks.get(i);
            String prefix = previous.substring(Math.max(0, previous.length() - chunkOverlap));
            overlapped.add(current.startsWith(prefix) ? current : prefix + current);
        }
        return overlapped;
    }

    private int findBoundary(String text, int start, int end) {
        int fallback = end;
        for (int i = end; i > start; i--) {
            char ch = text.charAt(i - 1);
            if ("。！？；;,.， \n".indexOf(ch) >= 0) {
                return i;
            }
        }
        return fallback;
    }

    private void flushBlock(List<String> blocks, StringBuilder current) {
        String block = current.toString().trim();
        if (!block.isEmpty()) {
            blocks.add(block);
        }
        current.setLength(0);
    }

    private void flushCurrentChunk(List<String> chunks, StringBuilder current) {
        String value = current.toString().trim();
        if (!value.isEmpty()) {
            chunks.add(value);
        }
        current.setLength(0);
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace("\uFEFF", "").replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            log.warn("Skipping empty knowledge content after normalization");
        }
        return normalized;
    }

    private boolean isMarkdown(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".md");
    }
}
