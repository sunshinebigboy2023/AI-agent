# 部署文件

这个目录集中放服务器部署入口文件：

- `docker-compose.yml`：启动后端和前端容器。
- `.env.example`：环境变量模板。

部署时保留当前目录结构，`deploy/docker-compose.yml` 会从 `../ai-agent` 构建后端和前端镜像。

需要配置的变量：

```bash
DASHSCOPE_API_KEY=your-dashscope-api-key
SEARXNG_BASE_URL=http://searxng:8080
SEARXNG_PORT=8080
FRONTEND_PORT=5021
OFFICE_ALLOWED_ORIGINS=https://your-domain.com
OFFICE_ENABLE_TERMINAL_TOOL=false
```

默认使用自托管 SearXNG 搜索，不需要搜索 API Key。阿里云 OpenSearch 变量可以作为备用搜索源配置。

公开部署时保持 `OFFICE_ENABLE_TERMINAL_TOOL=false`。
