import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '../types'

/** 认证状态管理 */
export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfoStr = localStorage.getItem('userInfo')
  const userInfo = ref<LoginResponse | null>(userInfoStr ? JSON.parse(userInfoStr) : null)

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 当前角色 */
  const role = computed(() => userInfo.value?.role || '')

  /** 是否为开发商 */
  const isDeveloper = computed(() => role.value === 'ROLE_DEVELOPER')

  /** 是否为客户 */
  const isCustomer = computed(() => role.value === 'ROLE_CUSTOMER')

  /** 显示名称 */
  const displayName = computed(() => userInfo.value?.displayName || '')

  /** 用户ID */
  const userId = computed(() => userInfo.value?.userId || 0)

  /** 设置登录信息 */
  function setLoginInfo(data: LoginResponse) {
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  /** 清除登录信息（退出登录） */
  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    role,
    isDeveloper,
    isCustomer,
    displayName,
    userId,
    setLoginInfo,
    logout
  }
})
