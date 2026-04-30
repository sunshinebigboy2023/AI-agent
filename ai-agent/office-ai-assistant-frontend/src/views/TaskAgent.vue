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
        ai-type="task"
        @send-message="sendMessage"
      />
    </section>

    <AppFooter />
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithOfficeAgent } from '../api'

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
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

const sendMessage = (message) => {
  addMessage(message, true, 'user-question')
  if (eventSource) {
    eventSource.close()
  }

  const aiMessageIndex = messages.value.length
  addMessage('', false, 'ai-answer')
  connectionStatus.value = 'connecting'
  eventSource = chatWithOfficeAgent(message)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]' && aiMessageIndex < messages.value.length) {
      messages.value[aiMessageIndex].content += data
    }
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  eventSource.onerror = () => {
    connectionStatus.value = 'error'
    if (aiMessageIndex < messages.value.length && !messages.value[aiMessageIndex].content) {
      messages.value[aiMessageIndex].content = '任务连接中断，请检查后端服务或稍后重试。'
    }
    eventSource.close()
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
  grid-template-columns: 280px minmax(0, 1fr);
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
