# Office AI Agent

面向办公场景的 AI Agent 演示项目，技术栈为 Spring Boot + Spring AI + Vue 3，覆盖 RAG 知识库、流式对话、多工具 Agent、前后端联调和 Docker 部署。

## 项目结构

- `ai-agent/`：后端主工程，包含 Spring Boot 服务、RAG、Agent、工具实现和测试。
- `ai-agent/office-ai-assistant-frontend/`：Vue 3 前端，包含办公助手、任务助手、知识库页面。
- `ai-agent/src/main/resources/document/`：内置知识文档。
- `deploy/`：Docker Compose、部署环境变量示例和前后端容器启动配置。

## 核心能力

- RAG 知识库：支持 `.md` / `.txt` 上传，按 chunk 建索引，启动时可重建，删除时按 chunk 全量清理。
- 办公助手：支持普通聊天和 RAG 增强问答，SSE 流式返回更稳定。
- 任务助手：支持工具调用、步骤轨迹、结构化 SSE 事件、结果截断和错误友好展示。
- 工具安全：默认关闭终端工具，限制工作目录、危险命令、内网地址、下载体积和抓取响应大小。

## 本地启动

后端：

```bash
cd ai-agent
cp .env.example .env
./mvnw spring-boot:run
```

前端：

```bash
cd ai-agent/office-ai-assistant-frontend
npm install
npm run dev
```

默认访问：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8123/api`

## Docker 启动

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
```

默认暴露：

- 前端：`http://localhost:${FRONTEND_PORT:-5021}`
- SearXNG：`http://localhost:${SEARXNG_PORT:-8080}`

## 关键配置

所有密钥都从环境变量读取，不要提交真实值。

- `DASHSCOPE_API_KEY`：DashScope API Key
- `SPRING_AI_DASHSCOPE_CHAT_OPTIONS_MODEL`：对话模型名，默认 `qwen-plus`
- `OFFICE_KNOWLEDGE_DIR`：知识库文件目录
- `OFFICE_KNOWLEDGE_CHUNK_SIZE`：chunk 大小，默认 `1000`
- `OFFICE_KNOWLEDGE_CHUNK_OVERLAP`：chunk overlap，默认 `150`
- `OFFICE_KNOWLEDGE_REBUILD_ON_STARTUP`：启动时重建知识索引，默认 `true`
- `OFFICE_ENABLE_TERMINAL_TOOL`：是否启用终端工具，默认 `false`
- `OFFICE_AGENT_MAX_STEPS`：Agent 最大步数，默认 `20`
- `OFFICE_AGENT_MAX_OBSERVATION_LENGTH`：工具观察结果最大长度，默认 `4000`
- `OFFICE_CHAT_MAX_MESSAGE_LENGTH`：聊天输入最大长度，默认 `8000`
- `OFFICE_TOOLS_WORKSPACE_DIR`：工具可访问工作目录

## RAG 流程

1. 上传知识文件后保存到 `OFFICE_KNOWLEDGE_DIR`
2. `KnowledgeDocumentSplitter` 按 Markdown 标题 / 段落或纯文本段落切分
3. 每个 chunk 生成独立 `Document` 和完整 metadata
4. `KnowledgeIndexService` 写入向量库，并维护 `fileId -> chunkIds` 本地索引元数据
5. 删除文件时按全部 chunk id 删除，避免索引残留
6. 使用 `SimpleVectorStore` 时，应用启动后自动扫描知识目录并重建索引

## Agent 工具调用流程

1. 前端通过结构化 SSE 连接 `/ai/office-agent/chat/stream-with-steps`
2. `OfficeAgent` 每次请求创建独立实例，避免多用户状态串号
3. `ToolCallAgent` 记录 THINK / ACT / OBSERVATION / FINAL / ERROR 轨迹
4. 工具结果统一截断和脱敏，再进入上下文和日志
5. 前端分别展示最终答案和执行过程，便于演示可观察性

## 安全说明

- 终端工具默认关闭
- 文件、下载、PDF 输出都限制在 workspace 目录
- 禁止 `rm -rf`、`shutdown`、`reboot`、`env`、敏感路径访问和危险重定向
- 禁止访问 `localhost`、`127.0.0.1` 和常见内网地址
- 网页抓取设置超时和最大响应体，避免把超长网页全文喂给模型

## 面试亮点

- 把演示型 RAG 改造成“文件存储、chunk 索引、删除一致性、启动重建”完整闭环
- 给 Agent 增加结构化执行轨迹和 SSE 事件，前端可直接展示中间步骤
- 对高风险工具做最小权限和输出截断设计，体现工程化安全意识
- 用 Docker Compose 串起前端、后端和 SearXNG，方便本地演示和线上部署

更多运行细节见 [ai-agent/README.md](./ai-agent/README.md)。
