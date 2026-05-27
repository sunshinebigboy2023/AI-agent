<template>
  <main class="assistant-page task">
    <header class="page-header">
      <button class="back-button" @click="goBack">←</button>
      <div>
        <p>多步骤任务</p>
        <h1>任务执行助手</h1>
      </div>
      <div class="status" :class="connectionStatus">{{ statusText }}</div>
    </header>

    <section class="page-body">
      <aside class="side-panel">
        <h2>可调用工具</h2>
        <ul>
          <li>网页搜索与抓取</li>
          <li>文件读写和资源下载</li>
          <li>终端操作</li>
          <li>PDF 文档生成</li>
        </ul>
      </aside>

      <ChatRoom
        class="chat-panel"
        :messages="messages"
        :connection-status="connectionStatus"
        :can-stop="true"
        ai-type="task"
        @send-message="sendMessage"
        @stop-stream="stopStream"
      />

      <section class="trace-panel">
        <div class="trace-header">
          <h2>执行过程</h2>
          <span>{{ steps.length }} 步</span>
        </div>
        <div v-if="!steps.length" class="trace-empty">开始任务后，这里会显示工具调用和中间结果。</div>
        <article v-for="(step, index) in steps" :key="index" class="trace-card">
          <div class="trace-meta">
            <strong>Step {{ step.stepNumber || index + 1 }}</strong>
            <span>{{ step.phase || 'INFO' }}</span>
          </div>
          <div v-if="step.toolName" class="trace-line">工具：{{ step.toolName }}</div>
          <div v-if="step.toolArguments" class="trace-line">参数：{{ step.toolArguments }}</div>
          <div v-if="step.searchSummary" class="trace-line">结果摘要：{{ step.searchSummary }}</div>
          <div v-if="step.searchResults?.length" class="search-results">
            <a
              v-for="(result, resultIndex) in step.searchResults"
              :key="resultIndex"
              class="search-result"
              :href="result.link"
              target="_blank"
              rel="noreferrer"
            >
              <strong>{{ result.title }}</strong>
              <span>{{ result.source }} · {{ result.relevance || '相关结果' }}</span>
              <p>{{ result.snippet }}</p>
            </a>
          </div>
          <div v-else-if="step.observation" class="trace-line">结果：{{ step.observation }}</div>
          <div v-if="step.modelOutput" class="trace-line">模型输出：{{ step.modelOutput }}</div>
          <div v-if="step.errorMessage || step.message" class="trace-line error">
            错误：{{ step.errorMessage || step.message }}
          </div>
        </article>
      </section>
    </section>

    <AppFooter />
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithOfficeAgentStructured, mapSseErrorMessage } from '../api'

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
const steps = ref([])
let eventSource = null

const statusText = computed(() => {
  if (connectionStatus.value === 'connecting') return '执行中'
  if (connectionStatus.value === 'error') return '连接异常'
  return '待命'
})

const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: Date.now()
  })
}

const normalizeStep = (step) => {
  const normalized = { ...step }
  if (typeof normalized.observation !== 'string') {
    return normalized
  }
  try {
    const parsed = JSON.parse(normalized.observation)
    if (parsed?.type === 'web_search_results') {
      normalized.searchSummary = parsed.summary
      normalized.searchResults = Array.isArray(parsed.results) ? parsed.results.slice(0, 4) : []
      normalized.observation = ''
    }
  } catch (error) {
    if (normalized.observation.length > 500) {
      normalized.observation = `${normalized.observation.slice(0, 500)}\n[内容过长，已截断]`
    }
  }
  return normalized
}

