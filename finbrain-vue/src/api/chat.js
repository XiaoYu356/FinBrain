import { aiRequest } from './request'

export function chat(data) {
  return aiRequest({
    url: '/chat',
    method: 'post',
    data
  })
}

export function chatStream(data, onMessage, onError, onComplete) {
  const eventSource = new EventSource(`/ai/chat/stream?message=${encodeURIComponent(data.message)}&user_id=${data.user_id}&session_id=${data.session_id}`)
  
  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      eventSource.close()
      if (onComplete) onComplete()
      return
    }
    try {
      const parsed = JSON.parse(event.data)
      if (onMessage) onMessage(parsed)
    } catch (e) {
      if (onMessage) onMessage({ content: event.data })
    }
  }
  
  eventSource.onerror = (error) => {
    eventSource.close()
    if (onError) onError(error)
  }
  
  return eventSource
}

export function getChatSessions() {
  return aiRequest({
    url: '/chat/sessions',
    method: 'get'
  })
}

export function getChatHistory(sessionId) {
  return aiRequest({
    url: `/chat/history/${sessionId}`,
    method: 'get'
  })
}
