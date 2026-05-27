<script setup lang="ts">
const props = defineProps<{
  propertyName?: string
  location?: string
  floorPlanType?: string
  areaSqm?: number
  totalPrice?: number
  pricePerSqm?: number
  customerName?: string
  customerPhone?: string
  orderNo?: string
}>()

function formatPrice(price: number | undefined | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}
</script>

<template>
  <div class="order-summary-card">
    <div class="order-title">{{ orderNo ? '订单已确认' : '订单预览' }}</div>
    <div v-if="orderNo" class="order-no">订单号: {{ orderNo }}</div>
    <div class="order-body">
      <div class="order-row">
        <span class="order-label">楼盘</span>
        <span class="order-value">{{ propertyName || '-' }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">位置</span>
        <span class="order-value">{{ location || '-' }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">户型</span>
        <span class="order-value">{{ floorPlanType || '-' }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">面积</span>
        <span class="order-value">{{ areaSqm ? areaSqm + '㎡' : '-' }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">单价</span>
        <span class="order-value">{{ formatPrice(pricePerSqm) }}/㎡</span>
      </div>
      <div class="order-row">
        <span class="order-label">总价</span>
        <span class="order-value price">{{ formatPrice(totalPrice) }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">客户</span>
        <span class="order-value">{{ customerName || '-' }}</span>
      </div>
      <div class="order-row">
        <span class="order-label">电话</span>
        <span class="order-value">{{ customerPhone || '-' }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-summary-card {
  background: #fef7ed;
  border: 2px solid #f5a623;
  border-radius: 10px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box;
}

.order-title {
  font-size: 14px;
  font-weight: 600;
  color: #f5a623;
  margin-bottom: 4px;
}

.order-no {
  font-size: 12px;
  color: #8a7a6a;
  margin-bottom: 8px;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-label {
  font-size: 13px;
  color: #8a7a6a;
}

.order-value {
  font-size: 13px;
  color: #4a3728;
  font-weight: 500;
  text-align: right;
}

.order-value.price {
  color: #f5a623;
  font-weight: 700;
  font-size: 14px;
}
</style>
