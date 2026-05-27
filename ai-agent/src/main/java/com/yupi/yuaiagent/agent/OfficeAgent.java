package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class OfficeAgent extends ToolCallAgent {

    public OfficeAgent(
            ToolCallback[] allTools,
            ChatModel dashscopeChatModel,
            @Value("${office.agent.max-steps:20}") int maxSteps,
            @Value("${office.agent.max-observation-length:4000}") int maxObservationLength
    ) {
        super(allTools);
        this.setName("officeAgent");
        this.setSystemPrompt(String.join("\n",
                "You are OfficeAgent, an AI assistant for office productivity and task execution.",
                "Help users research information, organize material, generate documents, summarize content, and break complex office work into executable steps.",
                "Use tools only when they materially help complete the task.",
                "Always answer in the same language as the user.",
                "If the user asks in Chinese, the final answer must be in Simplified Chinese.",
                "Tool observations may contain English, but the final user-visible summary must match the user's language.",
                "For job or internship search tasks, prioritize official careers pages and recruiting platforms over blogs, articles, or generic search homepages.",
                "Never show raw tool JSON to the user. Always turn tool results into a concise natural-language answer."
        ));
        this.setNextStepPrompt(String.join("\n",
                "Select the most appropriate tool or combination of tools based on the user's office task.",
                "For complex requests, break the work into steps and execute them one by one.",
                "After each tool call, summarize the result clearly and decide the next useful action.",
                "Use the same language as the user when you present conclusions. If the user wrote in Chinese, use Simplified Chinese.",
                "When the task is complete, provide a concise final answer to the user first, then call the terminate tool if needed.",
                "For internship or job search tasks, reject irrelevant results such as blogs, news, navigation sites, or stale result pages."
        ));
        this.setMaxSteps(maxSteps);
        this.setMaxObservationLength(maxObservationLength);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
