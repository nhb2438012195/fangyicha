<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { authApi } from '../../api/auth'
import type { RegisterRequest } from '../../types'
import { User, Lock, Phone, Message, Postcard } from '@element-plus/icons-vue'

const router = useRouter()

const formRef = ref<any>(null)
const loading = ref(false)
const errorMessage = ref('')
const usernameCheckTimer = ref<any>(null)

/** 注册表单 */
const registerForm = reactive<RegisterRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  idCard: '',
  agreement: false
})

/** 密码强度指示 */
const passwordStrength = computed(() => {
  const pwd = registerForm.password
  if (!pwd) return { level: 0, text: '', color: '' }
  let strength = 0
  if (pwd.length >= 8) strength++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) strength++
  if (/\d/.test(pwd)) strength++
  if (/[^a-zA-Z0-9]/.test(pwd)) strength++
  if (strength <= 1) return { level: 1, text: '弱', color: '#ef4444' }
  if (strength === 2) return { level: 2, text: '中', color: '#f59e0b' }
  return { level: 3, text: '强', color: '#10b981' }
})

/** 用户名唯一性检查 */
async function checkUsername() {
  if (!registerForm.username || registerForm.username.length < 3) return
  try {
    const res = await authApi.checkUsername(registerForm.username)
    if (res.data.exists) {
      errorMessage.value = '用户名已存在'
    } else {
      errorMessage.value = ''
    }
  } catch {
    // ignore
  }
}

function handleUsernameBlur() {
  if (usernameCheckTimer.value) clearTimeout(usernameCheckTimer.value)
  usernameCheckTimer.value = setTimeout(checkUsername, 500)
}

/** 表单校验规则 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  agreement: [
    {
      validator: (_rule: any, value: boolean, callback: Function) => {
        if (!value) {
          callback(new Error('请同意用户协议'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

/** 注册提交 */
async function handleRegister() {
  errorMessage.value = ''
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authApi.register(registerForm)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    errorMessage.value = error.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h1 class="register-title">创建账号</h1>
        <p class="register-subtitle">注册购房客户账号</p>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="true"
        class="register-error"
        @close="errorMessage = ''"
      />

      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        label-position="top"
        class="register-form"
        @keyup.enter="handleRegister"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-50个字符" :prefix-icon="User" @blur="handleUsernameBlur" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" :prefix-icon="User" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少6位" :prefix-icon="Lock" show-password />
            </el-form-item>
            <div v-if="registerForm.password" class="password-strength">
              <span class="strength-label">密码强度：</span>
              <el-progress
                :percentage="passwordStrength.level * 33"
                :color="passwordStrength.color"
                :stroke-width="6"
                :show-text="false"
                class="strength-bar"
              />
              <span class="strength-text" :style="{ color: passwordStrength.color }">{{ passwordStrength.text }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="11位手机号" :prefix-icon="Phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="选填" :prefix-icon="Message" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="registerForm.idCard" placeholder="选填" :prefix-icon="Postcard" />
        </el-form-item>

        <el-form-item prop="agreement">
          <el-checkbox v-model="registerForm.agreement">
            我已阅读并同意 <el-link type="primary" :underline="false">《用户协议》</el-link>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="register-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f0ea;
  padding: 24px;
}

.register-card {
  width: 580px;
  max-width: 100%;
  background: #fdf8f3;
  border-radius: 12px;
  padding: 40px 32px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.register-title {
  font-size: 24px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 8px;
}

.register-subtitle {
  font-size: 14px;
  color: #8a7a6a;
  margin: 0;
}

.register-error {
  margin-bottom: 16px;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: -12px;
  margin-bottom: 18px;
}

.strength-label {
  font-size: 12px;
  color: #8a7a6a;
  white-space: nowrap;
}

.strength-bar {
  flex: 1;
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 20px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.register-footer {
  text-align: center;
  color: #8a7a6a;
  font-size: 14px;
}

.login-link {
  color: #f5a623;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.login-link:hover {
  text-decoration: underline;
}
</style>
