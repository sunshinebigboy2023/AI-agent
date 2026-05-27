package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("${aliyun.opensearch.host:}")
    private String aliyunOpenSearchHost;

    @Value("${aliyun.opensearch.workspace:default}")
    private String aliyunOpenSearchWorkspace;

    @Value("${aliyun.opensearch.service-id:ops-web-search-001}")
    private String aliyunOpenSearchServiceId;

    @Value("${aliyun.opensearch.api-key:}")
    private String aliyunOpenSearchApiKey;

    @Value("${aliyun.opensearch.top-k:5}")
    private int aliyunOpenSearchTopK;

    @Value("${aliyun.opensearch.content-type:snippet}")
    private String aliyunOpenSearchContentType;

    @Value("${searxng.base-url:}")
    private String searxngBaseUrl;

    @Value("${searxng.top-k:5}")
    private int searxngTopK;

    @Value("${office.agent.enable-terminal-tool:false}")
    private boolean enableTerminalTool;

    @Value("${office.agent.max-observation-length:4000}")
    private int maxObservationLength;

    @Value("${office.tools.workspace-dir:${user.dir}/tmp/workspace}")
    private String workspaceDir;

    @Value("${office.tools.download.max-bytes:10485760}")
    private long maxDownloadBytes;

    @Value("${office.tools.web-scraping.timeout-ms:10000}")
    private int webScrapingTimeoutMs;

    @Value("${office.tools.web-scraping.max-body-size:200000}")
    private int webScrapingMaxBodySize;

    @Bean
    public ToolCallback[] allTools() {
        ToolExecutionSupport toolExecutionSupport = new ToolExecutionSupport(
                Path.of(workspaceDir),
                maxObservationLength
        );
        FileOperationTool fileOperationTool = new FileOperationTool(toolExecutionSupport);
        WebSearchTool webSearchTool = new WebSearchTool(
                aliyunOpenSearchHost,
                aliyunOpenSearchWorkspace,
                aliyunOpenSearchServiceId,
                aliyunOpenSearchApiKey,
                aliyunOpenSearchTopK,
                aliyunOpenSearchContentType,
                searxngBaseUrl,
                searxngTopK,
                toolExecutionSupport
        );
        WebScrapingTool webScrapingTool = new WebScrapingTool(toolExecutionSupport, webScrapingTimeoutMs, webScrapingMaxBodySize);
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool(toolExecutionSupport, maxDownloadBytes);
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(toolExecutionSupport);
        TerminateTool terminateTool = new TerminateTool();
        List<Object> tools = new ArrayList<>();
        tools.add(fileOperationTool);
        tools.add(webSearchTool);
        tools.add(webScrapingTool);
        tools.add(resourceDownloadTool);
        tools.add(pdfGenerationTool);
        tools.add(terminateTool);
        if (enableTerminalTool) {
            tools.add(new TerminalOperationTool(toolExecutionSupport));
        }
        return ToolCallbacks.from(tools.toArray());
    }
}
