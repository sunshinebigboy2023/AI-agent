package com.yupi.yuaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Integration test requires a configured model API key and tool environment.")
@SpringBootTest
class OfficeAgentTest {

    @Resource
    private OfficeAgent officeAgent;

    @Test
    void run() {
        String userPrompt = "请调研 AI 办公助手的常见功能，并生成一份 PDF 形式的产品需求草稿。";
        String answer = officeAgent.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
