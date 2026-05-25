<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { propertyApi } from '../../api/property'
import type { Property, PropertyQuery } from '../../types'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const propertyList = ref<Property[]>([])
const total = ref(0)

/** 查询参数 */
const queryParams = reactive<PropertyQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  floorPlanType: '',
  sortBy: 'created_time',
  sortOrder: 'desc'
})

/** 状态选项 */
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: '在售', label: '在售' },
  { value: '已售', label: '已售' },
  { value: '待开盘', label: '待开盘' }
]

/** 户型选项 */
const typeOptions = [
  { value: '', label: '全部户型' },
  { value: '一室一厅', label: '一室一厅' },
  { value: '两室一厅', label: '两室一厅' },
  { value: '三室两厅', label: '三室两厅' },
  { value: '四室两厅', label: '四室两厅' },
  { value: '复式', label: '复式' },
  { value: '别墅', label: '别墅' }
]

async function fetchProperties() {
  loading.value = true
  try {
    const res = await propertyApi.getMyList({ ...queryParams })
    propertyList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取房产列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  fetchProperties()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.floorPlanType = ''
  queryParams.page = 1
  fetchProperties()
}

function handlePageChange(page: number) {
  queryParams.page = page
  fetchProperties()
}

function handleSizeChange(size: number) {
  queryParams.pageSize = size
  queryParams.page = 1
  fetchProperties()
}

function handleSortChange({ prop, order }: { prop: string; order: string }) {
  if (prop && order) {
    queryParams.sortBy = prop
    queryParams.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  } else {
    queryParams.sortBy = 'created_time'
    queryParams.sortOrder = 'desc'
  }
  fetchProperties()
}

function goToCreate() {
  router.push('/developer/properties/create')
}

function goToEdit(id: number) {
  router.push(`/developer/properties/${id}/edit`)
}

async function handleDelete(id: number, name: string) {
  try {
    await ElMessageBox.confirm(`确定要删除房产"${name}"吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await propertyApi.delete(id)
    ElMessage.success('删除成功')
    fetchProperties()
  } catch {
    // 用户取消删除
  }
}

/** 格式化空置率显示 */
function formatVacancyRate(rate: number | null): string {
  if (rate === null || rate === undefined) return '-'
  return rate.toFixed(1) + '%'
}

/** 格式化金额 */
function formatPrice(price: number | null): string {
  if (!price) return '-'
  return '¥' + price.toLocaleString('zh-CN')
}

onMounted(fetchProperties)
</script>

<template>
  <div class="property-list-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">房产管理</h2>
        <p class="page-desc">管理您的楼盘信息，共 {{ total }} 个楼盘</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="goToCreate">添加房产</el-button>
    </div>

    <!-- 搜索区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" label-width="0" inline>
        <el-form-item>
          <el-input v-model="queryParams.keyword" placeholder="搜索楼盘名称/位置" clearable :prefix-icon="Search" style="width: 220px;" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px;" @change="handleSearch">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="queryParams.floorPlanType" placeholder="户型" clearable style="width: 120px;" @change="handleSearch">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 房产列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="propertyList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
        empty-text="暂无房产信息"
      >
        <el-table-column prop="propertyName" label="楼盘名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column prop="floorPlanType" label="户型" width="100" align="center" />
        <el-table-column prop="totalUnits" label="总户数" width="80" align="center" sortable="custom" />
        <el-table-column prop="vacancyRate" label="空置率" width="90" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.vacancyRate > 15 ? 'danger' : row.vacancyRate > 8 ? 'warning' : 'success'" size="small">
              {{ formatVacancyRate(row.vacancyRate) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pricePerSqm" label="单价" width="130" align="right" sortable="custom">
          <template #default="{ row }">{{ formatPrice(row.pricePerSqm) }}/㎡</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '在售' ? 'success' : row.status === '待开盘' ? 'warning' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" sortable="custom">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToEdit(row.id)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row.id, row.propertyName)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && propertyList.length === 0" class="empty-state">
        <el-empty description="暂无房产信息，点击上方按钮添加" />
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
  color: #1f2937;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.search-card .el-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

.empty-state {
  padding: 40px 0;
}
</style>
