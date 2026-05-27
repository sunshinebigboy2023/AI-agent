import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD
  ? '/api'
  : 'http://localhost:8123/api')

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

const CLIENT_ID_STORAGE_KEY = 'office_ai_client_id'

export const getOrCreateClientId = () => {
  const existing = window.localStorage.getItem(CLIENT_ID_STORAGE_KEY)
  if (existing) {
    return existing
  }
  const generated = window.crypto?.randomUUID?.() || `office_${Math.random().toString(36).slice(2, 14)}`
  window.localStorage.setItem(CLIENT_ID_STORAGE_KEY, generated)
  return generated
}

request.interceptors.request.use((config) => {
  const url = config.url || ''
  if (url.startsWith('/knowledge/')) {
    config.headers = {
      ...config.headers,
      'X-Client-Id': getOrCreateClientId()
    }
  }
  return config
})

const parseErrorPayload = (rawText) => {
  if (!rawText) {
    return { message: '请求失败' }
  }
  try {
    return JSON.parse(rawText)
  } catch (error) {
    return { message: rawText }
  }
}

export const mapApiErrorMessage = (error, fallbackMessage) => {
  const response = error?.response
  const status = response?.status
  const message = response?.data?.message || response?.data?.error || fallbackMessage
  if (message?.includes('缺少 X-Client-Id') || message?.includes('客户端标识')) {
    return '客户端标识缺失，请刷新页面重试'
  }
  if (status === 400) {
    return message || '请求参数错误，请刷新后重试'
  }
  if (status === 500) {
    return '后端内部错误，请稍后重试'
  }
  if (status === 502 || status === 503 || status === 504) {
    return '后端服务可能未启动'
  }
  return message || fallbackMessage || '请求失败，请稍后重试'
}

export const mapSseErrorMessage = (payload, fallbackMessage = '连接中断，请稍后重试。') => {
  const rawMessage = payload?.message || payload?.error || fallbackMessage
  if (rawMessage.includes('缺少 X-Client-Id') || rawMessage.includes('客户端标识')) {
    return '客户端标识缺失，请刷新页面重试'
  }
  if (rawMessage.includes('DashScope API') || rawMessage.includes('模型') || rawMessage.includes('quota')) {
    return '模型服务暂时不可用'
  }
  if (rawMessage.includes('500') || rawMessage.includes('Internal server error')) {
    return '后端处理失败，请稍后重试'
  }
  if (rawMessage.includes('502') || rawMessage.includes('503') || rawMessage.includes('504') || rawMessage.includes('Failed to fetch')) {
    return '连接中断，请检查服务状态'
  }
  return rawMessage || fallbackMessage
}

export const connectSSE = (url, params = {}, options = {}) => {
  const mergedParams = { ...params }
  if (options.clientId) {
    mergedParams.clientId = options.clientId
  }
  const queryString = Object.keys(mergedParams)
    .filter(key => mergedParams[key] !== undefined && mergedParams[key] !== null && mergedParams[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(mergedParams[key])}`)
    .join('&')
  const fullUrl = queryString ? `${API_BASE_URL}${url}?${queryString}` : `${API_BASE_URL}${url}`
  const listeners = new Map()
  const controller = new AbortController()
  const connection = {
    onerror: null,
    addEventListener(eventName, handler) {
      if (!listeners.has(eventName)) {
        listeners.set(eventName, [])
      }
      listeners.get(eventName).push(handler)
    },
    close() {
      controller.abort()
    }
  }

  const emit = (eventName, data) => {
    const handlers = listeners.get(eventName) || []
    const payload = { data }
    handlers.forEach(handler => handler(payload))
  }

  const emitTransportError = (message) => {
    const payload = { data: JSON.stringify({ message }) }
    if (typeof connection.onerror === 'function') {
      connection.onerror(payload)
    }
  }

  const parseEventChunk = (chunk) => {
    const lines = chunk.split('\n')
    let eventName = 'message'
    const dataLines = []
    lines.forEach((line) => {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^\s/, ''))
      }
    })
    if (dataLines.length) {
      emit(eventName, dataLines.join('\n'))
    }
  }

  fetch(fullUrl, {
    method: 'GET',
    headers: options.headers || {},
    signal: controller.signal
  }).then(async (response) => {
    if (!response.ok || !response.body) {
      const errorText = await response.text()
      const errorPayload = parseErrorPayload(errorText)
      const message = mapSseErrorMessage({ ...errorPayload, message: errorPayload.message || errorText || `SSE 请求失败 (${response.status})` })
      emit('error', JSON.stringify({ message }))
      emitTransportError(message)
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() || ''
      chunks.forEach(parseEventChunk)
    }
    if (buffer.trim()) {
      parseEventChunk(buffer)
    }
  }).catch((error) => {
    if (error.name !== 'AbortError') {
      const message = error.message || '连接中断，请稍后重试。'
      emit('error', JSON.stringify({ message }))
      emitTransportError(message)
    }
  })

  return connection
}

export const chatWithOfficeAssistant = (message, chatId) => {
  const clientId = getOrCreateClientId()
  return connectSSE('/ai/office_app/chat/rag/sse', { message, chatId }, {
    clientId,
    headers: { 'X-Client-Id': clientId }
  })
}

export const chatWithOfficeAssistantStructured = (message, chatId) => {
  const clientId = getOrCreateClientId()
  return connectSSE('/ai/office_app/chat/rag/stream', { message, chatId }, {
    clientId,
    headers: { 'X-Client-Id': clientId }
  })
}

export const chatWithOfficeAgent = (message) => {
  return connectSSE('/ai/office-agent/chat', { message })
}

export const chatWithOfficeAgentStructured = (message) => {
  return connectSSE('/ai/office-agent/chat/stream-with-steps', { message })
}

export const listKnowledgeFiles = () => {
  return request.get('/knowledge/files')
}

export const uploadKnowledgeFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/files', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const deleteKnowledgeFile = (fileId) => {
  return request.delete(`/knowledge/files/${encodeURIComponent(fileId)}`)
}

export const healthCheck = () => {
  return request.get('/health', { timeout: 5000 })
}

export default {
  request,
  chatWithOfficeAssistant,
  chatWithOfficeAssistantStructured,
  chatWithOfficeAgent,
  chatWithOfficeAgentStructured,
  listKnowledgeFiles,
  uploadKnowledgeFile,
  deleteKnowledgeFile,
  healthCheck,
  getOrCreateClientId
}
