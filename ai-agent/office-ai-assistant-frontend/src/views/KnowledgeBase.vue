<template>
  <main class="knowledge-page">
    <header class="page-header">
      <button class="back-button" @click="goBack">←</button>
      <div>
        <p>RAG 知识库</p>
        <h1>知识库管理</h1>
      </div>
      <button class="refresh-button" :disabled="loading" @click="loadFiles">刷新</button>
    </header>

    <section class="workspace">
      <section class="upload-panel">
        <div>
          <h2>上传文件</h2>
          <p>当前支持 UTF-8 编码的 Markdown 和文本文件，上传后会立即写入向量检索库。</p>
        </div>
        <form class="upload-form" @submit.prevent="uploadSelectedFile">
          <input
            class="file-input"
            type="file"
            accept=".md,.txt,text/markdown,text/plain"
            @change="handleFileChange"
          />
          <button class="primary-button" type="submit" :disabled="uploading || !selectedFile">
            {{ uploading ? '上传中' : '上传并索引' }}
          </button>
        </form>
        <p v-if="selectedFile" class="file-hint">待上传：{{ selectedFile.name }} · {{ formatSize(selectedFile.size) }}</p>
        <p v-if="notice" class="notice" :class="{ error: noticeType === 'error' }">{{ notice }}</p>
      </section>

      <section class="files-panel">
        <div class="panel-title">
          <h2>已上传文件</h2>
          <span>{{ files.length }} 个文件</span>
        </div>

        <div v-if="loading" class="empty-state">正在读取知识库文件...</div>
        <div v-else-if="!files.length" class="empty-state">还没有上传文件。</div>

        <div v-else class="file-table">
          <div class="file-row table-head">
            <span>文件名</span>
            <span>大小</span>
            <span>状态</span>
            <span>上传时间</span>
            <span></span>
          </div>
          <div v-for="file in files" :key="file.id" class="file-row">
            <span class="filename">{{ file.originalFilename }}</span>
            <span>{{ formatSize(file.size) }}</span>
            <span class="status">{{ file.status }}</span>
            <span>{{ formatDate(file.uploadedAt) }}</span>
            <button class="delete-button" :disabled="deletingId === file.id" @click="deleteFile(file.id)">
              {{ deletingId === file.id ? '删除中' : '删除' }}
            </button>
          </div>
        </div>
      </section>
    </section>

    <AppFooter />
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../components/AppFooter.vue'
import { deleteKnowledgeFile, listKnowledgeFiles, uploadKnowledgeFile } from '../api'

const router = useRouter()
const files = ref([])
const selectedFile = ref(null)
const loading = ref(false)
const uploading = ref(false)
const deletingId = ref('')
const notice = ref('')
const noticeType = ref('info')

const goBack = () => {
  router.push('/')
}

const setNotice = (message, type = 'info') => {
  notice.value = message
  noticeType.value = type
}

const handleFileChange = (event) => {
  selectedFile.value = event.target.files?.[0] || null
  notice.value = ''
}

const loadFiles = async () => {
  loading.value = true
  try {
    const response = await listKnowledgeFiles()
    files.value = response.data || []
  } catch (error) {
    setNotice('读取知识库文件失败，请确认后端服务已启动。', 'error')
  } finally {
    loading.value = false
  }
}

const uploadSelectedFile = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    await uploadKnowledgeFile(selectedFile.value)
    setNotice('上传成功，文件已写入知识库。')
    selectedFile.value = null
    await loadFiles()
  } catch (error) {
    const message = error.response?.data?.message || '上传失败，请确认文件类型为 .md 或 .txt。'
    setNotice(message, 'error')
  } finally {
    uploading.value = false
  }
}

const deleteFile = async (fileId) => {
  if (!window.confirm('确认删除这个知识文件及其索引分块吗？')) {
    return
  }
  deletingId.value = fileId
  try {
    await deleteKnowledgeFile(fileId)
    setNotice('文件已删除。')
    await loadFiles()
  } catch (error) {
    setNotice('删除失败，请稍后重试。', 'error')
  } finally {
    deletingId.value = ''
  }
}

const formatSize = (size) => {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(loadFiles)
</script>

<style scoped>
.knowledge-page {
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
.refresh-button,
.primary-button,
.delete-button {
  height: 38px;
  border-radius: 8px;
  font-weight: 700;
}

.back-button,
.refresh-button {
  border: 1px solid #cbd5e1;
  color: #334155;
  background: #fff;
}

.back-button {
  width: 38px;
  font-size: 18px;
}

.refresh-button {
  padding: 0 14px;
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
}

.workspace {
  flex: 1;
  display: grid;
  gap: 18px;
  width: min(1080px, calc(100% - 32px));
  margin: 18px auto 28px;
}

.upload-panel,
.files-panel {
  padding: 24px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.upload-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 420px);
  gap: 20px;
  align-items: center;
}

.upload-panel h2,
.files-panel h2 {
  color: #172033;
  font-size: 20px;
}

.upload-panel p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.upload-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
}

.file-input {
  min-width: 0;
  height: 38px;
  padding: 7px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
}

.primary-button {
  border: 0;
  color: #fff;
  background: #0f766e;
}

.primary-button:hover:not(:disabled) {
  background: #115e59;
}

.notice {
  grid-column: 1 / -1;
  padding: 10px 12px;
  border-radius: 8px;
  color: #0f766e;
  background: #ecfdf5;
}

.file-hint {
  grid-column: 1 / -1;
  color: #64748b;
  font-size: 13px;
}

.notice.error {
  color: #b91c1c;
  background: #fee2e2;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.panel-title span {
  color: #64748b;
  font-size: 13px;
}

.empty-state {
  padding: 28px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
}

.file-table {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.file-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 110px 90px 190px 80px;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-top: 1px solid #e2e8f0;
  color: #475569;
}

.file-row:first-child {
  border-top: 0;
}

.table-head {
  color: #172033;
  background: #f8fafc;
  font-weight: 700;
}

.filename {
  color: #172033;
  overflow-wrap: anywhere;
}

.status {
  width: fit-content;
  padding: 4px 8px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
}

.delete-button {
  border: 1px solid #fecaca;
  color: #b91c1c;
  background: #fff;
}

button:disabled {
  opacity: 0.58;
}

@media (max-width: 860px) {
  .upload-panel {
    grid-template-columns: 1fr;
  }

  .file-table {
    overflow-x: auto;
  }

  .file-row {
    min-width: 760px;
  }
}

@media (max-width: 640px) {
  .page-header {
    grid-template-columns: 40px minmax(0, 1fr);
    padding: 14px 16px;
  }

  .refresh-button {
    display: none;
  }

  .workspace {
    width: calc(100% - 20px);
    margin-top: 10px;
  }

  .upload-panel,
  .files-panel {
    padding: 18px;
  }

  .upload-form {
    grid-template-columns: 1fr;
  }
}
</style>
