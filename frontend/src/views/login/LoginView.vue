<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'
import type { LoginRequest } from '../../types'
import { User, Lock } from '@element-plus/icons-vue'

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
    <!-- 左侧品牌区域 -->
    <div class="login-brand">
      <div class="brand-bg">
        <div class="deco-circle deco-circle-1" />
        <div class="deco-circle deco-circle-2" />
        <div class="deco-circle deco-circle-3" />
        <div class="deco-circle deco-circle-4" />
        <div class="deco-arc deco-arc-1" />
        <div class="deco-arc deco-arc-2" />
      </div>
      <div class="brand-content">
        <div class="brand-logo">
          <span class="brand-logo-text">房</span>
        </div>
        <h1 class="brand-title">房易查</h1>
        <p class="brand-tagline">让购房更简单</p>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="login-card-wrapper">
      <div class="login-card">
        <div class="card-header">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-subtitle">请登录您的账号</p>
        </div>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="true"
          class="login-error"
          @close="errorMessage = ''"
        />

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="role" class="role-item">
            <el-radio-group v-model="loginForm.role" class="role-selector">
              <el-radio-button value="developer">开发商登录</el-radio-button>
              <el-radio-button value="customer">客户登录</el-radio-button>
            </el-radio-group>
          </el-form-item>

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

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
}

/* ===== 左侧品牌区 ===== */
.login-brand {
  flex: 0 0 60%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5a623 0%, #d4a373 100%);
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
}

.deco-circle-1 {
  width: 500px;
  height: 500px;
  top: -120px;
  right: -80px;
}

.deco-circle-2 {
  width: 350px;
  height: 350px;
  bottom: 10%;
  left: -60px;
}

.deco-circle-3 {
  width: 200px;
  height: 200px;
  top: 25%;
  left: 20%;
  background: rgba(255, 255, 255, 0.08);
}

.deco-circle-4 {
  width: 150px;
  height: 150px;
  bottom: 30%;
  right: 15%;
  background: rgba(255, 255, 255, 0.1);
}

.deco-arc {
  position: absolute;
  border: 2px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  pointer-events: none;
}

.deco-arc-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  left: -100px;
}

.deco-arc-2 {
  width: 400px;
  height: 400px;
  bottom: -80px;
  right: 10%;
  border-width: 1.5px;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.brand-logo-text {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(4px);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a3728;
  font-size: 34px;
  font-weight: 700;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.brand-tagline {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  letter-spacing: 4px;
}

/* ===== 右侧登录区 ===== */
.login-card-wrapper {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f0ea;
  padding: 24px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: #fdf8f3;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(180, 130, 80, 0.12);
}

.card-header {
  margin-bottom: 28px;
}

.card-title {
  font-size: 24px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 6px;
}

.card-subtitle {
  font-size: 14px;
  color: #8a7a6a;
  margin: 0;
}

.login-error {
  margin-bottom: 20px;
}

.login-form {
  margin-bottom: 8px;
}

/* ===== 角色选择器 ===== */
.role-item {
  margin-bottom: 20px;
}

.role-selector {
  display: flex;
  width: 100%;
}

.role-selector :deep(.el-radio-button) {
  flex: 1;
}

.role-selector :deep(.el-radio-button__inner) {
  width: 100%;
  justify-content: center;
  border-radius: 8px !important;
  border-color: #e8ddd0;
  color: #8a7a6a;
  background: #fff;
  font-size: 14px;
  padding: 8px 16px;
}

.role-selector :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: #f5a623;
  border-color: #f5a623;
  color: #fff;
  box-shadow: -1px 0 0 0 #f5a623;
}

.role-selector :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px 0 0 8px !important;
}

.role-selector :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 8px 8px 0 !important;
}

/* ===== 输入框 ===== */
.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid #e8ddd0;
  box-shadow: none;
  padding: 4px 12px;
  background: #fff;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #d4c5b5;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #f5a623;
  box-shadow: 0 0 0 1px #f5a623;
}

.login-form :deep(.el-input__prefix) {
  color: #8a7a6a;
  margin-right: 8px;
}

.login-form :deep(.el-input__inner) {
  color: #4a3728;
  font-size: 14px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #c4b5a5;
}

/* ===== 登录按钮 ===== */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  background: #f5a623;
  border-color: #f5a623;
  color: #fff;
}

.login-btn:hover {
  background: #e0961a;
  border-color: #e0961a;
}

.login-btn:active {
  background: #d4870e;
  border-color: #d4870e;
}

/* ===== 注册链接 ===== */
.login-footer {
  text-align: center;
  color: #8a7a6a;
  font-size: 14px;
}

.register-link {
  color: #f5a623;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
  color: #e0961a;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .login-brand {
    display: none;
  }

  .login-card-wrapper {
    flex: 0 0 100%;
  }

  .login-card {
    padding: 32px 24px;
  }
}
</style>
