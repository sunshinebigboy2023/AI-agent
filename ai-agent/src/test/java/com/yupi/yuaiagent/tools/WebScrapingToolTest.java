package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("Integration test depends on external web access.")
class WebScrapingToolTest {

    @Test
    void scrapeWebPage() {
        WebScrapingTool webScrapingTool = new WebScrapingTool(
                new ToolExecutionSupport(Path.of("target/test-workspace"), 4000),
                10000,
                200000
        );
        String result = webScrapingTool.scrapeWebPage("https://example.com");
        Assertions.assertNotNull(result);
    }
}
