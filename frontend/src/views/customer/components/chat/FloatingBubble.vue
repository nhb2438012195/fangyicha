<script setup lang="ts">
import { useChatStore } from '../../../../stores/chat'

const chatStore = useChatStore()

function handleClick() {
  if (chatStore.chatWindowOpen) {
    chatStore.closeChat()
  } else {
    chatStore.openChat()
  }
}
</script>

<template>
  <div class="floating-bubble" :class="{ active: chatStore.chatWindowOpen }" @click="handleClick">
    <!-- 未读/待处理圆点 -->
    <span v-if="chatStore.hasPendingOrder" class="bubble-badge" />
    <!-- 房子图标 SVG -->
    <svg class="bubble-icon" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#f5a623" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  </div>
</template>

<style scoped>
.floating-bubble {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #f5f0ea;
  box-shadow: 0 3px 12px rgba(180, 130, 80, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  cursor: pointer;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid #f5a623;
}

.floating-bubble:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(180, 130, 80, 0.25);
}

.floating-bubble.active {
  transform: scale(0.9);
  opacity: 0.8;
}

.bubble-icon {
  display: block;
}

.bubble-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f5a623;
  border: 2px solid #f5f0ea;
}

@media (max-width: 640px) {
  .floating-bubble {
    bottom: 16px;
    right: 16px;
  }
}
</style>
