package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.OfficeAgent;
import com.yupi.yuaiagent.app.OfficeAssistantApp;
import com.yupi.yuaiagent.rag.KnowledgeClientIdSupport;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Value("${office.chat.max-message-length:8000}")
    private int maxMessageLength;

    @Resource
    private OfficeAssistantApp officeAssistantApp;

    @Resource
    private ObjectProvider<OfficeAgent> officeAgentProvider;

    @Resource
    private KnowledgeClientIdSupport knowledgeClientIdSupport;

    @GetMapping("/office_app/chat/sync")
    public String doChatWithOfficeAssistantAppSync(
            @RequestParam String message,
            @RequestParam(required = false) String chatId
    ) {
        validateMessage(message);
        return officeAssistantApp.doChat(message, ensureChatId(chatId));
    }

    @GetMapping(value = "/office_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithOfficeAssistantAppSSE(
            @RequestParam String message,
            @RequestParam(required = false) String chatId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientIdHeader,
            @RequestParam(required = false) String clientId
    ) {
        validateMessage(message);
        String resolvedClientId = resolveClientId(clientIdHeader, clientId);
        if (resolvedClientId == null) {
            return Flux.just("缺少客户端标识，请刷新页面重试");
        }
        return officeAssistantApp.doChatWithRagByStream(message, ensureChatId(chatId), resolvedClientId)
                .onErrorResume(error -> Flux.just("模型服务暂时不可用，请稍后重试"));
    }

    @GetMapping(value = "/office_app/chat/rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithOfficeAssistantAppRagSSE(
            @RequestParam String message,
            @RequestParam(required = false) String chatId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientIdHeader,
            @RequestParam(required = false) String clientId
    ) {
        validateMessage(message);
        String resolvedClientId = resolveClientId(clientIdHeader, clientId);
        if (resolvedClientId == null) {
            return Flux.just("缺少客户端标识，请刷新页面重试");
        }
        return officeAssistantApp.doChatWithRagByStream(message, ensureChatId(chatId), resolvedClientId)
                .onErrorResume(error -> Flux.just("模型服务暂时不可用，请稍后重试"));
    }

    @GetMapping(value = "/office_app/chat/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithOfficeAssistantAppStructuredSSE(
            @RequestParam String message,
            @RequestParam(required = false) String chatId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientIdHeader,
            @RequestParam(required = false) String clientId
    ) {
        validateMessage(message);
        String resolvedChatId = ensureChatId(chatId);
        SseEmitter emitter = new SseEmitter(180000L);
        String resolvedClientId;
        try {
            resolvedClientId = resolveClientId(clientIdHeader, clientId);
        } catch (IllegalArgumentException e) {
            sendEvent(emitter, "error", Map.of("message", e.getMessage()));
            emitter.complete();
            return emitter;
        }
        if (resolvedClientId == null) {
            sendEvent(emitter, "error", Map.of("message", "缺少客户端标识，请刷新页面重试"));
            emitter.complete();
            return emitter;
        }
        Disposable disposable = officeAssistantApp.doChatWithRagByStream(message, resolvedChatId, resolvedClientId)
                .subscribe(chunk -> sendEvent(emitter, "message", Map.of("content", chunk)),
                        error -> {
                            sendEvent(emitter, "error", Map.of("message", normalizeStreamError(error)));
                            emitter.complete();
                        },
                        () -> {
                            sendEvent(emitter, "done", Map.of("chatId", resolvedChatId));
                            emitter.complete();
                        });
        emitter.onCompletion(disposable::dispose);
        emitter.onTimeout(disposable::dispose);
        return emitter;
    }

    @GetMapping("/office_app/chat/rag/debug")
    public OfficeAssistantApp.RagDebugResponse doChatWithOfficeAssistantAppRagDebug(
            @RequestParam String message,
            @RequestParam(required = false) String chatId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientIdHeader,
            @RequestParam(required = false) String clientId
    ) {
        validateMessage(message);
        String resolvedClientId = knowledgeClientIdSupport.requireValidClientId(resolveClientId(clientIdHeader, clientId));
        return officeAssistantApp.doChatWithRagDebug(message, ensureChatId(chatId), resolvedClientId);
    }

    @GetMapping(value = "/office_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithOfficeAssistantAppServerSentEvent(
            @RequestParam String message,
            @RequestParam(required = false) String chatId
    ) {
        validateMessage(message);
        return officeAssistantApp.doChatByStream(message, ensureChatId(chatId))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping(value = "/office_app/chat/sse_emitter")
    public SseEmitter doChatWithOfficeAssistantAppServerSseEmitter(
            @RequestParam String message,
            @RequestParam(required = false) String chatId
    ) {
        validateMessage(message);
        String resolvedChatId = ensureChatId(chatId);
        SseEmitter sseEmitter = new SseEmitter(180000L);
        Disposable disposable = officeAssistantApp.doChatByStream(message, resolvedChatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        sseEmitter.onCompletion(disposable::dispose);
        sseEmitter.onTimeout(disposable::dispose);
        return sseEmitter;
    }

    @GetMapping("/office-agent/chat")
    public SseEmitter doChatWithOfficeAgent(@RequestParam String message) {
        validateMessage(message);
        return officeAgentProvider.getObject().runStream(message);
    }

    @GetMapping(value = "/office-agent/chat/stream-with-steps", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithOfficeAgentWithSteps(@RequestParam String message) {
        validateMessage(message);
        return officeAgentProvider.getObject().runStructuredStream(message);
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required.");
        }
        if (message.length() > maxMessageLength) {
            throw new IllegalArgumentException("message must be no longer than " + maxMessageLength + " characters.");
        }
    }

    private String ensureChatId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? "chat_" + UUID.randomUUID() : chatId;
    }

    private String resolveClientId(String clientIdHeader, String clientIdQuery) {
        String candidate = (clientIdHeader != null && !clientIdHeader.isBlank()) ? clientIdHeader : clientIdQuery;
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        return knowledgeClientIdSupport.requireValidClientId(candidate);
    }

    private String normalizeStreamError(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.isBlank()) {
            return "后端处理失败，请稍后重试";
        }
        if (message.contains("DashScope") || message.contains("API key")) {
            return "模型服务暂时不可用";
        }
        return message;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
