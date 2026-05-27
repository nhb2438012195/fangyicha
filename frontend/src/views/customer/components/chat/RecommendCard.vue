<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  propertyId: number
  propertyName: string
  location: string
  floorPlanType: string
  areaSqm: number
  totalPrice: number
  pricePerSqm: number
  imageUrl?: string
  developerName?: string
  reason?: string
}>()

const router = useRouter()

function formatPrice(price: number): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

function handleClick() {
  router.push(`/customer/properties/${props.propertyId}`)
}

const areaText = computed(() => {
  return props.areaSqm ? props.areaSqm + '㎡' : ''
})
</script>

<template>
  <div class="recommend-card" @click="handleClick">
    <div class="card-thumb">
      <div v-if="imageUrl" class="thumb-img" :style="{ backgroundImage: `url(${imageUrl})` }" />
      <div v-else class="thumb-placeholder">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#c4b5a5" stroke-width="1.5">
          <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
      </div>
    </div>
    <div class="card-info">
      <div class="card-name">{{ propertyName }}</div>
      <div class="card-location">{{ location }}</div>
      <div class="card-detail">
        <span>{{ floorPlanType }}</span>
        <span v-if="areaText" class="detail-dot">{{ areaText }}</span>
      </div>
      <div class="card-price">{{ formatPrice(totalPrice) }}</div>
      <div v-if="reason" class="card-reason">{{ reason }}</div>
    </div>
  </div>
</template>

<style scoped>
.recommend-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid #e8ddd0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  width: 100%;
  box-sizing: border-box;
}

.recommend-card:hover {
  border-color: #f5a623;
  box-shadow: 0 2px 8px rgba(245, 166, 35, 0.1);
  transform: translateY(-1px);
}

.card-thumb {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  background: #f5f0ea;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #4a3728;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-location {
  font-size: 12px;
  color: #8a7a6a;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-detail {
  font-size: 12px;
  color: #8a7a6a;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-dot::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 3px;
  background: #d1c8b8;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.card-price {
  font-size: 14px;
  font-weight: 700;
  color: #f5a623;
  margin-top: 3px;
}

.card-reason {
  display: inline-block;
  font-size: 11px;
  color: #f5a623;
  background: #fef7ed;
  padding: 1px 6px;
  border-radius: 10px;
  margin-top: 3px;
}
</style>
