package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Integration test downloads an external resource.")
class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        String result = resourceDownloadTool.downloadResource("https://example.com/logo.png", "office-logo.png");
        Assertions.assertNotNull(result);
    }
}
