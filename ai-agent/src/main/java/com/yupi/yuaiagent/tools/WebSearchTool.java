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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Year;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    private static final List<String> JOB_INTENT_KEYWORDS = List.of(
            "实习", "实习生", "暑期实习", "岗位", "招聘", "校招", "intern", "internship", "job", "hiring", "career"
    );
    private static final List<String> JOB_REQUIRED_KEYWORDS = List.of(
            "实习", "实习生", "暑期", "intern", "internship", "hiring", "career", "job", "招聘", "岗位", "校招"
    );
    private static final List<String> DIRECTION_KEYWORDS = List.of(
            "ai agent", "agent", "大模型", "llm", "rag", "tool calling", "多智能体", "aigc", "生成式 ai", "nlp", "机器学习"
    );
    private static final List<String> EXCLUDED_KEYWORDS = List.of(
            "工具导航", "工具合集", "博客", "blog", "新闻", "paper", "论文", "课程", "google", "github 2024", "导航"
    );

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
                String searxngResult = isJobIntent(query) ? searchJobsWithSearxng(query) : searchWithSearxng(query);
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
            List<SearchResult> results = searchSearxngResults(query);
            if (results.isEmpty()) {
                return "SearXNG 未搜索到相关结果。";
            }
            return formatSearchResults(query, "general", "已返回网页搜索结果。", results);
        } catch (Exception e) {
            return "";
        }
    }

    private String searchJobsWithSearxng(String query) {
        List<SearchResult> mergedResults = new java.util.ArrayList<>();
        for (String expandedQuery : buildJobQueries(query)) {
            mergedResults.addAll(searchSearxngResults(expandedQuery));
            if (mergedResults.size() >= searxngTopK * 3) {
                break;
            }
        }
        List<SearchResult> filteredResults = rankAndFilterJobResults(mergedResults);
        if (filteredResults.isEmpty()) {
            return JSONUtil.createObj()
                    .set("type", "web_search_results")
                    .set("intent", "job_search")
                    .set("query", query)
                    .set("summary", "未找到足够相关的 AI Agent 暑期实习岗位。当前搜索结果相关性不足，建议改用公司官网、牛客、实习僧、Boss 直聘继续搜索。")
                    .set("results", new JSONArray())
                    .toString();
        }
        return formatSearchResults(query, "job_search", "已优先筛选岗位 / 实习类结果。", filteredResults);
    }

    private List<SearchResult> searchSearxngResults(String query) {
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
                return List.of();
            }
            JSONObject jsonObject = JSONUtil.parseObj(response.body());
            JSONArray results = jsonObject.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return List.of();
            }
            List<SearchResult> items = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(results.size(), searxngTopK); i++) {
                items.add(toSearchResult(results.getJSONObject(i)));
            }
            return items;
        } catch (Exception e) {
            return List.of();
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
                List<SearchResult> results = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(searchResults.size(), topK); i++) {
                    JSONObject item = searchResults.getJSONObject(i);
                    results.add(new SearchResult(
                            sanitize(item.getStr("title")),
                            sanitize(item.getStr("link")),
                            sanitize(StrUtil.blankToDefault(item.getStr("snippet"), item.getStr("content"))),
                            "OpenSearch",
                            "通用检索结果"
                    ));
                }
                return formatSearchResults(query, "general", "已返回网页搜索结果。", results);
            }
        } catch (Exception e) {
            return "阿里云 OpenSearch 搜索异常：" + e.getMessage();
        }
    }

    private SearchResult toSearchResult(JSONObject item) {
        return new SearchResult(
                sanitize(item.getStr("title")),
                sanitize(item.getStr("url")),
                sanitize(item.getStr("content")),
                sanitize(item.getStr("engine")),
                ""
        );
    }

    private boolean isJobIntent(String query) {
        String normalized = StrUtil.blankToDefault(query, "").toLowerCase();
        return JOB_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private List<String> buildJobQueries(String query) {
        int currentYear = Year.now().getValue();
        int nextYear = currentYear + 1;
        String normalized = StrUtil.blankToDefault(query, "").trim();
        Set<String> queries = new LinkedHashSet<>();
        queries.add(normalized + " " + currentYear);
        queries.add(normalized + " " + nextYear);
        queries.add("AI Agent 实习生 暑期 招聘 " + currentYear);
        queries.add("大模型 Agent 实习生 暑期招聘 " + currentYear);
        queries.add("LLM Agent 实习生 RAG Tool Calling 招聘 " + currentYear);
        queries.add("AI Agent intern summer " + currentYear + " China");
        queries.add("大模型应用开发 实习生 Agent RAG");
        if (containsAny(normalized, "字节", "bytedance")) {
            queries.add("site:jobs.bytedance.com AI Agent 实习生");
        }
        if (containsAny(normalized, "阿里", "alibaba")) {
            queries.add("site:campus.alibaba.com 大模型 Agent 实习");
        }
        if (containsAny(normalized, "腾讯", "tencent")) {
            queries.add("site:careers.tencent.com 大模型 实习生 Agent");
        }
        queries.add("site:app.mokahr.com AI Agent 实习生");
        queries.add("site:nowcoder.com AI Agent 实习");
        queries.add("site:shixiseng.com AI Agent 实习");
        return queries.stream().limit(6).toList();
    }

    private List<SearchResult> rankAndFilterJobResults(List<SearchResult> results) {
        Map<String, SearchResult> deduplicated = new LinkedHashMap<>();
        for (SearchResult result : results) {
            if (StrUtil.isBlank(result.link()) || StrUtil.isBlank(result.title())) {
                continue;
            }
            String haystack = (result.title() + " " + result.snippet() + " " + result.link()).toLowerCase();
            if (!containsAny(haystack, JOB_REQUIRED_KEYWORDS.toArray(String[]::new))) {
                continue;
            }
            if (!containsAny(haystack, DIRECTION_KEYWORDS.toArray(String[]::new))) {
                continue;
            }
            if (containsAny(haystack, EXCLUDED_KEYWORDS.toArray(String[]::new))) {
                continue;
            }
            if (haystack.contains("2024") && !haystack.contains(String.valueOf(Year.now().getValue()))) {
                continue;
            }
            SearchResult scored = new SearchResult(
                    result.title(),
                    result.link(),
                    truncateSnippet(result.snippet()),
                    result.source(),
                    explainRelevance(result.link(), haystack)
            );
            deduplicated.putIfAbsent(result.link(), scored);
        }
        return deduplicated.values().stream()
                .sorted((left, right) -> Integer.compare(score(right), score(left)))
                .limit(Math.max(3, Math.min(searxngTopK, 5)))
                .toList();
    }

    private int score(SearchResult result) {
        String haystack = (result.title() + " " + result.link() + " " + result.snippet()).toLowerCase();
        int score = 0;
        if (haystack.contains("jobs.") || haystack.contains("careers.") || haystack.contains("campus.")) {
            score += 40;
        }
        if (haystack.contains("mokahr") || haystack.contains("nowcoder") || haystack.contains("shixiseng")
                || haystack.contains("zhipin") || haystack.contains("liepin")) {
            score += 25;
        }
        if (containsAny(haystack, "ai agent", "llm", "rag", "大模型", "agent")) {
            score += 20;
        }
        if (containsAny(haystack, "intern", "实习", "岗位", "招聘")) {
            score += 15;
        }
        return score;
    }

    private String explainRelevance(String link, String haystack) {
        if (link.contains("jobs.") || link.contains("careers.") || link.contains("campus.")) {
            return "官方招聘页";
        }
        if (containsAny(haystack, "nowcoder", "shixiseng", "zhipin", "liepin", "mokahr")) {
            return "招聘平台岗位页";
        }
        return "岗位相关搜索结果";
    }

    private String formatSearchResults(String query, String intent, String summary, List<SearchResult> results) {
        JSONArray items = new JSONArray();
        for (SearchResult result : results) {
            items.add(JSONUtil.createObj()
                    .set("title", result.title())
                    .set("link", result.link())
                    .set("snippet", truncateSnippet(result.snippet()))
                    .set("source", StrUtil.blankToDefault(result.source(), "web"))
                    .set("relevance", result.relevance()));
        }
        return JSONUtil.createObj()
                .set("type", "web_search_results")
                .set("intent", intent)
                .set("query", query)
                .set("summary", summary)
                .set("results", items)
                .toString();
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = StrUtil.blankToDefault(text, "").toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String sanitize(String value) {
        return StrUtil.blankToDefault(value, "").replaceAll("\\s+", " ").trim();
    }

    private String truncateSnippet(String snippet) {
        String normalized = sanitize(snippet);
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180) + "...";
    }

    private String removeTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private record SearchResult(
            String title,
            String link,
            String snippet,
            String source,
            String relevance
    ) {
    }
}
