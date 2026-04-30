package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.rag.KnowledgeFileInfo;
import com.yupi.yuaiagent.rag.KnowledgeFileService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
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

    @Resource
    private KnowledgeFileService knowledgeFileService;

    @GetMapping
    public List<KnowledgeFileInfo> listFiles() throws IOException {
        return knowledgeFileService.listFiles();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeFileInfo uploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        return knowledgeFileService.upload(file);
    }

    @DeleteMapping("/{fileId}")
    public void deleteFile(@PathVariable String fileId) throws IOException {
        knowledgeFileService.delete(fileId);
    }
}
