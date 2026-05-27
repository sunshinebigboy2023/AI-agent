package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
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
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        logRetrievedDocuments(rewrittenMessage);
        return wrapStreamMetrics("office_app.chat.rag", message, chatId, chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(officeVectorStore))
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
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        logRetrievedDocuments(rewrittenMessage);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(officeVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public RagDebugResponse doChatWithRagDebug(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        List<RetrievedDocumentView> retrievedDocuments = retrieveDocuments(rewrittenMessage);
        String answer = doChatWithRag(message, chatId);
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

    private void logRetrievedDocuments(String query) {
        List<RetrievedDocumentView> documents = retrieveDocuments(query);
        if (!documents.isEmpty()) {
            log.info("RAG retrieved documents: {}", documents);
        }
    }

    private List<RetrievedDocumentView> retrieveDocuments(String query) {
        List<org.springframework.ai.document.Document> documents = officeVectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(4).build()
        );
        if (documents == null) {
            return List.of();
        }
        List<RetrievedDocumentView> views = new ArrayList<>();
        for (org.springframework.ai.document.Document document : documents) {
            int chunkIndex = 0;
            Object metadataChunkIndex = document.getMetadata().get("chunkIndex");
            if (metadataChunkIndex != null) {
                try {
                    chunkIndex = Integer.parseInt(metadataChunkIndex.toString());
                } catch (NumberFormatException ignored) {
                    chunkIndex = 0;
                }
            }
            views.add(new RetrievedDocumentView(
                    String.valueOf(document.getMetadata().getOrDefault("filename", "unknown")),
                    chunkIndex,
                    summarize(document.getText())
            ));
        }
        return views;
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
