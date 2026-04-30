package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class OfficeAgent extends ToolCallAgent {

    public OfficeAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("officeAgent");
        this.setSystemPrompt(String.join("\n",
                "You are OfficeAgent, an AI assistant for office productivity and task execution.",
                "Help users research information, organize material, generate documents, summarize content, and break complex office work into executable steps.",
                "Use tools only when they materially help complete the task."
        ));
        this.setNextStepPrompt(String.join("\n",
                "Select the most appropriate tool or combination of tools based on the user's office task.",
                "For complex requests, break the work into steps and execute them one by one.",
                "After each tool call, summarize the result clearly and decide the next useful action.",
                "When the task is complete, provide a concise final answer and use the terminate tool/function call."
        ));
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
