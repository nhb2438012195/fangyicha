<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { propertyApi } from '../../api/property'
import { FLOOR_PLAN_TYPES } from '../../types'
import type { Property } from '../../types'
import { Search } from '@element-plus/icons-vue'

/** 步骤定义 */
const steps = [
  { title: '选择位置', description: '选择您关注的区域' },
  { title: '选择户型', description: '选择偏好的户型类型' },
  { title: '价格范围', description: '设定预算区间' },
  { title: '查看结果', description: '浏览匹配的楼盘' }
]

const currentStep = ref(0)
const wizardData = reactive({
  location: '',
  floorPlanTypes: [] as string[],
  priceMin: undefined as number | undefined,
  priceMax: undefined as number | undefined
})

const loading = ref(false)
const matchingProperties = ref<Property[]>([])
const totalMatches = ref(0)

/** 位置预设选项 */
const locationOptions = [
  '广州市', '深圳市', '佛山市', '东莞市', '惠州市',
  '北京市', '上海市', '南京市', '苏州市', '珠海市'
]

/** 是否可以进行下一步 */
const canNext = computed(() => {
  switch (currentStep.value) {
    case 0: return wizardData.location.trim().length > 0
    case 1: return wizardData.floorPlanTypes.length > 0
    case 2: return true // 价格可选
    default: return true
  }
})

/** 是否完成所有步骤 */
const isFinished = computed(() => currentStep.value >= steps.length)

/** 下一步 */
function nextStep() {
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  }
  if (currentStep.value === steps.length - 1) {
    searchProperties()
  }
}

/** 上一步 */
function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/** 重置向导 */
function resetWizard() {
  currentStep.value = 0
  wizardData.location = ''
  wizardData.floorPlanTypes = []
  wizardData.priceMin = undefined
  wizardData.priceMax = undefined
  matchingProperties.value = []
  totalMatches.value = 0
}

/** 搜索匹配的房产 */
async function searchProperties() {
  loading.value = true
  try {
    const params: any = {
      page: 1,
      pageSize: 50
    }
    if (wizardData.location) {
      params.location = wizardData.location
    }
    if (wizardData.floorPlanTypes.length > 0) {
      params.floorPlanType = wizardData.floorPlanTypes.join(',')
    }
    if (wizardData.priceMin) {
      params.totalPriceMin = wizardData.priceMin
    }
    if (wizardData.priceMax) {
      params.totalPriceMax = wizardData.priceMax
    }

    const res = await propertyApi.list(params)
    matchingProperties.value = res.data?.records || []
    totalMatches.value = res.data?.total || 0
  } catch (error) {
    console.error('查询失败:', error)
  } finally {
    loading.value = false
  }
}

/** 步骤变更时自动查询结果（如果已经在结果页） */
watch(currentStep, (step) => {
  if (step === 3) {
    searchProperties()
  }
})

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

// 从 localStorage 恢复向导状态
onMounted(() => {
  const saved = localStorage.getItem('wizardState')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      wizardData.location = parsed.location || ''
      wizardData.floorPlanTypes = parsed.floorPlanTypes || []
      wizardData.priceMin = parsed.priceMin
      wizardData.priceMax = parsed.priceMax
    } catch { /* ignore */ }
  }
})

// 保存向导状态到 localStorage
watch(wizardData, () => {
  localStorage.setItem('wizardState', JSON.stringify({ ...wizardData }))
}, { deep: true })
</script>

