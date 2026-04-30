<template>
  <footer class="app-footer">
    <span>© {{ currentYear }} AI 办公助手</span>
    <span>面向办公效率场景的 AI 应用基础项目</span>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 18px 28px;
  color: #64748b;
  font-size: 13px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

@media (max-width: 640px) {
  .app-footer {
    padding: 16px;
  }
}
</style>
