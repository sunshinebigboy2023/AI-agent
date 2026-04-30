import axios from 'axios'

const API_BASE_URL = process.env.NODE_ENV === 'production'
  ? '/api'
  : 'http://localhost:8123/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

export const connectSSE = (url, params = {}) => {
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  const fullUrl = queryString ? `${API_BASE_URL}${url}?${queryString}` : `${API_BASE_URL}${url}`
  return new EventSource(fullUrl)
}

export const chatWithOfficeAssistant = (message, chatId) => {
  return connectSSE('/ai/office_app/chat/rag/sse', { message, chatId })
}

export const chatWithOfficeAgent = (message) => {
  return connectSSE('/ai/office-agent/chat', { message })
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

export default {
  request,
  chatWithOfficeAssistant,
  chatWithOfficeAgent,
  listKnowledgeFiles,
  uploadKnowledgeFile,
  deleteKnowledgeFile
}
