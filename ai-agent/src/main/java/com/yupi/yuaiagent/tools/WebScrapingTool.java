package com.yupi.yuaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {

    private final ToolExecutionSupport toolExecutionSupport;
    private final int timeoutMs;
    private final int maxBodySize;

    public WebScrapingTool(ToolExecutionSupport toolExecutionSupport, int timeoutMs, int maxBodySize) {
        this.toolExecutionSupport = toolExecutionSupport;
        this.timeoutMs = timeoutMs;
        this.maxBodySize = maxBodySize;
    }

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        return toolExecutionSupport.execute("WebScrapingTool.scrapeWebPage", url, () -> {
            toolExecutionSupport.validateExternalUrl(url);
            Document document = Jsoup.connect(url)
                    .timeout(timeoutMs)
                    .maxBodySize(maxBodySize)
                    .ignoreContentType(false)
                    .get();
            return document.body() == null ? document.title() : document.body().text();
        });
    }
}
