<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { propertyApi } from '../../api/property'
import { FLOOR_PLAN_TYPES, PROPERTY_STATUSES } from '../../types'
import type { Property, PropertyQuery } from '../../types'
import { Search, Refresh, Download, Printer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const propertyList = ref<Property[]>([])
const total = ref(0)

/** 查询表单 */
const queryForm = reactive<PropertyQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  floorPlanType: '',
  totalPriceMin: undefined,
  totalPriceMax: undefined,
  location: '',
  status: '在售',
  sortBy: 'total_price',
  sortOrder: 'asc'
})

/** 户型选项 */
const typeOptions = [{ value: '', label: '不限' }, ...FLOOR_PLAN_TYPES.map(t => ({ value: t, label: t }))]

async function fetchProperties() {
  loading.value = true
  try {
    const res = await propertyApi.list({ ...queryForm })
    propertyList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('查询失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.page = 1
  // 将搜索参数同步到 URL 查询字符串，支持分享和浏览器前进/后退
  const query: Record<string, any> = {}
  if (queryForm.keyword) query.keyword = queryForm.keyword
  if (queryForm.location) query.location = queryForm.location
  if (queryForm.floorPlanType) query.floorPlanType = queryForm.floorPlanType
  if (queryForm.totalPriceMin) query.totalPriceMin = queryForm.totalPriceMin
  if (queryForm.totalPriceMax) query.totalPriceMax = queryForm.totalPriceMax
  if (queryForm.status) query.status = queryForm.status
  router.push({ query }).then(() => fetchProperties())
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.location = ''
  queryForm.floorPlanType = ''
  queryForm.totalPriceMin = undefined
  queryForm.totalPriceMax = undefined
  queryForm.status = '在售'
  router.push({ query: {} }).then(() => fetchProperties())
}

function handlePageChange(page: number) {
  queryForm.page = page
  fetchProperties()
}

function handleSizeChange(size: number) {
  queryForm.pageSize = size
  queryForm.page = 1
  fetchProperties()
}

function handleSortChange({ prop, order }: { prop: string; order: string }) {
  if (prop) {
    queryForm.sortBy = prop
    queryForm.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  }
  fetchProperties()
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

/** 浏览器打印 */
function handlePrint() {
  window.print()
}

/** PDF下载 */
async function handleDownloadPdf() {
  const element = document.querySelector('.result-card') as HTMLElement
  if (!element) return
  ElMessage.info('正在生成PDF...')
  try {
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff'
    })
    const imgData = canvas.toDataURL('image/png')
    const pdf = new jsPDF('p', 'mm', 'a4')
    const imgWidth = 190
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    let heightLeft = imgHeight
    let position = 10

    pdf.setFontSize(16)
    pdf.text('房产查询报表', 105, 10, { align: 'center' })
    pdf.setFontSize(10)
    pdf.text('生成时间：' + new Date().toLocaleString(), 105, 18, { align: 'center' })

    pdf.addImage(imgData, 'PNG', 10, position + 10, imgWidth, imgHeight)
    heightLeft -= pdf.internal.pageSize.getHeight()

    while (heightLeft > 0) {
      position = heightLeft - imgHeight + 10
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 10, position, imgWidth, imgHeight)
      heightLeft -= pdf.internal.pageSize.getHeight()
    }
    pdf.save('房产查询报表_' + new Date().toISOString().slice(0, 10) + '.pdf')
    ElMessage.success('PDF下载成功')
  } catch (e) {
    ElMessage.error('PDF生成失败')
    console.error(e)
  }
}

// 从 URL 查询参数或 localStorage 恢复查询条件
onMounted(() => {
  const query = route.query
  const hasUrlParams = query.keyword || query.location || query.floorPlanType

  if (hasUrlParams) {
    // 优先从 URL 恢复
    if (query.keyword) queryForm.keyword = query.keyword as string
    if (query.location) queryForm.location = query.location as string
    if (query.floorPlanType) queryForm.floorPlanType = query.floorPlanType as string
    if (query.totalPriceMin) queryForm.totalPriceMin = Number(query.totalPriceMin)
    if (query.totalPriceMax) queryForm.totalPriceMax = Number(query.totalPriceMax)
    if (query.status) queryForm.status = query.status as string
  } else {
    // 回退到 localStorage
    const saved = localStorage.getItem('lastQuery')
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        Object.assign(queryForm, parsed)
      } catch { /* ignore */ }
    }
  }
  fetchProperties()
})
</script>

