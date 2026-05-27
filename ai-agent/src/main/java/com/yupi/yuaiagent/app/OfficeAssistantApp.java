package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class OfficeAssistantApp {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are a professional and reliable AI office assistant.",
            "Help users complete office work such as email drafting, meeting notes, weekly reports, outlines, summaries, task breakdowns, and communication polishing.",
            "First understand the user's real office goal, then provide structured and directly usable output.",
            "If information is missing, ask short clarification questions. If a document is requested, provide a title, structure, and copy-ready content.",
            "Keep answers professional, concise, actionable, and do not invent facts."
    );

    private final ChatClient chatClient;

    public OfficeAssistantApp(ChatModel dashscopeChatModel) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return wrapStreamMetrics("office_app.chat", message, chatId, chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content());
    }

    public Flux<String> doChatWithRagByStream(String message, String chatId) {
        return doChatWithRagByStream(message, chatId, null);
    }

    public Flux<String> doChatWithRagByStream(String message, String chatId, String clientId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        List<org.springframework.ai.document.Document> retrievedDocuments = retrieveKnowledgeDocuments(rewrittenMessage, clientId);
        logRetrievedDocuments(clientId, toRetrievedDocumentViews(retrievedDocuments));
        return wrapStreamMetrics("office_app.chat.rag", message, chatId, chatClient
                .prompt()
                .user(buildRagPrompt(message, rewrittenMessage, retrievedDocuments))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content());
    }

    record OfficeReport(String title, List<String> sections, List<String> actionItems) {
    }

    public OfficeReport doChatWithReport(String message, String chatId) {
        OfficeReport officeReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "\nGenerate an office report with a title, sections, and action items.")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(OfficeReport.class);
        log.info("officeReport: {}", officeReport);
        return officeReport;
    }

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private VectorStore officeVectorStore;

    public String doChatWithRag(String message, String chatId) {
        return doChatWithRag(message, chatId, null);
    }

    public String doChatWithRag(String message, String chatId, String clientId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        List<org.springframework.ai.document.Document> retrievedDocuments = retrieveKnowledgeDocuments(rewrittenMessage, clientId);
        logRetrievedDocuments(clientId, toRetrievedDocumentViews(retrievedDocuments));
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(buildRagPrompt(message, rewrittenMessage, retrievedDocuments))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public RagDebugResponse doChatWithRagDebug(String message, String chatId, String clientId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        List<RetrievedDocumentView> retrievedDocuments = toRetrievedDocumentViews(retrieveKnowledgeDocuments(rewrittenMessage, clientId));
        String answer = doChatWithRag(message, chatId, clientId);
        return new RagDebugResponse(rewrittenMessage, answer, retrievedDocuments);
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    private Flux<String> wrapStreamMetrics(String scenario, String message, String chatId, Flux<String> stream) {
        long start = System.currentTimeMillis();
        AtomicBoolean firstTokenLogged = new AtomicBoolean(false);
        return stream
                .doOnSubscribe(subscription -> log.info("{} started, chatId={}, messageLength={}, summary={}",
                        scenario, chatId, message.length(), summarize(message)))
                .doOnNext(chunk -> {
                    if (firstTokenLogged.compareAndSet(false, true)) {
                        log.info("{} first token in {} ms, chatId={}", scenario, System.currentTimeMillis() - start, chatId);
                    }
                })
                .doOnError(error -> log.warn("{} failed after {} ms, chatId={}, error={}",
                        scenario, System.currentTimeMillis() - start, chatId, error.getMessage()))
                .doFinally(signalType -> log.info("{} finished in {} ms, chatId={}, signal={}",
                        scenario, System.currentTimeMillis() - start, chatId, signalType));
    }

    private void logRetrievedDocuments(String clientId, List<RetrievedDocumentView> documents) {
        if (!documents.isEmpty()) {
            log.info("RAG retrieved documents for clientId={}: {}", clientId, documents);
        }
    }

    private List<org.springframework.ai.document.Document> retrieveKnowledgeDocuments(String query, String clientId) {
        List<org.springframework.ai.document.Document> documents = officeVectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(12).build()
        );
        if (documents == null) {
            return List.of();
        }
        List<org.springframework.ai.document.Document> filteredDocuments = new ArrayList<>();
        for (org.springframework.ai.document.Document document : documents) {
            if (clientId != null) {
                Object metadataClientId = document.getMetadata().get("clientId");
                if (metadataClientId == null || !clientId.equals(metadataClientId.toString())) {
                    continue;
                }
            }
            filteredDocuments.add(document);
            if (filteredDocuments.size() >= 4) {
                break;
            }
        }
        return filteredDocuments;
    }

    private List<RetrievedDocumentView> toRetrievedDocumentViews(List<org.springframework.ai.document.Document> documents) {
        List<RetrievedDocumentView> views = new ArrayList<>();
        for (org.springframework.ai.document.Document document : documents) {
            views.add(new RetrievedDocumentView(
                    String.valueOf(document.getMetadata().getOrDefault("filename", "unknown")),
                    parseChunkIndex(document),
                    summarize(document.getText())
            ));
        }
        return views;
    }

    private String buildRagPrompt(String originalMessage, String rewrittenMessage, List<org.springframework.ai.document.Document> documents) {
        StringBuilder prompt = new StringBuilder()
                .append("用户问题：").append(originalMessage).append("\n")
                .append("检索改写：").append(rewrittenMessage).append("\n\n");
        if (documents.isEmpty()) {
            prompt.append("知识库片段：未检索到当前用户的相关知识库内容。\n")
                    .append("请明确告知用户当前知识库中没有足够相关内容，不要虚构知识库来源；如有必要，可给出通用建议。");
            return prompt.toString();
        }
        prompt.append("仅参考以下当前用户知识库片段回答，并在内容不足时明确说明：\n");
        for (int i = 0; i < documents.size(); i++) {
            org.springframework.ai.document.Document document = documents.get(i);
            prompt.append("[片段 ").append(i + 1).append("] ")
                    .append(document.getMetadata().getOrDefault("filename", "unknown")).append(" / chunk ")
                    .append(parseChunkIndex(document)).append("\n")
                    .append(trimKnowledgeChunk(document.getText())).append("\n\n");
        }
        prompt.append("请优先依据以上知识库片段给出准确、简洁、可执行的回答。");
        return prompt.toString();
    }

    private int parseChunkIndex(org.springframework.ai.document.Document document) {
        Object metadataChunkIndex = document.getMetadata().get("chunkIndex");
        if (metadataChunkIndex != null) {
            try {
                return Integer.parseInt(metadataChunkIndex.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String trimKnowledgeChunk(String text) {
        String normalized = text == null ? "" : text.trim();
        return normalized.length() <= 800 ? normalized : normalized.substring(0, 800) + "...";
    }

    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }

    public record RagDebugResponse(
            String rewrittenQuery,
            String answer,
            List<RetrievedDocumentView> retrievedDocuments
    ) {
    }

    public record RetrievedDocumentView(
            String filename,
            int chunkIndex,
            String preview
    ) {
    }
}
