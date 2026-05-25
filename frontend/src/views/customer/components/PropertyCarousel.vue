<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  imageUrls: string
  propertyName: string
}>()

const images = computed(() => {
  if (!props.imageUrls) {
    // 没有图片时返回默认占位图（picsum.photos）
    return [`https://picsum.photos/seed/${encodeURIComponent(props.propertyName || 'default')}/800/450`]
  }
  const urls = props.imageUrls.split(',').map(u => u.trim()).filter(Boolean)
  if (urls.length === 0) {
    return [`https://picsum.photos/seed/${encodeURIComponent(props.propertyName || 'default')}/800/450`]
  }
  return urls
})
</script>

<template>
  <div class="property-carousel">
    <el-carousel
      height="450px"
      :interval="4000"
      arrow="always"
      indicator-position="none"
      trigger="click"
    >
      <el-carousel-item v-for="(url, index) in images" :key="index">
        <div
          class="carousel-image"
          :style="{ backgroundImage: `url(${url})` }"
        >
          <div class="image-overlay">
            <span class="image-counter">{{ index + 1 }} / {{ images.length }}</span>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 底部缩略图指示器 -->
    <div class="thumbnail-strip">
      <div
        v-for="(url, index) in images"
        :key="'thumb-' + index"
        class="thumbnail-item"
        :class="{ active: false }"
        :style="{ backgroundImage: `url(${url})` }"
      >
      </div>
    </div>
  </div>
</template>

<style scoped>
.property-carousel {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  background: #f5f0ea;
  margin-bottom: 16px;
}

.carousel-image {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
}

.image-overlay {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.thumbnail-strip {
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  background: #fff;
  overflow-x: auto;
  justify-content: center;
}

.thumbnail-item {
  width: 56px;
  height: 36px;
  border-radius: 4px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.2s;
  flex-shrink: 0;
}

.thumbnail-item:hover {
  border-color: #f5a623;
  transform: translateY(-1px);
}

.thumbnail-item.active {
  border-color: #f5a623;
}

.el-carousel {
  --el-carousel-arrow-font-size: 20px;
  --el-carousel-arrow-size: 36px;
  --el-carousel-arrow-background: rgba(0, 0, 0, 0.3);
}

:deep(.el-carousel__arrow) {
  border-radius: 50%;
}
</style>
