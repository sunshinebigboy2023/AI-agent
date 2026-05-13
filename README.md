# Office AI Agent

一个面向办公场景的 AI 助手项目，支持智能问答、知识库检索、文件知识管理和任务执行。

## 项目简介

- 后端：Spring Boot、Spring AI、DashScope
- 前端：Vue 3、Vite、axios
- 能力：SSE 流式对话、RAG 知识库、Markdown/TXT 文件上传、搜索/网页抓取/PDF 等工具调用

## 目录

- `ai-agent/`：项目主体，包含后端和前端源码
- `deploy/`：Docker Compose 部署文件

## 本地运行

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

详细配置见 [ai-agent/README.md](./ai-agent/README.md)。
