package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(
                aliyunOpenSearchHost,
                aliyunOpenSearchWorkspace,
                aliyunOpenSearchServiceId,
                aliyunOpenSearchApiKey,
                aliyunOpenSearchTopK,
                aliyunOpenSearchContentType,
                searxngBaseUrl,
                searxngTopK
        );
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        List<Object> tools = new ArrayList<>();
        tools.add(fileOperationTool);
        tools.add(webSearchTool);
        tools.add(webScrapingTool);
        tools.add(resourceDownloadTool);
        tools.add(pdfGenerationTool);
        tools.add(terminateTool);
        if (enableTerminalTool) {
            tools.add(new TerminalOperationTool());
        }
        return ToolCallbacks.from(tools.toArray());
    }
}
