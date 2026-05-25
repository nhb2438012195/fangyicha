<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi } from '../../api/customer'
import { INTENTION_OPTIONS, URGENCY_OPTIONS } from '../../types'

const loading = ref(true)
const submitting = ref(false)
const formRef = ref<any>(null)

interface ProfileForm {
  phone: string
  email: string
  intention: string[]
  preferredLocations: string
  budgetMin: number | undefined
  budgetMax: number | undefined
  urgency: string
}

const formData = reactive<ProfileForm>({
  phone: '',
  email: '',
  intention: [],
  preferredLocations: '',
  budgetMin: undefined,
  budgetMax: undefined,
  urgency: ''
})

const rules = {
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await customerApi.getProfile()
    const data = res.data
    formData.phone = data.phone || ''
    formData.email = data.email || ''
    formData.intention = data.intention ? data.intention.split(',') : []
    formData.preferredLocations = data.preferredLocations || ''
    formData.budgetMin = data.budgetMin || undefined
    formData.budgetMax = data.budgetMax || undefined
    formData.urgency = data.urgency || ''
  } catch (error) {
    console.error('加载个人信息失败:', error)
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

  // 校验预算
  if (formData.budgetMin && formData.budgetMax && formData.budgetMax < formData.budgetMin) {
    ElMessage.warning('最高预算不能低于最低预算')
    return
  }

  submitting.value = true
  try {
    await customerApi.updateProfile({
      phone: formData.phone,
      email: formData.email,
      intention: formData.intention.join(','),
      preferredLocations: formData.preferredLocations,
      budgetMin: formData.budgetMin,
      budgetMax: formData.budgetMax,
      urgency: formData.urgency
    })
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
  <div class="customer-profile-page">
    <div class="page-header">
      <h2 class="page-title">个人资料</h2>
      <p class="page-desc">管理您的个人信息和购房偏好</p>
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
        <el-divider content-position="left">联系信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">购房偏好</el-divider>

        <el-form-item label="购房意向">
          <el-checkbox-group v-model="formData.intention">
            <el-checkbox v-for="opt in INTENTION_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-checkbox-group>
          <div class="form-tip">可多选，点击标签可移除</div>
        </el-form-item>

        <el-form-item label="偏好区域">
          <el-input v-model="formData.preferredLocations" placeholder="如：广州市,深圳市（多个用逗号分隔）" />
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="最低预算">
              <el-input-number v-model="formData.budgetMin" :min="0" :step="100000" :precision="2" placeholder="不限" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高预算">
              <el-input-number v-model="formData.budgetMax" :min="0" :step="100000" :precision="2" placeholder="不限" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="购房紧迫度">
          <el-select v-model="formData.urgency" placeholder="请选择" clearable style="width: 200px;">
            <el-option v-for="opt in URGENCY_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitting ? '保存中...' : '保存资料' }}
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
  color: #4a3728;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 14px;
  color: #8a7a6a;
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
  color: #b0a090;
  margin-top: 4px;
}
</style>
