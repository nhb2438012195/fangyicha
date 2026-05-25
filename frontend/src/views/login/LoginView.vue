<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import type { LoginRequest } from '../../types'
import { User, Lock, Key } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

/** 登录表单数据 */
const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
  role: 'developer'
})

const loading = ref(false)
const errorMessage = ref('')

/** 登录角色选项 */
const roleOptions = [
  { value: 'developer', label: '开发商' },
  { value: 'customer', label: '购房客户' }
]

/** 登录表单校验规则 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const formRef = ref<any>(null)

/** 登录操作 */
async function handleLogin() {
  errorMessage.value = ''
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await authApi.login(loginForm)
    authStore.setLoginInfo(res.data)

    // 根据角色跳转
    const redirect = (route.query.redirect as string) || ''
    if (res.data.role === 'ROLE_DEVELOPER') {
      router.push(redirect || '/developer/dashboard')
    } else if (res.data.role === 'ROLE_CUSTOMER') {
      router.push(redirect || '/customer/dashboard')
    }
  } catch (error: any) {
    errorMessage.value = error.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Logo 区域 -->
      <div class="login-header">
        <div class="login-logo">
          <span class="logo-icon">房</span>
        </div>
        <h1 class="login-title">房易查</h1>
        <p class="login-subtitle">房地产客户购房查询系统</p>
      </div>

      <!-- 错误提示 -->
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="true"
        class="login-error"
        @close="errorMessage = ''"
      />

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        label-position="top"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-radio-group v-model="loginForm.role" class="role-selector">
            <el-radio-button value="developer">开发商登录</el-radio-button>
            <el-radio-button value="customer">客户登录</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册链接 -->
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="register-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px 32px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: #1a73e8;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.login-error {
  margin-bottom: 20px;
}

.login-form {
  margin-bottom: 8px;
}

.role-selector {
  display: flex;
  width: 100%;
}

.role-selector .el-radio-button {
  flex: 1;
}

.role-selector .el-radio-button__inner {
  width: 100%;
  justify-content: center;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.login-footer {
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.register-link {
  color: #1a73e8;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}
</style>
