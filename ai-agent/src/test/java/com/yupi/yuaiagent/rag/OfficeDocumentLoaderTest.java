package com.yupi.yuaiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OfficeDocumentLoaderTest {

    @Test
    void loadMarkdowns() {
        OfficeDocumentLoader officeDocumentLoader = new OfficeDocumentLoader(new PathMatchingResourcePatternResolver());
        List<Document> documents = officeDocumentLoader.loadMarkdowns();

        assertNotNull(documents);
    }
}
