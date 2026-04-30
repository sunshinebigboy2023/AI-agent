import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: 'AI 办公助手',
      description: '面向办公写作、资料总结、知识库问答和任务执行的 AI 助手工作台'
    }
  },
  {
    path: '/office-assistant',
    name: 'OfficeAssistant',
    component: () => import('../views/OfficeAssistant.vue'),
    meta: {
      title: 'AI 办公助手',
      description: '用于邮件、会议纪要、周报、方案和资料总结的智能办公助手'
    }
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    component: () => import('../views/KnowledgeBase.vue'),
    meta: {
      title: '知识库管理',
      description: '上传和管理 AI 办公助手的 RAG 知识库文件'
    }
  },
  {
    path: '/task-agent',
    name: 'TaskAgent',
    component: () => import('../views/TaskAgent.vue'),
    meta: {
      title: '任务执行助手',
      description: '支持搜索、抓取、下载、文件处理和 PDF 生成的任务型 AI 助手'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
