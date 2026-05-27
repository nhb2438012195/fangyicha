<script setup lang="ts">
import { useAuthStore } from './stores/auth'
import { useRoute } from 'vue-router'
import FloatingBubble from './views/customer/components/chat/FloatingBubble.vue'
import ChatWindow from './views/customer/components/chat/ChatWindow.vue'

const authStore = useAuthStore()
const route = useRoute()

/** 是否显示AI助手悬浮球 */
function showChat() {
  return authStore.isLoggedIn && authStore.isCustomer && !route.meta?.public
}
</script>

<template>
  <router-view />
  <template v-if="showChat()">
    <FloatingBubble />
    <ChatWindow />
  </template>
</template>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #4a3728;
  background: #f5f0ea;
}

/* Element Plus CSS 变量覆盖 - 暖色系 */
:root {
  --el-color-primary: #f5a623;
  --el-color-primary-light-3: #f8c065;
  --el-color-primary-light-5: #fbd698;
  --el-color-primary-dark-2: #e0961a;
  --el-bg-color: #fdf8f3;
  --el-bg-color-page: #f5f0ea;
  --el-border-color: #e8ddd0;
  --el-border-color-light: #e8ddd0;
  --el-text-color-primary: #4a3728;
  --el-text-color-secondary: #8a7a6a;
  --el-text-color-placeholder: #b0a090;
  --el-fill-color: #f5f0ea;
  --el-fill-color-light: #fdf8f3;
  --el-fill-color-lighter: #fdf8f3;
  --el-color-warning: #f5a623;
  --el-color-warning-light-3: #f8c065;
  --el-color-warning-dark-2: #e0961a;
}

/* Element Plus 全局覆盖 */
.el-card {
  border: none !important;
}

.el-table th.el-table__cell {
  background-color: #f5f0ea !important;
  color: #8a7a6a;
  font-weight: 600;
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background-color: #f5f0ea;
}

.el-button--primary {
  --el-button-bg-color: #f5a623;
  --el-button-border-color: #f5a623;
  --el-button-hover-bg-color: #e0961a;
  --el-button-hover-border-color: #e0961a;
  --el-button-active-bg-color: #e0961a;
  --el-button-active-border-color: #e0961a;
}

.el-button--primary.is-disabled {
  --el-button-disabled-bg-color: #f8c065;
  --el-button-disabled-border-color: #f8c065;
}

/* 侧边栏菜单激活状态 */
.el-menu {
  border-right: none !important;
}

.el-menu-item.is-active {
  background-color: #f5a623 !important;
  color: #ffffff !important;
}

/* radio-button 选中状态 */
.el-radio-button__original-radio:checked + .el-radio-button__inner {
  background-color: #f5a623 !important;
  border-color: #f5a623 !important;
  box-shadow: -1px 0 0 0 #f5a623 !important;
}

/* tag 标签 - primary色改为暖橙 */
.el-tag--primary {
  --el-tag-bg-color: #fef3e2;
  --el-tag-border-color: #f5a623;
  --el-tag-text-color: #e0961a;
}

/* 步骤条 */
.el-step__head.is-finish {
  color: #f5a623 !important;
  border-color: #f5a623 !important;
}

.el-step__head.is-process {
  color: #f5a623 !important;
  border-color: #f5a623 !important;
}

.el-step__title.is-finish {
  color: #f5a623 !important;
}

/* 分页器 */
.el-pagination.is-background .el-pager li.is-active {
  background-color: #f5a623 !important;
  border-color: #f5a623 !important;
}

/* 面包屑 */
.el-breadcrumb__inner.is-link {
  color: #8a7a6a !important;
}

.el-breadcrumb__inner {
  color: #8a7a6a !important;
}

/* 链接 */
.el-link.el-link--primary {
  --el-link-text-color: #f5a623;
  --el-link-hover-text-color: #e0961a;
}

/* 输入框 focus */
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #f5a623 inset !important;
}

/* 选择器 focus */
.el-select__wrapper.is-focus {
  box-shadow: 0 0 0 1px #f5a623 inset !important;
}

/* 所有可点击目标显示手型指针 */
.el-table__row,
[role="button"],
.clickable {
  cursor: pointer;
}

/* 打印样式 */
@media print {
  body {
    background: white !important;
  }
  .sidebar-container, .top-navbar, .el-button, .el-pagination {
    display: none !important;
  }
  .content-area {
    padding: 0 !important;
    margin: 0 !important;
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d1c8b8;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #b0a090;
}
</style>
