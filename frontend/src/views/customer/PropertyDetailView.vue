<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { propertyApi } from '../../api/property'
import { orderApi } from '../../api/order'
import type { Property } from '../../types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const property = ref<Property | null>(null)
const loading = ref(true)
const ordering = ref(false)

async function fetchDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await propertyApi.getById(id)
    property.value = res.data
  } catch (error) {
    console.error('获取房产详情失败:', error)
  } finally {
    loading.value = false
  }
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

async function handleBuy() {
  if (!property.value) return
  try {
    await ElMessageBox.confirm(
      `确认购买「${property.value.propertyName}」？\n总价：${formatPrice(property.value.totalPrice)}`,
      '确认下单',
      { confirmButtonText: '确认购买', cancelButtonText: '再想想', type: 'info' }
    )
    ordering.value = true
    const res = await orderApi.create(property.value.id)
    ElMessage.success('下单成功！订单号：' + res.data.orderNo)
    router.push(`/customer/orders/${res.data.id}`)
  } catch (error: any) {
    if (error?.toString().includes('cancel') || error === 'cancel') {
      // 用户取消，不做处理
    } else {
      ElMessage.error(error?.message || '下单失败')
    }
  } finally {
    ordering.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="property-detail-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.push('/customer/properties')">
        返回查询
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="card" style="height: 300px;" />
      </template>
    </el-skeleton>

    <div v-if="!loading && property" class="detail-content">
      <el-card shadow="never" class="detail-card">
        <div class="detail-header">
          <div class="title-section">
            <h2>{{ property.propertyName }}</h2>
            <el-tag
              :type="property.status === '在售' ? 'success' : 'info'"
              size="small"
            >{{ property.status }}</el-tag>
          </div>
          <div class="price-tag">{{ formatPrice(property.totalPrice) }}</div>
        </div>

        <el-divider />

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">位置</span>
            <span class="info-value">{{ property.location }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">户型</span>
            <span class="info-value">{{ property.floorPlanType }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">面积</span>
            <span class="info-value">{{ property.areaSqm }} ㎡</span>
          </div>
          <div class="info-item">
            <span class="info-label">单价</span>
            <span class="info-value">{{ formatPrice(property.pricePerSqm) }}/㎡</span>
          </div>
          <div class="info-item">
            <span class="info-label">装修情况</span>
            <span class="info-value">{{ property.decoration }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">楼层</span>
            <span class="info-value">{{ property.floorMin }} - {{ property.floorMax }} 层</span>
          </div>
          <div class="info-item">
            <span class="info-label">总户数</span>
            <span class="info-value">{{ property.totalUnits }} 户</span>
          </div>
          <div class="info-item">
            <span class="info-label">空置率</span>
            <span class="info-value">
              <el-tag
                :type="property.vacancyRate > 15 ? 'danger' : property.vacancyRate > 8 ? 'warning' : 'success'"
                size="small"
              >{{ property.vacancyRate?.toFixed(1) }}%</el-tag>
            </span>
          </div>
        </div>

        <el-divider />

        <div class="description-section">
          <h3>楼盘描述</h3>
          <p>{{ property.description }}</p>
        </div>

        <div class="action-bar" v-if="property.status === '在售'">
          <el-button
            type="primary"
            size="large"
            :loading="ordering"
            @click="handleBuy"
          >
            立即购买
          </el-button>
          <el-button size="large" @click="router.push('/customer/properties')">
            继续浏览
          </el-button>
        </div>
        <div class="action-bar" v-else>
          <el-tag type="info" size="large">该房产暂不可购买</el-tag>
        </div>
      </el-card>
    </div>

    <div v-if="!loading && !property" class="empty-state">
      <el-empty description="房产信息不存在" />
      <el-button type="primary" @click="router.push('/customer/properties')">
        返回查询列表
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.property-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.detail-content {
  margin-top: 8px;
}

.detail-card {
  border-radius: 10px;
  padding: 8px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-section h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.price-tag {
  font-size: 28px;
  font-weight: 700;
  color: #1a73e8;
  white-space: nowrap;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: #1f2937;
  font-weight: 500;
}

.description-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px;
}

.description-section p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.8;
  margin: 0;
}

.action-bar {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .detail-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