<template>
  <div class="wizard-page">
    <div class="page-header">
      <h2 class="page-title">引导查询</h2>
      <p class="page-desc">按照步骤逐步筛选，找到心仪的房源</p>
    </div>

    <!-- 步骤条 -->
    <el-steps :active="currentStep" align-center class="wizard-steps" finish-status="success">
      <el-step v-for="(step, index) in steps" :key="index" :title="step.title" :description="step.description" />
    </el-steps>

    <el-card shadow="never" class="wizard-content">
      <!-- 步骤 1: 选择位置 -->
      <div v-if="currentStep === 0" class="step-panel">
        <h3 class="step-title">选择您关注的区域</h3>
        <p class="step-desc">选择城市或输入具体区域</p>
        <div class="location-grid">
          <el-tag
            v-for="loc in locationOptions"
            :key="loc"
            :type="wizardData.location === loc ? 'primary' : 'info'"
            :hit="wizardData.location === loc"
            class="location-tag"
            @click="wizardData.location = loc"
          >
            {{ loc }}
          </el-tag>
        </div>
        <el-input
          v-model="wizardData.location"
          placeholder="或输入自定义位置..."
          clearable
          class="location-input"
        />
      </div>

      <!-- 步骤 2: 选择户型 -->
      <div v-if="currentStep === 1" class="step-panel">
        <h3 class="step-title">选择偏好的户型类型</h3>
        <p class="step-desc">可多选，选择所有符合需求的户型</p>
        <el-checkbox-group v-model="wizardData.floorPlanTypes" class="type-grid">
          <el-checkbox
            v-for="type in FLOOR_PLAN_TYPES"
            :key="type"
            :label="type"
            :value="type"
            class="type-checkbox"
          >
            <div class="type-card">
              <div class="type-name">{{ type }}</div>
            </div>
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <!-- 步骤 3: 价格范围 -->
      <div v-if="currentStep === 2" class="step-panel">
        <h3 class="step-title">设定预算范围</h3>
        <p class="step-desc">设定您的购房预算区间（非必填）</p>
        <el-row :gutter="24" class="price-range">
          <el-col :span="12">
            <div class="price-field">
              <label>最低预算</label>
              <el-input-number v-model="wizardData.priceMin" :min="0" :step="100000" :precision="2" placeholder="不限" style="width: 100%;" />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="price-field">
              <label>最高预算</label>
              <el-input-number v-model="wizardData.priceMax" :min="0" :step="100000" :precision="2" placeholder="不限" style="width: 100%;" />
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤 4: 结果展示 -->
      <div v-if="currentStep === 3" class="step-panel">
        <div class="result-summary">
          <h3 class="step-title">查询结果</h3>
          <span class="result-count">共找到 <strong>{{ totalMatches }}</strong> 个匹配的楼盘</span>
        </div>

        <el-skeleton :loading="loading" animated :count="3">
          <template #template>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
              <el-skeleton-item variant="card" style="height: 200px;" v-for="i in 3" :key="i" />
            </div>
          </template>
        </el-skeleton>

        <div v-if="!loading && matchingProperties.length === 0" class="empty-result">
          <el-empty description="没有找到匹配的楼盘，请调整查询条件" />
          <el-button type="primary" @click="resetWizard">重新查询</el-button>
        </div>

        <div v-if="!loading && matchingProperties.length > 0" class="result-grid">
          <el-card v-for="p in matchingProperties" :key="p.id" shadow="hover" class="property-card">
            <div class="property-header">
              <h4>{{ p.propertyName }}</h4>
              <el-tag :type="p.status === '在售' ? 'success' : 'info'" size="small">{{ p.status }}</el-tag>
            </div>
            <div class="property-detail">
              <div class="detail-row">
                <span class="detail-label">位置</span>
                <span class="detail-value">{{ p.location }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">户型</span>
                <span class="detail-value">{{ p.floorPlanType }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">面积</span>
                <span class="detail-value">{{ p.areaSqm }}㎡</span>
              </div>
              <div class="detail-row price">
                <span class="detail-label">总价</span>
                <span class="detail-value highlight">{{ formatPrice(p.totalPrice) }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 导航按钮 -->
      <div class="wizard-actions">
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="currentStep < steps.length - 1"
          type="primary"
          :disabled="!canNext"
          @click="nextStep"
        >
          下一步
        </el-button>
        <el-button v-if="currentStep === steps.length - 1" @click="resetWizard">重新开始</el-button>
      </div>
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

.wizard-steps {
  margin-bottom: 24px;
}

.wizard-content {
  border-radius: 10px;
  min-height: 400px;
}

.step-panel {
  padding: 20px 8px;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px;
}

.step-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 24px;
}

/* 位置选择 */
.location-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.location-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 20px;
  transition: all 0.2s;
}

.location-tag:hover {
  transform: translateY(-1px);
}

.location-input {
  max-width: 400px;
}

/* 户型选择 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.type-checkbox {
  display: flex;
  margin-right: 0;
}

.type-checkbox :deep(.el-checkbox__label) {
  width: 100%;
}

.type-card {
  padding: 16px;
  text-align: center;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: all 0.2s;
  width: 100%;
}

.type-card:hover {
  border-color: #1a73e8;
  background: #f0f6ff;
}

.type-name {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

/* 价格范围 */
.price-range {
  max-width: 600px;
}

.price-field label {
  display: block;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

/* 结果 */
.result-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-count {
  font-size: 14px;
  color: #6b7280;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.property-card {
  border-radius: 10px;
}

.property-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.property-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f4f6;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: #6b7280;
}

.detail-value {
  color: #1f2937;
}

.detail-value.highlight {
  color: #1a73e8;
  font-weight: 600;
  font-size: 16px;
}

.price .detail-label {
  align-self: center;
}

.empty-result {
  text-align: center;
  padding: 40px 0;
}

.wizard-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
