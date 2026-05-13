# AI Office Assistant

AI Office Assistant is a Spring Boot + Vue 3 project for office knowledge Q&A, writing assistance, and tool-driven task execution. It is designed as a clean base for future features such as document summarization, meeting notes, weekly reports, internal FAQ search, and workflow automation.

## Features

- Office assistant chat with streaming SSE responses
- RAG knowledge base backed by Spring AI `VectorStore`
- Web knowledge-base management for uploading, listing, and deleting `.md` / `.txt` files
- Built-in office FAQ documents under `src/main/resources/document`
- Task agent with tool calling for search, scraping, file operations, and PDF generation
- Vue 3 frontend with pages for chat, task agent, and knowledge management

## Tech Stack

Backend:
- Java 21
- Spring Boot 3.4
- Spring AI
- Spring AI Alibaba / DashScope
- PGVector support
- Knife4j / OpenAPI

Frontend:
- Vue 3
- Vite
- Vue Router
- axios
- Server-Sent Events

## Project Structure

```text
.
├── src/main/java/com/yupi/yuaiagent
│   ├── agent        # Tool-calling task agent
│   ├── app          # Office assistant orchestration
│   ├── controller   # REST and SSE APIs
│   ├── rag          # Knowledge loading, upload, and vector indexing
│   └── tools        # Search, scraping, file, terminal, PDF tools
├── src/main/resources
│   ├── application.yml
│   └── document     # Built-in knowledge base markdown files
├── office-ai-assistant-frontend
│   └── src          # Vue frontend
└── yu-image-search-mcp-server
    └── src          # Example MCP image search server
```

> The Java package is still `com.yupi.yuaiagent` for compatibility with the copied base project. Rename it later if you want full personal branding.

## Configuration

Do not commit real API keys. Configure secrets through environment variables:

```bash
DASHSCOPE_API_KEY=your-dashscope-api-key
SEARXNG_BASE_URL=http://localhost:8080
ALIYUN_OPENSEARCH_HOST=https://your-region.opensearch.aliyuncs.com
ALIYUN_OPENSEARCH_API_KEY=your-opensearch-api-key
ALIYUN_OPENSEARCH_WORKSPACE=default
ALIYUN_OPENSEARCH_SERVICE_ID=ops-web-search-001
OFFICE_KNOWLEDGE_DIR=./tmp/knowledge
OFFICE_ALLOWED_ORIGINS=http://localhost:3000
OFFICE_ENABLE_TERMINAL_TOOL=false
```

For local development, you may also create an ignored file:

```text
src/main/resources/application-local.yml
```

Example:

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
aliyun:
  opensearch:
    host: https://your-region.opensearch.aliyuncs.com
    api-key: your-opensearch-api-key
    workspace: default
    service-id: ops-web-search-001
searxng:
  base-url: http://localhost:8080
```

## Run Locally

Backend:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Backend URLs:

```text
http://localhost:8123/api
http://localhost:8123/api/health
http://localhost:8123/api/swagger-ui.html
```

Frontend:

```bash
cd office-ai-assistant-frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:3000/index.html
```

## Server Deployment

Server deployment files are centralized under the repository root `deploy/` directory. The frontend container serves static assets through nginx and proxies `/api` to the backend container. Uploaded knowledge files are mounted at `./ai-agent/tmp/knowledge`.

For public deployment, keep `OFFICE_ENABLE_TERMINAL_TOOL=false`. The terminal tool is intentionally disabled by default because it can execute host commands and is not appropriate for an unauthenticated public service.

## Main APIs

- `GET /api/ai/office_app/chat/sse` - Office assistant streaming chat with RAG
- `GET /api/ai/office_app/chat/rag/sse` - Explicit RAG streaming chat endpoint
- `GET /api/ai/office_app/chat/sync` - Synchronous office chat
- `GET /api/ai/office-agent/chat` - Tool-calling task agent
- `GET /api/knowledge/files` - List uploaded knowledge files
- `POST /api/knowledge/files` - Upload `.md` or `.txt` knowledge file
- `DELETE /api/knowledge/files/{fileId}` - Delete uploaded knowledge file

## Git Hygiene

Generated and local files are ignored:

- `target/`
- `node_modules/`
- `dist/`
- `logs/`
- `tmp/`
- `.env*`
- `application-local.yml`

Before pushing to GitHub, check that no secrets are present:

```bash
rg "api-key|secret|password|sk-" -g "!target/**" -g "!node_modules/**" -g "!dist/**" -g "!logs/**"
```

## Development Notes

- Uploaded knowledge files are stored under `tmp/knowledge` by default.
- Current upload MVP supports UTF-8 `.md` and `.txt` files.
- PGVector configuration exists under the `pgvector` profile, but the default local setup uses `SimpleVectorStore`.
- The task agent can include a terminal tool only when `OFFICE_ENABLE_TERMINAL_TOOL=true`. Keep it local-only or add authentication and command restrictions before public deployment.
