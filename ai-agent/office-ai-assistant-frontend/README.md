# AI 办公助手前端

基于 Vue 3 和 Vite 的办公助手工作台，包含两个入口：

- **AI 办公助手**：用于邮件、纪要、周报、方案和资料总结。
- **任务执行助手**：用于搜索、抓取、下载、文件处理和 PDF 生成等多步骤任务。

## 开发

```bash
npm install
npm run dev
```

## 构建

```bash
npm run build
```

## 后端接口

- `/api/ai/office_app/chat/sse`
- `/api/ai/office-agent/chat`

本地开发默认连接 `http://localhost:8123/api`。
