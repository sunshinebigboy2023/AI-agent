<template>
  <main class="assistant-page">
    <header class="page-header">
      <button class="back-button" @click="goBack">←</button>
      <div>
        <p>日常办公</p>
        <h1>AI 办公助手</h1>
      </div>
      <button class="knowledge-button" @click="openKnowledgeBase">知识库</button>
    </header>

    <section class="page-body">
      <aside class="side-panel">
        <h2>适合处理</h2>
        <ul>
          <li>邮件撰写与润色</li>
          <li>会议纪要和行动项</li>
          <li>周报、日报、方案大纲</li>
          <li>资料总结和知识库问答</li>
        </ul>
        <div class="rag-note">当前聊天已接入 RAG，会优先参考内置文档和你上传的知识库文件。</div>
      </aside>

      <ChatRoom
        class="chat-panel"
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="office"
        @send-message="sendMessage"
      />
    </section>

    <AppFooter />
  </main>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithOfficeAssistant } from '../api'

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const generateChatId = () => {
  return `office_${Math.random().toString(36).slice(2, 10)}`
}

const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: Date.now()
  })
}

const sendMessage = (message) => {
  addMessage(message, true)

  if (eventSource) {
    eventSource.close()
  }

  const aiMessageIndex = messages.value.length
  addMessage('', false)
  connectionStatus.value = 'connecting'
  eventSource = chatWithOfficeAssistant(message, chatId.value)

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
      messages.value[aiMessageIndex].content = '连接中断，请检查后端服务或稍后重试。'
    }
    eventSource.close()
  }
}

const goBack = () => {
  router.push('/')
}

const openKnowledgeBase = () => {
  router.push('/knowledge-base')
}

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('你好，我是 AI 办公助手。你可以让我写邮件、整理会议纪要、生成周报、总结资料，或基于知识库回答问题。', false)
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

.back-button,
.knowledge-button {
  height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #334155;
  background: #fff;
}

.back-button {
  width: 38px;
  font-size: 18px;
}

.knowledge-button {
  padding: 0 14px;
  font-weight: 700;
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

.rag-note {
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  color: #0f766e;
  background: #ecfdf5;
  line-height: 1.6;
  font-size: 13px;
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

  .knowledge-button {
    display: none;
  }

  .page-body {
    width: calc(100% - 20px);
    margin-top: 10px;
  }
}
</style>
