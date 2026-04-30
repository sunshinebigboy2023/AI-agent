# Office AI Assistant Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Turn the copied AI agent tutorial project into a clean AI office assistant foundation that can grow into document, meeting, report, and workflow features.

**Architecture:** Keep the existing Spring Boot + Spring AI backend and Vue + Vite frontend. Rename the public project identity and core application classes from the original tutorial/love-advisor framing into an office assistant framing, while preserving the existing SSE chat and tool-agent behavior for a safer first pass.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring AI, LangChain4j, Maven, Vue 3, Vite, axios, SSE.

---

### Task 1: Documentation And Product Identity

**Files:**
- Modify: `README.md`
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `Dockerfile`
- Modify: `yu-ai-agent-frontend/package.json`
- Modify: `yu-ai-agent-frontend/package-lock.json`
- Modify: `yu-ai-agent-frontend/index.html`

**Steps:**
1. Replace tutorial and author promotional README content with a concise AI office assistant README.
2. Rename Maven artifact and application metadata to `office-ai-assistant`.
3. Rename frontend package and page metadata to `office-ai-assistant-frontend`.
4. Update Docker jar name to match the Maven artifact.
5. Verify with text search that obvious source-brand references are removed from public docs and metadata.

### Task 2: Backend Office Assistant Renaming

**Files:**
- Rename/modify: `src/main/java/com/yupi/yuaiagent/app/LoveApp.java` to `OfficeAssistantApp.java`
- Rename/modify: `src/main/java/com/yupi/yuaiagent/agent/YuManus.java` to `OfficeAgent.java`
- Modify: `src/main/java/com/yupi/yuaiagent/controller/AiController.java`
- Modify RAG helper classes whose names contain `LoveApp`
- Modify related tests under `src/test/java/com/yupi/yuaiagent`

**Steps:**
1. Rename the business chat app to `OfficeAssistantApp`.
2. Replace the system prompt with office productivity behavior: writing, summaries, meeting notes, task breakdown, and professional communication.
3. Rename the autonomous agent from `YuManus` to `OfficeAgent`.
4. Keep existing endpoint paths for compatibility in this first pass, but rename Java methods and frontend API wrappers.
5. Update tests to compile against the new names.
6. Run Maven tests or, if model/API config blocks runtime tests, at least run Maven compile.

### Task 3: Frontend Workspace UI

**Files:**
- Modify: `yu-ai-agent-frontend/src/views/Home.vue`
- Modify: `yu-ai-agent-frontend/src/views/LoveMaster.vue`
- Modify: `yu-ai-agent-frontend/src/views/SuperAgent.vue`
- Modify: `yu-ai-agent-frontend/src/components/ChatRoom.vue`
- Modify: `yu-ai-agent-frontend/src/components/AiAvatarFallback.vue`
- Modify: `yu-ai-agent-frontend/src/components/AppFooter.vue`
- Modify: `yu-ai-agent-frontend/src/router/index.js`
- Modify: `yu-ai-agent-frontend/src/api/index.js`
- Modify: `yu-ai-agent-frontend/src/App.vue`

**Steps:**
1. Replace cyber demo homepage with a restrained office workspace landing screen.
2. Rename visible apps to `AI 办公助手` and `任务执行助手`.
3. Restyle chat surfaces with clean SaaS-like layout, stable dimensions, and responsive behavior.
4. Remove unused Vue starter import and promotional footer links.
5. Rename frontend API wrapper functions while preserving backend endpoint URLs for now.
6. Run `npm run build`.

### Task 4: Verification

**Files:**
- All modified files

**Steps:**
1. Run `rg` for `鱼皮`, `yupi`, `YuManus`, `LoveApp`, `恋爱大师`, and `yu-ai-agent`.
2. Decide which remaining occurrences are internal compatibility paths or stale build output.
3. Run backend build/compile.
4. Run frontend build.
5. Summarize changed files, verification results, and any remaining compatibility names.
