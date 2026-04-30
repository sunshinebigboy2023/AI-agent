# Git And RAG MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Turn the copied office AI assistant into a safer Git-ready project and add a usable web-managed RAG knowledge base MVP.

**Architecture:** Keep the existing Spring Boot + Vue split. Add a backend knowledge-base service that stores uploaded files under `tmp/knowledge`, converts supported text files to Spring AI `Document` objects, indexes them into the existing `VectorStore`, and exposes upload/list/delete endpoints. Add a frontend knowledge page and switch the office chat endpoint to use RAG by default.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring AI `VectorStore`, Vue 3, Vite, axios.

---

### Task 1: Git Project Hygiene

**Files:**
- Modify: `.gitignore`
- Modify: `ai-agent/.gitignore`
- Modify: `ai-agent/README.md`
- Modify: `ai-agent/src/main/resources/application.yml`
- Modify: `start.bat`
- Modify: `stop.bat`

**Steps:**
1. Remove committed-secret risk by reading API keys from environment variables.
2. Ignore generated folders and local-only config.
3. Rewrite README with accurate startup, environment, and feature notes.
4. Rewrite start/stop scripts with ASCII output to avoid terminal encoding issues.
5. Verify backend compile and frontend build still pass.

### Task 2: Knowledge Service

**Files:**
- Create: `ai-agent/src/main/java/com/yupi/yuaiagent/rag/KnowledgeFileInfo.java`
- Create: `ai-agent/src/main/java/com/yupi/yuaiagent/rag/KnowledgeFileService.java`
- Create: `ai-agent/src/test/java/com/yupi/yuaiagent/rag/KnowledgeFileServiceTest.java`

**Steps:**
1. Write tests for upload/list/delete behavior using a fake `VectorStore`.
2. Run the targeted test and confirm it fails because the service does not exist.
3. Implement the minimal service.
4. Run the targeted test and confirm it passes.

### Task 3: Knowledge API And RAG Chat

**Files:**
- Create: `ai-agent/src/main/java/com/yupi/yuaiagent/controller/KnowledgeController.java`
- Modify: `ai-agent/src/main/java/com/yupi/yuaiagent/controller/AiController.java`
- Modify: `ai-agent/src/main/java/com/yupi/yuaiagent/app/OfficeAssistantApp.java`

**Steps:**
1. Add upload/list/delete endpoints.
2. Add an SSE RAG chat endpoint.
3. Route the existing office chat SSE endpoint through RAG so the default web assistant uses the knowledge base.
4. Verify compilation.

### Task 4: Frontend Knowledge Page

**Files:**
- Modify: `ai-agent/office-ai-assistant-frontend/src/api/index.js`
- Modify: `ai-agent/office-ai-assistant-frontend/src/router/index.js`
- Modify: `ai-agent/office-ai-assistant-frontend/src/views/Home.vue`
- Modify: `ai-agent/office-ai-assistant-frontend/src/views/OfficeAssistant.vue`
- Create: `ai-agent/office-ai-assistant-frontend/src/views/KnowledgeBase.vue`

**Steps:**
1. Add upload/list/delete API wrappers.
2. Add a knowledge base route and home entry.
3. Add upload UI, file table, status messages, and delete action.
4. Label the assistant page as knowledge-base enabled.
5. Verify frontend build.

### Task 5: Final Verification

**Steps:**
1. Run backend targeted tests.
2. Run backend compile.
3. Run frontend build.
4. Run npm audit after safe dependency update.
5. Summarize residual risks.
