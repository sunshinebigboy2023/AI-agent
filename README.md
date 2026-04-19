# AI-agent

AI-agent 是一个基于 Java 21、Spring Boot 3 和 Spring AI 的个人智能体项目。项目围绕“AI 对话应用 + RAG 知识库 + 工具调用 + MCP 服务”展开，用来学习和实践大模型应用开发，也可以作为个人作品集项目持续扩展。

## 项目能力

- AI 多轮对话：基于 Spring AI `ChatClient` 构建对话流程，支持上下文记忆。
- 结构化输出：把模型回复转换为 Java Record，便于业务侧消费。
- RAG 知识库问答：加载本地 Markdown 文档，结合向量检索增强回答质量。
- 工具调用：为 AI 提供搜索、文件读写、网页抓取、资源下载、终端执行、PDF 生成等工具。
- 自主规划智能体：基于 ReAct 思路封装多步执行流程，让智能体按目标选择工具并逐步完成任务。
- MCP 扩展服务：内置图片搜索 MCP Server，可作为外部工具服务接入主应用。
- 接口文档：集成 Knife4j / OpenAPI，方便本地调试接口。

## 技术栈

- Java 21
- Spring Boot 3.4.x
- Spring AI
- Spring AI Alibaba / DashScope
- LangChain4j DashScope
- PGVector / PostgreSQL
- Ollama
- MCP
- Jsoup
- iText
- Knife4j
- Lombok

## 项目结构

```text
.
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/sunshinebigboy/aiagent
│   │   │   ├── agent        # ReAct 智能体核心实现
│   │   │   ├── app          # AI 对话应用
│   │   │   ├── advisor      # Spring AI Advisor 扩展
│   │   │   ├── chatmemory   # 对话记忆实现
│   │   │   ├── controller   # HTTP 接口
│   │   │   ├── rag          # RAG 文档加载、切分、检索配置
│   │   │   └── tools        # AI 可调用工具
│   │   └── resources
│   │       ├── application.yml
│   │       ├── document     # 本地知识库文档
│   │       └── mcp-servers.json
│   └── test
└── image-search-mcp-server  # 图片搜索 MCP 服务
```

## 本地运行

### 环境要求

- JDK 21+
- Maven 3.9+，也可以直接使用项目内置的 `mvnw`
- 可选：PostgreSQL + PGVector
- 可选：Ollama
- 可选：DashScope API Key、SearchAPI Key、高德地图 MCP Key

### 配置 API Key

主应用默认启用 `local` profile。建议新建 `src/main/resources/application-local.yml`，该文件已被 `.gitignore` 忽略，不会提交到仓库。

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的 DashScope API Key

search-api:
  api-key: 你的 SearchAPI Key
```

如果要使用本地模型，请先启动 Ollama，并在 `application.yml` 中调整模型名称：

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: gemma3:1b
```

### 启动主应用

```bash
./mvnw spring-boot:run
```

启动成功后：

- 健康检查：`http://localhost:8123/api/health`
- Knife4j 文档：`http://localhost:8123/api/doc.html`
- OpenAPI：`http://localhost:8123/api/v3/api-docs`

### 构建项目

```bash
./mvnw clean package -DskipTests
```

### 启动图片搜索 MCP 服务

```bash
cd image-search-mcp-server
./mvnw spring-boot:run
```

如果需要以 jar 包方式接入主应用，先构建 MCP 服务：

```bash
cd image-search-mcp-server
./mvnw clean package -DskipTests
```

然后确认 `src/main/resources/mcp-servers.json` 中的 jar 路径与实际文件一致。

## 主要模块说明

### AI 对话应用

`LoveApp` 是当前示例应用入口，演示了多轮对话、结构化输出、RAG 问答、工具调用和 MCP 调用。后续可以把它扩展为更通用的个人助手、学习助手、资料整理助手或自动化办公助手。

### RAG 知识库

`src/main/resources/document` 下的 Markdown 文件会被加载为知识库内容。可以替换为自己的学习笔记、业务文档、FAQ 或项目资料，让 AI 基于个人知识库回答问题。

### 工具调用

`tools` 包中封装了 AI 可调用的工具：

- `WebSearchTool`：联网搜索
- `WebScrapingTool`：网页抓取
- `FileOperationTool`：文件读写
- `ResourceDownloadTool`：资源下载
- `TerminalOperationTool`：终端命令执行
- `PDFGenerationTool`：PDF 生成
- `TerminateTool`：终止智能体执行

### 自主规划智能体

`AiSuperAgent` 基于工具调用能力实现多步任务执行。它会根据用户目标选择工具、观察结果并继续下一步，适合演示 Agent 的自主规划能力。

## 后续计划

- 增加面向前端调用的 Controller 接口。
- 把“恋爱问答”示例升级为更通用的个人知识库助手。
- 补充 PostgreSQL + PGVector 的 Docker Compose 启动配置。
- 增加统一异常处理和接口返回模型。
- 增加更多可复用工具，例如待办管理、网页摘要、代码分析。
- 整理部署文档，支持服务器或容器化部署。

## 作者

- GitHub：[@sunshinebigboy2023](https://github.com/sunshinebigboy2023)

## 说明

本项目主要用于个人学习、技术实践和作品展示。使用第三方平台、模型服务或 API 时，请根据对应服务的文档完成配置并遵守其使用条款。
