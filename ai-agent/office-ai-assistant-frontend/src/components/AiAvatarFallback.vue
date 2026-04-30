<template>
  <div class="ai-avatar-fallback" :class="type">
    <span>{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'office'
  }
})

const label = computed(() => props.type === 'task' ? 'T' : 'AI')
</script>

<style scoped>
.ai-avatar-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  background: #2563eb;
}

.task {
  background: #0f766e;
}
</style>
