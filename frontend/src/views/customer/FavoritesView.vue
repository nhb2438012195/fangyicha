<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '../../api/favorite'
import type { FavoriteItem } from '../../types'
import { Refresh, ArrowLeft } from '@element-plus/icons-vue'
import FavoriteCard from './components/FavoriteCard.vue'

const router = useRouter()
const favorites = ref<FavoriteItem[]>([])
const loading = ref(true)

async function fetchFavorites() {
  loading.value = true
  try {
    const res = await favoriteApi.getList()
    favorites.value = res.data || []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    favorites.value = []
  } finally {
    loading.value = false
  }
}

function goToProperty(id: number) {
  router.push(`/customer/properties/${id}`)
}

onMounted(fetchFavorites)
</script>

<template>
  <div class="favorites-page">
    <div class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.push('/customer/dashboard')">
        返回
      </el-button>
      <div class="header-content">
        <h2 class="page-title">我的收藏</h2>
        <p class="page-desc">您收藏的楼盘，共 {{ favorites.length }} 个</p>
      </div>
      <el-button
        text
        :icon="Refresh"
        :loading="loading"
        @click="fetchFavorites"
      >
        刷新
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
          <el-skeleton-item variant="rect" style="height: 280px;" v-for="i in 6" :key="i" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading && favorites.length > 0" class="favorites-grid">
      <FavoriteCard
        v-for="item in favorites"
        :key="item.propertyId"
        :item="item"
        @click="goToProperty"
      />
    </div>

    <div v-if="!loading && favorites.length === 0" class="empty-state">
      <el-empty description="暂无收藏的楼盘，去浏览楼盘吧" :image-size="80">
        <template #image>
          <svg viewBox="0 0 100 100" width="80" height="80" fill="none">
            <path d="M50 20 L50 80 M20 50 L80 50" stroke="#e8ddd0" stroke-width="2" stroke-linecap="round"/>
            <circle cx="50" cy="50" r="40" stroke="#e8ddd0" stroke-width="2" />
          </svg>
        </template>
        <el-button type="primary" @click="router.push('/customer/properties')">
          浏览楼盘
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.favorites-page {
  width: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.header-content {
  flex: 1;
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

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}

@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
