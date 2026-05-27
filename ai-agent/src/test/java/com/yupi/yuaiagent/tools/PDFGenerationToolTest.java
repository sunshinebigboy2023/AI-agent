package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("Integration test writes generated PDF files.")
class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(
                new ToolExecutionSupport(Path.of("target/test-workspace"), 4000)
        );
        String result = pdfGenerationTool.generatePDF("office-report.pdf", "AI office assistant report");
        Assertions.assertNotNull(result);
    }
}
