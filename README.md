# Office AI Agent

Office AI Agent 是一个面向办公场景的 AI 助手项目，基于 Spring Boot + Vue 3 构建，集成大模型对话、RAG 知识库检索、文件知识管理和工具调用能力。项目目标是把常见办公流程中的问答、资料检索、内容生成和简单任务处理集中到一个网页端助手中，提高信息获取和办公处理效率。

## 项目简介

该项目包含完整的前后端实现和 Docker 部署配置。后端负责 AI 对话编排、RAG 检索、知识文件管理和工具调用；前端提供聊天、任务助手和知识库管理页面；部署层通过 Docker Compose 组织前端 nginx 服务和后端服务。

适合展示的能力点：

- 大模型应用开发：接入 DashScope，通过 Spring AI 组织对话、RAG 和流式输出。
- RAG 知识库：支持内置知识文档和用户上传知识文件，用于办公资料问答。
- 工具调用 Agent：集成搜索、网页抓取、文件操作、PDF 生成等工具能力。
- 前后端联调：Vue 3 前端通过 REST API 和 SSE 与后端交互。
- 工程化部署：提供 Dockerfile、nginx 反向代理和 Docker Compose 部署文件。

## 核心功能

### AI 办公助手

- 支持办公场景下的自然语言问答。
- 使用 SSE 实现流式响应，提升长文本生成时的交互体验。
- 可结合知识库内容回答问题，减少纯模型回答的不确定性。

### 知识库管理

- 支持上传 `.md` 和 `.txt` 文件作为知识资料。
- 支持查看和删除已上传的知识文件。
- 上传文件会持久化保存，便于后续检索和问答。

### 任务助手

- 支持通过工具调用完成更复杂的任务。
- 已集成网页搜索、网页内容抓取、文件操作和 PDF 生成能力。
- 终端工具默认关闭，避免公开部署时产生安全风险。

### 前端页面

- 首页入口清晰区分聊天、任务助手和知识库管理。
- 聊天页面支持流式输出展示。
- 知识库页面支持文件上传、列表查看和删除操作。

## 技术栈

后端：

- Java 21
- Spring Boot 3.4
- Spring AI
- Spring AI Alibaba / DashScope
- Maven

前端：

- Vue 3
- Vite
- Vue Router
- axios
- Server-Sent Events

部署：

- Docker
- Docker Compose
- nginx

## 目录

- `ai-agent/`：项目主体，包含 Spring Boot 后端和 Vue 3 前端源码。
- `ai-agent/office-ai-assistant-frontend/`：前端项目。
- `ai-agent/src/main/java/`：后端核心代码。
- `ai-agent/src/main/resources/document/`：内置知识库文档。
- `deploy/`：Docker Compose 部署文件和环境变量示例。

## 项目亮点

- 使用 Spring AI 封装模型调用和知识库能力，代码结构清晰，便于扩展其他模型或检索方式。
- 将普通聊天、RAG 问答和工具调用 Agent 分成不同入口，方便按办公场景选择能力。
- 知识库文件管理做了上传限制、路径处理和持久化配置，避免简单 Demo 中常见的文件安全问题。
- 前后端通过统一 API 交互，前端使用 SSE 展示流式生成结果，体验更接近真实 AI 产品。
- 提供生产环境配置示例，默认关闭高风险终端工具，更适合部署到公网环境。

## 本地运行

后端需要 Java 21，前端需要 Node.js。

后端：

```bash
cd ai-agent
./mvnw spring-boot:run
```

前端：

```bash
cd ai-agent/office-ai-assistant-frontend
npm install
npm run dev
```

访问：

```text
http://localhost:3000
```

## 环境变量

不要提交真实密钥。可以参考示例文件：

```bash
cp ai-agent/.env.example ai-agent/.env
```

常用变量：

```bash
DASHSCOPE_API_KEY=your-dashscope-api-key
OFFICE_KNOWLEDGE_DIR=./tmp/knowledge
OFFICE_ENABLE_TERMINAL_TOOL=false
```

## Docker 部署

部署文件位于 `deploy/` 目录：

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
```

部署后由前端 nginx 服务统一暴露入口，并将 `/api` 请求反向代理到后端服务。

## 项目总结

该项目覆盖了 AI 应用开发中的模型接入、RAG 检索、流式交互、工具调用、前后端联调和容器化部署等关键环节：

> 基于 Spring Boot、Spring AI 和 Vue 3 开发办公 AI 助手系统，实现大模型流式对话、RAG 知识库问答、知识文件管理和工具调用 Agent，并使用 Docker Compose 完成前后端容器化部署。
