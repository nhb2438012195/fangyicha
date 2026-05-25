<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { customerApi } from '../../api/customer'
import type { CustomerDashboard, RecommendationItem } from '../../types'
import { Search, Guide, ChatDotSquare, OfficeBuilding, Tickets, Refresh } from '@element-plus/icons-vue'
import PreferredLocationEditor from './components/PreferredLocationEditor.vue'
import PropertyRecommendCard from './components/PropertyRecommendCard.vue'

const router = useRouter()
const dashboardData = ref<CustomerDashboard | null>(null)
const recommendations = ref<RecommendationItem[]>([])
const loading = ref(true)
const recLoading = ref(true)
const recError = ref(false)

// 推荐缓存 key
const CACHE_KEY = 'fangyicha_rec_cache'

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await customerApi.getDashboard()
    dashboardData.value = res.data
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchRecommendations(force: boolean = false) {
  recLoading.value = true
  recError.value = false

  // 尝试读取缓存
  if (!force) {
    const cached = loadCache()
    if (cached) {
      recommendations.value = cached
      recLoading.value = false
      return
    }
  }

  try {
    const res = await customerApi.getRecommendations()
    const data = res.data || []
    recommendations.value = data
    saveCache(data)
  } catch (error) {
    console.error('获取推荐失败:', error)
    recError.value = true
  } finally {
    recLoading.value = false
  }
}

function loadCache(): RecommendationItem[] | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const cached = JSON.parse(raw)
    const expiry = cached.expiry || 0
    if (Date.now() > expiry) {
      localStorage.removeItem(CACHE_KEY)
      return null
    }
    return cached.data || null
  } catch {
    localStorage.removeItem(CACHE_KEY)
    return null
  }
}

function saveCache(data: RecommendationItem[]) {
  try {
    const cache = {
      data,
      expiry: Date.now() + 5 * 60 * 1000 // 5分钟缓存
    }
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache))
  } catch {
    // 忽略存储失败
  }
}

function onLocationsChange() {
  // 偏好区域变更，清除缓存并刷新推荐
  localStorage.removeItem(CACHE_KEY)
  fetchRecommendations(true)
}

function goToProperty(id: number) {
  router.push(`/customer/properties/${id}`)
}

onMounted(() => {
  fetchDashboard()
  fetchRecommendations(false)
})

const quickActions = [
  { label: '房产查询', desc: '多条件搜索楼盘', icon: Search, color: '#f5a623', path: '/customer/properties' },
  { label: '引导查询', desc: '按步骤引导找房', icon: Guide, color: '#34a853', path: '/customer/wizard' },
  { label: '开发商列表', desc: '浏览合作开发商', icon: OfficeBuilding, color: '#f59e0b', path: '/customer/developers' },
  { label: '提交建议', desc: '向开发商提购房意向', icon: ChatDotSquare, color: '#9334e6', path: '/customer/suggestions/new' },
  { label: '我的订单', desc: '查看购房订单状态', icon: Tickets, color: '#ef4444', path: '/customer/orders' }
]
</script>

<template>
  <div class="customer-dashboard">
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <p class="page-desc">欢迎回来，开始寻找您的理想房源</p>
    </div>

    <!-- 偏好区域编辑器 -->
    <PreferredLocationEditor @change="onLocationsChange" />

    <!-- 用户数据卡片 -->
    <el-skeleton :loading="loading" animated>
      <template #template>
        <div style="display: flex; gap: 16px;">
          <el-skeleton-item variant="card" style="height: 100px; flex: 1;" v-for="i in 3" :key="i" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading" class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ dashboardData?.suggestionCount || 0 }}</div>
        <div class="stat-label">我的建议总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: #8a7a6a;">{{ dashboardData?.pendingCount || 0 }}</div>
        <div class="stat-label">待回复建议</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: #4a3728;">{{ dashboardData?.orderCount || 0 }}</div>
        <div class="stat-label">我的订单数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: #34a853;">{{ dashboardData?.pendingOrderCount || 0 }}</div>
        <div class="stat-label">待支付订单</div>
      </div>
    </div>

    <!-- 个性化推荐区域 -->
    <div class="recommend-section">
      <div class="section-header">
        <h3 class="section-title">为您推荐</h3>
        <el-button
          text
          size="small"
          :icon="Refresh"
          :loading="recLoading"
          @click="fetchRecommendations(true)"
        >
          刷新推荐
        </el-button>
      </div>

      <el-skeleton :loading="recLoading" animated>
        <template #template>
          <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
            <el-skeleton-item variant="card" style="height: 300px;" v-for="i in 3" :key="i" />
          </div>
        </template>
      </el-skeleton>

      <!-- 推荐卡片网格 -->
      <div v-if="!recLoading && recommendations.length > 0" class="recommend-grid">
        <PropertyRecommendCard
          v-for="item in recommendations"
          :key="item.propertyId"
          :item="item"
          @click="goToProperty"
        />
      </div>

      <!-- 推荐空状态 -->
      <div v-if="!recLoading && recommendations.length === 0 && !recError" class="empty-rec">
        <el-empty description="暂无推荐，请先设置您的偏好区域" :image-size="60">
          <template #image>
            <svg viewBox="0 0 100 100" width="60" height="60" fill="none">
              <circle cx="50" cy="50" r="40" stroke="#e8ddd0" stroke-width="2" />
              <path d="M35 50 L45 60 L65 40" stroke="#f5a623" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </template>
        </el-empty>
      </div>

      <!-- 推荐加载错误 -->
      <div v-if="!recLoading && recError" class="empty-rec">
        <el-empty description="获取推荐失败，请刷新重试" :image-size="60" />
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="action-grid">
        <el-card
          v-for="action in quickActions"
          :key="action.label"
          shadow="never"
          class="action-card"
          @click="router.push(action.path)"
        >
          <div class="action-inner">
            <el-avatar :size="48" :style="{ backgroundColor: action.color + '15', color: action.color }">
              <el-icon :size="24"><component :is="action.icon" /></el-icon>
            </el-avatar>
            <div class="action-info">
              <div class="action-label">{{ action.label }}</div>
              <div class="action-desc">{{ action.desc }}</div>
            </div>
            <el-icon class="action-arrow" color="#b0a090"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg></el-icon>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 使用引导 -->
    <div class="guide-section">
      <h3 class="section-title">使用引导</h3>
      <el-steps :active="1" align-center>
        <el-step title="查询房源" description="多条件或引导查询" />
        <el-step title="查看详情" description="了解楼盘详细情况" />
        <el-step title="提交意向" description="向开发商提交购房意向" />
        <el-step title="等待回复" description="关注开发商回复" />
      </el-steps>
    </div>
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fdf8f3;
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #4a3728;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #8a7a6a;
}

.recommend-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.empty-rec {
  padding: 40px 0;
  background: #fdf8f3;
  border-radius: 10px;
}

.quick-actions {
  margin-bottom: 32px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-card {
  cursor: pointer;
  border-radius: 10px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.action-info {
  flex: 1;
}

.action-label {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
  margin-bottom: 2px;
}

.action-desc {
  font-size: 12px;
  color: #8a7a6a;
}

.action-arrow {
  flex-shrink: 0;
}

.guide-section {
  background: #fdf8f3;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .action-grid {
    grid-template-columns: 1fr;
  }
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .recommend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
