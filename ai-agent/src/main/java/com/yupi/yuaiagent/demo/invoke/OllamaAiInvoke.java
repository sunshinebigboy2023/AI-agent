package com.yupi.yuaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;

public class OllamaAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) {
        AssistantMessage assistantMessage = ollamaChatModel.call(new Prompt("Hello, please help me draft a meeting summary."))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
