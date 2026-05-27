package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("Integration test downloads an external resource.")
class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool(
                new ToolExecutionSupport(Path.of("target/test-workspace"), 4000),
                1024 * 1024
        );
        String result = resourceDownloadTool.downloadResource("https://example.com/logo.png", "office-logo.png");
        Assertions.assertNotNull(result);
    }
}
