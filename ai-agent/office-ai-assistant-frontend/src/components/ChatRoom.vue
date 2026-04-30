<template>
  <section class="chat-container">
    <div class="chat-messages" ref="messagesContainer">
      <article
        v-for="(msg, index) in messages"
        :key="index"
        class="message-row"
        :class="{ user: msg.isUser }"
      >
        <div v-if="!msg.isUser" class="avatar">
          <AiAvatarFallback :type="aiType" />
        </div>
        <div class="message-bubble" :class="[msg.type]">
          <div class="message-content">
            {{ msg.content }}
            <span
              v-if="connectionStatus === 'connecting' && index === messages.length - 1 && !msg.isUser"
              class="typing-indicator"
            >|</span>
          </div>
          <time class="message-time">{{ formatTime(msg.time) }}</time>
        </div>
        <div v-if="msg.isUser" class="avatar user-avatar">我</div>
      </article>
    </div>

    <form class="chat-input-container" @submit.prevent="sendMessage">
      <textarea
        v-model="inputMessage"
        class="input-box"
        :placeholder="placeholder"
        :disabled="connectionStatus === 'connecting'"
        rows="1"
        @keydown.enter.exact.prevent="sendMessage"
      ></textarea>
      <button
        class="send-button"
        type="submit"
        :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
      >
        发送
      </button>
    </form>
  </section>
</template>

<script setup>
import { ref, nextTick, watch, onMounted, computed } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'office'
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)

const placeholder = computed(() => {
  return props.aiType === 'task'
    ? '描述要执行的任务，例如：调研一个主题并生成报告'
    : '输入办公需求，例如：帮我写一封项目跟进邮件'
})

const sendMessage = () => {
  const message = inputMessage.value.trim()
  if (!message) return
  emit('send-message', message)
  inputMessage.value = ''
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.messages.map(message => message.content).join(''), scrollToBottom)

onMounted(scrollToBottom)
</script>

<style scoped>
.chat-container {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  height: min(720px, calc(100vh - 170px));
  min-height: 520px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.chat-messages {
  min-height: 0;
  overflow-y: auto;
  padding: 22px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 82%;
  margin-bottom: 16px;
}

.message-row.user {
  margin-left: auto;
  justify-content: flex-end;
}

.avatar {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
}

.user-avatar {
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #334155;
  font-size: 12px;
  font-weight: 700;
}

.message-bubble {
  min-width: 120px;
  max-width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #172033;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.user .message-bubble {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.7;
  font-size: 15px;
}

.message-time {
  display: block;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
}

.user .message-time {
  color: rgba(255, 255, 255, 0.72);
}

.chat-input-container {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 82px;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.input-box {
  width: 100%;
  min-height: 44px;
  max-height: 120px;
  padding: 11px 14px;
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #172033;
  background: #fff;
  outline: none;
}

.input-box:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.send-button {
  min-height: 44px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #0f766e;
  font-weight: 700;
}

.send-button:hover:not(:disabled) {
  background: #115e59;
}

.send-button:disabled,
.input-box:disabled {
  opacity: 0.58;
}

.typing-indicator {
  display: inline-block;
  margin-left: 2px;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 720px) {
  .chat-container {
    height: calc(100vh - 140px);
    min-height: 460px;
  }

  .chat-messages {
    padding: 16px;
  }

  .message-row {
    max-width: 96%;
  }

  .chat-input-container {
    grid-template-columns: 1fr;
  }
}
</style>
