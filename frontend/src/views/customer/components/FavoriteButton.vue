<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { favoriteApi } from '../../../api/favorite'

const props = defineProps<{
  propertyId: number
  initialFavorited?: boolean
}>()

const emit = defineEmits<{
  change: [favorited: boolean]
}>()

const favorited = ref(props.initialFavorited ?? false)
const loading = ref(false)

async function toggleFavorite() {
  loading.value = true
  try {
    if (favorited.value) {
      await favoriteApi.remove(props.propertyId)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(props.propertyId)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
    emit('change', favorited.value)
  } catch (error: any) {
    if (error?.response?.status === 409) {
      favorited.value = true
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (props.initialFavorited === undefined) {
    try {
      const res = await favoriteApi.getStatus(props.propertyId)
      favorited.value = res.data.favorited
    } catch {
      // 静默失败，默认未收藏
    }
  }
})
</script>

<template>
  <el-button
    :type="favorited ? 'warning' : 'default'"
    :icon="favorited ? StarFilled : Star"
    :loading="loading"
    :class="{ 'is-favorited': favorited }"
    @click="toggleFavorite"
  >
    {{ favorited ? '已收藏' : '收藏' }}
  </el-button>
</template>

<style scoped>
.is-favorited {
  --el-button-bg-color: #fff7e6;
  --el-button-border-color: #f5a623;
  --el-button-text-color: #f5a623;
}
</style>
