<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { propertyApi } from '../../api/property'
import { orderApi } from '../../api/order'
import type { PropertyDetail, PriceHistoryItem } from '../../types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Tickets } from '@element-plus/icons-vue'
import PropertyCarousel from './components/PropertyCarousel.vue'
import PriceTrendChart from './components/PriceTrendChart.vue'
import PropertyLocation from './components/PropertyLocation.vue'
import FavoriteButton from './components/FavoriteButton.vue'

const route = useRoute()
const router = useRouter()

const property = ref<PropertyDetail | null>(null)
const priceHistory = ref<PriceHistoryItem[]>([])
const loading = ref(true)
const priceLoading = ref(true)
const ordering = ref(false)
const activeTab = ref('detail')

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

async function fetchPriceHistory() {
  priceLoading.value = true
  try {
    const id = Number(route.params.id)
    const res = await propertyApi.getPriceHistory(id, 24)
    priceHistory.value = res.data || []
  } catch (error) {
    console.error('获取价格历史失败:', error)
    priceHistory.value = []
  } finally {
    priceLoading.value = false
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

const infoItems = computed(() => {
  if (!property.value) return []
  return [
    { label: '位置', value: property.value.location },
    { label: '户型', value: property.value.floorPlanType },
    { label: '面积', value: property.value.areaSqm + ' ㎡' },
    { label: '单价', value: formatPrice(property.value.pricePerSqm) + '/㎡' },
    { label: '装修情况', value: property.value.decoration },
    { label: '楼层', value: property.value.floorMin + ' - ' + property.value.floorMax + ' 层' },
    { label: '总户数', value: property.value.totalUnits + ' 户' },
    { label: '空置率', value: property.value.vacancyRate?.toFixed(1) + '%' },
    { label: '开发商', value: property.value.developerName },
    { label: '状态', value: property.value.status }
  ]
})

onMounted(() => {
  fetchDetail()
  fetchPriceHistory()
})
</script>

<template>
  <div class="property-detail-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.push('/customer/properties')">
        返回查询
      </el-button>
    </div>

    <!-- 加载骨架 -->
    <template v-if="loading">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="rect" style="width:100%;height:450px;border-radius:12px;margin-bottom:16px;" />
          <el-skeleton-item variant="rect" style="height:200px;border-radius:10px;" />
        </template>
      </el-skeleton>
    </template>

    <!-- 房产不存在 -->
    <div v-if="!loading && !property" class="empty-state">
      <el-empty description="房产信息不存在" />
      <el-button type="primary" @click="router.push('/customer/properties')">
        返回查询列表
      </el-button>
    </div>

    <template v-if="!loading && property">
      <!-- 图片轮播 -->
      <PropertyCarousel
        :image-urls="property.imageUrls"
        :property-name="property.propertyName"
      />

      <!-- 头部信息 -->
      <el-card shadow="never" class="detail-header-card">
        <div class="detail-header">
          <div class="title-section">
            <h2>{{ property.propertyName }}</h2>
            <el-tag
              :type="property.status === '在售' ? 'success' : 'info'"
              size="small"
              class="status-tag"
            >{{ property.status }}</el-tag>
            <el-tag
              v-if="property.developerName"
              type="warning"
              size="small"
              class="developer-tag"
            >{{ property.developerName }}</el-tag>
          </div>
          <div class="price-tag">{{ formatPrice(property.totalPrice) }}</div>
        </div>
        <p class="location-sub">{{ property.location }}</p>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button
            v-if="property.status === '在售'"
            type="primary"
            size="large"
            :loading="ordering"
            @click="handleBuy"
          >
            立即购买
          </el-button>
          <FavoriteButton
            v-if="property"
            :property-id="property.id"
            :initial-favorited="property.favorited"
          />
          <el-button size="large" @click="router.push('/customer/properties')">
            继续浏览
          </el-button>
          <el-button
            size="large"
            text
            @click="router.push('/customer/orders')"
            :icon="Tickets"
          >
            我的订单
          </el-button>
        </div>
      </el-card>

      <!-- Tab 分层布局 -->
      <el-card shadow="never" class="tab-card">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <!-- Tab 1: 楼盘详情 -->
          <el-tab-pane label="楼盘详情" name="detail">
            <div class="info-grid">
              <div v-for="item in infoItems" :key="item.label" class="info-item">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value" :class="{ 'status-success': item.label === '状态' && item.value === '在售' }">
                  {{ item.value }}
                  <el-tag
                    v-if="item.label === '空置率'"
                    :type="Number(property.vacancyRate) > 15 ? 'danger' : Number(property.vacancyRate) > 8 ? 'warning' : 'success'"
                    size="small"
                    style="margin-left: 6px;"
                  >{{ property.vacancyRate?.toFixed(1) }}%</el-tag>
                </span>
              </div>
            </div>

            <el-divider />

            <div class="description-section">
              <h3>楼盘描述</h3>
              <p>{{ property.description || '暂无描述信息' }}</p>
            </div>
          </el-tab-pane>

          <!-- Tab 2: 价格走势 -->
          <el-tab-pane label="价格走势" name="price">
            <PriceTrendChart :data="priceHistory" :loading="priceLoading" />
          </el-tab-pane>

          <!-- Tab 3: 位置周边 -->
          <el-tab-pane label="位置周边" name="location">
            <PropertyLocation
              :location="property.location"
              :longitude="property.longitude"
              :latitude="property.latitude"
              :property-name="property.propertyName"
            />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.property-detail-page {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.detail-header-card {
  border-radius: 10px;
  margin-bottom: 16px;
  padding: 8px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title-section h2 {
  font-size: 24px;
  font-weight: 700;
  color: #4a3728;
  margin: 0;
}

.status-tag {
  font-size: 12px;
}

.developer-tag {
  font-size: 11px;
}

.price-tag {
  font-size: 28px;
  font-weight: 700;
  color: #f5a623;
  white-space: nowrap;
}

.location-sub {
  font-size: 13px;
  color: #8a7a6a;
  margin: 6px 0 0;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.tab-card {
  border-radius: 10px;
  min-height: 300px;
}

.detail-tabs {
  --el-tabs-header-height: 44px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: #8a7a6a;
  padding: 0 20px;
}

:deep(.el-tabs__item.is-active) {
  color: #f5a623;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: #f5a623;
  height: 3px;
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
  padding: 12px;
  background: #fdf8f3;
  border-radius: 8px;
  transition: background 0.2s;
}

.info-item:hover {
  background: #faf3ea;
}

.info-label {
  font-size: 12px;
  color: #b0a090;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: #4a3728;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.info-value.status-success {
  color: #34a853;
}

.description-section {
  padding: 4px 0;
}

.description-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 8px;
}

.description-section p {
  font-size: 14px;
  color: #8a7a6a;
  line-height: 1.8;
  margin: 0;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .detail-header {
    flex-direction: column;
    gap: 8px;
  }
  .price-tag {
    font-size: 22px;
  }
  .title-section h2 {
    font-size: 20px;
  }
}
</style>