const sendMessage = (message) => {
  addMessage(message, true, 'user-question')
  steps.value = []
  if (eventSource) {
    eventSource.close()
  }

  const aiMessageIndex = messages.value.length
  addMessage('', false, 'ai-answer')
  connectionStatus.value = 'connecting'
  eventSource = chatWithOfficeAgentStructured(message)

  eventSource.addEventListener('message', (event) => {
    const payload = JSON.parse(event.data)
    if (payload.content && aiMessageIndex < messages.value.length) {
      messages.value[aiMessageIndex].content += `${payload.content}\n`
    }
  })

  const appendStep = (event) => {
    steps.value.push(normalizeStep(JSON.parse(event.data)))
  }

  eventSource.addEventListener('step', appendStep)
  eventSource.addEventListener('tool_call', appendStep)
  eventSource.addEventListener('tool_result', appendStep)

  eventSource.addEventListener('done', () => {
    connectionStatus.value = 'disconnected'
    eventSource?.close()
  })

  eventSource.addEventListener('error', (event) => {
    const payload = event.data ? JSON.parse(event.data) : { message: '任务执行失败' }
    steps.value.push(normalizeStep(payload))
    if (aiMessageIndex < messages.value.length && !messages.value[aiMessageIndex].content) {
      messages.value[aiMessageIndex].content = mapSseErrorMessage({
        message: payload.errorMessage || payload.message || '任务执行失败'
      }, '任务执行失败')
    }
    connectionStatus.value = 'error'
    eventSource?.close()
  })

  eventSource.onerror = (event) => {
    const payload = event?.data ? JSON.parse(event.data) : { message: '任务连接中断，请检查后端服务或稍后重试。' }
    const message = mapSseErrorMessage(payload, '任务连接中断，请检查后端服务或稍后重试。')
    const hasContent = aiMessageIndex < messages.value.length && messages.value[aiMessageIndex].content
    connectionStatus.value = hasContent ? 'disconnected' : 'error'
    if (!hasContent && aiMessageIndex < messages.value.length) {
      messages.value[aiMessageIndex].content = message
    }
    eventSource.close()
  }
}

const stopStream = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
    connectionStatus.value = 'disconnected'
    steps.value.push({ phase: 'FINAL', observation: '任务已手动停止。' })
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  addMessage('你好，我是任务执行助手。你可以让我检索资料、整理信息、下载资源或生成文档。', false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.assistant-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f7fb;
}

.page-header {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 18px 28px;
  border-bottom: 1px solid #dbe4ef;
  background: #fff;
}

.back-button {
  width: 38px;
  height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #334155;
  background: #fff;
  font-size: 18px;
}

.page-header p {
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.page-header h1 {
  margin-top: 3px;
  font-size: 22px;
  color: #0f172a;
  letter-spacing: 0;
}

.status {
  padding: 8px 12px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 13px;
  font-weight: 700;
}

.status.connecting {
  color: #1d4ed8;
  background: #dbeafe;
}

.status.error {
  color: #b91c1c;
  background: #fee2e2;
}

.page-body {
  flex: 1;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 320px;
  gap: 18px;
  width: min(1180px, calc(100% - 32px));
  margin: 18px auto 24px;
}

.side-panel {
  height: fit-content;
  padding: 20px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.side-panel h2 {
  margin-bottom: 14px;
  color: #172033;
  font-size: 16px;
}

.side-panel ul {
  display: grid;
  gap: 10px;
  list-style: none;
}

.side-panel li {
  padding: 10px 12px;
  border-radius: 8px;
  color: #475569;
  background: #f8fafc;
  line-height: 1.5;
}

.trace-panel {
  padding: 20px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
  overflow: auto;
}

.trace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.trace-empty {
  padding: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  background: #f8fafc;
}

.trace-card {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 12px;
}

.trace-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #0f172a;
  margin-bottom: 8px;
}

.trace-line {
  color: #475569;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
  font-size: 13px;
}

.trace-line.error {
  color: #b91c1c;
}

.search-results {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.search-result {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  color: #0f172a;
  text-decoration: none;
  background: #fff;
}

.search-result span {
  color: #0f766e;
  font-size: 12px;
}

.search-result p {
  color: #475569;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .page-body {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: none;
  }
}

@media (max-width: 640px) {
  .page-header {
    grid-template-columns: 40px minmax(0, 1fr);
    padding: 14px 16px;
  }

  .status {
    display: none;
  }

  .page-body {
    width: calc(100% - 20px);
    margin-top: 10px;
  }
}
</style>
