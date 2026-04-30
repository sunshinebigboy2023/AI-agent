package com.yupi.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Disabled("Integration tests require a configured model API key and external services.")
@SpringBootTest
class OfficeAssistantAppTest {

    @Resource
    private OfficeAssistantApp officeAssistantApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        String answer = officeAssistantApp.doChat("帮我写一封项目延期说明邮件。", chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        OfficeAssistantApp.OfficeReport officeReport = officeAssistantApp.doChatWithReport(
                "请根据今天的项目讨论内容生成一份简短会议纪要。",
                chatId
        );
        Assertions.assertNotNull(officeReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String answer = officeAssistantApp.doChatWithRag("公司的报销流程是什么？", chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String answer = officeAssistantApp.doChatWithTools("搜索最近的 AI 办公趋势，并整理成三条要点。", chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String answer = officeAssistantApp.doChatWithMcp("搜索适合汇报 PPT 封面的科技办公图片。", chatId);
        Assertions.assertNotNull(answer);
    }
}
