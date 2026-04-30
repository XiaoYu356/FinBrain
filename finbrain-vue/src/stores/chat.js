import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const sessionId = ref('')
  const isLoading = ref(false)

  const addMessage = (message) => {
    messages.value.push(message)
  }

  const clearMessages = () => {
    messages.value = []
  }

  const setSessionId = (id) => {
    sessionId.value = id
  }

  const newSession = () => {
    sessionId.value = generateSessionId()
    messages.value = []
  }

  const generateSessionId = () => {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  const initSession = () => {
    if (!sessionId.value) {
      sessionId.value = generateSessionId()
    }
  }

  return {
    messages,
    sessionId,
    isLoading,
    addMessage,
    clearMessages,
    setSessionId,
    newSession,
    initSession
  }
})
