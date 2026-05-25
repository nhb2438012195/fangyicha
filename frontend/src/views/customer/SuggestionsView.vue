<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { suggestionApi } from '../../api/suggestion'
import type { Suggestion } from '../../types'

const router = useRouter()
const loading = ref(false)
const suggestionList = ref<Suggestion[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

async function fetchSuggestions() {
  loading.value = true
  try {
    const res = await suggestionApi.getMyList({ page: page.value, pageSize: pageSize.value })
    suggestionList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取建议列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  fetchSuggestions()
}

function getStatusType(status: string) {
  switch (status) {
    case '待回复': return 'warning'
    case '已回复': return 'success'
    case '已关闭': return 'info'
    default: return ''
  }
}

onMounted(fetchSuggestions)
</script>

<template>
  <div class="customer-suggestions-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的建议</h2>
        <p class="page-desc">查看您向开发商提交的购房意向及回复</p>
      </div>
      <el-button type="primary" @click="router.push('/customer/suggestions/new')">提交新建议</el-button>
    </div>

    <el-card shadow="never" class="list-card">
      <el-table :data="suggestionList" v-loading="loading" stripe empty-text="您还没有提交过购房意向">
        <el-table-column prop="developerName" label="开发商" min-width="160" />
        <el-table-column prop="preferredType" label="偏好户型" width="120" align="center">
          <template #default="{ row }">{{ row.preferredType || '不限' }}</template>
        </el-table-column>
        <el-table-column label="预算范围" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.priceMin || row.priceMax">
              ¥{{ (row.priceMin || 0).toLocaleString() }} - ¥{{ (row.priceMax || 0).toLocaleString() }}
            </span>
            <span v-else class="text-muted">不限</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.replyContent">{{ row.replyContent }}</span>
            <span v-else class="text-muted">等待回复...</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
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

.list-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

.text-muted {
  color: #b0a090;
}
</style>
