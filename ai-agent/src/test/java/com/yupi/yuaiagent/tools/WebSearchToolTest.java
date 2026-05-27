package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

@Disabled("Integration test requires valid Alibaba Cloud OpenSearch configuration.")
@SpringBootTest
class WebSearchToolTest {

    @Value("${aliyun.opensearch.host:}")
    private String host;

    @Value("${aliyun.opensearch.workspace:default}")
    private String workspace;

    @Value("${aliyun.opensearch.service-id:ops-web-search-001}")
    private String serviceId;

    @Value("${aliyun.opensearch.api-key:}")
    private String apiKey;

    @Test
    void searchWeb() {
        WebSearchTool webSearchTool = new WebSearchTool(
                host, workspace, serviceId, apiKey, 5, "snippet", "", 5,
                new ToolExecutionSupport(Path.of("target/test-workspace"), 4000)
        );
        String result = webSearchTool.searchWeb("AI office assistant meeting summary examples");
        Assertions.assertNotNull(result);
    }
}
