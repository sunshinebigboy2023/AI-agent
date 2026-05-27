# AI Office Assistant

后端主工程，负责办公聊天、RAG 检索、知识文件索引、Agent 工具调用和 SSE 输出。

## 运行

```bash
cp .env.example .env
./mvnw spring-boot:run
```

默认接口前缀：`http://localhost:8123/api`

## 重点模块

- `rag/`：知识文件上传、chunk 切分、索引重建、RAG 检索
- `agent/`：ReAct / Tool Call Agent、步骤轨迹、结构化 SSE
- `tools/`：搜索、网页抓取、文件、下载、PDF、终端工具
- `controller/`：办公助手、知识库、健康检查接口

## 核心配置

- `DASHSCOPE_API_KEY`
- `SPRING_AI_DASHSCOPE_CHAT_OPTIONS_MODEL`
- `OFFICE_KNOWLEDGE_DIR`
- `OFFICE_KNOWLEDGE_CHUNK_SIZE`
- `OFFICE_KNOWLEDGE_CHUNK_OVERLAP`
- `OFFICE_KNOWLEDGE_REBUILD_ON_STARTUP`
- `OFFICE_ENABLE_TERMINAL_TOOL`
- `OFFICE_AGENT_MAX_STEPS`
- `OFFICE_AGENT_MAX_OBSERVATION_LENGTH`
- `OFFICE_CHAT_MAX_MESSAGE_LENGTH`
- `OFFICE_TOOLS_WORKSPACE_DIR`

## 安全默认值

- 终端工具默认关闭
- 文件和下载操作仅允许 workspace 目录
- 禁止访问本地和内网地址
- 工具输出默认截断，避免超长内容污染模型上下文
