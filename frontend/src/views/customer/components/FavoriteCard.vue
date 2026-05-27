<script setup lang="ts">
import type { FavoriteItem } from '../../../types'

defineProps<{
  item: FavoriteItem
}>()

const emit = defineEmits<{
  click: [propertyId: number]
}>()

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}
</script>

<template>
  <el-card
    shadow="never"
    class="favorite-card"
    @click="emit('click', item.propertyId)"
  >
    <div class="card-image">
      <img
        :src="item.imageUrl || `https://picsum.photos/seed/${encodeURIComponent(item.propertyName)}/400/300`"
        :alt="item.propertyName"
        loading="lazy"
      />
      <div class="card-overlay">
        <span class="overlay-text">查看详情</span>
      </div>
    </div>
    <div class="card-body">
      <div class="card-title">{{ item.propertyName }}</div>
      <div class="card-location">{{ item.location }}</div>
      <div class="card-meta">
        <span class="meta-type">{{ item.floorPlanType }}</span>
        <span class="meta-area">{{ item.areaSqm }}㎡</span>
        <span class="meta-decoration">{{ item.decoration }}</span>
      </div>
      <div class="card-price-row">
        <span class="card-price">{{ formatPrice(item.totalPrice) }}</span>
        <span class="card-unit-price">{{ formatPrice(item.pricePerSqm) }}/㎡</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.favorite-card {
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  border: 1px solid #e8ddd0;
}

.favorite-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-image {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #f0ebe5;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.favorite-card:hover .card-image img {
  transform: scale(1.05);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.25s;
}

.favorite-card:hover .card-overlay {
  background: rgba(0, 0, 0, 0.15);
}

.overlay-text {
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.5);
  padding: 4px 14px;
  border-radius: 20px;
  opacity: 0;
  transform: translateY(8px);
  transition: all 0.25s;
}

.favorite-card:hover .overlay-text {
  opacity: 1;
  transform: translateY(0);
}

.card-body {
  padding: 12px 14px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-location {
  font-size: 12px;
  color: #8a7a6a;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.card-meta span {
  font-size: 11px;
  color: #8a7a6a;
  background: #f5f0ea;
  padding: 2px 6px;
  border-radius: 3px;
}

.card-price-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #f5a623;
}

.card-unit-price {
  font-size: 12px;
  color: #b0a090;
}
</style>
