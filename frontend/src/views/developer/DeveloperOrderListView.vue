<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '../../api/order'
import type { Order } from '../../types'
import { ORDER_STATUSES } from '../../types'

const router = useRouter()

const loading = ref(true)
const orderList = ref<Order[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  ...ORDER_STATUSES.map(s => ({ value: s, label: s }))
]

async function fetchOrders() {
  loading.value = true
  try {
    const res = await orderApi.getReceivedOrders({
      status: statusFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    orderList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleStatusFilter(val: string) {
  statusFilter.value = val
  currentPage.value = 1
  fetchOrders()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchOrders()
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    '待支付': 'warning',
    '已支付': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}

onMounted(fetchOrders)
</script>

<template>
  <div class="order-list-page">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
      <p class="page-desc">查看和管理收到的客户订单</p>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">订单状态：</span>
        <el-radio-group :model-value="statusFilter" @change="handleStatusFilter">
          <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <el-table
        :data="orderList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="(row: Order) => router.push(`/developer/orders/${row.id}`)"
        empty-text="暂无订单记录"
      >
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <span style="font-family: monospace; font-size: 13px;">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="propertyName" label="房产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="customerPhone" label="联系电话" width="140" />
        <el-table-column prop="totalPrice" label="总价" width="130" align="right">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170">
          <template #default="{ row }">{{ row.createdTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click.stop="router.push(`/developer/orders/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="() => fetchOrders()"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
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

.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #8a7a6a;
  white-space: nowrap;
}

.list-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
