package com.sunshinebigboy.aiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "AI-agent原创项目.pdf";
        String content = "AI-agent原创项目 https://github.com/sunshinebigboy2023/AI-agent";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}