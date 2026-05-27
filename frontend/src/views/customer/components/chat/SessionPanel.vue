<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useChatStore } from '../../../../stores/chat'
import { ElMessageBox } from 'element-plus'

const chatStore = useChatStore()
const searchQuery = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)

/** 过滤后的会话列表 */
const filteredSessions = computed(() => {
  if (!searchQuery.value.trim()) {
    return chatStore.sessions
  }
  const q = searchQuery.value.toLowerCase()
  return chatStore.sessions.filter(s => s.title.toLowerCase().includes(q))
})

/** 相对时间显示 */
function relativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + '分钟前'
  if (hours < 24) return hours + '小时前'
  if (days < 2) return '昨天'
  if (days < 7) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekdays[new Date(dateStr).getDay()]
  }
  return dateStr.substring(0, 10)
}

/** 新建对话 */
async function handleNewSession() {
  await chatStore.createNewSession()
  searchQuery.value = ''
}

/** 切换到指定会话 */
function handleSwitch(sessionId: number) {
  chatStore.switchSession(sessionId)
}

/** 删除会话 */
async function handleDelete(e: Event, sessionId: number) {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm('确认删除该对话？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await chatStore.deleteSession(sessionId)
  } catch {
    // user cancelled
  }
}

/** 自动聚焦搜索框 */
watch(() => chatStore.sessionPanelOpen, (open) => {
  if (open) {
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  }
})
</script>

<template>
  <Transition name="panel-slide">
    <div v-if="chatStore.sessionPanelOpen" class="session-panel-overlay" @click.self="chatStore.sessionPanelOpen = false">
      <div class="session-panel" @click.stop>
        <div class="panel-header">
          <button class="new-session-btn" @click="handleNewSession">新建对话</button>
        </div>

        <div class="search-area">
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            class="search-input"
            placeholder="搜索历史对话..."
          />
          <span v-if="searchQuery" class="search-clear" @click="searchQuery = ''">X</span>
        </div>

        <div class="session-list" v-if="filteredSessions.length > 0">
          <div
            v-for="session in filteredSessions"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === chatStore.currentSessionId }"
            @click="handleSwitch(session.id)"
          >
            <div class="session-info">
              <div class="session-title">{{ session.title }}</div>
              <div class="session-time">{{ relativeTime(session.updatedTime) }}</div>
            </div>
            <button
              class="delete-btn"
              @click="(e: Event) => handleDelete(e, session.id)"
              title="删除"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18M8 6V4a1 1 0 011-1h6a1 1 0 011 1v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14z" />
              </svg>
            </button>
          </div>
        </div>

        <div v-else class="empty-sessions">
          <p>还没有对话记录</p>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.session-panel-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.1);
}

.session-panel {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 280px;
  background: #f5f0ea;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(180, 130, 80, 0.15);
  animation: slideIn 200ms ease-out;
}

@keyframes slideIn {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}

.panel-header {
  padding: 12px;
  flex-shrink: 0;
}

.new-session-btn {
  width: 100%;
  padding: 10px 16px;
  background: #f5a623;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.new-session-btn:hover {
  background: #e0961a;
}

.search-area {
  padding: 0 12px 8px;
  position: relative;
  flex-shrink: 0;
}

.search-input {
  width: 100%;
  padding: 8px 28px 8px 12px;
  border: 1px solid #e8ddd0;
  border-radius: 8px;
  font-size: 14px;
  color: #4a3728;
  background: #fff;
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #f5a623;
}

.search-input::placeholder {
  color: #c4b5a5;
}

.search-clear {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #c4b5a5;
  cursor: pointer;
  font-size: 12px;
  padding: 2px;
}

.search-clear:hover {
  color: #8a7a6a;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}

.session-item:hover {
  background: rgba(180, 130, 80, 0.08);
}

.session-item.active {
  background: rgba(245, 166, 35, 0.1);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  color: #4a3728;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.session-time {
  font-size: 12px;
  color: #c4b5a5;
  margin-top: 2px;
}

.delete-btn {
  opacity: 0;
  background: none;
  border: none;
  color: #c4b5a5;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.15s;
  flex-shrink: 0;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #e85c41;
  background: rgba(232, 92, 65, 0.1);
}

.empty-sessions {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c4b5a5;
  font-size: 14px;
  padding: 20px;
}

.empty-sessions p {
  margin: 0;
}
</style>