<template>
  <div class="property-search-page">
    <div class="page-header">
      <h2 class="page-title">房产查询</h2>
      <p class="page-desc">多条件搜索符合条件的楼盘</p>
    </div>

    <!-- 搜索条件面板 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" label-width="80px" label-position="right">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="关键字">
              <el-input v-model="queryForm.keyword" placeholder="楼盘名称/位置" clearable :prefix-icon="Search" @clear="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="位置">
              <el-input v-model="queryForm.location" placeholder="输入位置模糊匹配" clearable @clear="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="户型">
              <el-select v-model="queryForm.floorPlanType" clearable style="width: 100%;" @change="handleSearch">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最低总价">
              <el-input-number v-model="queryForm.totalPriceMin" :min="0" :step="100000" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高总价">
              <el-input-number v-model="queryForm.totalPriceMax" :min="0" :step="100000" placeholder="选填" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="queryForm.status" clearable style="width: 100%;" @change="handleSearch">
                <el-option label="不限" value="" />
                <el-option v-for="s in PROPERTY_STATUSES" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <div class="search-actions">
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <span class="result-count">共找到 <strong>{{ total }}</strong> 个符合条件的楼盘</span>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 结果列表 -->
    <el-card shadow="never" class="result-card">
      <div class="result-header">
        <span>查询结果</span>
        <el-button :icon="Printer" text @click="handlePrint">打印报表</el-button>
        <el-button :icon="Download" text type="primary" @click="handleDownloadPdf">下载PDF</el-button>
      </div>

      <el-table
        :data="propertyList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @sort-change="handleSortChange"
        empty-text="没有找到匹配的楼盘"
      >
        <el-table-column prop="propertyName" label="楼盘名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column prop="floorPlanType" label="户型" width="100" align="center" />
        <el-table-column prop="areaSqm" label="面积(㎡)" width="100" align="right" sortable="custom" />
        <el-table-column prop="totalPrice" label="总价" width="130" align="right" sortable="custom">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column prop="vacancyRate" label="空置率" width="90" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.vacancyRate > 15 ? 'danger' : row.vacancyRate > 8 ? 'warning' : 'success'" size="small">
              {{ row.vacancyRate?.toFixed(1) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="decoration" label="装修" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '在售' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 打印报表区域（仅供打印时显示） -->
    <div class="print-only print-report">
      <h2>房产查询报表</h2>
      <p>查询时间：{{ new Date().toLocaleString() }}</p>
      <p>查询条件：位置-{{ queryForm.location || '不限' }}，户型-{{ queryForm.floorPlanType || '不限' }}</p>
      <table>
        <thead>
          <tr>
            <th>楼盘名称</th>
            <th>位置</th>
            <th>户型</th>
            <th>面积</th>
            <th>总价</th>
            <th>空置率</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in propertyList" :key="p.id">
            <td>{{ p.propertyName }}</td>
            <td>{{ p.location }}</td>
            <td>{{ p.floorPlanType }}</td>
            <td>{{ p.areaSqm }}㎡</td>
            <td>¥{{ (p.totalPrice / 10000).toFixed(0) }}万</td>
            <td>{{ p.vacancyRate?.toFixed(1) }}%</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page-header {
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

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-count {
  font-size: 13px;
  color: #6b7280;
  margin-left: 12px;
}

.result-card {
  border-radius: 10px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  margin-bottom: 4px;
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
  border-bottom: 1px solid #f3f4f6;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

.print-only {
  display: none;
}

@media print {
  .search-card, .result-header, .pagination-wrapper, .page-header {
    display: none;
  }
  .print-only.print-report {
    display: block;
  }
  .el-card {
    box-shadow: none !important;
    border: none !important;
  }
}

.print-report {
  padding: 20px;
}

.print-report h2 {
  text-align: center;
  margin-bottom: 16px;
}

.print-report table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.print-report th, .print-report td {
  border: 1px solid #d1d5db;
  padding: 8px 12px;
  text-align: left;
  font-size: 12px;
}

.print-report th {
  background: #f3f4f6;
  font-weight: 600;
}
</style>
