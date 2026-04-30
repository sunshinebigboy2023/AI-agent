package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.OfficeAgent;
import com.yupi.yuaiagent.app.OfficeAssistantApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private OfficeAssistantApp officeAssistantApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("/office_app/chat/sync")
    public String doChatWithOfficeAssistantAppSync(String message, String chatId) {
        return officeAssistantApp.doChat(message, chatId);
    }

    @GetMapping(value = "/office_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithOfficeAssistantAppSSE(String message, String chatId) {
        return officeAssistantApp.doChatWithRagByStream(message, chatId);
    }

    @GetMapping(value = "/office_app/chat/rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithOfficeAssistantAppRagSSE(String message, String chatId) {
        return officeAssistantApp.doChatWithRagByStream(message, chatId);
    }

    @GetMapping(value = "/office_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithOfficeAssistantAppServerSentEvent(String message, String chatId) {
        return officeAssistantApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping(value = "/office_app/chat/sse_emitter")
    public SseEmitter doChatWithOfficeAssistantAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        officeAssistantApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    @GetMapping("/office-agent/chat")
    public SseEmitter doChatWithOfficeAgent(String message) {
        OfficeAgent officeAgent = new OfficeAgent(allTools, dashscopeChatModel);
        return officeAgent.runStream(message);
    }
}
