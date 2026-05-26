<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { developerApi } from '../../api/developer'
import { propertyApi } from '../../api/property'
import type { Developer, Property } from '../../types'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const developerId = Number(route.params.id)

const loading = ref(true)
const developer = ref<Developer | null>(null)
const properties = ref<Property[]>([])

async function loadData() {
  loading.value = true
  try {
    const [devRes, propRes] = await Promise.all([
      developerApi.getById(developerId),
      propertyApi.list({ developerId, page: 1, pageSize: 100 })
    ])
    developer.value = devRes.data
    properties.value = propRes.data?.records || []
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  return '¥' + (price / 10000).toFixed(0) + '万'
}

function goBack() {
  router.push('/customer/developers')
}

function goToSubmitSuggestion() {
  router.push(`/customer/suggestions/new?developerId=${developerId}`)
}

onMounted(loadData)
</script>

<template>
  <div class="developer-detail-page">
    <el-button text :icon="ArrowLeft" @click="goBack" class="back-btn">返回列表</el-button>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="rect" style="height: 200px; border-radius: 10px;" />
        <el-skeleton-item variant="rect" style="height: 300px; border-radius: 10px; margin-top: 16px;" />
      </template>
    </el-skeleton>

    <template v-if="!loading && developer">
      <!-- 公司信息 -->
      <el-card shadow="never" class="info-card">
        <div class="company-header">
          <el-avatar :size="64" style="background-color: #f5a623; font-size: 28px;">
            {{ developer.companyName?.charAt(0) || '?' }}
          </el-avatar>
          <div class="company-meta">
            <h2 class="company-name">{{ developer.companyName }}</h2>
            <el-button type="primary" @click="goToSubmitSuggestion">提交购房意向</el-button>
          </div>
        </div>

        <el-descriptions :column="2" border class="company-info" size="small">
          <el-descriptions-item label="联系人">{{ developer.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ developer.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ developer.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="营业执照">{{ developer.businessLicense || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公司地址" :span="2">{{ developer.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公司简介" :span="2">
            <p style="margin: 0; line-height: 1.6;">{{ developer.description || '暂无简介' }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 该开发商的楼盘列表 -->
      <el-card shadow="never" class="property-card">
        <h3 class="section-title">旗下楼盘（{{ properties.length }}个）</h3>
        <el-table :data="properties" stripe empty-text="暂无楼盘信息">
          <el-table-column prop="propertyName" label="楼盘名称" min-width="160" />
          <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
          <el-table-column prop="floorPlanType" label="户型" width="100" align="center" />
          <el-table-column prop="areaSqm" label="面积(㎡)" width="100" align="right" />
          <el-table-column prop="totalPrice" label="总价" width="120" align="right">
            <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '在售' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.back-btn {
  margin-bottom: 16px;
  font-size: 14px;
}

.info-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.company-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0e8e0;
}

.company-meta {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.company-name {
  font-size: 22px;
  font-weight: 600;
  color: #4a3728;
  margin: 0;
}

.company-info {
  margin-top: 8px;
}

.property-card {
  border-radius: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 16px;
}
</style>
