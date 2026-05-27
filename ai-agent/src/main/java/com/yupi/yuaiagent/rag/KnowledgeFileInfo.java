package com.yupi.yuaiagent.rag;

public record KnowledgeFileInfo(
        String id,
        String clientId,
        String originalFilename,
        long size,
        String contentType,
        String status,
        long uploadedAt
) {
}
