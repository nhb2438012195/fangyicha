<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { suggestionApi } from '../../api/suggestion'
import { developerApi } from '../../api/developer'
import { FLOOR_PLAN_TYPES } from '../../types'
import type { Developer } from '../../types'

const router = useRouter()
const route = useRoute()
const developerIdParam = Number(route.query.developerId)

const submitting = ref(false)
const developerList = ref<Developer[]>([])
const formRef = ref<any>(null)

const formData = reactive({
  developerId: developerIdParam || undefined,
  preferredType: '',
  priceMin: undefined as number | undefined,
  priceMax: undefined as number | undefined,
  notes: ''
})

const rules = {
  developerId: [{ required: true, message: '请选择开发商', trigger: 'change' }]
}

async function loadDevelopers() {
  try {
    const res = await developerApi.list({ page: 1, pageSize: 100 })
    developerList.value = res.data?.records || []
  } catch (error) {
    console.error('加载开发商列表失败:', error)
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
    await suggestionApi.submit(formData as any)
    ElMessage.success('建议提交成功')
    router.push('/customer/suggestions')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/customer/suggestions')
}

onMounted(loadDevelopers)
</script>

<template>
  <div class="new-suggestion-page">
    <div class="page-header">
      <h2 class="page-title">提交购房意向</h2>
      <p class="page-desc">向开发商提交您的购房意向和需求</p>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        label-position="right"
        class="main-form"
      >
        <el-form-item label="选择开发商" prop="developerId">
          <el-select v-model="formData.developerId" placeholder="请选择开发商" style="width: 100%;">
            <el-option
              v-for="dev in developerList"
              :key="dev.id"
              :label="dev.companyName"
              :value="dev.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="偏好户型">
          <el-select v-model="formData.preferredType" placeholder="户型偏好（选填）" clearable style="width: 100%;">
            <el-option v-for="t in FLOOR_PLAN_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="最低预算">
              <el-input-number v-model="formData.priceMin" :min="0" :step="100000" :precision="2" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高预算">
              <el-input-number v-model="formData.priceMax" :min="0" :step="100000" :precision="2" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注说明">
          <el-input
            v-model="formData.notes"
            type="textarea"
            :rows="4"
            placeholder="请描述您的购房需求或想要咨询的问题..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : '提交意向' }}
            </el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
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
  max-width: 700px;
}

.main-form {
  padding: 8px 0;
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}
</style>
