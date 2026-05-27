import { ref } from 'vue'

export function useEnterToSend(onSend) {
  const isComposing = ref(false)
  const suppressNextEnter = ref(false)
  let suppressTimer = null

  const clearSuppressTimer = () => {
    if (suppressTimer) {
      window.clearTimeout(suppressTimer)
      suppressTimer = null
    }
  }

  const handleCompositionStart = () => {
    clearSuppressTimer()
    isComposing.value = true
  }

  const handleCompositionEnd = () => {
    isComposing.value = false
    suppressNextEnter.value = true
    clearSuppressTimer()
    suppressTimer = window.setTimeout(() => {
      suppressNextEnter.value = false
      suppressTimer = null
    }, 60)
  }

  const shouldSuppressSend = (event) => {
    return !!(event?.isComposing || isComposing.value || event?.keyCode === 229 || suppressNextEnter.value)
  }

  const handleKeydown = (event) => {
    if (shouldSuppressSend(event)) {
      return
    }
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      onSend()
    }
  }

  const handleSubmit = (event) => {
    if (shouldSuppressSend(event)) {
      event?.preventDefault?.()
      return
    }
    event?.preventDefault?.()
    onSend()
  }

  return {
    isComposing,
    suppressNextEnter,
    handleCompositionStart,
    handleCompositionEnd,
    handleKeydown,
    handleSubmit
  }
}
