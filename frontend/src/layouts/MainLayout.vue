<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Sidebar from './components/Sidebar.vue'
import { Expand, Fold } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout-container" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <!-- 侧边栏 -->
    <Sidebar />
    <!-- 右侧主内容区 -->
    <div class="main-area">
      <!-- 顶部导航栏 -->
      <header class="top-navbar">
        <div class="navbar-left">
          <el-button text @click="toggleSidebar" class="collapse-btn">
            <el-icon :size="20">
              <Fold v-if="!sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb>
            <el-breadcrumb-item :to="authStore.isDeveloper ? '/developer/dashboard' : '/customer/dashboard'">
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item>
              <router-link to="" style="color: #1f2937; text-decoration: none;">
                {{ (useRouter().currentRoute.value as any).meta?.title || '' }}
              </router-link>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="navbar-right">
          <el-dropdown trigger="click" @command="handleLogout">
            <span class="user-dropdown">
              <el-avatar :size="32" :style="{ backgroundColor: authStore.isDeveloper ? '#1a73e8' : '#34a853' }">
                {{ authStore.displayName?.charAt(0) || '?' }}
              </el-avatar>
              <span class="user-dropdown-name">{{ authStore.displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.main-area {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.3s;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.sidebar-collapsed .main-area {
  margin-left: 64px;
}

.top-navbar {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  color: #6b7280;
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.user-dropdown:hover {
  background-color: #f3f4f6;
}

.user-dropdown-name {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.content-area {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0;
  }
  .content-area {
    padding: 16px;
  }
}
</style>
