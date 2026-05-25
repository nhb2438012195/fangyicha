<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { Odometer, HomeFilled, DataAnalysis, InfoFilled, ChatDotSquare, Search, Guide, OfficeBuilding, UserFilled, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

defineProps<{ collapsed: boolean }>()

/** 根据角色获取菜单项 */
const menuItems = computed(() => {
  if (authStore.isDeveloper) {
    return [
      { path: '/developer/dashboard', title: '工作台', icon: Odometer },
      { path: '/developer/properties', title: '房产管理', icon: HomeFilled },
      { path: '/developer/analytics', title: '空置率分析', icon: DataAnalysis },
      { path: '/developer/profile', title: '公司信息', icon: InfoFilled },
      { path: '/developer/suggestions', title: '客户建议', icon: ChatDotSquare },
      { path: '/developer/orders', title: '订单管理', icon: Tickets }
    ]
  }
  if (authStore.isCustomer) {
    return [
      { path: '/customer/dashboard', title: '工作台', icon: Odometer },
      { path: '/customer/properties', title: '房产查询', icon: Search },
      { path: '/customer/wizard', title: '引导查询', icon: Guide },
      { path: '/customer/developers', title: '开发商', icon: OfficeBuilding },
      { path: '/customer/suggestions', title: '我的建议', icon: ChatDotSquare },
      { path: '/customer/profile', title: '个人资料', icon: UserFilled },
      { path: '/customer/orders', title: '我的订单', icon: Tickets }
    ]
  }
  return []
})

/** 当前激活的菜单路径 */
const activePath = computed(() => route.path)

/** 菜单点击导航 */
function handleMenuSelect(index: string) {
  router.push(index)
}
</script>

<template>
  <div class="sidebar-container" :class="{ collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-logo" @click="router.push(authStore.isDeveloper ? '/developer/dashboard' : '/customer/dashboard')">
      <div class="logo-icon">房</div>
      <span class="logo-text" :class="{ hide: collapsed }">房易查</span>
    </div>

    <!-- 用户信息 -->
    <div class="sidebar-user" :class="{ hide: collapsed }">
      <el-avatar :size="collapsed ? 32 : 40" :style="{ backgroundColor: authStore.isDeveloper ? '#f5a623' : '#34a853' }">
        {{ authStore.displayName?.charAt(0) || '?' }}
      </el-avatar>
      <div v-show="!collapsed" class="user-info">
        <div class="user-name">{{ authStore.displayName }}</div>
        <div class="user-role">{{ authStore.isDeveloper ? '开发商' : '购房客户' }}</div>
      </div>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activePath"
      :router="false"
      class="sidebar-menu"
      background-color="#1a0f0a"
      text-color="#b0a090"
      active-text-color="#ffffff"
      @select="handleMenuSelect"
    >
      <el-menu-item
        v-for="item in menuItems"
        :key="item.path"
        :index="item.path"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span :class="{ hide: collapsed }">{{ item.title }}</span>
      </el-menu-item>
    </el-menu>

    <!-- 底部退出按钮 -->
    <div class="sidebar-footer">
      <el-button text class="logout-btn" @click="authStore.logout(); router.push('/login')">
        <el-icon><el-icon><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg></el-icon></el-icon>
        <span>退出登录</span>
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.sidebar-container {
  width: 240px;
  height: 100vh;
  background: #1a0f0a;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  transition: width 0.3s ease;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: padding 0.3s ease, justify-content 0.3s ease;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: #f5a623;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  margin-right: 12px;
  flex-shrink: 0;
  transition: margin-right 0.3s ease;
}

.logo-text {
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}

.sidebar-user {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: padding 0.3s ease, justify-content 0.3s ease;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  color: #b0a090;
  font-size: 12px;
  margin-top: 2px;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
  overflow-y: auto;
}

.sidebar-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #f5a623 !important;
  color: #ffffff !important;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.logout-btn {
  color: #b0a090;
  width: 100%;
  justify-content: flex-start;
  font-size: 14px;
}

.logout-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* ===== 折叠状态 ===== */
.sidebar-container.collapsed {
  width: 64px;
}

.sidebar-container.collapsed .sidebar-logo {
  justify-content: center;
  padding: 0 8px;
}

.sidebar-container.collapsed .sidebar-logo .logo-icon {
  margin-right: 0;
}

.sidebar-container.collapsed .sidebar-user {
  justify-content: center;
  padding: 12px 8px;
}

.sidebar-container.collapsed .sidebar-menu .el-menu-item {
  justify-content: center;
  padding: 0 8px !important;
}

.hide {
  display: none !important;
}
</style>
