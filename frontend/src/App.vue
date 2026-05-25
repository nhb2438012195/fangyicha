<script setup lang="ts">
// 根组件 - 仅提供路由视图出口
</script>

<template>
  <router-view />
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
  color: #1f2937;
  background: #f5f7fa;
}

/* Element Plus 全局覆盖 */
.el-card {
  border: none !important;
}

.el-table th.el-table__cell {
  background-color: #f8fafc !important;
  color: #6b7280;
  font-weight: 600;
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background-color: #f8fafc;
}

.el-button--primary {
  --el-button-bg-color: #1a73e8;
  --el-button-border-color: #1a73e8;
  --el-button-hover-bg-color: #1557b0;
  --el-button-hover-border-color: #1557b0;
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
  background: #d1d5db;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
