package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件操作工具类（提供文件读写功能）
 */
public class FileOperationTool {

    private final ToolExecutionSupport toolExecutionSupport;

    public FileOperationTool(ToolExecutionSupport toolExecutionSupport) {
        this.toolExecutionSupport = toolExecutionSupport;
    }

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        return toolExecutionSupport.execute("FileOperationTool.readFile", fileName, () -> {
            Path filePath = toolExecutionSupport.resolveWorkspacePath(fileName, "file");
            return Files.readString(filePath, StandardCharsets.UTF_8);
        });
    }

    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content
    ) {
        return toolExecutionSupport.execute("FileOperationTool.writeFile", fileName, () -> {
            Path filePath = toolExecutionSupport.resolveWorkspacePath(fileName, "file");
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content == null ? "" : content, StandardCharsets.UTF_8);
            return "File written successfully to: " + filePath;
        });
    }
}
