<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { developerApi } from '../../api/developer'
import type { Developer } from '../../types'

const loading = ref(true)
const submitting = ref(false)
const formRef = ref<any>(null)

const formData = reactive<Partial<Developer>>({
  companyName: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  businessLicense: '',
  description: ''
})

const rules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function loadProfile() {
  loading.value = true
  try {
    const authRes = await (await import('../../api/auth')).authApi.getCurrentUser()
    const userData = authRes.data
    formData.companyName = userData.companyName || ''
    formData.contactPerson = userData.contactPerson || ''
    formData.phone = userData.phone || ''
    formData.email = userData.email || ''

    // Load full profile
    if (userData.userId) {
      const profileRes = await developerApi.getById(userData.userId)
      const p = profileRes.data
      formData.companyName = p.companyName
      formData.contactPerson = p.contactPerson
      formData.phone = p.phone
      formData.email = p.email
      formData.address = p.address || ''
      formData.businessLicense = p.businessLicense || ''
      formData.description = p.description || ''
    }
  } catch (error) {
    console.error('加载公司信息失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await developerApi.updateProfile(formData)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <h2 class="page-title">公司信息管理</h2>
      <p class="page-desc">管理您的公司基本资料</p>
    </div>

    <el-card shadow="never" class="form-card">
      <el-skeleton :loading="loading" animated :count="6">
        <template #template>
          <div style="padding: 20px;">
            <el-skeleton-item variant="text" style="width: 40%; margin-bottom: 20px;" v-for="i in 6" :key="i" />
          </div>
        </template>
      </el-skeleton>

      <el-form
        v-if="!loading"
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        label-position="right"
        class="main-form"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="formData.companyName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="formData.contactPerson" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="formData.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="公司地址">
          <el-input v-model="formData.address" />
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="营业执照号">
              <el-input v-model="formData.businessLicense" disabled />
              <div class="form-tip">营业执照号由系统录入，不可修改</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="公司简介">
          <el-input v-model="formData.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitting ? '保存中...' : '保存信息' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.form-card {
  border-radius: 10px;
  max-width: 800px;
}

.main-form {
  padding: 8px 0;
}

.form-tip {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
