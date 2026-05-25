<script setup lang="ts">
import { ref, computed } from 'vue'
import { Location } from '@element-plus/icons-vue'

const props = defineProps<{
  location: string
  longitude: number | null
  latitude: number | null
  propertyName: string
}>()

// 周边地标数据（模拟）
const nearbyLandmarks = computed(() => {
  if (!props.location) return []
  const landmarks: Record<string, Array<{ name: string, distance: string, type: string }>> = {
    '株洲市天元区': [
      { name: '神农城商圈', distance: '1.2km', type: '商业' },
      { name: '湖南工业大学', distance: '1.5km', type: '教育' },
      { name: '株洲市中心医院', distance: '2.0km', type: '医疗' },
      { name: '神农湖公园', distance: '0.8km', type: '公园' }
    ],
    '株洲市芦淞区': [
      { name: '芦淞商圈', distance: '0.5km', type: '商业' },
      { name: '株洲市一中', distance: '1.0km', type: '教育' },
      { name: '株洲市人民医院', distance: '1.5km', type: '医疗' },
      { name: '湘江风光带', distance: '0.3km', type: '公园' }
    ],
    '株洲市荷塘区': [
      { name: '荷塘商圈', distance: '0.8km', type: '商业' },
      { name: '株洲市四中', distance: '1.2km', type: '教育' },
      { name: '荷塘公园', distance: '0.5km', type: '公园' }
    ],
    '株洲市石峰区': [
      { name: '石峰公园', distance: '0.6km', type: '公园' },
      { name: '株洲市九方中学', distance: '1.0km', type: '教育' },
      { name: '田心商圈', distance: '1.5km', type: '商业' }
    ],
    '广州市': [
      { name: '天河城商圈', distance: '1.0km', type: '商业' },
      { name: '中山大学', distance: '3.0km', type: '教育' },
      { name: '珠江公园', distance: '1.5km', type: '公园' },
      { name: '中山三院', distance: '2.0km', type: '医疗' }
    ],
    '深圳市': [
      { name: '万象天地', distance: '1.0km', type: '商业' },
      { name: '深圳大学', distance: '2.0km', type: '教育' },
      { name: '人才公园', distance: '1.0km', type: '公园' },
      { name: '港大深圳医院', distance: '2.5km', type: '医疗' }
    ]
  }

  for (const [key, value] of Object.entries(landmarks)) {
    if (props.location && props.location.includes(key)) {
      return value
    }
  }
  return [
    { name: '周边商圈', distance: '1-2km', type: '商业' },
    { name: '附近学校', distance: '1-3km', type: '教育' },
    { name: '邻近医院', distance: '2-3km', type: '医疗' },
    { name: '附近公园', distance: '0.5-2km', type: '公园' }
  ]
})

// 高德/百度地图静态图URL（模拟）
const mapUrl = computed(() => {
  if (props.longitude && props.latitude) {
    return `https://picsum.photos/seed/map-${props.propertyName}/800/300`
  }
  return null
})

const typeColors: Record<string, string> = {
  '商业': '#f5a623',
  '教育': '#9334e6',
  '医疗': '#ef4444',
  '公园': '#34a853'
}
</script>

<template>
  <div class="property-location">
    <!-- 地图占位 -->
    <div class="map-placeholder" v-if="mapUrl">
      <img :src="mapUrl" :alt="propertyName + '位置'" class="map-image" />
      <div class="map-marker" v-if="longitude && latitude">
        <el-icon :size="28" color="#ef4444"><Location /></el-icon>
        <span class="marker-label">{{ propertyName }}</span>
      </div>
    </div>
    <div class="map-placeholder map-fallback" v-else>
      <el-icon :size="48" color="#b0a090"><Location /></el-icon>
      <p class="map-fallback-text">暂无地理位置信息</p>
    </div>

    <!-- 地址信息 -->
    <div class="address-section">
      <h4 class="section-subtitle">详细地址</h4>
      <p class="address-text">
        <el-icon><Location /></el-icon>
        {{ location }}
      </p>
      <p class="coords-text" v-if="longitude && latitude">
        经度 {{ longitude }} | 纬度 {{ latitude }}
      </p>
    </div>

    <!-- 周边地标 -->
    <div class="landmarks-section">
      <h4 class="section-subtitle">周边配套</h4>
      <div class="landmarks-grid">
        <div
          v-for="(landmark, index) in nearbyLandmarks"
          :key="index"
          class="landmark-item"
        >
          <span
            class="landmark-type"
            :style="{ backgroundColor: (typeColors[landmark.type] || '#8a7a6a') + '15', color: typeColors[landmark.type] || '#8a7a6a' }"
          >
            {{ landmark.type }}
          </span>
          <span class="landmark-name">{{ landmark.name }}</span>
          <span class="landmark-distance">{{ landmark.distance }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.property-location {
  padding: 8px 0;
}

.map-placeholder {
  position: relative;
  width: 100%;
  height: 250px;
  border-radius: 10px;
  overflow: hidden;
  background: #f0ebe5;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.map-marker {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  animation: marker-bounce 2s infinite;
}

@keyframes marker-bounce {
  0%, 100% { transform: translate(-50%, -100%); }
  50% { transform: translate(-50%, -110%); }
}

.marker-label {
  background: #4a3728;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
}

.map-fallback {
  flex-direction: column;
  gap: 8px;
}

.map-fallback-text {
  color: #b0a090;
  font-size: 14px;
  margin: 0;
}

.section-subtitle {
  font-size: 14px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 10px;
}

.address-section {
  margin-bottom: 20px;
}

.address-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #4a3728;
  margin: 0 0 4px;
}

.coords-text {
  font-size: 12px;
  color: #8a7a6a;
  margin: 0 0 0 28px;
}

.landmarks-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.landmark-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fdf8f3;
  border-radius: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}

.landmark-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.landmark-type {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}

.landmark-name {
  flex: 1;
  font-size: 13px;
  color: #4a3728;
}

.landmark-distance {
  font-size: 12px;
  color: #8a7a6a;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .landmarks-grid {
    grid-template-columns: 1fr;
  }
  .map-placeholder {
    height: 180px;
  }
}
</style>
