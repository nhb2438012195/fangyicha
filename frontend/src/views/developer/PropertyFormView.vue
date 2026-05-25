<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { propertyApi } from '../../api/property'
import { FLOOR_PLAN_TYPES, PROPERTY_STATUSES, DECORATION_TYPES } from '../../types'
import type { Property } from '../../types'

const router = useRouter()
const route = useRoute()
const isEdit = !!route.params.id
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)

/** 表单数据 */
const formData = reactive<Partial<Property>>({
  propertyName: '',
  location: '',
  longitude: undefined,
  latitude: undefined,
  floorMin: undefined,
  floorMax: undefined,
  floorPlanType: '',
  totalUnits: undefined,
  vacantUnits: undefined,
  pricePerSqm: undefined,
  totalPrice: undefined,
  areaSqm: undefined,
  decoration: '',
  status: '在售',
  description: ''
})

/** 表单校验规则 */
const rules = {
  propertyName: [{ required: true, message: '请输入楼盘名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入地理位置', trigger: 'blur' }],
  floorPlanType: [{ required: true, message: '请选择户型', trigger: 'change' }],
  totalUnits: [{ required: true, message: '请输入总户数', trigger: 'blur' }],
  vacantUnits: [{ required: true, message: '请输入空置户数', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 自动计算总价 */
function autoCalculateTotalPrice() {
  if (formData.pricePerSqm && formData.areaSqm) {
    formData.totalPrice = Math.round(formData.pricePerSqm * formData.areaSqm * 100) / 100
  }
}

/** 加载编辑数据 */
async function loadProperty() {
  if (!isEdit) return
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await propertyApi.getById(id)
    const data = res.data
    Object.assign(formData, {
      propertyName: data.propertyName,
      location: data.location,
      longitude: data.longitude,
      latitude: data.latitude,
      floorMin: data.floorMin,
      floorMax: data.floorMax,
      floorPlanType: data.floorPlanType,
      totalUnits: data.totalUnits,
      vacantUnits: data.vacantUnits,
      pricePerSqm: data.pricePerSqm,
      totalPrice: data.totalPrice,
      areaSqm: data.areaSqm,
      decoration: data.decoration,
      status: data.status,
      description: data.description
    })
  } catch (error) {
    ElMessage.error('加载房产信息失败')
    router.push('/developer/properties')
  } finally {
    loading.value = false
  }
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit) {
      await propertyApi.update(Number(route.params.id), formData)
      ElMessage.success('更新成功')
    } else {
      await propertyApi.create(formData)
      ElMessage.success('创建成功')
    }
    router.push('/developer/properties')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/developer/properties')
}

onMounted(loadProperty)
</script>

<template>
  <div class="property-form-page">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? '编辑房产' : '添加房产' }}</h2>
    </div>

    <el-card shadow="never" class="form-card">
      <el-skeleton :loading="loading" animated :count="8">
        <template #template>
          <div style="padding: 20px;">
            <el-skeleton-item variant="text" style="width: 30%; margin-bottom: 16px;" v-for="i in 8" :key="i" />
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
        <el-divider content-position="left">基本信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="楼盘名称" prop="propertyName">
              <el-input v-model="formData.propertyName" placeholder="请输入楼盘名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="户型" prop="floorPlanType">
              <el-select v-model="formData.floorPlanType" placeholder="请选择户型" style="width: 100%;">
                <el-option v-for="t in FLOOR_PLAN_TYPES" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地理位置" prop="location">
          <el-input v-model="formData.location" placeholder="请输入详细地址" />
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="formData.longitude" :precision="7" :step="0.01" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="formData.latitude" :precision="7" :step="0.01" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">楼栋信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="最低楼层">
              <el-input-number v-model="formData.floorMin" :min="0" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高楼层">
              <el-input-number v-model="formData.floorMax" :min="0" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="装修情况">
              <el-select v-model="formData.decoration" placeholder="选择" clearable style="width: 100%;">
                <el-option v-for="d in DECORATION_TYPES" :key="d" :label="d" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="总户数" prop="totalUnits">
              <el-input-number v-model="formData.totalUnits" :min="0" :precision="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="空置户数" prop="vacantUnits">
              <el-input-number v-model="formData.vacantUnits" :min="0" :precision="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" style="width: 100%;">
                <el-option v-for="s in PROPERTY_STATUSES" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">价格信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="单价(元/㎡)">
              <el-input-number v-model="formData.pricePerSqm" :min="0" :precision="2" style="width: 100%;" @change="autoCalculateTotalPrice" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积(㎡)">
              <el-input-number v-model="formData.areaSqm" :min="0" :precision="2" style="width: 100%;" @change="autoCalculateTotalPrice" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总价(元)">
              <el-input-number v-model="formData.totalPrice" :min="0" :precision="2" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">其他信息</el-divider>

        <el-form-item label="楼盘描述">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="请输入楼盘描述信息" maxlength="2000" show-word-limit />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ submitting ? '保存中...' : (isEdit ? '保存修改' : '创建') }}
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
  margin: 0;
}

.form-card {
  border-radius: 10px;
  max-width: 900px;
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
