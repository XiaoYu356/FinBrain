<template>
  <div class="chat-container">
    <el-row :gutter="20" style="height: 100%;">
      <el-col :span="6" style="height: 100%;">
        <el-card class="session-card">
          <template #header>
            <div class="session-header">
              <span>历史会话</span>
              <el-button type="primary" size="small" @click="newSession">新对话</el-button>
            </div>
          </template>
          <div class="session-list">
            <div 
              v-for="session in sessions" 
              :key="session.sessionId" 
              class="session-item"
              :class="{ active: session.sessionId === chatStore.sessionId }"
              @click="loadSession(session.sessionId)"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span class="session-title">{{ session.title }}</span>
              <el-icon class="delete-icon" @click.stop="handleDeleteSession(session.sessionId)"><Delete /></el-icon>
            </div>
            <div v-if="sessions.length === 0" class="empty-sessions">
              暂无历史会话
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18" style="height: 100%;">
        <el-card class="chat-card">
          <div class="chat-messages" ref="messagesRef">
            <div v-if="chatStore.messages.length === 0" class="empty-chat">
              <el-icon :size="64" color="#C0C4CC"><ChatDotRound /></el-icon>
              <p>您好！我是FinBrain智能顾问，有什么可以帮助您的？</p>
              <div class="quick-questions">
                <el-button v-for="q in quickQuestions" :key="q" size="small" @click="sendQuickQuestion(q)">
                  {{ q }}
                </el-button>
              </div>
            </div>
            <div v-for="(msg, index) in chatStore.messages" :key="index" class="message-item" :class="msg.role">
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" icon="User" />
                <el-avatar v-else style="background: #409EFF;">
                  <el-icon><Monitor /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
              </div>
            </div>
            <div v-if="chatStore.isLoading" class="message-item assistant">
              <div class="message-avatar">
                <el-avatar style="background: #409EFF;">
                  <el-icon><Monitor /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text typing">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题..."
              @keydown.enter.ctrl="sendMessage"
            />
            <el-button type="primary" :loading="chatStore.isLoading" @click="sendMessage" style="margin-top: 10px;">
              发送
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { chat, getChatSessions, getChatHistory, deleteChatSession, createChatSession } from '@/api/chat'
import { ElMessageBox } from 'element-plus'
import { marked } from 'marked'

const chatStore = useChatStore()
const userStore = useUserStore()

const inputMessage = ref('')
const messagesRef = ref()
const sessions = ref([])

const quickQuestions = [
  '推荐稳健型理财产品',
  '如何计算投资收益',
  '查看我的资产情况',
  '帮我分析风险等级'
]

const formatMessage = (content) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch (e) {
    return content
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const fetchSessions = async () => {
  try {
    const res = await getChatSessions()
    sessions.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const loadSession = async (sessionId) => {
  chatStore.setSessionId(sessionId)
  try {
    const res = await getChatHistory(sessionId)
    const historyMessages = (res.data || []).map(msg => ({
      role: msg.role,
      content: msg.content
    }))
    chatStore.loadMessages(historyMessages)
    scrollToBottom()
  } catch (e) {
    console.error(e)
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isLoading) return
  
  const message = inputMessage.value.trim()
  inputMessage.value = ''
  
  chatStore.addMessage({ role: 'user', content: message })
  chatStore.isLoading = true
  scrollToBottom()
  
  try {
    const res = await chat({
      message: message,
      sessionId: chatStore.sessionId
    })
    
    chatStore.addMessage({ role: 'assistant', content: res.data.response || '抱歉，我无法理解您的问题。' })
  } catch (e) {
    chatStore.addMessage({ role: 'assistant', content: '抱歉，服务暂时不可用，请稍后再试。' })
  } finally {
    chatStore.isLoading = false
    scrollToBottom()
    fetchSessions()
  }
}

const sendQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const handleDeleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定删除该会话吗？删除后不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatSession(sessionId)
    if (chatStore.sessionId === sessionId) {
      chatStore.newSession()
    }
    fetchSessions()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const newSession = async () => {
  try {
    const res = await createChatSession()
    const session = res.data
    chatStore.setSessionId(session.sessionId)
    chatStore.clearMessages()
    sessions.value.unshift(session)
  } catch (e) {
    console.error(e)
  }
}

onMounted(async () => {
  await fetchSessions()
  if (sessions.value.length > 0) {
    const latestSession = sessions.value[0]
    chatStore.setSessionId(latestSession.sessionId)
    await loadSession(latestSession.sessionId)
  } else {
    await newSession()
  }
})
</script>

<style scoped>
.chat-container {
  height: calc(100vh - 100px);
  padding: 0;
}

.session-card {
  height: 100%;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-list {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.session-item {
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.3s;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
  color: #409EFF;
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.empty-sessions {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 20px;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 400px;
  max-height: calc(100vh - 280px);
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
}

.message-item.user .message-text {
  background: #409EFF;
  color: #fff;
}

.message-item.assistant .message-text {
  background: #f5f7fa;
  color: #303133;
}

.typing {
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  animation: typing 1s infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.chat-input {
  border-top: 1px solid #EBEEF5;
  padding-top: 15px;
}
</style>
