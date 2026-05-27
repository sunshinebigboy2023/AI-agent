package com.yupi.yuaiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 资源下载工具
 */
public class ResourceDownloadTool {

    private final ToolExecutionSupport toolExecutionSupport;
    private final long maxDownloadBytes;

    public ResourceDownloadTool(ToolExecutionSupport toolExecutionSupport, long maxDownloadBytes) {
        this.toolExecutionSupport = toolExecutionSupport;
        this.maxDownloadBytes = maxDownloadBytes;
    }

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url, @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {
        return toolExecutionSupport.execute("ResourceDownloadTool.downloadResource", url + " -> " + fileName, () -> {
            toolExecutionSupport.validateExternalUrl(url);
            Path filePath = toolExecutionSupport.resolveWorkspacePath(fileName, "download");
            Files.createDirectories(filePath.getParent());
            try (HttpResponse response = HttpRequest.get(url).timeout(15000).execute()) {
                byte[] bytes = response.bodyBytes();
                if (bytes.length > maxDownloadBytes) {
                    throw new IllegalArgumentException("下载文件过大，已拒绝");
                }
                Files.write(filePath, bytes);
            }
            return "Resource downloaded successfully to: " + filePath;
        });
    }
}
