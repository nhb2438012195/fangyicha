<script setup lang="ts">
import { ref, nextTick, watch, onMounted } from 'vue'
import { useChatStore } from '../../../../stores/chat'
import { useAuthStore } from '../../../../stores/auth'
import SessionPanel from './SessionPanel.vue'
import RecommendCard from './RecommendCard.vue'
import OrderSummaryCard from './OrderSummaryCard.vue'

const chatStore = useChatStore()
const authStore = useAuthStore()
const messageListRef = ref<HTMLDivElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const inputText = ref('')
const showScrollBtn = ref(false)

/** 发送消息 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || chatStore.loading) return
  inputText.value = ''
  chatStore.sendMessage(text)
  // 发送后重新聚焦输入框
  nextTick(() => {
    inputRef.value?.focus()
  })
}

/** 回车发送 */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

/** Escape关闭 */
function handleKeyup(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    chatStore.closeChat()
  }
}

/** 滚动到底部 */
function scrollToBottom(smooth = true) {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTo({
        top: messageListRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

/** 监听消息变化自动滚动 */
watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})

/** 监听滚动位置显示/隐藏scroll-to-bottom按钮 */
function handleScroll() {
  if (!messageListRef.value) return
  const el = messageListRef.value
  showScrollBtn.value = el.scrollHeight - el.scrollTop - el.clientHeight > 100
}

/** 滚动到底部 */
function scrollToBottomBtn() {
  scrollToBottom()
}

/** 欢迎消息 + 快捷操作 */
function handleQuickAction(text: string) {
  chatStore.sendMessage(text)
}

/** 去除 AI 回复中的 markdown 标记 */
function formatText(text: string) {
  return text.replace(/\*\*/g, '')
}

/** 加载会话列表 */
onMounted(() => {
  if (authStore.isLoggedIn && authStore.isCustomer) {
    chatStore.loadSessions()
  }
})
</script>

<template>
  <div v-if="chatStore.chatWindowOpen" class="chat-window" @keyup="handleKeyup">
    <!-- Header -->
    <div class="chat-header">
      <div class="header-left">
        <button class="header-btn menu-btn" @click="chatStore.toggleSessionPanel()" title="会话列表">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12h18M3 6h18M3 18h18" />
          </svg>
        </button>
        <span class="header-title">房易小助手</span>
      </div>
      <div class="header-right">
        <button class="header-btn close-btn" @click="chatStore.closeChat()" title="关闭">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Session Panel (overlay) -->
    <SessionPanel />

    <!-- Body -->
    <div class="chat-body" ref="messageListRef" @scroll="handleScroll">
      <!-- Welcome message for new sessions -->
      <div v-if="chatStore.isNewSession" class="welcome-message">
        <div class="welcome-avatar">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="#f5a623" stroke="none">
            <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
        </div>
        <div class="welcome-bubble">
          <div class="welcome-name">房易小助手</div>
          <div class="welcome-text">你好！我是房易小助手，可以帮你查找楼盘、推荐合适的房子、管理收藏，甚至帮你下单。想先了解什么？</div>
          <div class="quick-actions">
            <button class="quick-chip" @click="handleQuickAction('推荐几个株洲天元区的楼盘')">推荐几个楼盘</button>
            <button class="quick-chip" @click="handleQuickAction('看看我的收藏')">看看我的收藏</button>
            <button class="quick-chip" @click="handleQuickAction('三室两厅有什么选择')">三室两厅有什么选择</button>
          </div>
        </div>
      </div>

      <!-- Messages -->
      <div v-for="(msg, index) in chatStore.messages" :key="index" class="message-row" :class="msg.role">
        <!-- AI Avatar -->
        <div v-if="msg.role === 'assistant' && !msg.loading" class="msg-avatar">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="#f5a623" stroke="none">
            <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
        </div>
        <div class="msg-content" :class="msg.role">
          <!-- Loading state -->
          <div v-if="msg.loading" class="typing-indicator">
            <span class="typing-dot" />
            <span class="typing-dot" />
            <span class="typing-dot" />
          </div>

          <!-- Error state -->
          <div v-else-if="msg.error" class="error-bubble">
            <span class="error-text">{{ msg.content }}</span>
            <button class="retry-btn" @click="chatStore.retryLastMessage()">重试</button>
          </div>

          <!-- Normal message -->
          <template v-else-if="msg.role === 'assistant'">
            <div v-if="index === 0 || chatStore.messages[index-1]?.role !== 'assistant'" class="msg-label">房易小助手</div>

            <!-- Text content (always show if there is content) -->
            <div v-if="msg.content" class="bubble-text">
              <span v-if="msg.content.length <= 500 || msg.expanded">{{ formatText(msg.content) }}</span>
              <span v-else>{{ formatText(msg.content).substring(0, 500) + '...' }}</span>
              <button v-if="msg.content.length > 500" class="toggle-btn" @click="chatStore.toggleExpand(index)">
                {{ msg.expanded ? '收起' : '展开全文' }}
              </button>
            </div>

            <!-- Recommendation cards -->
            <div v-if="msg.messageType === 'recommendation' && msg.metadata?.cards" class="card-list">
              <RecommendCard
                v-for="(card, ci) in msg.metadata.cards.slice(0, 3)"
                :key="ci"
                :property-id="card.propertyId"
                :property-name="card.propertyName"
                :location="card.location"
                :floor-plan-type="card.floorPlanType"
                :area-sqm="card.areaSqm"
                :total-price="card.totalPrice"
                :price-per-sqm="card.pricePerSqm"
                :image-url="card.imageUrl"
                :developer-name="card.developerName"
                :reason="card.reason"
              />
              <div v-if="msg.metadata.cards.length > 3" class="more-link">
                还有 {{ msg.metadata.cards.length - 3 }} 个匹配楼盘
              </div>
            </div>

            <!-- Favorites cards -->
            <div v-if="msg.messageType === 'favorites' && msg.metadata?.cards" class="card-list">
              <RecommendCard
                v-for="(card, ci) in msg.metadata.cards.slice(0, 5)"
                :key="ci"
                :property-id="card.propertyId"
                :property-name="card.propertyName"
                :location="card.location"
                :floor-plan-type="card.floorPlanType"
                :area-sqm="card.areaSqm"
                :total-price="card.totalPrice"
                :price-per-sqm="card.pricePerSqm"
                :image-url="card.imageUrl"
                :developer-name="card.developerName"
              />
            </div>

            <!-- Order summary card -->
            <div v-if="msg.messageType === 'order_summary' && msg.metadata" class="card-list">
              <OrderSummaryCard
                :order-id="msg.metadata.orderId"
                :property-name="msg.metadata.propertyName"
                :location="msg.metadata.location"
                :floor-plan-type="msg.metadata.floorPlanType"
                :area-sqm="msg.metadata.areaSqm"
                :total-price="msg.metadata.totalPrice"
                :price-per-sqm="msg.metadata.pricePerSqm"
                :customer-name="msg.metadata.customerName"
                :customer-phone="msg.metadata.customerPhone"
                :order-no="msg.metadata.orderNo"
              />
            </div>
          </template>

          <!-- User message -->
          <template v-else>
            <div class="msg-label user-label">我</div>
            <div class="bubble-text">{{ msg.content }}</div>
          </template>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="chat-footer">
      <div class="input-area">
        <input
          ref="inputRef"
          v-model="inputText"
          type="text"
          class="chat-input"
          placeholder="输入问题..."
          @keydown="handleKeydown"
          :disabled="chatStore.loading"
        />
        <button
          class="send-btn"
          :class="{ active: inputText.trim() && !chatStore.loading }"
          :disabled="!inputText.trim() || chatStore.loading"
          @click="handleSend"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Scroll to bottom button -->
    <button v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottomBtn">
      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 5v14M19 12l-7 7-7-7" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.chat-window {
  position: fixed;
  bottom: 92px;
  right: 24px;
  width: 380px;
  height: 560px;
  background: #fdf8f3;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(180, 130, 80, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 9998;
  overflow: hidden;
  animation: windowIn 250ms cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes windowIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

/* Header */
.chat-header {
  height: 48px;
  background: #3d2c1e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-btn {
  background: none;
  border: none;
  color: #c4b5a5;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.header-btn:hover {
  color: #fff;
  background: rgba(255,255,255,0.1);
}

/* Body */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

/* Welcome */
.welcome-message {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.welcome-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fef7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.welcome-bubble {
  background: #f5f0ea;
  border-radius: 4px 12px 12px 12px;
  padding: 10px 12px;
  max-width: 280px;
}

.welcome-name {
  font-size: 13px;
  font-weight: 600;
  color: #f5a623;
  margin-bottom: 4px;
}

.welcome-text {
  font-size: 14px;
  color: #4a3728;
  line-height: 1.6;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.quick-chip {
  font-size: 13px;
  color: #f5a623;
  background: #fef7ed;
  border: 1px solid #f5a623;
  border-radius: 16px;
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.quick-chip:hover {
  background: #f5a623;
  color: #fff;
}

/* Messages */
.message-row {
  display: flex;
  gap: 8px;
  max-width: 100%;
}

.message-row.user {
  justify-content: flex-end;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fef7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  align-self: flex-start;
}

.msg-content {
  max-width: 280px;
}

.msg-content.user {
  max-width: 280px;
}

.msg-label {
  font-size: 13px;
  font-weight: 600;
  color: #f5a623;
  margin-bottom: 3px;
}

.msg-label.user-label {
  color: #4a3728;
  text-align: right;
}

.bubble-text {
  background: #f5f0ea;
  color: #4a3728;
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 12px;
  border-radius: 4px 12px 12px 12px;
  word-break: break-word;
}

.message-row.user .bubble-text {
  background: #f5a623;
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}

.toggle-btn {
  display: block;
  margin-top: 4px;
  background: none;
  border: none;
  color: #f5a623;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.toggle-btn:hover {
  text-decoration: underline;
}

/* Typing indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 10px 12px;
  background: #f5f0ea;
  border-radius: 4px 12px 12px 12px;
  min-width: 50px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f5a623;
  animation: bounce 1.2s infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* Error */
.error-bubble {
  background: #fef0f0;
  color: #e85c41;
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 12px;
  border-radius: 4px 12px 12px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.retry-btn {
  background: none;
  border: 1px solid #f5a623;
  color: #f5a623;
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}

.retry-btn:hover {
  background: #f5a623;
  color: #fff;
}

/* Card list */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 4px 0;
}

.more-link {
  font-size: 13px;
  color: #f5a623;
  text-align: center;
  cursor: pointer;
  padding: 4px;
}

.more-link:hover {
  text-decoration: underline;
}

/* Footer */
.chat-footer {
  padding: 8px 12px;
  flex-shrink: 0;
  border-top: 1px solid #e8ddd0;
  background: #fdf8f3;
}

.input-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: #4a3728;
  background: transparent;
  padding: 4px 0;
}

.chat-input::placeholder {
  color: #c4b5a5;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8ddd0;
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
  flex-shrink: 0;
}

.send-btn.active {
  background: #f5a623;
}

.send-btn.active:hover {
  background: #e0961a;
}

.send-btn:disabled {
  cursor: not-allowed;
}

/* Scroll to bottom */
.scroll-bottom-btn {
  position: absolute;
  bottom: 66px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e8ddd0;
  box-shadow: 0 2px 8px rgba(180, 130, 80, 0.12);
  color: #8a7a6a;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.15s;
}

.scroll-bottom-btn:hover {
  background: #f5f0ea;
  border-color: #f5a623;
  color: #f5a623;
}

/* Mobile */
@media (max-width: 640px) {
  .chat-window {
    width: 100%;
    height: calc(100vh - 80px);
    bottom: 0;
    right: 0;
    border-radius: 12px 12px 0 0;
  }

  .msg-content, .welcome-bubble {
    max-width: 240px;
  }
}
</style>
