package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.rag.KnowledgeFileInfo;
import com.yupi.yuaiagent.rag.KnowledgeFileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/knowledge/files")
public class KnowledgeController {

    private final KnowledgeFileService knowledgeFileService;

    public KnowledgeController(KnowledgeFileService knowledgeFileService) {
        this.knowledgeFileService = knowledgeFileService;
    }

    @GetMapping
    public List<KnowledgeFileInfo> listFiles(@RequestHeader(value = "X-Client-Id", required = false) String clientId) throws IOException {
        return knowledgeFileService.listFiles(clientId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeFileInfo uploadFile(
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return knowledgeFileService.upload(clientId, file);
    }

    @DeleteMapping("/{fileId}")
    public void deleteFile(
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @PathVariable String fileId
    ) throws IOException {
        knowledgeFileService.delete(clientId, fileId);
    }
}
