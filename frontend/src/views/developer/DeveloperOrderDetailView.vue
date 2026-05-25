<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '../../api/order'
import type { Order, OrderLog } from '../../types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const logs = ref<OrderLog[]>([])
const loading = ref(true)
const logsLoading = ref(true)

async function fetchDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await orderApi.getById(id)
    order.value = res.data
  } catch (error: any) {
    console.error('获取订单详情失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchLogs() {
  logsLoading.value = true
  try {
    const id = Number(route.params.id)
    const res = await orderApi.getLogs(id)
    logs.value = res.data || []
  } catch (error) {
    console.error('获取订单日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    '待支付': 'warning',
    '已支付': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}

async function handleComplete() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确认完成订单 ${order.value.orderNo}？`,
      '确认完成',
      { confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'info' }
    )
    ElMessage.info('正在处理...')
    await new Promise(resolve => setTimeout(resolve, 500))
    const res = await orderApi.complete(order.value.id)
    order.value = res.data
    ElMessage.success('订单已确认完成')
    fetchLogs()
  } catch (error: any) {
    if (error === 'cancel' || error?.toString().includes('cancel')) return
    ElMessage.error(error?.message || '操作失败')
  }
}

onMounted(() => {
  fetchDetail()
  fetchLogs()
})
</script>

<template>
  <div class="order-detail-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.push('/developer/orders')">
        返回订单列表
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="card" style="height: 200px;" />
        <div style="margin-top: 16px;">
          <el-skeleton-item variant="card" style="height: 150px;" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading && order" class="detail-content">
      <el-card shadow="never" class="detail-card">
        <div class="order-header">
          <div class="order-title">
            <h2>订单详情</h2>
            <el-tag :type="getStatusType(order.status)" size="medium">{{ order.status }}</el-tag>
          </div>
          <div class="order-no">
            <span class="label">订单号：</span>
            <span class="value monospace">{{ order.orderNo }}</span>
          </div>
        </div>

        <el-divider />

        <!-- 房产信息 -->
        <div class="section">
          <h3 class="section-title">房产信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">楼盘名称</span>
              <span class="info-value">{{ order.propertyName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">位置</span>
              <span class="info-value">{{ order.propertyLocation }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">户型</span>
              <span class="info-value">{{ order.floorPlanType }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">面积</span>
              <span class="info-value">{{ order.areaSqm }} ㎡</span>
            </div>
            <div class="info-item">
              <span class="info-label">单价</span>
              <span class="info-value">{{ formatPrice(order.pricePerSqm) }}/㎡</span>
            </div>
            <div class="info-item">
              <span class="info-label">总价</span>
              <span class="info-value price">{{ formatPrice(order.totalPrice) }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 客户信息 -->
        <div class="section">
          <h3 class="section-title">客户信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">姓名</span>
              <span class="info-value">{{ order.customerName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ order.customerPhone }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">开发商</span>
              <span class="info-value">{{ order.developerName }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 时间信息 -->
        <div class="section">
          <h3 class="section-title">时间信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">下单时间</span>
              <span class="info-value">{{ order.createdTime }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">支付时间</span>
              <span class="info-value">{{ order.paidTime || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">完成时间</span>
              <span class="info-value">{{ order.completedTime || '-' }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 时间线 -->
        <div class="section">
          <h3 class="section-title">订单进度</h3>
          <el-timeline>
            <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="log.createdTime"
              :color="log.action === '支付订单' ? '#34a853' : log.action === '确认完成' ? '#f5a623' : log.action === '取消订单' ? '#ef4444' : '#b0a090'"
            >
              <p style="margin: 0;">
                <strong>{{ log.action }}</strong>
                <span v-if="log.toStatus" style="margin-left: 8px;">
                  → <el-tag size="small" :type="getStatusType(log.toStatus)">{{ log.toStatus }}</el-tag>
                </span>
              </p>
              <p style="margin: 4px 0 0; font-size: 12px; color: #b0a090;">{{ log.detail }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- 操作按钮 -->
        <div class="action-bar" v-if="order.status === '已支付'">
          <el-button type="primary" size="large" @click="handleComplete">
            确认完成
          </el-button>
        </div>
      </el-card>
    </div>

    <div v-if="!loading && !order" class="empty-state">
      <el-empty description="订单不存在" />
      <el-button type="primary" @click="router.push('/developer/orders')">
        返回订单列表
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.order-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.detail-card {
  border-radius: 10px;
  padding: 8px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.order-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-title h2 {
  font-size: 22px;
  font-weight: 700;
  color: #4a3728;
  margin: 0;
}

.order-no {
  font-size: 14px;
}

.order-no .label {
  color: #b0a090;
}

.order-no .value {
  color: #4a3728;
  font-weight: 500;
}

.monospace {
  font-family: monospace;
  font-size: 14px;
}

.section {
  margin: 8px 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #b0a090;
}

.info-value {
  font-size: 15px;
  color: #4a3728;
  font-weight: 500;
}

.info-value.price {
  color: #f5a623;
  font-weight: 700;
  font-size: 18px;
}

.action-bar {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .order-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
