<template>
  <main class="home-page">
    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Office AI Workspace</p>
          <h1>AI 办公助手</h1>
        </div>
        <div class="status-pill" :class="healthStatus">{{ healthText }}</div>
      </header>

      <section class="summary-band">
        <div class="summary-copy">
          <h2>把重复的办公表达、整理和检索交给 AI</h2>
          <p>用于邮件、会议纪要、周报、方案梳理、资料总结和多步骤任务执行。当前版本已经接入流式聊天、RAG 知识库、文件上传和工具调用能力。</p>
        </div>
        <div class="metric-row" aria-label="能力概览">
          <div class="metric">
            <strong>RAG</strong>
            <span>知识库问答</span>
          </div>
          <div class="metric">
            <strong>SSE</strong>
            <span>流式响应</span>
          </div>
          <div class="metric">
            <strong>Tools</strong>
            <span>任务执行</span>
          </div>
        </div>
      </section>

      <section class="app-grid" aria-label="应用入口">
        <button class="app-card" @click="navigateTo('/office-assistant')">
          <span class="app-mark">AI</span>
          <span class="app-content">
            <strong>AI 办公助手</strong>
            <small>写邮件、改文案、生成纪要、总结资料，并基于知识库回答问题。</small>
          </span>
          <span class="app-arrow">→</span>
        </button>

        <button class="app-card knowledge" @click="navigateTo('/knowledge-base')">
          <span class="app-mark">KB</span>
          <span class="app-content">
            <strong>知识库管理</strong>
            <small>上传 Markdown 或文本文件，自动写入 RAG 检索库。</small>
          </span>
          <span class="app-arrow">→</span>
        </button>

        <button class="app-card accent" @click="navigateTo('/task-agent')">
          <span class="app-mark">T</span>
          <span class="app-content">
            <strong>任务执行助手</strong>
            <small>调用搜索、网页抓取、下载、文件处理和 PDF 工具。</small>
          </span>
          <span class="app-arrow">→</span>
        </button>
      </section>

      <section class="roadmap">
        <div>
          <h2>后续可扩展方向</h2>
          <p>这套基础结构适合继续接入会议音频整理、企业知识库、工作流编排、会话历史和权限管理。</p>
        </div>
        <ul>
          <li>文档上传总结</li>
          <li>会议纪要生成</li>
          <li>日报周报草稿</li>
          <li>企业知识库问答</li>
        </ul>
      </section>
    </section>

    <AppFooter />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../components/AppFooter.vue'
import { healthCheck } from '../api'

const router = useRouter()
const healthStatus = ref('checking')

const healthText = computed(() => {
  if (healthStatus.value === 'online') return '服务在线'
  if (healthStatus.value === 'offline') return '服务离线'
  return '检测中'
})

const navigateTo = (path) => {
  router.push(path)
}

const checkServiceHealth = async () => {
  healthStatus.value = 'checking'
  try {
    await healthCheck()
    healthStatus.value = 'online'
  } catch (error) {
    healthStatus.value = 'offline'
  }
}

onMounted(checkServiceHealth)
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f7fafc 0%, #eef5f8 48%, #f4f7fb 100%);
}

.workspace {
  width: min(1160px, calc(100% - 32px));
  margin: 0 auto;
  padding: 32px 0 42px;
  flex: 1;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  margin-bottom: 28px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

h1 {
  color: #0f172a;
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 0;
}

.status-pill {
  flex-shrink: 0;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  color: #475569;
  background: #fff;
  font-size: 13px;
}

.status-pill.online {
  border-color: #99f6e4;
  color: #0f766e;
  background: #ccfbf1;
}

.status-pill.offline {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fee2e2;
}

.summary-band {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 28px;
  align-items: end;
  padding: 34px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.summary-copy h2,
.roadmap h2 {
  color: #172033;
  font-size: 24px;
  line-height: 1.35;
  letter-spacing: 0;
}

.summary-copy p,
.roadmap p {
  max-width: 680px;
  margin-top: 12px;
  color: #526176;
  line-height: 1.8;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(3, 110px);
  gap: 12px;
}

.metric {
  min-height: 86px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.metric strong {
  display: block;
  color: #1d4ed8;
  font-size: 22px;
  line-height: 1.2;
}

.metric span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 22px;
}

.app-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 28px;
  gap: 16px;
  align-items: center;
  min-height: 132px;
  padding: 24px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  color: #172033;
  background: #fff;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.app-card:hover {
  transform: translateY(-2px);
  border-color: #93c5fd;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.10);
}

.app-card.accent:hover,
.app-card.knowledge:hover {
  border-color: #5eead4;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.10);
}

.app-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  color: #fff;
  background: #2563eb;
  font-size: 14px;
  font-weight: 800;
}

.accent .app-mark {
  background: #0f766e;
}

.knowledge .app-mark {
  background: #475569;
}

.app-content strong {
  display: block;
  margin-bottom: 8px;
  font-size: 18px;
}

.app-content small {
  display: block;
  color: #64748b;
  line-height: 1.7;
  font-size: 14px;
}

.app-arrow {
  color: #64748b;
  font-size: 22px;
}

.roadmap {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 360px);
  gap: 24px;
  align-items: start;
  margin-top: 22px;
  padding: 26px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.roadmap ul {
  display: grid;
  gap: 10px;
  list-style: none;
}

.roadmap li {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #334155;
  background: #f8fafc;
}

@media (max-width: 980px) {
  .summary-band,
  .roadmap {
    grid-template-columns: 1fr;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .metric-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .workspace {
    width: min(100% - 24px, 1160px);
    padding-top: 20px;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  h1 {
    font-size: 28px;
  }

  .summary-band,
  .roadmap,
  .app-card {
    padding: 18px;
  }

  .metric-row {
    grid-template-columns: 1fr;
  }

  .app-card {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .app-arrow {
    display: none;
  }
}
</style>
