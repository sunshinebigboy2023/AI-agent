# Office AI Agent

面向办公写作、知识库问答和任务执行的网页端 AI 服务。项目由 Spring Boot 后端和 Vue 3 前端组成。

## 目录说明

- `ai-agent/`：后端源码、前端源码、Dockerfile 和应用配置。
- `deploy/`：服务器部署入口文件，包含 Docker Compose 和环境变量模板。
- `docs/`：项目规划文档。

## 部署要点

- 默认只暴露前端端口，nginx 会把 `/api` 反向代理到后端。
- 上传知识库文件会持久化到 `./ai-agent/tmp/knowledge`。
- `OFFICE_ENABLE_TERMINAL_TOOL` 默认关闭。公开部署时不要开启终端工具。
- 如果挂在域名后面，把 `OFFICE_ALLOWED_ORIGINS` 改成你的站点来源，例如 `https://office.example.com`。

服务器部署相关文件集中在 [deploy](./deploy)。

更多开发说明见 [ai-agent/README.md](./ai-agent/README.md)。
