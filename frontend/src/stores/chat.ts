import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { aiApi } from '../api/ai'
import type { AiSessionDTO, AiMessageDTO } from '../types'

/** 聊天消息（UI状态增强） */
export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  messageType: string
  metadata: Record<string, any> | null
  loading?: boolean
  error?: boolean
  expanded?: boolean
}

/** 聊天状态管理 */
export const useChatStore = defineStore('chat', () => {
  const sessions = ref<AiSessionDTO[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const sessionPanelOpen = ref(false)
  const chatWindowOpen = ref(false)
  const hasPendingOrder = ref(false)
  const typing = ref(false)

  /** 是否为新会话（无消息） */
  const isNewSession = computed(() => messages.value.length === 0)

  /** 当前会话信息 */
  const currentSession = computed(() => {
    return sessions.value.find(s => s.id === currentSessionId.value) || null
  })

  /** 加载会话列表 */
  async function loadSessions() {
    try {
      const res = await aiApi.getSessions()
      sessions.value = res.data || []
    } catch {
      sessions.value = []
    }
  }

  /** 切换到指定会话 */
  async function switchSession(sessionId: number) {
    currentSessionId.value = sessionId
    messages.value = []
    sessionPanelOpen.value = false
    await loadMessages(sessionId)
  }

  /** 创建新会话 */
  async function createNewSession() {
    try {
      const res = await aiApi.createSession()
      const newSession: AiSessionDTO = {
        id: res.data.id,
        title: '新对话',
        createdTime: new Date().toISOString(),
        updatedTime: new Date().toISOString(),
        messageCount: 0
      }
      sessions.value.unshift(newSession)
      currentSessionId.value = newSession.id
      messages.value = []
      sessionPanelOpen.value = false
    } catch (e) {
      console.error('创建会话失败', e)
    }
  }

  /** 加载会话消息 */
  async function loadMessages(sessionId: number) {
    try {
      const res = await aiApi.getMessages(sessionId)
      const msgList = (res.data || []).map((m: AiMessageDTO) => ({
        role: m.role as 'user' | 'assistant',
        content: m.content,
        messageType: m.messageType,
        metadata: m.metadata,
        loading: false,
        error: false,
        expanded: false
      }))
      messages.value = msgList
    } catch {
      messages.value = []
    }
  }

  /** 发送消息 */
  async function sendMessage(text: string) {
    if (!text.trim() || loading.value) return

    // Add user message
    messages.value.push({
      role: 'user',
      content: text,
      messageType: 'text',
      metadata: null
    })

    // Add loading placeholder
    const loadingIndex = messages.value.length
    messages.value.push({
      role: 'assistant',
      content: '',
      messageType: 'text',
      metadata: null,
      loading: true
    })

    loading.value = true
    typing.value = true

    try {
      const res = await aiApi.chat({ sessionId: currentSessionId.value, message: text })
      const data = res.data

      // Update session ID for new sessions
      if (!currentSessionId.value && data.sessionId) {
        currentSessionId.value = data.sessionId
        await loadSessions()
      }

      // Replace loading message with actual response
      const reply = data.reply
      if (reply) {
        messages.value[loadingIndex] = {
          role: 'assistant',
          content: reply.text,
          messageType: reply.type || 'text',
          metadata: reply.data || null,
          loading: false,
          error: false,
          expanded: reply.text.length > 500
        }

        // Update session title if needed
        if (currentSessionId.value) {
          const session = sessions.value.find(s => s.id === currentSessionId.value)
          if (session) {
            session.updatedTime = new Date().toISOString()
          }
        }

        // Check for pending order in metadata
        if (reply.data && reply.data.previewId) {
          hasPendingOrder.value = true
        }
      }
    } catch (e) {
      // Show error state
      messages.value[loadingIndex] = {
        role: 'assistant',
        content: '消息发送失败',
        messageType: 'text',
        metadata: null,
        loading: false,
        error: true,
        expanded: false
      }
    } finally {
      loading.value = false
      typing.value = false
    }
  }

  /** 重试发送最后一条消息 */
  async function retryLastMessage() {
    // Find the last user message
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') {
        const lastUserMsg = messages.value[i].content
        // Remove error messages
        while (messages.value.length > i + 1) {
          messages.value.pop()
        }
        // Remove the user message too and resend
        messages.value.pop()
        await sendMessage(lastUserMsg)
        return
      }
    }
  }

  /** 删除会话 */
  async function deleteSession(sessionId: number) {
    try {
      await aiApi.deleteSession(sessionId)
      sessions.value = sessions.value.filter(s => s.id !== sessionId)
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = null
        messages.value = []
      }
    } catch {
      // ignore
    }
  }

  /** 切换会话面板 */
  function toggleSessionPanel() {
    sessionPanelOpen.value = !sessionPanelOpen.value
  }

  /** 打开聊天窗口 */
  function openChat() {
    chatWindowOpen.value = true
    if (!currentSessionId.value) {
      createNewSession()
    }
  }

  /** 关闭聊天窗口 */
  function closeChat() {
    chatWindowOpen.value = false
    sessionPanelOpen.value = false
  }

  /** 收起/展开长消息 */
  function toggleExpand(index: number) {
    if (messages.value[index]) {
      messages.value[index].expanded = !messages.value[index].expanded
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    loading,
    sessionPanelOpen,
    chatWindowOpen,
    hasPendingOrder,
    typing,
    isNewSession,
    currentSession,
    loadSessions,
    switchSession,
    createNewSession,
    loadMessages,
    sendMessage,
    retryLastMessage,
    deleteSession,
    toggleSessionPanel,
    openChat,
    closeChat,
    toggleExpand
  }
})
