package com.yupi.yuaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    private final String host;
    private final String workspace;
    private final String serviceId;
    private final String apiKey;
    private final int topK;
    private final String contentType;
    private final String searxngBaseUrl;
    private final int searxngTopK;
    private final ToolExecutionSupport toolExecutionSupport;

    public WebSearchTool(
            String host,
            String workspace,
            String serviceId,
            String apiKey,
            int topK,
            String contentType,
            String searxngBaseUrl,
            int searxngTopK,
            ToolExecutionSupport toolExecutionSupport
    ) {
        this.host = removeTrailingSlash(host);
        this.workspace = StrUtil.blankToDefault(workspace, "default");
        this.serviceId = StrUtil.blankToDefault(serviceId, "ops-web-search-001");
        this.apiKey = apiKey;
        this.topK = Math.max(1, Math.min(topK, 10));
        this.contentType = StrUtil.blankToDefault(contentType, "snippet");
        this.searxngBaseUrl = removeTrailingSlash(searxngBaseUrl);
        this.searxngTopK = Math.max(1, Math.min(searxngTopK, 10));
        this.toolExecutionSupport = toolExecutionSupport;
    }

    @Tool(description = "Search the web by using self-hosted SearXNG first, with Alibaba Cloud OpenSearch as fallback")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        return toolExecutionSupport.execute("WebSearchTool.searchWeb", query, () -> {
            if (StrUtil.isNotBlank(searxngBaseUrl)) {
                String searxngResult = searchWithSearxng(query);
                if (StrUtil.isNotBlank(searxngResult)) {
                    return searxngResult;
                }
            }
            if (StrUtil.isBlank(host) || StrUtil.isBlank(apiKey)) {
                return "联网搜索未配置，请设置 SEARXNG_BASE_URL，或设置 ALIYUN_OPENSEARCH_HOST 和 ALIYUN_OPENSEARCH_API_KEY。";
            }
            if (host.contains("your-region.opensearch.aliyuncs.com")
                    || apiKey.contains("replace-with-your-opensearch-api-key")) {
                return "SearXNG 搜索不可用，阿里云 OpenSearch 仍是占位配置。请检查 SearXNG 服务或填写真实阿里云 OpenSearch 配置。";
            }
            return searchWithAliyunOpenSearch(query);
        });
    }

    private String searchWithSearxng(String query) {
        try {
            try (HttpResponse response = HttpRequest.get(searxngBaseUrl + "/search")
                    .header("X-Real-IP", "127.0.0.1")
                    .header("X-Forwarded-For", "127.0.0.1")
                    .header("User-Agent", "office-ai-agent/1.0")
                    .form(Map.of(
                            "q", query,
                            "format", "json"
                    ))
                    .timeout(30000)
                    .execute()) {
                if (response.getStatus() != HttpStatus.HTTP_OK) {
                    return "";
                }
                JSONObject jsonObject = JSONUtil.parseObj(response.body());
                JSONArray results = jsonObject.getJSONArray("results");
                if (results == null || results.isEmpty()) {
                    return "SearXNG 未搜索到相关结果。";
                }
                List<Object> objects = results.subList(0, Math.min(searxngTopK, results.size()));
                return objects.stream().map(obj -> {
                    JSONObject item = (JSONObject) obj;
                    return JSONUtil.createObj()
                            .set("title", item.getStr("title"))
                            .set("link", item.getStr("url"))
                            .set("snippet", item.getStr("content"))
                            .set("engine", item.getStr("engine"))
                            .toString();
                }).collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String searchWithAliyunOpenSearch(String query) {
        String url = String.format("%s/v3/openapi/workspaces/%s/web-search/%s", host, workspace, serviceId);
        JSONObject requestBody = JSONUtil.createObj()
                .set("query", query)
                .set("query_rewrite", true)
                .set("top_k", topK)
                .set("content_type", contentType);
        try {
            try (HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute()) {
                String body = response.body();
                JSONObject jsonObject = JSONUtil.parseObj(body);
                if (response.getStatus() != HttpStatus.HTTP_OK || jsonObject.containsKey("code")) {
                    return "阿里云 OpenSearch 搜索失败：" + jsonObject.getStr("message", body);
                }
                JSONObject result = jsonObject.getJSONObject("result");
                if (result == null) {
                    return "阿里云 OpenSearch 搜索返回为空。";
                }
                JSONArray searchResults = result.getJSONArray("search_result");
                if (searchResults == null || searchResults.isEmpty()) {
                    return "未搜索到相关结果。";
                }
                List<Object> objects = searchResults.subList(0, Math.min(topK, searchResults.size()));
                return objects.stream().map(obj -> {
                    JSONObject item = (JSONObject) obj;
                    return JSONUtil.createObj()
                            .set("title", item.getStr("title"))
                            .set("link", item.getStr("link"))
                            .set("snippet", item.getStr("snippet"))
                            .set("content", item.getStr("content"))
                            .toString();
                }).collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "阿里云 OpenSearch 搜索异常：" + e.getMessage();
        }
    }

    private String removeTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
