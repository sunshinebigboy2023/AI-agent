# AI Office Assistant

这是一个 Spring Boot + Vue 3 的办公 AI 助手项目，主要用于办公问答、知识库检索和简单任务执行。

## 核心功能

- AI 办公助手对话，支持 SSE 流式返回
- RAG 知识库问答
- 上传、查看、删除 `.md` / `.txt` 知识文件
- 任务助手工具调用：搜索、网页抓取、文件操作、PDF 生成
- Vue 3 前端页面：聊天、任务助手、知识库管理

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring AI / DashScope
- Vue 3 / Vite
- Docker / Docker Compose

## 配置

不要提交真实密钥。复制示例文件后填写自己的环境变量：

```bash
cp .env.example .env
```

主要变量：

```bash
DASHSCOPE_API_KEY=your-dashscope-api-key
OFFICE_KNOWLEDGE_DIR=./tmp/knowledge
OFFICE_ENABLE_TERMINAL_TOOL=false
```

## 本地运行

后端：

```bash
./mvnw spring-boot:run
```

前端：

```bash
cd office-ai-assistant-frontend
npm install
npm run dev
```

访问：

```text
http://localhost:3000
```

## 部署

部署文件在仓库根目录的 `deploy/` 下：

```bash
cd ../deploy
cp .env.example .env
docker compose up -d --build
```

公开部署时保持：

```bash
OFFICE_ENABLE_TERMINAL_TOOL=false
```
