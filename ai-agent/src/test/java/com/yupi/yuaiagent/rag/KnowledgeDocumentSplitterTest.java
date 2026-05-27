package com.yupi.yuaiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeDocumentSplitterTest {

    @Test
    void shouldKeepSmallFileAsSingleChunk() {
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(1000, 150);
        KnowledgeFileInfo fileInfo = new KnowledgeFileInfo("file-1", "notes.txt", 10, "text/plain", "indexed", 1L);

        List<Document> documents = splitter.split(fileInfo, "会议纪要\n今天同步了项目计划。");

        assertEquals(1, documents.size());
        assertEquals("file-1_chunk_0", documents.getFirst().getId());
    }

    @Test
    void shouldSplitLongMarkdownIntoMultipleChunksWithMetadata() {
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(80, 20);
        KnowledgeFileInfo fileInfo = new KnowledgeFileInfo("file-2", "guide.md", 100, "text/markdown", "indexed", 1L);
        String content = """
                # 标题一
                第一段内容很长，用于模拟一份需要切分的 Markdown 文档。第一段内容很长，用于模拟一份需要切分的 Markdown 文档。

                ## 标题二
                第二段继续补充更多办公流程说明，确保切分时能保留标题和段落边界。
                """;

        List<Document> documents = splitter.split(fileInfo, content);

        assertTrue(documents.size() > 1);
        Document first = documents.getFirst();
        assertEquals("guide.md", first.getMetadata().get("filename"));
        assertEquals("guide.md", first.getMetadata().get("originalFilename"));
        assertEquals("knowledge-file", first.getMetadata().get("source"));
        assertEquals(0, first.getMetadata().get("chunkIndex"));
        assertEquals(documents.size(), first.getMetadata().get("totalChunks"));
    }

    @Test
    void shouldApplyOverlapBetweenAdjacentChunks() {
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(80, 10);
        KnowledgeFileInfo fileInfo = new KnowledgeFileInfo("file-3", "policy.txt", 100, "text/plain", "indexed", 1L);
        String content = "这是第一段很长的报销制度说明，用于触发分块处理。".repeat(8);

        List<Document> documents = splitter.split(fileInfo, content);

        assertTrue(documents.size() > 1);
        String previous = documents.getFirst().getText();
        String current = documents.get(1).getText();
        String overlap = previous.substring(previous.length() - 10);
        assertTrue(current.startsWith(overlap));
    }

    @Test
    void shouldIgnoreEmptyContentSafely() {
        KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(1000, 150);
        KnowledgeFileInfo fileInfo = new KnowledgeFileInfo("file-4", "empty.txt", 0, "text/plain", "indexed", 1L);

        List<Document> documents = splitter.split(fileInfo, "   \n\n ");

        assertTrue(documents.isEmpty());
    }
}
