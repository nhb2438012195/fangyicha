<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { developerApi } from '../../api/developer'
import type { DeveloperDashboard } from '../../types'

const router = useRouter()
const dashboardData = ref<DeveloperDashboard | null>(null)
const loading = ref(true)

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await developerApi.getDashboard()
    dashboardData.value = res.data
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchDashboard)

/** 指标卡片配置 */
const metricCards = [
  { label: '房产总数', field: 'propertyCount', unit: '个', color: '#f5a623', bgColor: '#fef3e2', icon: '🏠' },
  { label: '总户数', field: 'totalUnits', unit: '户', color: '#34a853', bgColor: '#e6f4ea', icon: '📊' },
  { label: '平均空置率', field: 'avgVacancyRate', unit: '%', color: '#f59e0b', bgColor: '#fef3c7', icon: '📈' },
  { label: '待回复建议', field: 'pendingSuggestions', unit: '条', color: '#ea4335', bgColor: '#fce8e6', icon: '💬' },
  { label: '在售楼盘', field: 'onSaleCount', unit: '个', color: '#34a853', bgColor: '#e6f4ea', icon: '✅' },
  { label: '待开盘楼盘', field: 'pendingCount', unit: '个', color: '#9334e6', bgColor: '#f3e8ff', icon: '⏳' },
  { label: '总订单数', field: 'orderCount', unit: '单', color: '#f5a623', bgColor: '#fef3e2', icon: '📋' },
  { label: '待支付订单', field: 'pendingOrderCount', unit: '单', color: '#f59e0b', bgColor: '#fef3c7', icon: '⏳' },
  { label: '已支付订单', field: 'paidOrderCount', unit: '单', color: '#34a853', bgColor: '#e6f4ea', icon: '💰' }
]

function getMetricValue(item: typeof metricCards[0]): string | number {
  if (!dashboardData.value) return '-'
  const val = (dashboardData.value as any)[item.field]
  if (val === null || val === undefined) return '-'
  if (item.field === 'avgVacancyRate') {
    return Number(val).toFixed(1)
  }
  return val
}

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <p class="page-desc">欢迎回来，以下是您的业务概况</p>
    </div>

    <!-- 加载状态 -->
    <el-skeleton :loading="loading" animated :count="9" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
      <template #template>
        <el-skeleton-item variant="rect" style="height: 140px; border-radius: 10px;" />
      </template>
    </el-skeleton>

    <!-- 指标卡片 -->
    <div v-if="!loading" class="metric-grid">
      <div
        v-for="card in metricCards"
        :key="card.field"
        class="metric-card"
        :style="{ borderLeftColor: card.color }"
      >
        <div class="metric-icon" :style="{ backgroundColor: card.bgColor }">
          <span style="font-size: 24px;">{{ card.icon }}</span>
        </div>
        <div class="metric-info">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">
            <span class="metric-number">{{ getMetricValue(card) }}</span>
            <span class="metric-unit">{{ card.unit }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="action-grid">
        <el-card shadow="never" class="action-card" @click="navigateTo('/developer/properties/create')">
          <div class="action-content">
            <el-icon :size="32" color="#f5a623"><svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg></el-icon>
            <span>添加房产</span>
          </div>
        </el-card>
        <el-card shadow="never" class="action-card" @click="navigateTo('/developer/analytics')">
          <div class="action-content">
            <el-icon :size="32" color="#34a853"><svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zm5.6 8H19v6h-2.8v-6z"/></svg></el-icon>
            <span>查看分析</span>
          </div>
        </el-card>
        <el-card shadow="never" class="action-card" @click="navigateTo('/developer/suggestions')">
          <div class="action-content">
            <el-icon :size="32" color="#f59e0b"><svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg></el-icon>
            <span>客户建议</span>
          </div>
        </el-card>
        <el-card shadow="never" class="action-card" @click="navigateTo('/developer/profile')">
          <div class="action-content">
            <el-icon :size="32" color="#9334e6"><svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></el-icon>
            <span>公司信息</span>
          </div>
        </el-card>
        <el-card shadow="never" class="action-card" @click="navigateTo('/developer/orders')">
          <div class="action-content">
            <el-icon :size="32" color="#f5a623"><svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm-2 14l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg></el-icon>
            <span>订单管理</span>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1200px;
}

.page-header {
  margin-bottom: 24px;
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.metric-card {
  background: #fdf8f3;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-info {
  flex: 1;
  min-width: 0;
}

.metric-label {
  font-size: 13px;
  color: #8a7a6a;
  margin-bottom: 4px;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.metric-number {
  font-size: 28px;
  font-weight: 700;
  color: #4a3728;
  line-height: 1.2;
}

.metric-unit {
  font-size: 13px;
  color: #b0a090;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.action-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  font-size: 14px;
  color: #4a3728;
  font-weight: 500;
}

@media (max-width: 768px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
