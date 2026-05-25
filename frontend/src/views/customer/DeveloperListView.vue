<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { developerApi } from '../../api/developer'
import type { Developer } from '../../types'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const developerList = ref<Developer[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function fetchDevelopers() {
  loading.value = true
  try {
    const res = await developerApi.list({ keyword: keyword.value, page: page.value, pageSize: pageSize.value })
    developerList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取开发商列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchDevelopers()
}

function handlePageChange(p: number) {
  page.value = p
  fetchDevelopers()
}

function goToDetail(id: number) {
  router.push(`/customer/developers/${id}`)
}

onMounted(fetchDevelopers)
</script>

<template>
  <div class="developer-list-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">开发商列表</h2>
        <p class="page-desc">浏览所有合作的开发商，了解更多楼盘信息</p>
      </div>
    </div>

    <!-- 搜索 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="{ keyword }" inline @keyup.enter="handleSearch">
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索公司名称" clearable :prefix-icon="Search" style="width: 300px;" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-skeleton :loading="loading" animated :count="4">
      <template #template>
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;">
          <el-skeleton-item variant="card" style="height: 160px;" v-for="i in 4" :key="i" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading && developerList.length === 0" class="empty-state">
      <el-empty description="没有找到匹配的开发商" />
    </div>

    <div v-if="!loading && developerList.length > 0" class="developer-grid">
      <el-card
        v-for="dev in developerList"
        :key="dev.id"
        shadow="hover"
        class="developer-card"
        @click="goToDetail(dev.id)"
      >
        <div class="card-content">
          <div class="card-avatar">
            <el-avatar :size="56" style="background-color: #f5a623; font-size: 24px;">
              {{ dev.companyName?.charAt(0) || '?' }}
            </el-avatar>
          </div>
          <div class="card-info">
            <h3 class="company-name">{{ dev.companyName }}</h3>
            <div class="contact-row">
              <span class="label">联系人：</span>
              <span>{{ dev.contactPerson || '-' }}</span>
            </div>
            <div class="contact-row">
              <span class="label">电话：</span>
              <span>{{ dev.phone || '-' }}</span>
            </div>
            <p class="company-desc">{{ dev.description?.slice(0, 80) }}{{ (dev.description?.length || 0) > 80 ? '...' : '' }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
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

.search-card {
  margin-bottom: 20px;
  border-radius: 10px;
}

.developer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.developer-card {
  cursor: pointer;
  border-radius: 10px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.developer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  gap: 16px;
}

.card-avatar {
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.company-name {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 8px;
}

.contact-row {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 4px;
}

.contact-row .label {
  color: #8a7a6a;
}

.company-desc {
  font-size: 12px;
  color: #8a7a6a;
  margin: 8px 0 0;
  line-height: 1.5;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 768px) {
  .developer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
