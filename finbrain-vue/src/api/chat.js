import { request } from './request'

export function chat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data
  })
}

export function getChatSessions() {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}

export function createChatSession() {
  return request({
    url: '/chat/sessions',
    method: 'post'
  })
}

export function getChatHistory(sessionId) {
  return request({
    url: `/chat/history/${sessionId}`,
    method: 'get'
  })
}

export function deleteChatSession(sessionId) {
  return request({
    url: `/chat/sessions/${sessionId}`,
    method: 'delete'
  })
}
