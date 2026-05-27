package com.yupi.yuaiagent.tools;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    private final ToolExecutionSupport toolExecutionSupport;

    public PDFGenerationTool(ToolExecutionSupport toolExecutionSupport) {
        this.toolExecutionSupport = toolExecutionSupport;
    }

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        return toolExecutionSupport.execute("PDFGenerationTool.generatePDF", fileName, () -> {
            Path filePath = toolExecutionSupport.resolveWorkspacePath(fileName, "pdf");
            Files.createDirectories(filePath.getParent());
            try (PdfWriter writer = new PdfWriter(filePath.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                Paragraph paragraph = new Paragraph(content);
                document.add(paragraph);
            }
            return "PDF generated successfully to: " + filePath;
        });
    }
}
